/*function loadXMLDoc(url)
{
	if (window.XMLHttpRequest)
	{
		// code for IE7+, Firefox, Chrome, Opera, Safari
		xmlhttp=new XMLHttpRequest();
		xmlhttp.open("GET",url,false);
		xmlhttp.send(null);
	}
	else
	  {// code for IE6, IE5
	  xmlhttp=new ActiveXObject("Microsoft.XMLHTTP");
	  xmlhttp.open("GET",url,false);
	  xmlhttp.send();
	  }
	document.getElementById('test').innerHTML=xmlhttp.responseText;	
}
*/
/*
 * 
 *to send a request to the server we use open() or send()
 *open() takes 3 arguments: 
 *			the first argument specify the type of the request GET or POST.
 *			the second argument specifies the location of the server resources
 *			the third argument specifies if the request should be handled asynchronously or not.
 *
 **/