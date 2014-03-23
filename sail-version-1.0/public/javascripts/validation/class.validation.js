/**
 * Validate the manual investment addition form
 **/
$("#change-range-form").validate({
  rules: {
    datefrom: {
      required: true,
      validDate: true
    },
    dateto: {
      required: true,
      validDate: true
    }
  },
  messages: {
    datefrom: {
      required: validationM.dateRequired
    },
    dateto: {
      required: validationM.dateRequired 
    }
  },
  errorPlacement: function(error, element) {
     error.insertAfter(element.parent());  
  }
});

$.validator.addMethod("validDate",
  function(value, element) {
    var date = moment(value, "DD-MM-YYYY")
    return date.isValid();
  },
  validationM.invalidDate);