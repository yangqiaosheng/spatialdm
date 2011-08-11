$(document).ready(function(){
    $("#showhide").toggle(function(){
        $("#legendInfo").hide("blind", 500);
        $("#showhide").attr('value', 'show');
    }, function(){
        $("#legendInfo").show("blind", 500);
        $("#showhide").attr('value', 'hide');
    });
});
$(document).ready(function(){
    $("#QarryShowhide").toggle(function(){
        $("#move1").hide("blind", 500);
        $("#QarryShowhide").attr('value', 'show');
    }, function(){
        $("#move1").show("blind", 500);
        $("#QarryShowhide").attr('value', 'hide');
    });
});
