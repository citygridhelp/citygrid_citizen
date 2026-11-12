/**
 * Paste your live profile/page URLs below. Empty = icon hidden.
 * Example: linkedin: "https://www.linkedin.com/in/your-handle"
 */
window.CG_SOCIAL = window.CG_SOCIAL || {
  linkedin: "",
  x: "",
  youtube: "",
  instagram: ""
};

(function () {
  var map = window.CG_SOCIAL || {};
  var root = document.querySelector("[data-social-row]");
  if (!root) return;
  var any = false;
  root.querySelectorAll("[data-social]").forEach(function (a) {
    var key = a.getAttribute("data-social");
    var url = (map[key] || "").trim();
    if (!url) {
      a.hidden = true;
      return;
    }
    a.href = url;
    a.hidden = false;
    any = true;
  });
  root.hidden = !any;
})();
