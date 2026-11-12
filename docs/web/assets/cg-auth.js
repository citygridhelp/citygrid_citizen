/**
 * Shared Supabase auth helpers for the City Grid microsite.
 * Matches the Android app: email+password signup → OTP confirm → password login.
 * Same auth.users / citizen_profiles / reports.reporter_auth_id.
 */
(function (global) {
  "use strict";

  var REPORT_COLS =
    "id,city_key,created_at_ms,area_label,severity,issue_type," +
    "citizen_visible_status,status,citizen_note,latitude,longitude";

  function configOk() {
    var c = global.CG_SUPABASE;
    return !!(c && c.url && c.anonKey && !String(c.url).includes("YOUR_PROJECT"));
  }

  function createClient() {
    if (!configOk()) return null;
    if (!global.supabase || typeof global.supabase.createClient !== "function") {
      return null;
    }
    var c = global.CG_SUPABASE;
    return global.supabase.createClient(c.url, c.anonKey, {
      auth: {
        persistSession: true,
        autoRefreshToken: true,
        detectSessionInUrl: true,
        storageKey: "cg-web-auth",
      },
    });
  }

  function normalizeEmail(email) {
    return String(email || "")
      .trim()
      .toLowerCase();
  }

  function roleFromQuery() {
    try {
      var q = new URLSearchParams(global.location.search);
      var r = (q.get("role") || "citizen").toLowerCase();
      return r === "commercial" ? "commercial" : "citizen";
    } catch (_) {
      return "citizen";
    }
  }

  function destinationForRole(role) {
    if (role === "commercial") return "../commercial/index.html";
    return "../citizen/reports/index.html";
  }

  async function emailExists(client, email) {
    try {
      var { data, error } = await client.rpc("email_exists", {
        p_email: normalizeEmail(email),
      });
      if (error) return null;
      return !!data;
    } catch (_) {
      return null;
    }
  }

  async function ensureCitizenProfile(client) {
    try {
      await client.rpc("citizen_reporter_user_id", { p_desired: "" });
    } catch (_) {
      /* best-effort — reports RLS still uses auth.uid() */
    }
  }

  async function getSession(client) {
    var { data } = await client.auth.getSession();
    return data && data.session ? data.session : null;
  }

  async function startSignup(client, name, email, password) {
    var normalized = normalizeEmail(email);
    var cleanName = String(name || "").trim();
    var exists = await emailExists(client, normalized);
    if (exists === true) {
      return resendSignup(client, normalized);
    }
    var { error } = await client.auth.signUp({
      email: normalized,
      password: password,
      options: cleanName
        ? { data: { full_name: cleanName } }
        : undefined,
    });
    if (error) {
      var msg = (error.message || "").toLowerCase();
      if (
        msg.includes("already") ||
        msg.includes("registered") ||
        msg.includes("exists")
      ) {
        return { ok: false, code: "EMAIL_ALREADY_REGISTERED" };
      }
      return { ok: false, code: "FAILED", message: error.message };
    }
    return { ok: true, code: "CODE_SENT" };
  }

  async function resendSignup(client, email) {
    var normalized = normalizeEmail(email);
    var { error } = await client.auth.resend({
      type: "signup",
      email: normalized,
    });
    if (error) {
      var msg = (error.message || "").toLowerCase();
      if (
        msg.includes("already") ||
        msg.includes("registered") ||
        msg.includes("confirmed") ||
        msg.includes("exists")
      ) {
        return { ok: false, code: "EMAIL_ALREADY_REGISTERED" };
      }
      if ((await emailExists(client, normalized)) === true) {
        return { ok: false, code: "EMAIL_ALREADY_REGISTERED" };
      }
      return { ok: false, code: "FAILED", message: error.message };
    }
    return { ok: true, code: "CODE_SENT" };
  }

  async function verifySignup(client, email, token) {
    var normalized = normalizeEmail(email);
    var clean = String(token || "").trim();
    var lastMsg = "";
    for (var i = 0; i < 2; i++) {
      var type = i === 0 ? "email" : "signup";
      var { error } = await client.auth.verifyOtp({
        email: normalized,
        token: clean,
        type: type,
      });
      if (!error) {
        await ensureCitizenProfile(client);
        return { ok: true, code: "SUCCESS" };
      }
      lastMsg = (error.message || "").toLowerCase();
    }
    if (lastMsg.includes("expired")) return { ok: false, code: "EXPIRED" };
    if (lastMsg.includes("invalid") || lastMsg.includes("token")) {
      return { ok: false, code: "INVALID_CODE" };
    }
    return { ok: false, code: "FAILED", message: lastMsg };
  }

  async function login(client, email, password) {
    var normalized = normalizeEmail(email);
    var { error } = await client.auth.signInWithPassword({
      email: normalized,
      password: password,
    });
    if (!error) {
      await ensureCitizenProfile(client);
      return { ok: true, code: "SUCCESS" };
    }
    var msg = (error.message || "").toLowerCase();
    if (msg.includes("not confirmed") || msg.includes("not verified")) {
      return { ok: false, code: "EMAIL_NOT_VERIFIED" };
    }
    if ((await emailExists(client, normalized)) === false) {
      return { ok: false, code: "NO_ACCOUNT" };
    }
    return { ok: false, code: "WRONG_PASSWORD" };
  }

  async function signOut(client) {
    await client.auth.signOut();
  }

  async function fetchMyReports(client) {
    var session = await getSession(client);
    if (!session) return { ok: false, code: "NOT_SIGNED_IN", reports: [] };
    var uid = session.user.id;
    var { data, error } = await client
      .from("reports")
      .select(REPORT_COLS)
      .eq("reporter_auth_id", uid)
      .order("created_at_ms", { ascending: false });
    if (error) {
      return { ok: false, code: "FAILED", message: error.message, reports: [] };
    }
    return { ok: true, reports: data || [] };
  }

  function issueLabel(type) {
    var t = String(type || "POTHOLE").toUpperCase();
    var map = {
      POTHOLE: "Pothole",
      MANHOLE: "Manhole",
      WATERLOG: "Waterlog",
      GARBAGE: "Garbage",
      STREETLIGHT: "Street light",
      SPEED_BREAKER: "Speed breaker",
      OTHER: "Other",
    };
    return map[t] || t.replace(/_/g, " ").toLowerCase().replace(/^\w/, function (c) {
      return c.toUpperCase();
    });
  }

  function statusLabel(row) {
    return String(row.citizen_visible_status || row.status || "OPEN")
      .replace(/_/g, " ");
  }

  function formatWhen(ms) {
    var n = Number(ms);
    if (!n) return "—";
    try {
      return new Date(n).toLocaleString(undefined, {
        dateStyle: "medium",
        timeStyle: "short",
      });
    } catch (_) {
      return String(ms);
    }
  }

  global.CgAuth = {
    configOk: configOk,
    createClient: createClient,
    normalizeEmail: normalizeEmail,
    roleFromQuery: roleFromQuery,
    destinationForRole: destinationForRole,
    emailExists: emailExists,
    ensureCitizenProfile: ensureCitizenProfile,
    getSession: getSession,
    startSignup: startSignup,
    resendSignup: resendSignup,
    verifySignup: verifySignup,
    login: login,
    signOut: signOut,
    fetchMyReports: fetchMyReports,
    issueLabel: issueLabel,
    statusLabel: statusLabel,
    formatWhen: formatWhen,
  };
})(typeof window !== "undefined" ? window : globalThis);
