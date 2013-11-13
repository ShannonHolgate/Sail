$(function() {
  /**
  * Validate the register form
  **/
  $( "#register-form" ).validate({
    rules: {
      email: {
        required: true,
        email: true
      },
      name: {
        required: true,
        minlength: 3
      },
      password: {
        required: true,
        minlength: 8
      } 
    },
    messages: {
      email: {
        required: validationM.emailrequired,
        email: validationM.emailInvalid
      },
      name: {
        required: validationM.nameRequired,
        minlength:$.format(validationM.nameMinLength)
      },
      password: {
        required: validationM.passwordRequired,
        minlength: $.format(validationM.passwordMinLength)
      } 
    }
  });
});