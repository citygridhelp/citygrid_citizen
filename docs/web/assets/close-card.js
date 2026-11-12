(function () {
  function goBack() {
    if (window.history.length > 1) {
      window.history.back();
      return;
    }
    window.location.href = "../index.html";
  }

  document.querySelectorAll("[data-close-card]").forEach(function (el) {
    el.addEventListener("click", function (e) {
      e.preventDefault();
      goBack();
    });
  });
})();
