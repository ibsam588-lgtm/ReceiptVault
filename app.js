const screens = Array.from(document.querySelectorAll(".screen"));
const navButtons = Array.from(document.querySelectorAll("[data-go]"));
const tabs = Array.from(document.querySelectorAll(".tab"));

function showScreen(name) {
  screens.forEach((screen) => {
    screen.classList.toggle("active", screen.dataset.screen === name);
  });

  tabs.forEach((tab) => {
    tab.classList.toggle("active", tab.dataset.go === name);
  });
}

navButtons.forEach((button) => {
  button.addEventListener("click", () => showScreen(button.dataset.go));
});

document.querySelectorAll(".result-card[data-go]").forEach((card) => {
  card.addEventListener("click", () => showScreen(card.dataset.go));
});

if (window.lucide) {
  window.lucide.createIcons();
}
