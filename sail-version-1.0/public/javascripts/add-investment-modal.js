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

    /**
    * Add a keyup listener to the enter key on the manual form
    * submits the form
    **/
    $('#add-manual-form').keyup(function(event) {
        if(event.keyCode == 13){
            submitManual();
        }
    });

    /**
    * Add a click listener to the add manual button on the manual form
    * submits the form
    **/
    $('#add-manual-button').click(function() {
        submitManual();
    });

    /**
    * Submit form handler
    * Appends the asset class to the form and changes the currency to 
    * regular decimals
    **/
    function submitManual() {
        $('#add-manual-form').validate();
        if (currentInvestment && $('#add-manual-form').valid()) {
            var input = $("<input>")
               .attr("type", "hidden")
               .attr("name", "assetclass").val(currentInvestment);
            $('#add-manual-form').append($(input));  
            menuCurrencyInputs.toNumber();
            $('#add-manual-form').submit();  
        }
    }

    /**
    * Add a keyup listener to the enter key on the auto form
    * submits the form
    **/
    $('#add-auto-form').keyup(function(event) {
        if(event.keyCode == 13){
            submitAuto();
        }
    });

    /**
    * Add a click listener to the add auto button on the manual form
    * submits the form
    **/
    $('#add-button').click(function() {
        submitAuto();
    });

    /**
    * Submit form handler
    * Appends the asset class to the form and changes the currency to 
    * regular decimals
    **/
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

    /**
    * Add a click listener to the add auto links in the menu
    * Sets up the automated investment modal popup
    **/
    autoPopAnchors.click(function() {
        /**
        * Set up the Modal form with the investment chosen to add
        */
        resetModals();
        var investment = $(this).closest('.menu-item').find('a')[0].innerHTML;
        currentInvestment = investment;
        var titleMessage = '<h3>'+addInvestmentM.addTitle(investment)+'</h3>';
        var description = '<p>'+addInvestmentM.addMessage(investment)+'</p>';
        queryInput.parent().removeAttr('style');

        autoPopupMessage.html(titleMessage+description);
    });

    /**
    * Add a click listener to the link from the automated form to the manual form
    * Needed to carry across the current investment class
    **/
    $('.temp-investment').click(function () {
        var titleMessage = '<h3>'+addInvestmentM.addTitle(currentInvestment)+'</h3>';
        var description = '<p>'+addInvestmentM.addManualMessage(currentInvestment)+'</p>';
        manualName.parent().parent().parent().show();
        addHideButtons.hide("fast");
        manualPopupMessage.html(titleMessage+description);
    });

    /**
    * Add a keyup listener to the symbol query field to search on enter
    **/
    $(queryInput).keyup(function(event){
        if(event.keyCode == 13){
            $(search).click();
        }
    });

    /**
    * Add a click listener to the add manual links in the menu
    * Sets up the manual investment modal popup
    **/
    manualPopAnchors.click(function() {
        resetModals();
        var investment = $(this).closest('.menu-item').find('a')[0].innerHTML;
        currentInvestment = investment;
        var titleMessage = '<h3>'+addInvestmentM.addTitle(currentInvestment)+'</h3>';
        var description = '<p>'+addInvestmentM.addManualMessage(currentInvestment)+'</p>';
        manualName.parent().parent().parent().show();
        addHideButtons.hide("fast");
        manualPopupMessage.html(titleMessage+description);
    });

    /**
    * Add a click listener to the symbol search button
    * checks if the input is not empty then calls the investment query web service
    * The Json returned then checks the investment class against the chosen.
    * Only equity investments should be shown for Shares
    * Populates the results select list in the automated form
    **/
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
                message: '<h1>'+globalM.loading+'</h1>', 
                timeout: 10000 
            }); 
            /**
             * Do the Ajax call to get a list of results matching the ticker symbol or name
             **/
            $.getJSON( tickerUrl, function( data ) { 
                if (typeof data !== 'undefined' && data.length > 0) {
                    $.each(data, function() {
                        if (currentInvestment!=globalM.shares) {
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
                        //Results exists - Show them
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

    /**
    * Add a click listener to the close links
    * Resets the modals
    **/
    closeModalAnchors.click(function() {
        resetModals();
    });

    /**
    * Add a second click listener to clear the automated investment fields
    **/
    manualPopAnchors.click(function() {
        queryInput.val('');   
        resetModalFields();
        noResultsDiv.hide(); 
    })

    /**
    * Resets the Modal fields to empty values
    */
    function resetModalFields() {
        resultOptions.empty();
        quantityInput.val('');
        searchResultsDivs.hide();
        addHideButtons.show();  
    }

    /**
    * Completely Resets the Modal popups and sets the default message and title to be used when 
    * An asset class is not chosen
    */
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
        var titleMessage = '<h3>'+addInvestmentM.addDefaultTitle+'</h3>';
        var description = '<p>'+addInvestmentM.addDefaultMessage+'</p>';
        manualName.parent().parent().parent().hide();
        queryInput.parent().hide();
        autoPopupMessage.html(titleMessage+description);
        manualPopupMessage.html(titleMessage+description);
    }

}); 