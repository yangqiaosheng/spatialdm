
var timeline;
function onLoad() {
  var bandInfos = [
    Timeline.createBandInfo({
         width:          "25%", 
         intervalUnit:   Timeline.DateTime.HOUR, 
         intervalPixels: 50
     }),
     Timeline.createBandInfo({
         width:          "25%", 
         intervalUnit:   Timeline.DateTime.DAY, 
         intervalPixels: 80
     }),
     Timeline.createBandInfo({
         width:          "25%", 
         intervalUnit:   Timeline.DateTime.MONTH, 
         intervalPixels: 100
     }),
     Timeline.createBandInfo({
         width:          "25%", 
         intervalUnit:   Timeline.DateTime.YEAR, 
         intervalPixels: 200
     })
   ];
   bandInfos[1].syncWith = 0;
   bandInfos[1].highlight = true;
   bandInfos[2].syncWith = 1;
   bandInfos[2].highlight = true;
   bandInfos[3].syncWith = 2;
   bandInfos[3].highlight = true;
   timeline = Timeline.create(document.getElementById("timeline"), bandInfos);
 }
 
 var resizeTimerID = null;
 function onResize() {
     if (resizeTimerID == null) {
         resizeTimerID = window.setTimeout(function() {
             resizeTimerID = null;
             timeline .layout();
         }, 500);
     }
 }