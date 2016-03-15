/**
 *
 * @author Ioannis Chrysakis (hrysakis@ics.forth.gr)
 */
var loadingimg = "<center><img id=\"loading\" alt=\"loading\" src=\"images/loadingAnimation.gif\"></center>";


//Retrieve parameters from url when passed through iframe
function getURLParameters(paramName) {
    var sPageURL = window.location.search.substring(1);
    var sURLVariables = sPageURL.split('&');

    for (var i = 0; i < sURLVariables.length; i++)
    {
        var sParameterName = sURLVariables[i].split('=');
        if (sParameterName[0] == paramName)
        {
            return sParameterName[1];

        }
    }
}


//Gets the context path
function getContextPath() {
    return window.location.pathname.substring(0, window.location.pathname.indexOf("/", 2));
}


//Displays main warnings to the user
function showJSON(id, msg)

{
 
    $("#" + id).text(msg.slice(1,-1)); 
    //JCH-Note: SOS ebala text anti gia html gia na mhn mou xalaei ta URIS
    // to slice(1,-1) afairei prwto kai telutaio xarakthra (')
    
    $("#" + id).dialog(
    {
        title: 'JSON Representation',
        modal: true,
        width: 600

    });
}


//Displays main warnings to the user
function showDialog(id, msg)
{
    $("#" + id).html(msg);
    $("#" + id).dialog(
    {
        title: 'Warning',
        //position: ['center', 'right'],
        modal: true
        //background: '#ff0000'
        //dialogClass: 'ui-dialog-osx'
        //position: ['center', 'right'],
        //height: 150,
        //width: 350
     });
}

//Displays warnings on top mainly in the case of visualization
function showTopDialog(id, msg)

{
    $("#" + id).html(msg);
    $("#" + id).dialog(
    {
        title: 'Warning',
        position: ['top'],
        of: window,
        modal: true
    });
}

//Normalizes a javascript array and convert it to string
function normalizeJSArray(JSArray) {
    $.unique(JSArray); // remove duplicates
    var str = JSON.stringify(JSArray); //JCH:send a js arrray as a string
    str = str.replace(/[[\]]/g, ""); //remove brackets
    str = str.replace(/"/g, "");   //removes "
    return str;
}
