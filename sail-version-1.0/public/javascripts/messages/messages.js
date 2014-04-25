/**
* The validaion messages used in the form validator
**/
var validationM = {
	emailRequired:"An email address is required",
	emailInvalid:"Please enter a valid email address",
	nameRequired:"A Name is required",
	nameMinLength:"Name must contain at least {0} letters",
	nameMaxLength:"Name must contain less than {0} letters",
	passwordRequired:"A password is required",
	passwordMinLength:"Password must contain at least {0} letters",
	passwordMustMatch:"Both passwords must match",

	quantityRequired:"A quantity is required",
	dateRequired:"A date is required",

	invalidInteger:"Please enter a valid integer number",
	invalidCurrency:"Please enter a valid currency value",
	valueRequired:"A value is required",
	invalidDate:"Date must be in the format DD-MM-YYYY",

	answerRequired:"Please provide an answer to this question"
}

/**
* The Global user messages
**/
var globalM = {
	loading:"Loading",
	shares:"Shares"
}

/**
* The Add investment modal messages
**/
var addInvestmentM = {
	addDefaultTitle: "Add Investment",
	addDefaultMessage: "Please choose an investment to add in the menu on the left",
	addTitle:function(assetClass) {return "Add " + assetClass},
	addMessage:function(assetClass) {return "Enter the name or symbol below of the "+assetClass+" you would like to add"},
	addManualMessage:function(assetClass) {return "Enter the name and current value of the "+assetClass+" you would like to add"},
}

var editInvestment = {
	editTitle:function(assetClass) {return "Edit "+assetClass+" Value"},
	editMessage:function(assetClass) {return "Select the "+assetClass+" you would like to change the quantity or value of"},
	noInvMessage:function(assetClass) {return "You have no current "+assetClass+" investments, please use the menu on the left to add an asset"},
	editDefaultTitle: "Edit Investment Value",
	editDefaultMessage: "Please choose an investment to edit in the menu on the left"
}