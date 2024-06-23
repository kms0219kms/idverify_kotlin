const rsa = new RSAKey();

$(document).ready(async function () {
  KioskBoard.run('.virtual-input', {
    keysArrayOfObjects: null,
    keysJsonUrl: '/js/vendors/kioskboard-keys-english.json',

    theme: 'flat',
    cssAnimations: false,
    allowRealKeyboard: true,
    allowMobileKeyboard: true,
  });

  const getPublicKey = await fetch("/api/bridge/getRsaPublicKey.do");
  const [modulus, exponent] = (await getPublicKey.text()).split(",");

  rsa.setPublic(modulus, exponent);
});

$(".virtual-input").each(function () {
  $(this).click(function (e) {
    e.target.dataset.value = e.target.value.replaceAll(" ", "");

    $('#KioskBoard-VirtualKeyboard').bind('destroyed', function () {
      $(".virtual-input").each(function () {
        if ($(this).attr("data-value").replaceAll(" ", "") === "") {
          $(this).attr("data-value", " ")
        }
      })
    })
  });
});

$(".virtual-input").each(function () {
  $(this).change(function (e) {
    e.target.dataset.value = e.target.value.replaceAll(" ", "");
  });
});

$("#chk_all").change(function () {
  const isChecked = $(this).is(":checked");

  $("input[type='checkbox']").prop("checked", isChecked);
  $("#submit").prop("disabled", !isChecked);
});

$(".agree_area ul input[type='checkbox']").change(function () {
  const isAllChecked = $(".agree_area ul input[type='checkbox']").length ===
      $(".agree_area ul input[type='checkbox']:checked").length;

  $("#chk_all").prop("checked", isAllChecked);
  $("#submit").prop("disabled", !isAllChecked);
});
