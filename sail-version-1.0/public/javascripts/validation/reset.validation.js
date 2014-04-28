$(function() {
  /**
  * Validate the reset password form
  **/
  $( "#reset-form" ).validate({
    rules: {
      email: {
        required: true,
        email: true
      },
      password: {
        required: true,
        minlength: 8
      },
      confirm: {
        required: true,
        minlength: 8,
        passwordMatch: true
      } 
    },
    messages: {
      email: {
        required: validationM.emailRequired,
        email: validationM.emailInvalid
      },
      password: {
        required: validationM.passwordRequired,
        minlength: $.format(validationM.passwordMinLength)
      },
      confirm: {
        required: validationM.passwordRequired,
        minlength: $.format(validationM.passwordMinLength)
      } 
    }
  });

  /**
  * Add a new validation method to ensure both passwords match
  **/
  $.validator.addMethod("passwordMatch",
    function(value, element) {
      return $('#password').val() == value
    },
    validationM.passwordMustMatch
  );
});