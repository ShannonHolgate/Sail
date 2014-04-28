$(function () {
  /**
   * Validate the login form
   **/
  $("#login-form").validate({
    rules: {
      email: {
        required: true,
        email: true
      },
      password: {
        required: true,
        minlength: 8
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
      }
    }
  });

  /**
   * Validate the reset request form
   **/
  $("#reset-request-form").validate({
    rules: {
      email: {
        required: true,
        email: true
      }
    },
    messages: {
      email: {
        required: validationM.emailRequired,
        email: validationM.emailInvalid
      }
    }
  });
});