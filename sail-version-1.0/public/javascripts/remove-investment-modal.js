$(function () {
    /** 
    * JS to control the messages and sumbission of the remove investment popup
    **/

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

    var investmentUrl = "http://"+requestHost+"/service/investments/";

    var quantityArray = new Object();

    $('#remove-inv-form').keyup(function(event) {
        if(event.keyCode == 13){
            submitRemove();
        }
    });

    $('#remove-button').click(function() {
        submitRemove();
    });

    function submitRemove() {
        $('#remove-inv-form').validate();
        if (removeInvestment && $('#remove-inv-form').valid()) {
            var input = $("<input>")
               .attr("type", "hidden")
               .attr("name", "assetclass").val(removeInvestment);
            var id = $("<input>")
               .attr("type", "hidden")
               .attr("name", "id").val($("#remove-name-select option:selected").val());
            $('#remove-inv-form').append($(input));  
            $('#remove-inv-form').append($(id)); 
            $('#remove-inv-form').submit();  
        }
    }

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

            // Clear down the quantity object
            quantityArray = new Object();

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
                    }); 
                    if (removeNameSelect.children().size() < 1) {
                        // There are no matching results
                        showNoInvestmentsDiv(assetClass) ;
                        $.unblockUI();
                    }
                    else {
                        // Show the investment options
                        $(removeNameSelect).change();
                        removeHideButtons.hide();
                        showFormDetails(assetClass);
                        $.unblockUI();  
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

    removeNameSelect.change(function() {
        quantitySelect.empty();
        var selectedValue = $("#remove-name-select option:selected").val();
        if (quantityArray.hasOwnProperty(selectedValue)) {
            if (quantityArray[selectedValue] != 0) {
                for(i=1; i <= quantityArray[selectedValue]; i++){
                    quantitySelect.append($("<option />").val(i).text(i)); 
                }

                quantityHidden.show();
            }
            else
                valueHidden.show();
        }
        else
            valueHidden.show();
    });

    closeRemoveAnchors.click(function() {
        resetEditModal();
    });

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

    function showNoInvestmentsDiv(assetClass) {
        var titleMessage = '<h3>Edit '+assetClass+' Value</h3>';
        var description = '<p>You have no current '+assetClass+' investments, please use the menu on the left to add an asset</p>';
        removeMessage.html(titleMessage+description);
    }

    function showFormDetails(assetClass) {
        var titleMessage = '<h3>Edit '+assetClass+' Value</h3>';
        var description = '<p>Select the '+assetClass+' you would like to change the value of</p>';
        removeNameSelect.parent().parent().parent().removeAttr('style');

        removeMessage.html(titleMessage+description);
    }

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

    function resetEditFields() {
        removeNameSelect.empty();
        valueInput.val('');
        $('#password').val('')
        removeNameSelect.parent().parent().parent().hide();
        quantitySelect.empty();
        removeHideButtons.show();  
    }
}); 