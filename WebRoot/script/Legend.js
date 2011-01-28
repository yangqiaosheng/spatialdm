$(document).ready(function(){
    $("#showhide").toggle(function(){
        $("#legendInfo").hide("blind", 500);
        $("#showhide").attr('value', 'show');
    }, function(){
        $("#legendInfo").show("blind", 500);
        $("#showhide").attr('value', 'hide');
    });
});
