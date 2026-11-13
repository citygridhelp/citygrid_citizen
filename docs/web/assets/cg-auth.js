/**
 * Shared Supabase auth helpers for the City Grid microsite.
 * Matches the Android app: email+password signup → OTP confirm → password login.
 * Same auth.users / citizen_profiles / reports.reporter_auth_id.
 */
(function (global) {
  "use strict";

  var REPORT_COLS =
    "id,city_key,created_at_ms,area_label,severity,issue_type," +
    "citizen_visible_status,status,citizen_note,latitude,longitude," +
    "photo_path,wide_photo_path,completion_top_path,completion_wide_path";

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
    // Citizen lands on homepage (logged-in chrome); hub is linked from nav.
    return "../index.html";
  }

  var LEVELS = [
    { n: 1, title: "Explorer", cost: 0 },
    { n: 2, title: "Neighbour", cost: 50 },
    { n: 3, title: "Local guide", cost: 120 },
    { n: 4, title: "Grid guide", cost: 280 },
    { n: 5, title: "Champion", cost: 600 },
  ];

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

  async function fetchMyRewards(client) {
    var session = await getSession(client);
    if (!session) {
      return { ok: false, code: "NOT_SIGNED_IN", balance: 0, ledger: [] };
    }
    var uid = session.user.id;
    var balRes = await client
      .from("citizen_reward_balances")
      .select("total_points,reporter_user_id,updated_at")
      .eq("reporter_auth_id", uid)
      .maybeSingle();
    var ledRes = await client
      .from("citizen_reward_ledger")
      .select("event_type,points,place_key,place_name,reference_id,created_at_ms")
      .eq("reporter_auth_id", uid)
      .order("created_at_ms", { ascending: false })
      .limit(40);
    if (balRes.error && ledRes.error) {
      return {
        ok: false,
        code: "FAILED",
        message: balRes.error.message || ledRes.error.message,
        balance: 0,
        ledger: [],
      };
    }
    var ledger = ledRes.data || [];
    var balance =
      balRes.data && typeof balRes.data.total_points === "number"
        ? balRes.data.total_points
        : ledger.reduce(function (s, r) {
            return s + (Number(r.points) || 0);
          }, 0);
    return {
      ok: true,
      balance: balance,
      reporterUserId:
        (balRes.data && balRes.data.reporter_user_id) || "",
      ledger: ledger,
      level: levelFromLedger(ledger),
    };
  }

  async function fetchMyRatings(client) {
    var session = await getSession(client);
    if (!session) return { ok: false, code: "NOT_SIGNED_IN", ratings: [] };
    var uid = session.user.id;
    var { data, error } = await client
      .from("place_ratings")
      .select(
        "place_key,place_name,place_kind,city_key,latitude,longitude,rating,review_text,created_at_ms,updated_at"
      )
      .eq("reporter_auth_id", uid)
      .order("updated_at", { ascending: false })
      .limit(30);
    if (error) {
      return { ok: false, code: "FAILED", message: error.message, ratings: [] };
    }
    return { ok: true, ratings: data || [] };
  }

  async function fetchMyProfile(client) {
    var session = await getSession(client);
    if (!session) return { ok: false, code: "NOT_SIGNED_IN" };
    var uid = session.user.id;
    var { data, error } = await client
      .from("citizen_profiles")
      .select("reporter_user_id,email")
      .eq("auth_id", uid)
      .maybeSingle();
    if (error) return { ok: false, code: "FAILED", message: error.message };
    return {
      ok: true,
      reporterUserId: (data && data.reporter_user_id) || "",
      email: (data && data.email) || (session.user && session.user.email) || "",
    };
  }

  function levelFromLedger(ledger) {
    var max = 1;
    (ledger || []).forEach(function (row) {
      var t = String(row.event_type || "");
      if (t.indexOf("LEVEL_UNLOCK_L") !== 0) return;
      var n = parseInt(t.replace("LEVEL_UNLOCK_L", ""), 10);
      if (!isNaN(n) && n > max) max = n;
    });
    max = Math.max(1, Math.min(5, max));
    var meta = LEVELS[max - 1] || LEVELS[0];
    var next = LEVELS[max] || null;
    return {
      number: max,
      title: meta.title,
      next: next,
      nextCost: next ? next.cost : null,
    };
  }

  function reportStats(reports) {
    var open = 0;
    var progress = 0;
    var done = 0;
    var month = 0;
    var now = Date.now();
    var monthAgo = now - 30 * 24 * 60 * 60 * 1000;
    (reports || []).forEach(function (r) {
      var st = String(r.citizen_visible_status || r.status || "OPEN").toUpperCase();
      if (st.indexOf("COMPLETE") >= 0 || st === "CLOSED" || st === "RESOLVED") done++;
      else if (st.indexOf("PROGRESS") >= 0 || st.indexOf("ASSIGN") >= 0) progress++;
      else open++;
      if (Number(r.created_at_ms) >= monthAgo) month++;
    });
    return {
      total: (reports || []).length,
      open: open,
      progress: progress,
      done: done,
      month: month,
    };
  }

  function eventLabel(type) {
    var t = String(type || "");
    if (t.indexOf("LEVEL_UNLOCK_L") === 0) return "Unlocked " + t.replace("LEVEL_UNLOCK_", "");
    var map = {
      PLACE_RATING_STAR: "Place rating",
      PLACE_RATING_CORE: "Place questionnaire",
      PLACE_RATING_HOURS: "Hours pack",
      PLACE_RATING_DEPTH: "Depth pack",
      PLACE_RATING_NOTES: "Place notes",
      CGC_V1_CONVERT: "CGC convert",
      CGC_SPEND: "Spent CGC",
    };
    return map[t] || t.replace(/_/g, " ").toLowerCase();
  }

  var ISSUE_TYPES = [
    { id: "POTHOLE", label: "Pothole" },
    { id: "MANHOLE", label: "Manhole" },
    { id: "SPEED_BREAKER", label: "Speed breaker" },
    { id: "LIGHT", label: "Street light" },
    { id: "FLOOD", label: "Waterlog / flood" },
    { id: "GARBAGE", label: "Garbage" },
    { id: "RECKLESS", label: "Reckless driving" },
    { id: "INTIMIDATION", label: "Intimidation" },
    { id: "HARASSMENT", label: "Harassment" },
    { id: "STRAY_DOG", label: "Stray dog" },
    { id: "ACCIDENT_PRONE", label: "Accident-prone spot" },
  ];

  var SEVERITIES = ["MINOR", "MODERATE", "SEVERE", "CRITICAL"];

  /** Rough metro centers for city_key guess (same keys as the app). */
  var CITY_CENTERS = [
    { key: "BENGALURU", lat: 12.9716, lon: 77.5946 },
    { key: "MUMBAI", lat: 19.076, lon: 72.8777 },
    { key: "DELHI", lat: 28.6139, lon: 77.209 },
    { key: "HYDERABAD", lat: 17.385, lon: 78.4867 },
    { key: "CHENNAI", lat: 13.0827, lon: 80.2707 },
    { key: "KOLKATA", lat: 22.5726, lon: 88.3639 },
    { key: "PUNE", lat: 18.5204, lon: 73.8567 },
    { key: "AHMEDABAD", lat: 23.0225, lon: 72.5714 },
    { key: "JAIPUR", lat: 26.9124, lon: 75.7873 },
    { key: "LUCKNOW", lat: 26.8467, lon: 80.9462 },
    { key: "KALABURAGI", lat: 17.3297, lon: 76.8343 },
    { key: "MYSORE", lat: 12.2958, lon: 76.6394 },
  ];

  function issueLabel(type) {
    var t = String(type || "POTHOLE").toUpperCase();
    var map = {
      POTHOLE: "Pothole",
      MANHOLE: "Manhole",
      WATERLOG: "Waterlog",
      FLOOD: "Waterlog / flood",
      GARBAGE: "Garbage",
      STREETLIGHT: "Street light",
      LIGHT: "Street light",
      SPEED_BREAKER: "Speed breaker",
      RECKLESS: "Reckless driving",
      INTIMIDATION: "Intimidation",
      HARASSMENT: "Harassment",
      STRAY_DOG: "Stray dog",
      ACCIDENT_PRONE: "Accident-prone spot",
      OTHER: "Other",
    };
    return map[t] || t.replace(/_/g, " ").toLowerCase().replace(/^\w/, function (c) {
      return c.toUpperCase();
    });
  }

  function guessCityKey(lat, lon) {
    var la = Number(lat);
    var lo = Number(lon);
    if (!isFinite(la) || !isFinite(lo)) return "STATE:WEB";
    var best = null;
    var bestM = Infinity;
    CITY_CENTERS.forEach(function (c) {
      var d = haversineM(la, lo, c.lat, c.lon);
      if (d < bestM) {
        bestM = d;
        best = c.key;
      }
    });
    // ~80 km metro radius; outside → STATE:WEB so RLS/public feed still accept
    if (!best || bestM > 80000) return "STATE:WEB";
    return best;
  }

  function allocateParkingId() {
    var hex = "";
    var arr = new Uint8Array(6);
    if (global.crypto && crypto.getRandomValues) crypto.getRandomValues(arr);
    else for (var i = 0; i < 6; i++) arr[i] = (Math.random() * 256) | 0;
    for (var j = 0; j < arr.length; j++) {
      hex += ("0" + arr[j].toString(16)).slice(-2);
    }
    return "pk_" + hex;
  }

  async function uploadEvidence(client, reportId, file, kind) {
    if (!file) return { ok: false, code: "NO_FILE", path: "" };
    var name = kind === "wide" ? "wide.jpg" : "close.jpg";
    var path = String(reportId) + "/" + name;
    var { error } = await client.storage.from("evidence").upload(path, file, {
      upsert: true,
      contentType: file.type || "image/jpeg",
    });
    if (error) return { ok: false, code: "FAILED", message: error.message, path: "" };
    return { ok: true, path: path };
  }

  async function uploadParkingPhoto(client, listingId, file) {
    if (!file) return { ok: true, path: "" };
    var path = String(listingId) + "/site.jpg";
    var { error } = await client.storage.from("parking").upload(path, file, {
      upsert: true,
      contentType: file.type || "image/jpeg",
    });
    if (error) return { ok: false, code: "FAILED", message: error.message, path: "" };
    return { ok: true, path: path };
  }

  async function createReport(client, input) {
    var session = await getSession(client);
    if (!session) return { ok: false, code: "NOT_SIGNED_IN" };
    await ensureCitizenProfile(client);
    var profile = await fetchMyProfile(client);
    var pw = (profile.ok && profile.reporterUserId) || "";
    if (!pw) {
      try {
        var rpc = await client.rpc("citizen_reporter_user_id", { p_desired: "" });
        if (rpc.data) pw = String(rpc.data);
      } catch (_) {}
    }
    var id = Number(input.id) || Date.now();
    var lat = Number(input.latitude);
    var lon = Number(input.longitude);
    var cityKey = String(input.city_key || guessCityKey(lat, lon) || "STATE:WEB");
    var issue = String(input.issue_type || "POTHOLE").toUpperCase();
    var severity = String(input.severity || "MODERATE").toUpperCase();
    if (SEVERITIES.indexOf(severity) < 0) severity = "MODERATE";

    var closePath = "";
    var widePath = "";
    if (input.closeFile) {
      var up = await uploadEvidence(client, id, input.closeFile, "close");
      if (!up.ok) return { ok: false, code: "UPLOAD_FAILED", message: up.message };
      closePath = up.path;
    }
    if (input.wideFile) {
      var upW = await uploadEvidence(client, id, input.wideFile, "wide");
      if (upW.ok) widePath = upW.path;
    }

    var row = {
      id: id,
      city_key: cityKey,
      created_at_ms: id,
      reporter_user_id: pw || "",
      reporter_auth_id: session.user.id,
      photo_path: closePath,
      wide_photo_path: widePath,
      latitude: isFinite(lat) ? lat : null,
      longitude: isFinite(lon) ? lon : null,
      area_label: String(input.area_label || "").trim(),
      severity: severity,
      citizen_note: String(input.citizen_note || "").trim(),
      issue_type: issue,
      status: "OPEN",
      citizen_visible_status: "OPEN",
      assignee_key: "",
      assignee_corp: "",
      assignee_zone: "",
      assignee_name: "",
      assignee_role: "",
      assignee_addr: "",
      field_officer_key: "",
      field_officer_name: "",
      field_officer_role: "",
    };

    var { data, error } = await client.from("reports").insert(row).select(REPORT_COLS).maybeSingle();
    if (error) return { ok: false, code: "FAILED", message: error.message };
    return { ok: true, report: data };
  }

  async function createParkingListing(client, input) {
    var session = await getSession(client);
    if (!session) return { ok: false, code: "NOT_SIGNED_IN" };
    await ensureCitizenProfile(client);
    var profile = await fetchMyProfile(client);
    var pw = (profile.ok && profile.reporterUserId) || "";
    var id = String(input.id || allocateParkingId());
    var now = Date.now();
    var lat = Number(input.latitude);
    var lon = Number(input.longitude);
    if (!isFinite(lat) || !isFinite(lon)) {
      return { ok: false, code: "NO_LOCATION", message: "Location is required." };
    }
    var photoPath = "";
    if (input.photoFile) {
      var up = await uploadParkingPhoto(client, id, input.photoFile);
      if (!up.ok) return { ok: false, code: "UPLOAD_FAILED", message: up.message };
      photoPath = up.path;
    }
    var row = {
      id: id,
      owner_auth_id: session.user.id,
      owner_user_id: pw || "",
      owner_email: (session.user && session.user.email) || "",
      city_key: String(input.city_key || guessCityKey(lat, lon) || "STATE:WEB"),
      title: String(input.title || "").trim() || "Parking",
      address: String(input.address || "").trim(),
      city_state: String(input.city_state || "").trim(),
      latitude: lat,
      longitude: lon,
      capacity_cars: Number(input.capacity_cars) || 0,
      capacity_bikes: Number(input.capacity_bikes) || 0,
      accepted_sizes: input.accepted_sizes || ["small_car", "hatch", "suv", "bike"],
      amenities: input.amenities || [],
      site_photo_path: photoPath,
      hourly_car_inr: Number(input.hourly_car_inr) || 0,
      hourly_bike_inr: Number(input.hourly_bike_inr) || 0,
      day_car_inr: Number(input.day_car_inr) || 0,
      day_bike_inr: Number(input.day_bike_inr) || 0,
      is_open: input.is_open !== false,
      created_at_ms: now,
      updated_at_ms: now,
    };
    var { data, error } = await client
      .from("parking_listings")
      .upsert(row, { onConflict: "id" })
      .select()
      .maybeSingle();
    if (error) return { ok: false, code: "FAILED", message: error.message };
    return { ok: true, listing: data };
  }

  async function fetchCityAlerts(client, cityKey, limit) {
    var since = Date.now() - 14 * 24 * 60 * 60 * 1000;
    var q = client
      .from("reports")
      .select(
        "id,city_key,created_at_ms,area_label,severity,issue_type,citizen_visible_status,latitude,longitude,photo_path"
      )
      .gte("created_at_ms", since)
      .order("created_at_ms", { ascending: false })
      .limit(limit || 12);
    if (cityKey && cityKey !== "STATE:WEB") {
      q = q.eq("city_key", cityKey);
    }
    var { data, error } = await q;
    if (error) return { ok: false, message: error.message, alerts: [] };
    var alerts = (data || []).filter(function (r) {
      var st = String(r.citizen_visible_status || "").toUpperCase();
      if (st.indexOf("COMPLETE") >= 0) return false;
      var sev = String(r.severity || "").toUpperCase();
      var issue = String(r.issue_type || "").toUpperCase();
      return (
        sev === "SEVERE" ||
        sev === "CRITICAL" ||
        issue === "FLOOD" ||
        issue === "ACCIDENT_PRONE"
      );
    });
    return { ok: true, alerts: alerts };
  }

  function computeReportStreak(reports) {
    var days = {};
    (reports || []).forEach(function (r) {
      var ms = Number(r.created_at_ms);
      if (!ms) return;
      var d = new Date(ms);
      var key =
        d.getFullYear() +
        "-" +
        ("0" + (d.getMonth() + 1)).slice(-2) +
        "-" +
        ("0" + d.getDate()).slice(-2);
      days[key] = true;
    });
    var streak = 0;
    var cursor = new Date();
    for (var i = 0; i < 366; i++) {
      var key =
        cursor.getFullYear() +
        "-" +
        ("0" + (cursor.getMonth() + 1)).slice(-2) +
        "-" +
        ("0" + cursor.getDate()).slice(-2);
      if (!days[key]) {
        if (i === 0) {
          // allow missing today — check yesterday start
          cursor.setDate(cursor.getDate() - 1);
          continue;
        }
        break;
      }
      streak++;
      cursor.setDate(cursor.getDate() - 1);
    }
    return streak;
  }

  function weeklyMission(reports, ratings) {
    var now = Date.now();
    var weekAgo = now - 7 * 24 * 60 * 60 * 1000;
    var reportN = (reports || []).filter(function (r) {
      return Number(r.created_at_ms) >= weekAgo;
    }).length;
    var rateN = (ratings || []).filter(function (r) {
      var t = r.updated_at ? Date.parse(r.updated_at) : Number(r.created_at_ms);
      return t && t >= weekAgo;
    }).length;
    var reportGoal = 3;
    var rateGoal = 2;
    var reportDone = Math.min(reportN, reportGoal);
    var rateDone = Math.min(rateN, rateGoal);
    var complete = reportDone >= reportGoal || rateDone >= rateGoal;
    return {
      title: complete
        ? "Weekly mission complete"
        : "This week: file 3 reports or rate 2 stops",
      reportDone: reportDone,
      reportGoal: reportGoal,
      rateDone: rateDone,
      rateGoal: rateGoal,
      complete: complete,
      reportN: reportN,
      rateN: rateN,
    };
  }

  function hostBadge(listings) {
    var lots = listings || [];
    if (!lots.length) {
      return { tier: "none", title: "List a bay to start", next: "Add a lot with photo + pin" };
    }
    var withPhoto = lots.filter(function (l) {
      return !!(l.site_photo_path && String(l.site_photo_path).trim());
    }).length;
    var priced = lots.filter(function (l) {
      return (Number(l.hourly_car_inr) || 0) > 0 || (Number(l.day_car_inr) || 0) > 0;
    }).length;
    if (withPhoto && priced && lots.length >= 2) {
      return { tier: "gold", title: "Gold host path", next: "Keep vacancy fresh for Super-host" };
    }
    if (withPhoto && priced) {
      return { tier: "silver", title: "Silver host", next: "List a 2nd bay for Gold" };
    }
    return { tier: "bronze", title: "Bronze host", next: "Add site photo + hourly price" };
  }

  function civicBriefText(stats, ratingsLen, cityKey, streak) {
    return (
      "City Grid civic brief" +
      (cityKey ? " · " + cityKey : "") +
      "\n" +
      "Reports on my login: " +
      (stats.total || 0) +
      " (open " +
      ((stats.open || 0) + (stats.progress || 0)) +
      ", done " +
      (stats.done || 0) +
      ")\n" +
      "Last 30 days: " +
      (stats.month || 0) +
      " · Places rated: " +
      (ratingsLen || 0) +
      " · Report streak: " +
      (streak || 0) +
      " day(s)\n" +
      "Filed with photo + GPS — independent citizen grid, not a municipal portal."
    );
  }

  function inviteUrl(reporterUserId) {
    var base = "../index.html";
    try {
      var path = global.location.pathname || "";
      var m = path.match(/^(.*?\/(?:web|docs\/web)\/)/);
      if (m) {
        base = global.location.origin + m[1] + "index.html";
      } else {
        base = new URL("../index.html", global.location.href).href;
      }
    } catch (_) {}
    if (reporterUserId) {
      return base + (base.indexOf("?") >= 0 ? "&" : "?") + "ref=" + encodeURIComponent(reporterUserId);
    }
    return base;
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

  /** Public evidence object URL (bucket public after migration 0008). Paths like `123/close.jpg`. */
  function evidenceUrl(objectPath) {
    if (!configOk() || !objectPath) return "";
    var path = String(objectPath).trim().replace(/^\/+/, "");
    if (!path) return "";
    if (path.indexOf("http://") === 0 || path.indexOf("https://") === 0) return path;
    // Skip device filesystem paths that sometimes leak into local-only rows
    if (path.indexOf(":\\") >= 0 || path.indexOf("/data/") === 0 || path.indexOf("file:") === 0) {
      return "";
    }
    var base = String(global.CG_SUPABASE.url).replace(/\/$/, "");
    var encoded = path
      .split("/")
      .map(function (seg) {
        return encodeURIComponent(seg);
      })
      .join("/");
    return base + "/storage/v1/object/public/evidence/" + encoded;
  }

  function proofsUrl(objectPath) {
    if (!configOk() || !objectPath) return "";
    var path = String(objectPath).trim().replace(/^\/+/, "");
    if (!path) return "";
    if (path.indexOf("http://") === 0 || path.indexOf("https://") === 0) return path;
    if (path.indexOf(":\\") >= 0) return "";
    var base = String(global.CG_SUPABASE.url).replace(/\/$/, "");
    var encoded = path
      .split("/")
      .map(function (seg) {
        return encodeURIComponent(seg);
      })
      .join("/");
    return base + "/storage/v1/object/public/proofs/" + encoded;
  }

  function haversineM(lat1, lon1, lat2, lon2) {
    var R = 6371000;
    var toRad = Math.PI / 180;
    var dLat = (lat2 - lat1) * toRad;
    var dLon = (lon2 - lon1) * toRad;
    var a =
      Math.sin(dLat / 2) * Math.sin(dLat / 2) +
      Math.cos(lat1 * toRad) *
        Math.cos(lat2 * toRad) *
        Math.sin(dLon / 2) *
        Math.sin(dLon / 2);
    return 2 * R * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
  }

  /** OSM static preview for a lat/lon (place ratings have no photo column yet). */
  function staticMapUrl(lat, lon, w, h, zoom) {
    var la = Number(lat);
    var lo = Number(lon);
    if (!isFinite(la) || !isFinite(lo)) return "";
    var width = w || 176;
    var height = h || 132;
    var z = zoom || 16;
    return (
      "https://staticmap.openstreetmap.de/staticmap.php?center=" +
      encodeURIComponent(la + "," + lo) +
      "&zoom=" +
      z +
      "&size=" +
      width +
      "x" +
      height +
      "&maptype=mapnik&markers=" +
      encodeURIComponent(la + "," + lo + ",lightblue1")
    );
  }

  function parkingPhotoUrl(objectPath) {
    if (!configOk() || !objectPath) return "";
    var path = String(objectPath).trim().replace(/^\/+/, "");
    if (!path) return "";
    if (path.indexOf("http://") === 0 || path.indexOf("https://") === 0) return path;
    if (path.indexOf(":\\") >= 0) return "";
    var base = String(global.CG_SUPABASE.url).replace(/\/$/, "");
    var encoded = path
      .split("/")
      .map(function (seg) {
        return encodeURIComponent(seg);
      })
      .join("/");
    return base + "/storage/v1/object/public/parking/" + encoded;
  }

  async function fetchMyParking(client) {
    var session = await getSession(client);
    if (!session) return { ok: false, code: "NOT_SIGNED_IN", listings: [] };
    var uid = session.user.id;
    var { data, error } = await client
      .from("parking_listings")
      .select(
        "id,title,address,city_state,city_key,latitude,longitude,capacity_cars,capacity_bikes," +
          "hourly_car_inr,hourly_bike_inr,day_car_inr,day_bike_inr,is_open,site_photo_path," +
          "updated_at_ms,created_at_ms,accepted_sizes,amenities"
      )
      .eq("owner_auth_id", uid)
      .order("updated_at_ms", { ascending: false });
    if (error) {
      return { ok: false, code: "FAILED", message: error.message, listings: [] };
    }
    return { ok: true, listings: data || [] };
  }

  async function updateParkingListing(client, id, patch) {
    var session = await getSession(client);
    if (!session) return { ok: false, code: "NOT_SIGNED_IN" };
    var { data, error } = await client
      .from("parking_listings")
      .update(patch)
      .eq("id", id)
      .eq("owner_auth_id", session.user.id)
      .select()
      .maybeSingle();
    if (error) return { ok: false, code: "FAILED", message: error.message };
    return { ok: true, listing: data };
  }

  async function deleteParkingListing(client, id) {
    var session = await getSession(client);
    if (!session) return { ok: false, code: "NOT_SIGNED_IN" };
    var { error } = await client
      .from("parking_listings")
      .delete()
      .eq("id", id)
      .eq("owner_auth_id", session.user.id);
    if (error) return { ok: false, code: "FAILED", message: error.message };
    return { ok: true };
  }

  function reportsNear(reports, lat, lon, radiusM, cityKey) {
    var list = reports || [];
    var nearby = list.filter(function (r) {
      var rLat = Number(r.latitude);
      var rLon = Number(r.longitude);
      if (isFinite(rLat) && isFinite(rLon) && isFinite(lat) && isFinite(lon)) {
        return haversineM(lat, lon, rLat, rLon) <= (radiusM || 3500);
      }
      if (cityKey && r.city_key && String(r.city_key) === String(cityKey)) return true;
      return false;
    });
    nearby.sort(function (a, b) {
      return (Number(b.created_at_ms) || 0) - (Number(a.created_at_ms) || 0);
    });
    return nearby;
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
    fetchMyRewards: fetchMyRewards,
    fetchMyRatings: fetchMyRatings,
    fetchMyProfile: fetchMyProfile,
    levelFromLedger: levelFromLedger,
    reportStats: reportStats,
    eventLabel: eventLabel,
    issueLabel: issueLabel,
    statusLabel: statusLabel,
    formatWhen: formatWhen,
    evidenceUrl: evidenceUrl,
    proofsUrl: proofsUrl,
    staticMapUrl: staticMapUrl,
    parkingPhotoUrl: parkingPhotoUrl,
    fetchMyParking: fetchMyParking,
    updateParkingListing: updateParkingListing,
    deleteParkingListing: deleteParkingListing,
    createReport: createReport,
    createParkingListing: createParkingListing,
    uploadEvidence: uploadEvidence,
    uploadParkingPhoto: uploadParkingPhoto,
    allocateParkingId: allocateParkingId,
    guessCityKey: guessCityKey,
    fetchCityAlerts: fetchCityAlerts,
    computeReportStreak: computeReportStreak,
    weeklyMission: weeklyMission,
    hostBadge: hostBadge,
    civicBriefText: civicBriefText,
    inviteUrl: inviteUrl,
    ISSUE_TYPES: ISSUE_TYPES,
    SEVERITIES: SEVERITIES,
    CITY_CENTERS: CITY_CENTERS,
    haversineM: haversineM,
    reportsNear: reportsNear,
  };
})(typeof window !== "undefined" ? window : globalThis);
