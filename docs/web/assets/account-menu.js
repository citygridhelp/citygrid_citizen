/**
 * Person-icon account menu (Sign out) — same pattern as the homepage header.
 * Include after cg-auth.js. Markup: .account-menu with #accountMenuBtn + #accountDropdown + #headerSignOut
 * Optional: [data-guest-only] / [data-auth-only] / [data-guest-cta] / [data-auth-cta] like homepage.
 */
(function () {
  "use strict";

  if (!window.CgAuth || !CgAuth.configOk()) return;
  var client = CgAuth.createClient();
  if (!client) return;

  var menuBtn = document.getElementById("accountMenuBtn");
  var dropdown = document.getElementById("accountDropdown");
  var signOutBtn = document.getElementById("headerSignOut");
  var guestEls = document.querySelectorAll("[data-guest-only], [data-guest-cta]");
  var authEls = document.querySelectorAll("[data-auth-only], [data-auth-cta]");

  function homeHref() {
    var a = document.querySelector('a.brand[href]');
    if (a && a.getAttribute("href")) return a.getAttribute("href");
    return "/";
  }

  function setLoggedIn(session) {
    var on = !!session;
    document.body.classList.toggle("is-signed-in", on);
    guestEls.forEach(function (el) {
      el.classList.toggle("is-auth-hidden", on);
    });
    authEls.forEach(function (el) {
      el.classList.toggle("is-auth-hidden", !on);
    });
    if (!on && dropdown) {
      dropdown.hidden = true;
      if (menuBtn) menuBtn.setAttribute("aria-expanded", "false");
    }
  }

  CgAuth.getSession(client).then(setLoggedIn);

  if (menuBtn && dropdown) {
    menuBtn.addEventListener("click", function (e) {
      e.stopPropagation();
      var open = dropdown.hidden;
      dropdown.hidden = !open;
      menuBtn.setAttribute("aria-expanded", open ? "true" : "false");
    });
    document.addEventListener("click", function () {
      dropdown.hidden = true;
      menuBtn.setAttribute("aria-expanded", "false");
    });
    dropdown.addEventListener("click", function (e) {
      e.stopPropagation();
    });
  }

  if (signOutBtn) {
    signOutBtn.addEventListener("click", async function () {
      await CgAuth.signOut(client);
      setLoggedIn(null);
      location.replace(homeHref());
    });
  }
})();
