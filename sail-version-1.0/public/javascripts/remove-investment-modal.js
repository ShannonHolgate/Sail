$(function () {
    /** 
    * JS to control the messages and sumbission of the remove investment popup
    **/

    //Global variables for the remove investment modal
    var removePopAnchors = $('.remove-pop');
    var removeNameSelect = $('#remove-name-select');
    var removeQuantitySelect = $('#remove-quantity-select');
    var quantityHidden = $('.quantity-hidden');
    var quantitySelect = $('#remove-quantity-select');
    var valueHidden = $('.value-hidden');
    var valueInput = $('#remove-value-input');
    var removeMessage = $('#remove-details');
    var removeHideButtons = $('.remove-hide');
    var closeRemoveAnchors = $('.close-modal');
    var removeCheckbox = $('#remove-checkbox');

    /**
    * Create the Ajax URLs
    **/
    var investmentUrl = "http://"+requestHost+"/service/investments/";

    /**
    * Create empty array objects to hold the values and quantities for the modal
    */
    var quantityArray = new Object();
    var valueArray = new Object();

    /**
    * Add a keyup listener to the enter key on the remove form
    * submits the form
    **/
    $('#remove-inv-form').keyup(function(event) {
        if(event.keyCode == 13){
            submitRemove();
        }
    });

    /**
    * Add a click listener to the remove investment button on the remove form
    * submits the form
    **/
    $('#remove-button').click(function() {
        submitRemove();
    });

    /**
    * Submit form handler
    * Appends the asset class, remove all flag and asset name
    * to the form and changes the currency to 
    * regular decimals
    **/
    function submitRemove() {
        $('#remove-inv-form').validate();
        if (removeInvestment && $('#remove-inv-form').valid()) {
            var input = $("<input>")
               .attr("type", "hidden")
               .attr("name", "assetclass").val(removeInvestment);
            var id = $("<input>")
               .attr("type", "hidden")
               .attr("name", "id").val($("#remove-name-select option:selected").val());
            var removeAll = $("<input>")
                .attr("type", "hidden")
                .attr("name", "removeallbool").val(removeCheckbox.is(':checked'));
            $('#remove-inv-form').append($(input));  
            $('#remove-inv-form').append($(id)); 
            $('#remove-inv-form').append($(removeAll));
            menuCurrencyInputs.toNumber();
            $('#remove-inv-form').submit();  
        }
    }

    /**
    * Add a click listener to the remove links in the menu
    * Sets up the update investment modal popup
    **/
    removePopAnchors.click(function() {
        /**
        * Set up the Modal form with the investment chosen to add
        */
        var investment = $(this).closest('.menu-item').find('a')[0].innerHTML;
        removeInvestment = investment;

        /**
        * Clear the modal for
        */
        resetEditModal();

        /**
        * Get the investments in this asset class
        */
        getInvestments(investment);
    });

    /** 
    * Executes the Ajax request to retrieve the investments held for the asset class chosen
    * Populates the quantityArray and valueArray based on the investments returned
    * Shows the form
    */
    function getInvestments(assetClass) {
        if (assetClass) {
            /**
            *  Block the UI until the Ajax request returns #
            */
            $.blockUI({ 
                message: '<h1>Loading</h1>', 
                timeout: 10000 
            }); 

            // Build up the investment url
            var investmentQueryUrl = investmentUrl + assetClass;

            // Clear down the quantity and value objects
            quantityArray = new Object();
            valueArray = new Object();

            /**
             * Do the Ajax call to get a list of results for the asset class chosen
             **/
            $.getJSON( investmentQueryUrl, function( data ) { 
                if (typeof data !== 'undefined' && data.length > 0) {
                    $.each(data, function() {
                        removeNameSelect.append($("<option />").val(this.id).text(this.name));   
                        if (this.symbol) {
                            quantityArray[this.id] = this.quantity;
                        }                         
                        valueArray[this.id] = this.value;
                    }); 
                    if (removeNameSelect.children().size() < 1) {
                        // There are no matching results
                        showNoInvestmentsDiv(assetClass) ;
                    }
                    else {
                        // Show the investment options
                        $(removeNameSelect).change();
                        removeHideButtons.hide();
                        showFormDetails(assetClass);
                    }
                }
                else {
                    // There are no matching results
                    showNoInvestmentsDiv(assetClass) ;
                    $.unblockUI();
                }
            }).fail( function(d, textStatus, error) {
                /**
                * Upon failure run away
                **/
                showNoInvestmentsDiv(assetClass);
                $.unblockUI();
            });   

        }
    }

    /** 
    * Add a change listener to the Investment name drop down
    * Checks if the chosen investment id exists in the quantityArray and populates the quantity drop down
    * with integers up to the quantity to allow for adjustment
    * If the name does not exist, populate the value input field
    */
    removeNameSelect.change(function() {
        //Empty the quantity drop down
        quantitySelect.empty();

        //Get the id of the investment chosen
        var selectedValue = $("#remove-name-select option:selected").val();

        //Check if the id exists in the quantity array
        if (quantityArray.hasOwnProperty(selectedValue)) {
            if (quantityArray[selectedValue] != 0) {
                for(i=1; i <= quantityArray[selectedValue]; i++){
                    //Build up the drop down with values up to the quantity of the investment
                    quantitySelect.append($("<option />").val(i).text(i)); 
                }
                //Set the current quantity as selected
                $('#remove-quantity-select option').last().prop('selected',true);
                //Show the quantity drop down and hide the value
                quantityHidden.show();
                valueHidden.hide();
            }
            else {
                /**
                * The quantity does not exist, therefore it is a manual investment
                * Hide the quantity drop down, populate the value field and show it
                **/
                quantityHidden.hide();
                if (valueArray.hasOwnProperty(selectedValue)) valueHidden.val(valueArray[selectedValue]);
                menuCurrencyInputs.formatCurrency({symbol:'£'}); 
                valueHidden.show();

            }
        }
        else {
            /**
            * The quantity does not exist, therefore it is a manual investment
            * Hide the quantity drop down, populate the value field and show it
            **/
            quantityHidden.hide();
            if (valueArray.hasOwnProperty(selectedValue)) valueHidden.val(valueArray[selectedValue]);
            menuCurrencyInputs.formatCurrency({symbol:'£'});
            valueHidden.show();
        }
        //Unblock the ui
        $.unblockUI();  
    });

    /**
    * Add a click listener to the close links
    * Resets the modal
    **/
    closeRemoveAnchors.click(function() {
        resetEditModal();
    });

    /**
    * Add a change listener to the remove all checkbox to disable the quantity drop down and value drop down
    * Ensures the fields are ignored during validation
    */
    removeCheckbox.change(function() {
        if (document.getElementById('remove-checkbox').checked) {
            document.getElementById('remove-quantity-select').disabled = true;  
            document.getElementById('remove-value-input').disabled = true;
            valueInput.val('');
            valueInput.addClass("ignore");
            valueInput.removeClass("error");
        }
        else {
            document.getElementById('remove-quantity-select').disabled = false;   
            document.getElementById('remove-value-input').disabled = false;
            valueInput.removeClass("ignore");
        }
        $('#remove-inv-form').validate().resetForm();
        $('#remove-inv-form').validate();
    });

    /**
    * If no investments exist this should be displayed to the user
    */
    function showNoInvestmentsDiv(assetClass) {
        var titleMessage = '<h3>Edit '+assetClass+' Value</h3>';
        var description = '<p>You have no current '+assetClass+' investments, please use the menu on the left to add an asset</p>';
        removeMessage.html(titleMessage+description);
    }

    /**
    * Shows the update form and populates with the asset class
    */
    function showFormDetails(assetClass) {
        var titleMessage = '<h3>Edit '+assetClass+' Value</h3>';
        var description = '<p>Select the '+assetClass+' you would like to change the quantity or value of</p>';
        removeNameSelect.parent().parent().parent().removeAttr('style');

        removeMessage.html(titleMessage+description);
    }

    /**
    * Completely Resets the Modal popup and sets the default message and title to be used when 
    * An asset class is not chosen
    */
    function resetEditModal() {
        resetEditFields();  
        currentInvestment = "";
        var titleMessage = '<h3>Edit Investment Value</h3>';
        var description = '<p>Please choose an investment to edit in the menu on the left</p>';

        quantityHidden.hide();
        valueHidden.hide();
        $('#remove-inv-form').validate().resetForm();
        valueHidden.removeClass('error');
        $('#password').removeClass('error');

        menuCurrencyInputs.val('0.00');
        menuCurrencyInputs.formatCurrency({symbol:'£'});

        if (document.getElementById('remove-checkbox').checked) $(removeCheckbox).trigger('click');
        removeNameSelect.parent().parent().parent().hide();
        removeMessage.html(titleMessage+description);
    }

    /**
    * Resets the Modal fields to empty values
    */
    function resetEditFields() {
        removeNameSelect.empty();
        valueInput.val('');
        $('#password').val('')
        removeNameSelect.parent().parent().parent().hide();
        quantitySelect.empty();
        removeHideButtons.show();  
    }
}); 