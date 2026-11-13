(function () {
  function fallbackFrom(el) {
    var f = el && el.getAttribute("data-close-fallback");
    if (f) return f;
    return "../index.html";
  }

  function goBack(fallback) {
    var dest = fallback || "../index.html";
    try {
      var ref = document.referrer || "";
      if (ref) {
        var u = new URL(ref);
        // Only use history.back when we clearly came from another page on this site.
        if (u.origin === location.origin && u.href.split("#")[0] !== location.href.split("#")[0]) {
          window.history.back();
          return;
        }
      }
    } catch (_) {}
    // No usable referrer → go to the page’s fallback (My hub for reports).
    window.location.href = dest;
  }

  document.querySelectorAll("[data-close-card]").forEach(function (el) {
    el.addEventListener("click", function (e) {
      e.preventDefault();
      goBack(fallbackFrom(el));
    });
  });
})();
