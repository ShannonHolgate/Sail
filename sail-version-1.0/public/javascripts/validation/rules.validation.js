$.validator.addMethod('integer', function(value, element, param) {
      return (value != 0) && (value == parseInt(value, 10));
  }, 
  validationM.invalidInteger);  

$.validator.addMethod('currency', function(value, element, param) {
    var newValue = value.replace(/\u00A3/g, '');
    newValue = newValue.replace(/,/g, ''); 
    return !isNaN(parseFloat(newValue)) && isFinite(newValue);
  }, 
  validationM.invalidCurrency);  

$.validator.addMethod("passwordMatch",
  function(value, element) {
    return $('#password').val() == value
  },
  validationM.passwordMustMatch);