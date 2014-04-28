$(function () {
  /**
   * Validate the change name form
   **/
  $("#name-change-form").validate({
    rules: {
      email: {
        required: true,
        email: true
      },
      newname: {
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
        required: validationM.emailRequired,
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

  /**
   * Validate the change email form
   **/
  $("#email-change-form").validate({
    rules: {
      password: {
        required: true,
        minlength: 8
      },
      email: {
        required: true,
        email: true
      },
      confirm: {
        required: true,
        email: true,
        emailMatch: true
      }
    },
    messages: {
      password: {
        required: validationM.passwordRequired,
        minlength: $.format(validationM.passwordMinLength)
      },
      email: {
        required: validationM.emailRequired,
        email: validationM.emailInvalid
      },
      confirm: {
        required: validationM.emailRequired,
        email: validationM.emailInvalid
      }
    }
  });

  /**
  * Add a new validation method to ensure both emails match
  **/
  $.validator.addMethod("emailMatch",
    function(value, element) {
      return $('#email').val() == value
    },
    validationM.emailMustMatch
  );

  /**
   * Validate the change password form
   **/
  $("#password-change-form").validate({
    rules: {
      oldpassword: {
        required: true,
        minlength: 8
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
      oldpassword: {
        required: validationM.passwordRequired,
        minlength: $.format(validationM.passwordMinLength)
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

  /**
   * Validate the new fund form
   **/
  $("#new-fund-form").validate({
    rules: {
      email: {
        required: true,
        email: true
      },
      password: {
        required: true,
        minlength: 8
      },
      answer: {
        required: true
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
      answer: {
        required: validationM.answerRequired
      } 
    }
  });

});