/** ==== Custom Event: 가상키패드 사라짐 감지하기 ==== */
const destroyedEvent = new Event("destroyed");
const observer = new MutationObserver(function (mutations) {
  mutations.forEach(function (mutation) {
    mutation.removedNodes.forEach(function (node) {
      node.dispatchEvent(destroyedEvent);
    });
  });
});

observer.observe(document.documentElement, {
  childList: true,
  subtree: true,
});
/** ==== Custom Event: 가상키패드 사라짐 감지하기 ==== */