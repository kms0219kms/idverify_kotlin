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
