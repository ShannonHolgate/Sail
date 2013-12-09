var navBar = $('#nav-bar'),
        showLeft = $('#showLeft'),
        body = $('body');
        pushLeft = $('.push-left')

body.toggleClass('nav-bar-push-toright');
 
showLeft.click(function(){
    showLeft.toggleClass('active');
    navBar.toggleClass('nav-bar-closed');
    body.toggleClass('nav-bar-push-toright');
    pushLeft.toggleClass('push-left-open');
});