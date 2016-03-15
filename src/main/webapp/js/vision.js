/**
 *
 * @author Ioannis Chrysakis (hrysakis@ics.forth.gr)
 * This javascript file contains methods that handle all the visualization options
 * using the Google Charts library.
 */
//RETRIEVE PARAMETERS FROM IFRAME 
var changesGraph = getURLParameters('userChangesOntology');
var vold = getURLParameters('vfrom');
var vnew = getURLParameters('vto');
var vold_cut = vold.substr(vold.lastIndexOf('/') + 1);
var vnew_cut = vnew.substr(vnew.lastIndexOf('/') + 1);
var sradio = getURLParameters('sradio');
var versions = getURLParameters('userDatasetURI');
var clist = getURLParameters('clist');
var sclist = getURLParameters('sclist');
var cclist = getURLParameters('cclist');
var viewtype = getURLParameters('viewtype');
var tempontology = getURLParameters('tempontology');

var defaultChartWidth = 600;
var defaultChartHeight = 300;
var barchart_fixed_width = 900;
var googlelementsMinHeight = 1250;
 
var vset;
var data; //used to store query results which are taken as input for displaying the first column chart
var extdata = null; // used to store query results from extended functionality
var user_chartype ="pie"; //used to store user's preference on charttype

google.load('visualization', '1.0', {'packages': ['corechart', 'table']});

// Set a callback to run when the API is loaded.
google.setOnLoadCallback(init);

var mysclist = ''; //used for user defined list after the appearance of the first chart results
var mycclist = '';  // similarly to above
var myvlist = ''; //used to stored selected versions at the evo case
var groupby = ''; //used to store the group by option at the evo case


var custom_compare = false;
if (sradio === 'compareradio') {
    custom_compare = true;
}
// Send the query to the data source.
function init() {

    $('#vision_title').html(loadingimg);
    // Specify the data source URL from the VisualizeServlet.
    var query = new google.visualization.Query('visualize?changesgraph=' + changesGraph + "&sradio=" + sradio + "&vold=" + vold + "&vnew=" + vnew + "&dataseturi=" + versions + "&sclist=" + sclist + "&cclist=" + cclist + "&clist=" + clist + "&viewtype=" + viewtype + "&tempontology=" + tempontology);
    // Send the query with a callback function.              
    query.send(handleQueryResponse);
}

// Handle the query response.
function handleQueryResponse(response) {

    if (response.isError()) {
        alert('Error in query: ' + response.getMessage() + ' ' + response.getDetailedMessage());
        return;
    }

    data = response.getDataTable(); //get results in a table and put them to data    

    if (data === null || data.getNumberOfRows() === 0) { //(No results, i.e for custom compare case)
        //showTopDialog('vision_examplanations', 'No results to show!');
        handleEmptyResults();
        $('#loading').remove();
    }
    else {

        if (custom_compare) { //(custom) compare case
            // ta data ta phra apo to servlet kai periexoun ta apotelesmata pros probolh
            var chart = new google.visualization.Table(document.getElementById('table_div'));
            // Draw the visualization of a table

            google.visualization.events.addListener(chart, 'error', errorHandler);
            chart.draw(data, {width: defaultChartWidth, height: 'auto', is3D: true, title: 'Numerical analysis'});

            vset = vold_cut + " vs. " + vnew_cut;

            //Draw the pie at with setColumns [0,1]
            var view = new google.visualization.DataView(data);
            view.setColumns([0, 1]);
            initChartArea();
            drawChart('pie_view1', user_chartype, view);

            $('#vision_title').html("Compare Versions (" + vset + ")");
            $('#vision_examplanations').html("A numerical and graphical analysis of changes is depicted between the compared versions.");
            $('#evotable_criteria').removeClass("invisible");
            buildChangesRadio(true);

        }

        else { //evolution case     
            //JCH:Just for debuging reasons view the 
            //Use user-defined class for table properties that take as input class
            /*var cssClassNames = {
             'headerRow': 'italic-darkblue-font large-font bold-font',
             'tableRow': 'beige-background',
             'oddTableRow': 'beige-background',
             'selectedTableRow': 'orange-background large-font',
             'hoverTableRow': '',
             'headerCell': 'gold-border',
             'tableCell': '',

             'rowNumberCell': 'underline-blue-font'}; */
            ///chart = new google.visualization.Table(document.getElementById('table_div'));
            ///  chart.draw(data, { width: 600, height: 300, is3D: true, title:'Numerical analysis'}); 
            //chart.draw(data, { cssClassNames: cssClassNames, width: 600, height: 300, is3D: true, title:'Numerical analysis'}); 

            var col_options = {
                bar: {groupWidth: '80%'},
                width: 900,
                height: 350,
                //for column use 800x400
                //hAxis: {textStyle: {fontSize: 11, color: '#00F'} //blue color on axis
                hAxis: {textStyle: {fontSize: 11, color: '#000000'}
                },
                legend: {textStyle: {fontSize: 12}},
                backgroundColor: 'transparent',
                /*className:'underline-blue-font',*/
                chartArea: {left: 100, top: 30},
                //legend: {position: 'bottom'},
                isStacked: true
            };

            var ndata = transposeDateDataTable(data); //reverse columns with rows  
            var stackedchart = new google.visualization.BarChart(document.getElementById('col_stacked'));

            //ColumnChart vs. BarChart
            //Draw the stacked bar

            $('#vision_title').html("Dataset revision history");
            $('#vision_examplanations').html("This chart shows the evolution history for the dataset.");

            google.visualization.events.addListener(stackedchart, 'error', errorHandler);

            stackedchart.draw(ndata, col_options);
            $('#evotable_criteria').removeClass("invisible");

            buildgroupByRadios();
            buildEvoVersionRadio();
            buildChangesRadio(false);


        }


    }

}



function getMoreVisualizations(on_custom_compare) {
    var custom_compare = "no";
    if (on_custom_compare) {
        myvlist = vnew;
        custom_compare = "yes";
    }
    else {
        groupby = $("input[name='groupby']:checked").val(); //versionsradio vs. changesradio
        myvlist = fetchVersionsList();
    }
    fetchMyChangesList();

    $('#pie_view2').html(loadingimg);
    var query = new google.visualization.Query('extvis?changesgraph=' + changesGraph + "&dataseturi=" + versions + "&groupby=" + groupby + "&sclist=" + mysclist + "&cclist=" + mycclist + "&vlist=" + myvlist + "&tempontology=" + tempontology + "&oncompare=" + custom_compare + "&vfrom=" + vold + "&vto=" + vnew);

    query.send(handleExtQueryResponse);

}



function handleExtQueryResponse(response) {
    if (response.isError()) {
        alert('Error in query: ' + response.getMessage() + ' ' + response.getDetailedMessage());
        return;
    }
    else {

        extdata = response.getDataTable();
        //alert('rows:'+extdata.getNumberOfRows());
        if (extdata === null || extdata.getNumberOfRows() === 0) { //(No results, i.e for not detected changes)
           
            //showTopDialog('vision_examplanations', 'No results to show!');
            $('#loading').remove();
            handleEmptyResults();

        }
        else {

            //debug-table
            //var chart = new google.visualization.Table(document.getElementById('table_div'));
            // chart.draw(extdata, { width: 600, height: 300, is3D: true, title:'Numerical analysis'}); 
            //alert('handleExtQueryResponse!'+newdata.getNumberOfRows());
            $('#loading').remove();

            initChartArea();
            drawChart('pie_view1', user_chartype, extdata);
           
        }

    }


}

function resetVisOptions() {
    $('#versionsradio').prop('checked', true);
    $('.vsoption').prop('checked', false);
    $('#vcbox_ALL').prop('checked', true);
    $('.choption').prop('checked', false);
    $('.tabright').prop('checked', false);
    $('#simple_changes_list').html('');
    $('#complex_changes_list').html('');
    $('#allcbox').prop('checked', true);

}


function resetVisResults() {
    //$('#pie_title').html('');
    //charttype
    //$('#pie_examplanations').html('');
    $('#pie_view1').html('');
    $('#limit_results').html('');
}
function fetchMyChangesList() {
    mysclist = '';
    mycclist = '';

    var ccchangesJSArray = [];
    var schangesJSArray = [];

    $(".tabright:checked").each(function() {
        //alert(this.value);

        if ($(this).hasClass("simclass")) {
            //alert(this.value +" in SIMPLE LIST");

            $('#sccbox').prop('checked', false);
            schangesJSArray.push(this.value);
        }
        if ($(this).hasClass("coclass")) {
            //alert(this.value +" in COMPLEX LIST");
            $('#cccbox').prop('checked', false);
            ccchangesJSArray.push(this.value);
        }

    });// end of each  



    mysclist = normalizeJSArray(schangesJSArray);
    mycclist = normalizeJSArray(ccchangesJSArray);


    if ($('#cccbox').prop('checked')) {
        mycclist = 'null';
    }

    if ($('#sccbox').prop('checked')) {
        mysclist = 'null';

    }
    if ($('#allcbox').prop('checked')) {
        mysclist = 'null';
        mycclist = 'null';

    }

}

function fetchVersionsList() {
    var versionsJSArray = [];
    vlist = '';
    $(".vsoption:checked").each(function() {
        //alert(this.value);
        if (this.value === 'ALL') {
            vlist = this.value;
        }

        else {

            versionsJSArray.push(this.value);
            vlist = normalizeJSArray(versionsJSArray);
        }

    }
    );// end of each       
    return vlist;
}
//Prepei na topotheththei sto telos
function handleVersionsInteraction() {
    //alert('handleVersionsInteraction');
    $('.vsoption').click(function() {
        var vsoption = $(this).val();
        //alert('vsoption:::'+vsoption);
        if (vsoption !== 'ALL') {
            $('#vcbox_ALL').prop('checked', false);
        }
        else {
            $('.vsoption').not($(this)).prop('checked', false);
        }

    });
}


function buildChartTypeCombo() {
    var table_option = "<option value=\"table\">Table</option>\n";
    if (custom_compare) {
        table_option = "";

    }
    var chart_combo = "<select id=\"chartcombo\" onchange=\"prepareChart(this.value);\">\n"
           // + "<option value=\"nop\">Select a different chart type</option>\n"
            + "<option value=\"pie\">Pie chart</option>\n"
            + "<option value=\"col\">Column chart</option>\n"
            + "<option value=\"bar\">Bar chart</option>\n"
            + "<option value=\"line\">Line chart</option>\n"
            + "<option value=\"area\">Area chart</option>\n"
            + table_option
            + "</select><br><br>\n";
    //alert(chart_combo);
    $('#charttype').append(chart_combo);
    $('#chartcombo').val(user_chartype);
    //user_chartype
}

function buildgroupByRadios() {

    var gpradios = "<form action=\"\"><p><b>Group by</b>:<br></p><p>\n\
<input checked=\"\" name=\"groupby\" value=\"versionsradio\" id=\"versionsradio\" type=\"radio\" class=\"radioclass\"/>\n" +
            "  Versions\n" +
            "  </p>\n" +
            "  <p>\n" +
            "  <input name=\"groupby\" value=\"changesradio\" id=\"changesradio\" type=\"radio\" class=\"radioclass\"/>\n" +
            "  Changes\n" +
            "</p></form>\n";
    //alert(gpradios);
    $('#groupby').html(gpradios); 

}

function buildEvoVersionRadio() {
    var vpairs = "<p><b>Select version(s)</b>:" + "</p>" + "<p>\n" +
            "<div class=\"scrollit\">" +
            "<input checked=\"\" name=\"vcbox\" id=\"vcbox_ALL\" type=\"checkbox\" value=\"ALL\" class=\"vsoption\">\n" +
            " ALL</p> \n";
    var buttons = getVisButtons(false);

    $.get('OntologyQueryServlet', {qtype: 'evoversions', dataset: changesGraph, datasetversions: versions, valuestype: "cbox"}).done(function(responseText) {

        /// $('#sel_versions').html(vpairs);
        $('#sel_versions').html(vpairs + responseText + "</div>"); 

        $('#sel_versions').append("<br>" + buttons);

        handleVersionsInteraction();
    });
}

function getVisButtons(on_custom_compare) {

    var buttons = "<input type=\"button\"  class=\"button\" value=\"OK\" onclick=\"getMoreVisualizations(" + on_custom_compare + ");\"/>\n" +
            "<input type=\"submit\"  class=\"button\" value=\"Reset\" onclick=\"resetVisOptions();\"/> ";
    return buttons;

}

function buildChangesRadio(on_custom_compare) {
    var buttons = getVisButtons(on_custom_compare);
    var init_html = "<p><b>Select change(s)</b>:" + "</p>" + "<p>\n" +
            "<div class=\"scrollit\"><input name=\"allcbox\" id=\"allcbox\" type=\"checkbox\" class=\"choption\" checked>\n" +
            " ALL</p> \n" +
            "<p>\n" +
            "<input name=\"sccbox\" id=\"sccbox\" type=\"checkbox\" class=\"choption\">\n" +
            " Simple Changes</p> \n" +
            "<div id=\"simple_changes_list\">\n" +
            "</div>\n" +
            "<p>\n" +
            " <input name=\"cccbox\" id=\"cccbox\" type=\"checkbox\" class=\"choption\">\n" +
            " Complex Changes  </p>\n" +
            "<div id=\"complex_changes_list\">  \n" +
            "</div>\n</div>";

    //alert(init_html);
    $('#sel_changes').html(init_html);
    if (on_custom_compare) {
        $('#sel_changes').append("<br>" + buttons);
    }
    handleChangesInteraction(versions, changesGraph); //JCH (move? from d2v.js (build checkboxes but the list of changes are fixed by fetchMyChangesList
    handleVersionsInteraction();

}

function prepareChart(chartype) {
    if (chartype !== 'nop') {

        var plotdata = '';
        if (extdata === null) {
            //switch to first data for custom compare case
            plotdata = data;
        }
        else {
            plotdata = extdata;
        }
        user_chartype = chartype;
        drawChart('pie_view1', chartype, plotdata);
    }
}


function initChartArea() {

    $('#pie_title').html('Graphical Analysis');
    $('#charttype').html('Select a chart type: ');
    buildChartTypeCombo();
    $('#pie_examplanations').html('Click on the chart for more details, or refine your criteria and press OK.');
}


function drawChart(divID, chart_type, data) {

 if (groupby === 'versionsradio') {
        chartTitle = "Number of occurences for the selected changes in the selected versions grouped by version pairs";
    }
    else{
        chartTitle = "Number of occurences for the selected changes in the selected versions grouped by changes";
    }


    /*var data = google.visualization.arrayToDataTable([
     ['Task', 'Hours per Day'],
     ['Work',     11],
     ['Eat',      2],
     ['Commute',  2],
     ['Watch TV', 2],
     ['Sleep',    7]
     ]);*/

    resetVisResults();
    var chart_options = {
         title: chartTitle,
        'width': defaultChartWidth,
        'height': defaultChartHeight,
        'backgroundColor': 'transparent',
         chartArea: {left: 40, top: 30},
        'pointSize':12, //megethos koukidas       
        hAxis : {
            slantedText: true
        //maxAlternation: 1, // use a maximum of 1 line of labels
        //showTextEvery: 1, // show every label if possible
        //minTextSpacing: 4 // minimum spacing between adjacent labels, in pixels
        //hAxis.maxTextLines
        }        
    };

    var chart;
    var piechart = new google.visualization.PieChart(document.getElementById(divID));
    var colchart = new google.visualization.ColumnChart(document.getElementById(divID));
    var barchart = new google.visualization.BarChart(document.getElementById(divID));
    var linechart = new google.visualization.LineChart(document.getElementById(divID));
    var areachart = new google.visualization.AreaChart(document.getElementById(divID));
    var tablechart = new google.visualization.Table(document.getElementById(divID));

    if (chart_type === 'pie') {
        chart = piechart;
  /*     google.visualization.events.addListener(chart, 'onmouseover', function(entry) {
         chart.setSelection([{row: entry.row}]);
         google.visualization.events.addListener(chart, 'onmouseout', function(entry) {
         chart.setSelection([]);
        });       
    }); */

     chart_options = {
        'width': defaultChartWidth,
        'height': defaultChartHeight,
        'backgroundColor': 'transparent',
        chartArea: {left: 40, top: 30},
        title: chartTitle,
        'is3D': true,
        'sliceVisibilityThreshold': 0 //ignore other at pie, etc
        //tooltip: { trigger: 'selection' }

    };


    }
    else if (chart_type === 'col') {
        chart = colchart;
    }
    else if (chart_type === 'bar') {
        chart = barchart;
    }
    else if (chart_type === 'line') {
        chart = linechart;
    }
    else if (chart_type === 'area') {
        chart = areachart;
    }
    else if (chart_type === 'table') {
        chart = tablechart;
        chart_options = {
        'backgroundColor': 'transparent',
         chartArea: {left: 40, top: 30}

    };
        $('#pie_examplanations').html("<br> Click on the table for more details or refine your criteria ans press OK.");
    }

    google.visualization.events.addListener(chart, 'error', errorHandler);
    //alert("drawchart::::::" +chart_options);
    chart.draw(data, chart_options);

    function selectHandler() {

        //EVO-CASE
        var selectedItem1 = chart.getSelection()[0];
        if (selectedItem1) {
            var piece = data.getValue(selectedItem1.row, 0);
            //alert('The user selected the piece.......'+piece);

            if (!custom_compare) {
                var myversion = '';
                var mychange = '';
                if (groupby === 'versionsradio') {
                    myversion = piece.substr(piece.lastIndexOf('S.') + 3); //myversion contains version label
                }
                else {
                    mychange = piece;
                }
                showLimitResults(myversion, mychange,false);

            }
            else {//CUSTOM-COMPARE
                mychange = piece;
                showLimitResults(vnew, piece, false); //vnew contains full version uri

            }
        }
    }
    google.visualization.events.addListener(chart, 'select', selectHandler);

}


function transposeDateDataTable(dt) {

    var ndt = new google.visualization.DataTable;

    ndt.addColumn('string', dt.getColumnLabel(0));
    for (var x = 1; x < dt.getNumberOfColumns(); x++)
        ndt.addRow([dt.getColumnLabel(x)]);
    for (var x = 0; x < dt.getNumberOfRows(); x++) {
        ndt.addColumn('number', dt.getValue(x, 0));
        for (var y = 1; y < dt.getNumberOfColumns(); y++)
            ndt.setValue(y - 1, x + 1, dt.getValue(x, y));
    }

    return ndt;
}


function getMySelfFrame()
{
    return (window.parent.document.getElementsByTagName("iframe"))[0];
}

function resizeIframe() {

    var myself = getMySelfFrame();
    var table = document.getElementById("resultstable");
    var evocriteria = document.getElementById("evotable_criteria");
    var evocriteria_width = evocriteria.offsetWidth;
    //alert('evocriteria_width:'+evocriteria.offsetWidth);
    var pie_view1_fixed_width = defaultChartWidth;
    var pie_view1_fixed_height = defaultChartHeight;
   
  
    if ((evocriteria_width + pie_view1_fixed_width) > barchart_fixed_width ){
        googlelementsMinHeight = googlelementsMinHeight + pie_view1_fixed_height;
    }
    //setting frame according to resultstable height
    
    myself.style.height = googlelementsMinHeight + table.offsetHeight; //1250 +300(pie_view
    myself.style.width = '400%';
    console.log('table.offsetHeight:' + table.offsetHeight);
    console.log('table.offsetWidth;:' + table.offsetWidth);
}

function showMoreButtons(myversion, mychange){

 var more_button = "<input type=\"button\" class=\"largebutton\" value=\"Show all results\" onclick=\"showLimitResults("+ "'" +myversion+ "'" +","+"'" +mychange+"'" +",true);\"/> ";
 var gototop_button  = "<input type=\"button\" class=\"largebutton\" value=\"Go to top\" onclick=\"parent.scrollTo(0,0);\"/> ";
 $('#show_more').html(more_button +gototop_button);

}

function showLimitResults(myversion, mychange, nolimit) {

    /*console.log('mysclist:' + mysclist);
     console.log('myvlist:' +myvlist);
     console.log ('mycclist:' +mycclist);
     console.log('(SELECTED)myversion:'+myversion);
     console.log('(SELECTED)mychange:' +mychange);
     */
    var limit = 'default';
    if (nolimit){
        limit = 'unlimited';
    }

    $('#limit_results').html(''); 

    $('#pie_view2').html(loadingimg + "<br>");
    $.get('OntologyQueryServlet', {qtype: 'limitresults', limit:limit, changename: mychange, newversion: myversion, oldversion: vold, datasetversions: versions, dataset: changesGraph, sclist: mysclist, cclist: mycclist, vlist: myvlist, sradio: sradio, groupby: groupby}).done(function(responseText) {
        //alert(responseText);
        if (responseText !== 'empty') {


            $('#limit_results').append(responseText);
            resizeIframe();
            showMoreButtons(myversion, mychange);


        }
        else {
            handleEmptyResults();
        }

        $('#loading').remove();

    });
}

function handleEmptyResults(){
    
    showTopDialog('vision_examplanations', 'No results to show!');
    resetVisResults();
    $('#pie_examplanations').html('');
}

function errorHandler(errorMessage) {
    //curisosity, check out the error in the console
    console.log(errorMessage);
    //alert(errorMessage);
    //simply remove the error, the user never see it
    $('#vision_title').html('');
    //showTopDialog('vision_examplanations', 'No results to show!');
    handleEmptyResults();
    google.visualization.errors.removeError(errorMessage.id); //shalow google message

}




