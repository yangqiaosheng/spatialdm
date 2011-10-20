var number = new Array(200);
var field = new Array(200);
var nameString = new Array(200);


var nameStringHistogram = new Array(200);
var number_Histogram = new Array(200);
var field_Histogram = new Array(200);

// in this string I map months, years, days hours.
var text1 = "";
var numberHeader = new Array(7);
var fieldHeader = new Array(7);
var N = 100;


function get_set_Field(){
	var local_nameString = new Array(200);
	for (var i=0;i<N; i++){ 
		local_nameString[i] = document.getElementById('controltime' + i);
	}
	return local_nameString;
}
function get_set_number(){
	var local_nameString = new Array(200);
	for (var i=0;i<N; i++){
		local_nameString[i] = 0;
	}
	return local_nameString;	
}

function activateTheHeaderOfTable() {
	for ( var i = 1; i < 6; i++) {
		fieldHeader[i] = document.getElementById('controltime_header' + i);
		numberHeader[i] = 0;
	}
}

function loadDataForTable(){
	var local_nameString = new Array(200);
	for (var i = 1; i < N; i++){       
        switch (i) {
            case 1:
            	local_nameString[i] = "2005";
            break;
            case 2:
            	local_nameString[i] = "2006";
            break;
            case 3:
            	local_nameString[i] = "2007";
            break;
            case 4:
            	local_nameString[i] = "2008";
            break;
            case 5:
            	local_nameString[i] = "2009";
            break;
            case 6:
            	local_nameString[i] = "2010";
	    break;
	    case 7:
            	local_nameString[i] = "2011";
            break;
            // ***********************************************************************
            case 8:
            	local_nameString[i] = "January";
            break;
            case 9:
            	local_nameString[i] = "February";
            break;
            case 10:
            	local_nameString[i] = "March";
            break;
            case 11:
            	local_nameString[i] = "April";
            break;
            case 12:
            	local_nameString[i] = "May";
            break;
            case 13:
            	local_nameString[i] = "June";
            break;
            case 14:
            	local_nameString[i] = "July";
            break;
            case 15:
            	local_nameString[i] = "August";
            break;
            case 16:
            	local_nameString[i] = "September";
            break;
            case 17:
            	local_nameString[i] = "October";
            break;
            case 18:
            	local_nameString[i] = "November";
            break;
            case 19:
            	local_nameString[i] = "December";
            break;
            // ***********************************************************************
            
            case 20:
            	local_nameString[i] = "1";
            break;
            case 21:
            	local_nameString[i] = "2";
            break;
            case 22:
            	local_nameString[i] = "3";
            break;
            case 23:
            	local_nameString[i] = "4";
            break;
            case 24:
            	local_nameString[i] = "5";
            break;
            case 25:
            	local_nameString[i] = "6";
            break;
            case 26:
            	local_nameString[i] = "7";
            break;
            case 27:
            	local_nameString[i] = "8";
            break;
            case 28:
            	local_nameString[i] = "9";
            break;
            case 29:
            	local_nameString[i] = "10";
            break;
            case 30:
            	local_nameString[i] = "11";
            break;
            case 31:
            	local_nameString[i] = "12";
            break;
            case 32:
            	local_nameString[i] = "13";
            break;
            case 33:
            	local_nameString[i] = "14";
            break;
            case 34:
            	local_nameString[i] = "15";
            break;
            case 35:
            	local_nameString[i] = "16";
            break;
            case 36:
            	local_nameString[i] = "17";
            break;
            case 37:
            	local_nameString[i] = "18";
            break;
            case 38:
            	local_nameString[i] = "19";
            break;
            case 39:
            	local_nameString[i] = "20";
            break;
            case 40:
            	local_nameString[i] = "21";
            break;
            case 41:
            	local_nameString[i] = "22";
            break;
            case 42:
            	local_nameString[i] = "23";
            break;
            case 43:
            	local_nameString[i] = "24";
            break;
            case 44:
            	local_nameString[i] = "25";
            break;
            case 45:
            	local_nameString[i] = "26";
            break;
            case 46:
            	local_nameString[i] = "27";
            break;
            case 47:
            	local_nameString[i] = "28";
            break;
            case 48:
            	local_nameString[i] = "29";
            break;
            case 49:
            	local_nameString[i] = "30";
            break;
            case 50:
            	local_nameString[i] = "31";
            break;
            
            // ***********************************************************************
            case 51:
            	local_nameString[i] = "0";
            break;
            case 52:
            	local_nameString[i] = "1";
            break;
            case 53:
            	local_nameString[i] = "2";
            break;
            case 54:
            	local_nameString[i] = "3";
            break;
            case 55:
            	local_nameString[i] = "4";
            break;
            case 56:
            	local_nameString[i] = "5";
            break;
            case 57:
            	local_nameString[i] = "6";
            break;
            case 58:
            	local_nameString[i] = "7";
            break;
            case 59:
            	local_nameString[i] = "8";
            break;
            case 60:
            	local_nameString[i] = "9";
            break;
            case 61:
            	local_nameString[i] = "10";
            break;
            case 62:
            	local_nameString[i] = "11";
            break;
            case 63:
            	local_nameString[i] = "12";
            break;
            case 64:
            	local_nameString[i] = "13";
            break;
            case 65:
            	local_nameString[i] = "14";
            break;
            case 66:
            	local_nameString[i] = "15";
            break;
            case 67:
            	local_nameString[i] = "16";
            break;
            case 68:
            	local_nameString[i] = "17";
            break;
            case 69:
            	local_nameString[i] = "18";            
            break;
            case 70:
            	local_nameString[i] = "19";
            break;
            case 71:
            	local_nameString[i] = "20";
            break;
            case 72:
            	local_nameString[i] = "21";
            break;
            case 73:
            	local_nameString[i] = "22";
            break;
            case 74:
            	local_nameString[i] = "23";
            break;
            // ***********************************************************************
            case 75:
            	local_nameString[i] = "Sunday";
            break;
            case 76:
            	local_nameString[i] = "Monday";
            break;
            case 77:
            	local_nameString[i] = "Tuesday";
            break;
            case 78:
            	local_nameString[i] = "Wednesday";
            break;
            case 79:
            	local_nameString[i] = "Thursday";
            break;
            case 80:
            	local_nameString[i] = "Friday";
            break;
            case 81:
            	local_nameString[i] = "Saturday";
            break;
        }      
    }
    return local_nameString;
}
// this fucntion receive a parameter as number of the argument- even if the
// parameter is not visible
/*function selectbutton() {
    var argv = selectbutton.arguments;
    number[argv[0]]++;
    // even, odd for selection deselection
    if (number[argv[0]] % 2 != 0) {
        field[argv[0]].style.background = '#ffffff';
    } else {
        field[argv[0]].style.background = '#06A8FA';
    }
}
*/
function selectbuttonHeader() {
    var argv = selectbuttonHeader.arguments;
    numberHeader[argv[0]]++;
    // even, odd for selection deselection
    fieldHeader[argv[0]].style.background = '#8AA8F3';
    if (argv[0] == 1) {
        // years
        for (var i = 1; i <= 6; i++) {
            // in the beginning I have the years
            if (number[i] % 2 != 0) {
                // I take the selections
                selectbutton(i);
            } else {
                selectbutton(i);
            }
        }
    }
    if (argv[0] == 2) {
        for (var i = 7; i <= 18; i++) {
            // in the beginning I have the
            // months
            if (number[i] % 2 != 0) {
                // I take the selections
                selectbutton(i);
            } else {
                selectbutton(i);
            }
        }
    }
    if (argv[0] == 3) {
        for (var i = 19; i <= 49; i++) {
            // in the beginning I have the days
            if (number[i] % 2 != 0) {
                // I take the selections
                selectbutton(i);
            } else {
                selectbutton(i);
            }
        }
    }
    if (argv[0] == 4) {
        for (var i = 50; i <= 73; i++) {
            if (number[i] % 2 != 0) {
                selectbutton(i);
            } else {
                selectbutton(i);
            }
        }
    }
    if (argv[0] == 5) {
        for (var i = 74; i <= 80; i++) {
            if (number[i] % 2 != 0) {
                selectbutton(i);
            } else {
                selectbutton(i);
            }
        }
    }
}

function ask() {
   // alert("ask");
    text1 = "";    
    headerXML = createHeaderXML();
    bodyXML = timeController1XML(text1);
    sendToServerCalendarData(headerXML, bodyXML);    
    // alert("globalPolygonSelected "+ globalPolygonSelected);
}

function refreshButtons() {    
    deleteHistory();
//     resetTable();
}

// function resetTable(){
//   for (var i=1; i<=80;i++){
//    if (number[i] % 2 != 0) {
// 	number[i]++;	
//         field[i].style.background = '#06A8FA';
//     } else {
//         field[i].style.background = '#06A8FA';
//     }
//   }
// }