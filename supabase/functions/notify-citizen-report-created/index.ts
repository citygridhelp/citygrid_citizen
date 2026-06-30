/**
 * Sends a confirmation email when a signed-in citizen report is inserted.
 * Trigger: Database Webhook on public.reports INSERT.
 *
 * Secrets (supabase secrets set ...):
 *   BREVO_API_KEY, MAIL_FROM, MAIL_FROM_NAME (optional)
 *   WEBHOOK_SECRET (optional) — must match webhook HTTP header x-webhook-secret
 *
 * Auto: SUPABASE_URL, SUPABASE_SERVICE_ROLE_KEY
 */
import "jsr:@supabase/functions-js/edge-runtime.d.ts";
import { createClient } from "npm:@supabase/supabase-js@2";

type ReportRow = {
  id: number;
  city_key: string;
  created_at_ms: number;
  reporter_user_id: string;
  reporter_auth_id: string | null;
  latitude: number | null;
  longitude: number | null;
  area_label: string;
  severity: string;
  citizen_note: string;
};

type WebhookPayload = {
  type?: string;
  table?: string;
  schema?: string;
  record?: ReportRow;
};

function formatTicket(reportId: number): string {
  const digits = String(reportId).padStart(8, "0");
  return `CG-${digits.slice(-8)}`;
}

function formatCityLabel(cityKey: string): string {
  return cityKey
    .trim()
    .split(/[\s_]+/)
    .filter(Boolean)
    .map((w) => w.charAt(0).toUpperCase() + w.slice(1).toLowerCase())
    .join(" ");
}

function formatIst(ms: number): string {
  return new Intl.DateTimeFormat("en-IN", {
    timeZone: "Asia/Kolkata",
    day: "numeric",
    month: "short",
    year: "numeric",
    hour: "2-digit",
    minute: "2-digit",
    hour12: false,
  }).format(new Date(ms)) + " IST";
}

function formatLocation(row: ReportRow): string {
  const parts: string[] = [];
  const area = row.area_label?.trim();
  if (area) parts.push(area);
  parts.push(formatCityLabel(row.city_key || ""));
  let text = parts.filter(Boolean).join(", ");
  if (row.latitude != null && row.longitude != null) {
    text += ` (${row.latitude.toFixed(5)}, ${row.longitude.toFixed(5)})`;
  }
  return text || "—";
}

function buildEmailBody(row: ReportRow, ticket: string): string {
  const userId = row.reporter_user_id?.trim() || "—";
  const note = row.citizen_note?.trim() || "—";
  const severity = row.severity?.trim() || "MODERATE";
  return [
    "City Grid — Report received",
    "",
    `Ticket:     ${ticket}`,
    `User ID:    ${userId}`,
    `Location:   ${formatLocation(row)}`,
    `Severity:   ${severity}`,
    `Note:       ${note}`,
    `Submitted:  ${formatIst(row.created_at_ms)}`,
    "",
    "You can track this report in the app under My Reports.",
  ].join("\n");
}

async function sendBrevoEmail(
  to: string,
  subject: string,
  text: string,
): Promise<void> {
  const apiKey = Deno.env.get("BREVO_API_KEY");
  const from = Deno.env.get("MAIL_FROM");
  const fromName = Deno.env.get("MAIL_FROM_NAME") ?? "City Grid";
  if (!apiKey || !from) {
    throw new Error("BREVO_API_KEY and MAIL_FROM must be set");
  }
  const res = await fetch("https://api.brevo.com/v3/smtp/email", {
    method: "POST",
    headers: {
      "api-key": apiKey,
      "Content-Type": "application/json",
      Accept: "application/json",
    },
    body: JSON.stringify({
      sender: { name: fromName, email: from },
      to: [{ email: to }],
      subject,
      textContent: text,
    }),
  });
  if (!res.ok) {
    const detail = await res.text();
    throw new Error(`Brevo API ${res.status}: ${detail}`);
  }
}

Deno.serve(async (req) => {
  if (req.method !== "POST") {
    return new Response("Method not allowed", { status: 405 });
  }

  const webhookSecret = Deno.env.get("WEBHOOK_SECRET");
  if (webhookSecret) {
    const header = req.headers.get("x-webhook-secret");
    if (header !== webhookSecret) {
      return new Response("Unauthorized", { status: 401 });
    }
  }

  let payload: WebhookPayload;
  try {
    payload = await req.json();
  } catch {
    return new Response("Invalid JSON", { status: 400 });
  }

  if (payload.type !== "INSERT" || payload.table !== "reports") {
    return new Response(JSON.stringify({ skipped: "not a reports INSERT" }), {
      status: 200,
      headers: { "Content-Type": "application/json" },
    });
  }

  const row = payload.record;
  if (!row?.id) {
    return new Response("Missing record", { status: 400 });
  }

  if (!row.reporter_auth_id) {
    return new Response(JSON.stringify({ skipped: "guest report" }), {
      status: 200,
      headers: { "Content-Type": "application/json" },
    });
  }

  const supabaseUrl = Deno.env.get("SUPABASE_URL")!;
  const serviceKey = Deno.env.get("SUPABASE_SERVICE_ROLE_KEY")!;
  const admin = createClient(supabaseUrl, serviceKey, {
    auth: { persistSession: false, autoRefreshToken: false },
  });

  const ticket = formatTicket(row.id);

  const { error: claimError } = await admin.from("report_email_log").insert({
    report_id: row.id,
    recipient: "pending",
    ticket,
  });

  if (claimError) {
    const code = (claimError as { code?: string }).code;
    if (code === "23505") {
      return new Response(JSON.stringify({ skipped: "already sent", report_id: row.id }), {
        status: 200,
        headers: { "Content-Type": "application/json" },
      });
    }
    console.error("report_email_log claim failed", claimError);
    return new Response(JSON.stringify({ error: claimError.message }), {
      status: 500,
      headers: { "Content-Type": "application/json" },
    });
  }

  const { data: userData, error: userError } = await admin.auth.admin.getUserById(
    row.reporter_auth_id,
  );
  if (userError || !userData?.user?.email) {
    await admin.from("report_email_log").delete().eq("report_id", row.id);
    console.error("auth lookup failed", userError);
    return new Response(JSON.stringify({ error: "reporter email not found" }), {
      status: 422,
      headers: { "Content-Type": "application/json" },
    });
  }

  const recipient = userData.user.email.trim().toLowerCase();
  const subject = `City Grid — Report ${ticket} received`;
  const body = buildEmailBody(row, ticket);

  try {
    await sendBrevoEmail(recipient, subject, body);
  } catch (e) {
    await admin.from("report_email_log").delete().eq("report_id", row.id);
    console.error("Brevo send failed", e);
    return new Response(JSON.stringify({ error: String(e) }), {
      status: 502,
      headers: { "Content-Type": "application/json" },
    });
  }

  const { error: logError } = await admin
    .from("report_email_log")
    .update({ recipient })
    .eq("report_id", row.id);

  if (logError) {
    console.error("report_email_log update failed", logError);
  }

  return new Response(
    JSON.stringify({ ok: true, report_id: row.id, ticket, recipient }),
    { status: 200, headers: { "Content-Type": "application/json" } },
  );
});
