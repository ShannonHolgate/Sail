$(function () {

  $.validator.addMethod('integer', function(value, element, param) {
      return (value != 0) && (value == parseInt(value, 10));
  }, validationM.invalidInteger);  

  $.validator.addMethod('currency', function(value, element, param) {
    var newValue = value.replace(/\u00A3/g, '');
    newValue = newValue.replace(/,/g, ''); 
    return !isNaN(parseFloat(newValue)) && isFinite(newValue);
  }, validationM.invalidCurrency);  

  /**
   * Validate the automatic investment form
   **/
  $("#add-auto-form").validate({
    rules: {
      quantity: {
        required: true,
        integer: true
      }
    },
    messages: {
      quantity: {
        required: validationM.quantityRequired
      }
    },
    errorPlacement: function(error, element) {
       error.insertAfter(element.parent());  
    }
  });

  /**
   * Validate the manual investment addition form
   **/
  $("#add-manual-form").validate({
    rules: {
      name: {
        required: true,
        minlength: 3,
        maxlength: 30
      },
      currentvalue: {
        required: true,
        currency: true  
      }
    },
    messages: {
      name: {
        required: validationM.nameRequired,
        minlength: $.format(validationM.nameMinLength),
        maxlength: $.format(validationM.nameMaxLength)
      },
      currentvalue: {
        required: validationM.valueRequired 
      }
    },
    errorPlacement: function(error, element) {
       error.insertAfter(element.parent());  
    }
  });

  /**
   * Validate the investment removal form
   **/
  $("#remove-inv-form").validate({
    rules: {
      value: {
        required: true,
        currency: true  
      },
      password: {
        required: true,
        minlength: 8
      }
    },
    messages: {
      value: {
        required: validationM.valueRequired 
      },
      password: {
        required: validationM.passwordRequired,
        minlength: $.format(validationM.passwordMinLength)
      }
    },
    errorPlacement: function(error, element) {
       error.insertAfter("#password");  
    }
  });
});