/**
 * Validate the manual investment addition form
 **/
$("#risk-form").validate({
	errorPlacement: function(error, element) {
		error.insertAfter(element.parent().parent());  
	},
	highlight: function (element, errorClass, validClass) {
    	$(element).parent().parent().parent().addClass('error');
	},
	unhighlight: function (element, errorClass, validClass) {
	    $(element).parent().parent().parent().removeClass('error');
	}
});
$.validator.addMethod("qRequired", $.validator.methods.required,
	validationM.answerRequired); 

$.validator.addClassRules({
    option: {
        qRequired: true
    }
});