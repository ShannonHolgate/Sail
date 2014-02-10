$(function () {
    /** 
    * JS to control the messages and sumbission of the add investment popup
    **/

    //Global variables for the auto-investment modal
    var autoPopAnchors = $('.auto-pop');
    var autoPopupMessage = $('#auto-details');
    var search = $('#ticker-search-submit');
    var queryInput = $('#ticker-search');
    var resultOptions = $('#result-list');
    var quantityInput = $('#quantity');
    var addHideButtons = $('.add-hide');
    var searchResultsDivs = $('.search-results');
    var closeModalAnchors = $('.close-modal');
    var noResultsDiv = $('.no-results');

    //Global variables for the manual-investment modal
    var manualPopAnchors = $('.manual-pop');
    var manualPopupMessage = $('#manual-details');
    var manualName = $('#manual-name');
    var manualValue = $('#manual-value');
    var currentInvestment;
    var tempInvestment;

    /**
    * Create the Ajax URLs
    **/
    var tickerQueryUrl = "http://" + requestHost + "/service/findsymbols/";

    $('#add-manual-form').keyup(function(event) {
        if(event.keyCode == 13){
            submitManual();
        }
    });

    $('#add-manual-button').click(function() {
        submitManual();
    });

    function submitManual() {
        $('#add-manual-form').validate();
        if (currentInvestment && $('#add-manual-form').valid()) {
            var input = $("<input>")
               .attr("type", "hidden")
               .attr("name", "assetclass").val(currentInvestment);
            $('#add-manual-form').append($(input));  
            $('#add-manual-form').submit();  
        }
    }

    $('#add-auto-form').keyup(function(event) {
        if(event.keyCode == 13){
            submitAuto();
        }
    });

    $('#add-button').click(function() {
        submitAuto();
    });

    function submitAuto() {
        $('#add-auto-form').validate();
        if (currentInvestment && $('#add-auto-form').valid()) {
            var input = $("<input>")
               .attr("type", "hidden")
               .attr("name", "assetclass").val(currentInvestment);
            $('#add-auto-form').append($(input));  
            $('#add-auto-form').submit();  
        }
    }

    autoPopAnchors.click(function() {
        /**
        * Set up the Modal form with the investment chosen to add
        */
        resetModals();
        var investment = $(this).closest('.menu-item').find('a')[0].innerHTML;
        currentInvestment = investment;
        var titleMessage = '<h3>Add '+investment+'</h3>';
        var description = '<p>Enter the ticker symbol below of the '+investment+' you would like to add</p>';
        queryInput.parent().removeAttr('style');

        autoPopupMessage.html(titleMessage+description);
    });

    $('.temp-investment').click(function () {
        var titleMessage = '<h3>Add '+currentInvestment+'</h3>';
        var description = '<p>Enter the name and current value of the '+currentInvestment+' you would like to add</p>';
        manualName.parent().parent().parent().show();
        addHideButtons.hide("fast");
        manualPopupMessage.html(titleMessage+description);
    });

    $(queryInput).keyup(function(event){
        if(event.keyCode == 13){
            $(search).click();
        }
    });

    manualPopAnchors.click(function() {
        resetModals();
        var investment = $(this).closest('.menu-item').find('a')[0].innerHTML;
        currentInvestment = investment;
        var titleMessage = '<h3>Add '+currentInvestment+'</h3>';
        var description = '<p>Enter the name and current value of the '+currentInvestment+' you would like to add</p>';
        manualName.parent().parent().parent().show();
        addHideButtons.hide("fast");
        manualPopupMessage.html(titleMessage+description);
    });

    search.click(function() {
        // Get the search form value
        var input = queryInput.val();
        // Ensure the entered search value is valid
        if ($.trim(input)) {

            // Create the Ajax url
            var tickerUrl = tickerQueryUrl + input;
            // Clear the current result list
            resultOptions.empty();

            /**
            *  Block the UI until the Ajax request returns #
            */
            $.blockUI({ 
                message: '<h1>Loading</h1>', 
                timeout: 10000 
            }); 
            /**
             * Do the Ajax call to get a list of results matching the ticker symbol or name
             **/
            $.getJSON( tickerUrl, function( data ) { 
                if (typeof data !== 'undefined' && data.length > 0) {
                    $.each(data, function() {
                        if (currentInvestment!='Shares') {
                            if (this.type!='S') {
                                resultOptions.append($("<option />").val(this.symbol+"~"+this.name).text(this.symbol+" ~ "+this.exch+" ~ "+this.name+" ~ "+this.typeDisp));    
                            }    
                        }
                        else {
                            if (this.type=='S') {
                                resultOptions.append($("<option />").val(this.symbol+"~"+this.name).text(this.symbol+" ~ "+this.exch+" ~ "+this.name));    
                            }  
                        }
                        
                    }); 
                    if (resultOptions.children().size() < 1) {
                        // There are no matching results
                        resetModalFields();
                        noResultsDiv.show();
                        $.unblockUI();
                    }
                    else {
                        addHideButtons.hide();
                        noResultsDiv.hide();
                        searchResultsDivs.show();
                        $.unblockUI();  
                    }
                }
                else {
                    // There are no matching results
                    resetModalFields();
                    noResultsDiv.show();
                    $.unblockUI();
                }
            }).fail( function(d, textStatus, error) {
                /**
                * Upon failure run away
                **/
                $.unblockUI();
                noResultsDiv.show();
                resetModalFields();
            });     
        }
    });

    closeModalAnchors.click(function() {
        resetModals();
    });

    manualPopAnchors.click(function() {
        queryInput.val('');   
        resetModalFields();
        noResultsDiv.hide(); 
    })

    function resetModalFields() {
        resultOptions.empty();
        quantityInput.val('');
        searchResultsDivs.hide();
        addHideButtons.show();  
    }

    function resetModals() {
        resetModalFields();  
        manualName.val('');
        manualValue.val('');
        noResultsDiv.hide(); 
        queryInput.val('');

        $('#add-auto-form').validate().resetForm();
        $('#add-manual-form').validate().resetForm();
        quantityInput.removeClass('error');
        manualValue.removeClass('error');
        manualName.removeClass('error');

        menuCurrencyInputs.val('0.00');
        menuCurrencyInputs.formatCurrency({symbol:'£'});

        currentInvestment = "";
        var titleMessage = '<h3>Add Investment</h3>';
        var description = '<p>Please choose an investment to add in the menu on the left</p>';
        manualName.parent().parent().parent().hide();
        queryInput.parent().hide();
        autoPopupMessage.html(titleMessage+description);
        manualPopupMessage.html(titleMessage+description);
    }

}); 