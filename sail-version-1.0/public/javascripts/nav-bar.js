/**
 * The JS to push the nav bar onto and off the screen
 **/

var navBar = $('#nav-bar'),
        showLeft = $('#showLeft'),
        body = $('body');
        pushLeft = $('.push-left')
 
showLeft.click(function(){
    showLeft.toggleClass('active');
    navBar.toggleClass('nav-bar-closed');
    body.toggleClass('nav-bar-push-toright');
    pushLeft.toggleClass('push-left-open');
});

$('.menu-item').click(function() {
	if ($(this).hasClass('open'))
		$(this).removeClass('open');
	else {	
		$('.menu-item').removeClass('open');
		$(this).addClass('open');	
	}
});

