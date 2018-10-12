/**
 *
 * @author Ioannis Chrysakis (hrysakis@ics.forth.gr)
 * This javascript file contains the main logic to build the core D2V layout.
 */
var selected_change; //keeps the just added selected_change name
var scno = 0;  //keeps the number of assigned simple changes
var jfno = 1; // keeps the number of added join filters
var sfno = 1; // keeps the nunber of added selection filters
var vfno = 1; // keeps the number of added version filters
var paramno = 0; // keeps the number of added parameters
var selected_change_alt_name; //keeps the alternative name of a selected change
var selected_radio, vfrom, vto; //keeps value for selected radio button of evolution/custom_compare and selected old/new version
var sclist = ''; //keeps the list of selected simple changes
var cclist = ''; //keeps the list of selected complex changes
var username; //keeps the username
var userDatasetURI; //keeps the specified dataset URI for a user
var userChangesOntology;//keeps the specified changes ontology for a user
var paramNames = [];   //keeps a list of complex change parameter names
var json_string; //keeps the json string that represents the sparql query for each complex change 

function createCCTables(on_edit, prev_ccname, selectedtd) {//called on save advanced definition option (link) or on edit advanced definition
    resetTables();
    if (!on_edit) {
        $('.clickbuttons1').removeClass('fillmarktd');
        $(selectedtd).addClass('fillmarktd');
    }
    if (checkDataset()) {

        var basics_html =
                "<form id=\"ccbasicsform\" name=\"ccbasicsform\" method=\"post\"><table border=\"1\" cellpadding=\"10\" width=\"500\">\n" +
                " <tbody><tr>\n" +
                "<th width=\"236\">Basic Complex change information</th></tr><tr>\n" +
                "<td width=\"236\">\n" +
                "\n" +
                "\n" +
                "<p><label for=\"cc_name\">Name*:</label></p>\n" +
                "<p>\n" +
                " <input class=\"input-ext\" name=\"cc_name\" id=\"cc_name\" type=\"text\">\n" +
                "</p>\n" +
                "<label for=\"priority\"><p>Priority*:</label></p>\n" +
                "<p>\n" +
                "\n" +
                "<input class=\"input-ext\" name=\"priority\" id=\"priority\" type=\"text\">\n" +
                "</p> \n" +
                "<div id=\"sc-combo\">\n" +
                "<p><label for=\"assigned_sc\">\n" +
                "Assign Simple Change</label></p>\n" +
                "<select class=\"select-ext\" name=\"assigned_sc\" id=\"assigned_sc\">\n" +
                "</select>\n" +
                "<input class=\"AddRemoveButton\" type=\"button\" onclick=\"addSCBlock(false,'')\" name=\"addSC\" value=\"+\">\n" +
                "</div>\n" +
                "<div id=\"cc-param\">" +
                "<p><label for=\"ccparam\">Parameter*:\n" +
                "</label></p> \n" +
                "<input class=\"input-ext\" name=\"ccparam\" id=\"ccparam\" type=\"text\">\n" +
                "<input id=\"addparambutton\" class=\"AddRemoveButton\" type=\"button\" onclick=\"addExtCCParams(false)\" name=\"add\" value=\"+\">\n" +
                "<label for=\"cc_desc\"><p>Description:\n" +
                "</label></p> \n" +
                "<textarea rows=\"4\" class=\"input-ext\" id=\"cc_desc\" name=\"cc_desc\" cols=\"50\"></textarea>\n" +
                "</div>\n" +
                "</tr>\n" +
                "<tr>\n" +
                "\n" +
                "</tr>\n" +
                "</tbody></table>\n" +
                "</form>";

        getSimpleChanges(); //fill-in assign simple changes combo...

        var init_param_html =
                "<table border=\"1\" cellpadding=\"10\" width=\"500\">\n" +
                "<tbody><tr>\n" +
                "<th>Parameters</th></tr><tr>\n" +
                "<td>\n" +
                "<div id=\"params-filters\">\n" +
                "</div>" +
                "<div id=\"update-params-btn\">\n" +
                "</div>" +
                "</tr>\n" +
                "<tr>\n" +
                " \n" +
                "</tr>\n" +
                "<tr>\n" +
                "\n" +
                "</tr>\n" +
                "</tbody></table>";

        var ver_filter_init = getVerFilterHTML();

        var init_vfliters_html = "<table border=\"1\" cellpadding=\"10\" width=\"500\">\n" +
                "<tbody><tr>\n" +
                "<th>Version Filters</th></tr><tr>\n" +
                "<td>\n" +
                "<div id=\"version-filters\">\n" +
                ver_filter_init +
                "</div>" +
                "</div>" +
                "</tr>\n" +
                "<tr>\n" +
                " \n" +
                "</tr>\n" +
                "<tr>\n" +
                "\n" +
                "</tr>\n" +
                "</tbody></table>";

        var sbutton = "<input name=\"saveExtButton\" id=\"saveExtButton\" class=\"largebutton button\" value=\"Save\" onclick=\"saveExtCCDef(" + on_edit + "," + "'" + prev_ccname + "'" + ")\" type=\"submit\">\n";
        var rbutton = "<input name=\"resetExtButton\" id=\"resetExtButton\" class=\"largebutton button\" value=\"Reset\" onclick=\"resetTables()\" type=\"button\">\n";
        var viewQdiv = "<div id=\"viewQ\" class=\"inside\"></div>";


        var final_html = basics_html + init_param_html + init_vfliters_html + sbutton + rbutton + viewQdiv;
        resetTables();
        $('#cc_types').html(final_html);

    }
}

function addVerFilterBlock() {
    vfno++;
    var ver_filter_init = getVerFilterHTML();
    var ver_filter_html =
            "<div>" +
            ver_filter_init +
            "<input class=\"AddRemoveButton\" type=\"button\"  name=\"removeVF\" value=\"-\" onclick=\" $(this).parent().remove()\"></div>";
    "</div>" +
            $("#version-filters").append(ver_filter_html);
//getSimpleChangeSpecifiedParameters(schange, "", param_combo_id,"",false,true);

    updateDynamicCombos(false);
}

function getVerFilterHTML() {
    var html =
            "<div class=\"vfilter\" id=\"vfilter" + vfno + "\">\n" +
            "<span class=\"span_vf_labels\"><label for=\"vfilsub" + vfno + "\"> Subject </label> </span> <span class=\"span_vf_labels\"> <label for=\"vfilprop" + vfno + "\"> Predicate </label> </span> <span class=\"span_vf_labels\"> <label for=\"vfilobj" + vfno + "\"> Object </label> </span> <span class=\"span_vf_labels\"> <label for=\"appear" + vfno + "\"> Appearance </label></span></p>" +
            "<select style=\"width:100px; float: left;\" onchange=\"this.nextElementSibling.value=this.value; this.nextElementSibling.title=this.value;\" name=\"vfilsub" + vfno + "\" id=\"vfilsub" + vfno + "\" class=\"dynamic_combo vfilter_row" + "\">" +
            "<input onchange=\"this.title=this.value;\" id=\"vsub_text" + vfno + "\" style=\"width: 80px; margin-left: -99px; margin-top: 1px; border: none; float: left;\" class=\"vfilter_row" + "\"/>" +
            "</select>\n" +
            "<select style=\"width:100px; float: left;\" onchange=\"this.nextElementSibling.value=this.value; this.nextElementSibling.title=this.value;\" name=\"vfilprop" + vfno + "\" id=\"vfilprop" + vfno + "\" class=\"dynamic_combo vfilter_row" + "\">" +
            "<input onchange=\"this.title=this.value;\" id=\"vprop_text" + vfno + "\" style=\"width: 80px; margin-left: -99px; margin-top: 1px; border: none; float: left;\" class=\"vfilter_row" + "\"/>" +
            "</select>\n" +
            "<select style=\"width:100px; float: left;\" onchange=\"this.nextElementSibling.value=this.value; this.nextElementSibling.title=this.value;\" name=\"vfilobj" + vfno + "\" id=\"vfilobj" + vfno + "\" class=\"dynamic_combo vfilter_row" + "\">" +
            "<input onchange=\"this.title=this.value;\" id=\"vobj_text" + vfno + "\" style=\"width: 80px; margin-left: -99px; margin-top: 1px; border: none; float: left;\" class=\"vfilter_row" + "\"/>" +
            "</select>\n" +
            "<select onchange=\"this.title=$(this).children('option:selected').attr('title');\" style=\"width:100px\" name=\"appear" + vfno + "\" id=\"appear" + vfno + "\" class=\"vfilter_row" + "\"\">" +
            "<option value=\"nop\"> </option>\n" +
            "<option title=\"exists in New Version\" value=\"EXISTS_IN_V2\">in Vnew</option>" +
            "<option title=\"exists in Old Version\" value=\"EXISTS_IN_V1\">in Vold</option>" +
            "<option title=\"Not exists in New Version\" value=\"NOT_EXISTS_IN_V2\">Not in Vnew</option>" +
            "<option title=\"Not exists in Old Version\" value=\"NOT_EXISTS_IN_V1\">Not in Vold</option>" +
            "</select>\n" +
            "<label for=\"vsub_text" + vfno + "\" class=\"hidden\">''</label><label for=\"vprop_text" + vfno + "\"class=\"hidden\">''</label><label for=\"vobj_text" + vfno + "\"class=\"hidden\">''</label><input class=\"AddRemoveButton\" type=\"button\" id=\"vbut" + vfno + "\" value=\"+\" onclick=\"addVerFilterBlock()\">\n";
    return html;
}


function addSCBlock(on_edit, selected_change, sc_uri, sel_filters, join_filters, is_optional) {
    scno++;

    var selected_change_full_name;

    if (!on_edit) {
        selected_change = $('#assigned_sc').val();
        selected_change_full_name = scno + ":" + selected_change;
    }

    else {
        selected_change_full_name = sc_uri;
    }

    // alert('selected_change_full_name'+selected_change_full_name);

    if (!selected_change !== '' && selected_change !== 'nop' && selected_change !== null) {

        var joinfilterID1 = "join_combo" + scno;
        var joinfilterID2 = "join_combo2" + scno;
        var selfilterID = "select_combo" + scno;
        var selvalueID = "select_tvalue" + scno;
        var optionalID = "optional_" + selected_change_alt_name;

        selected_change_alt_name = "_" + scno + "_" + selected_change;
        //alert('scno'+scno);
        var pbuttonID = "plusbutton" + sfno + selected_change_alt_name;
        //JCH: div ids cannot contains : for this reason we use the selected_change_alt_name instead of selected_change_full_name


        //name for joincombo2 used for dynamic_update
        var join_combo1 = "<select onchange=\"this.title=this.value;\" style=\"width:140px\" name=\"join_combo" + scno + "\" id=\"" + joinfilterID1 + "\" class=\"join_combo" + selected_change_alt_name + "\">\n";
        var join_combo2 = "<select onchange=\"this.title=this.value;\" style=\"width:180px\" name=\"" + selected_change_full_name + "\" id=\"" + joinfilterID2 + "\" class=\"dynamic_combo join_combo" + selected_change_alt_name + "\">\n";
        var remove_button = "<input disabled class=\"AddRemoveButton\" type=\"button\"  name=\"removeSJF\" value=\"-\" onclick=\" $(this).parent().remove()\">";
        var join_add_button = "<input class=\"AddRemoveButton\" type=\"button\"  id=\"" + pbuttonID + "\" value=\"+\" onclick=\"addJoinFilterBlock('" + selected_change + "'" + "," + "'" + selected_change_alt_name + "'" + "," + "'" + selected_change_full_name + "'" + ")\">\n" + remove_button;

        /*JCH:disabled1*/
        if ($('.scblock').length < 1) { //no join combo for the first assigned sc

            //TOFIX initially disabled + dynamic
            join_combo1 = "<select onchange=\"this.title=this.value;\" style=\"width:140px\" disabled=\"disabled\" name=\"join_combo" + scno + "\" id=\"" + joinfilterID1 + "\"  class=\"join_combo" + selected_change_alt_name + "\">\n";
            join_combo2 = "<select onchange=\"this.title=this.value;\" style=\"width:180px\" disabled=\"disabled\" name=\"" + selected_change_full_name + "\" class=\"join_combo\" id=\"" + joinfilterID2 + "\"class=\"join_combo" + selected_change_alt_name + "\">\n";
            join_add_button = "<input disabled class=\"AddRemoveButton\" type=\"button\" value=\"+\" onclick=\"addJoinFilterBlock('" + selected_change + "'" + "," + "'" + selected_change_alt_name + "'" + "," + "'" + selected_change_full_name + "'" + ")\">\n";

        }

        var filters_html =
                "<label for=\"" + joinfilterID2 + "\" class=\"hidden\">''</label><div class=\"scblock\" id=\"" + selected_change_alt_name + "\"><table border=\"1\" cellpadding=\"10\" width=\"420\">\n" +
                " <tbody><tr>\n" +
                "<th class=\"simplechangeclass\">" + selected_change_full_name + "</th>" +
                "<input class=\"AddRemoveButton\" type=\"button\" onclick=\"deleteSCBlock('" + selected_change_alt_name + "')\" value=\"-\" style=\"float: right;\">\n" +
                "</tr><tr>\n" +
                "<td>\n" +
                "\n" +
                "<div>\n" +
                "<div id=\"join_filters" + selected_change_alt_name + "\">" +
                "<p><label for=\"" + joinfilterID1 + "\">Join Filter</label></p>\n" +
                join_combo1 +
                "    <option value=\"\"></option>\n" +
                "    <option value=\"Select a parameter\">Select a parameter</option>\n" +
                "  </select>\n" +
                join_combo2 +
                "    <option value=\"\"></option>\n" +
                "    <option value=\"Select a parameter\">Select a parameter</option>\n" +
                "  </select>\n"
                + join_add_button +
                "</div>\n" +
                "<div id=\"sel_filters" + selected_change_alt_name + "\">" +
                "<p><label for=\"" + selfilterID + "\">Selection Filter</label></p>\n" +
                "<select onchange=\"this.title=this.value;\" style=\"width:140px\" name=\"select_combo" + scno + "\" id=\"" + selfilterID + "\" class=\"select_combo" + selected_change_alt_name + "\">\n" +
                "<option value=\"\"></option>\n" +
                "<option value=\"Select a parameter\">Select a parameter</option>\n" +
                "</select>\n" +
                "<label for=\"" + selvalueID + "\" class=\"hidden\">''</label><input onchange=\"this.title=this.value;\" style=\"width:180px\" name=\"select_tvalue" + scno + "\" id=\"" + selvalueID + "\" type=\"text\" class=\"smallsize select_combo" + selected_change_alt_name + "\">\n" +
                "<input class=\"AddRemoveButton\" type=\"button\" value=\"+\" onclick=\"addSelFilterBlock('" + selected_change + "'" + "," + "'" + selected_change_alt_name + "'" + "," + "'" + selected_change_full_name + "'" + ")\">\n" +
                remove_button +
                "</div>\n" +
                "</div>\n" +
                "<br>" +
                "<p>" +
                "<input name=\"" + selected_change_full_name + "\" id=\"" + "" + optionalID + "\" type=\"checkbox\" class=\"optionalclass\"><label for=\"" + optionalID + "\">Optional Detection</label>" +
                "</p>" +
                "</tr>\n" +
                "<tr>\n" +
                " \n" +
                "</tr>\n" +
                "<tr>\n" +
                "\n" +
                "</tr>\n" +
                "</tbody></table></div>\n";

        $('#cc_params').append(filters_html);

        getSimpleChangeParameters(selected_change, selected_change_full_name, joinfilterID1, selfilterID);
        //getSimpleChangeSpecifiedParameters(selected_change, selected_change_full_name, joinfilterID2, "", true);

        $('#assigned_sc').val(''); //clear the name of just added sc
    }



    if (on_edit) {
        if (is_optional) {
            $("#" + optionalID).prop('checked', true);
        }

        //JCH-TODEBUG: to true prepei na ginei false otan exw rename
        //Recheck when I have only join filter.

        updateDynamicCombos(true, on_edit);// forced synchronous call

        //alert("NOW CHECK:" +$('#'+joinfilterID2).html());
        retrieveSCFilter(selected_change, selected_change_alt_name, selected_change_full_name, sel_filters, selfilterID, selvalueID, true);
        retrieveSCFilter(selected_change, selected_change_alt_name, selected_change_full_name, join_filters, joinfilterID1, joinfilterID2, false);


    } //end-of on edit case

    else {
        updateDynamicCombos(true, on_edit);
    }
//setParamFieldStatus();
}

function retrieveVerFilter(ver_filters) {
    //console.log("VF:::::::::" + ver_filters.toString());
    var param_val;
    var sub, pred, obj, pres;
    var flength = ver_filters.length;
    //console.log('flength:'+flength);
    for (var i = 0; i < flength; i++) {
        param_val = ver_filters[i];
        //for(var key in param_val){
        sub = param_val["Subject"];
        pred = param_val["Predicate"];
        obj = param_val["Object"];
        pres = param_val["Presence"];
        if (flength === 1 || i === 0) { //set single version filter (has been already created)
            setVerFilter(vfno, sub, pred, obj, pres);

        }
        else {

            addVerFilterBlock();
            setVerFilter(vfno, sub, pred, obj, pres);
        }

    }
}

function setVerFilter(vfno, sub, pred, obj, pres) {
    //console.log('setting ver filter:'+vfno);
    //alert('vfno:'+vfno);
    $("#vsub_text" + vfno).val(sub);
    $("#vprop_text" + vfno).val(pred);
    $("#vobj_text" + vfno).val(obj);
    $("#appear" + vfno).val(pres);
}

//used to retrieve sel/join filters on edit for each simple change
function retrieveSCFilter(selected_change, selected_change_alt_name, selected_change_full_name, filters_array, comboID, comboORtextID, selfiltercase) {


    var deleq, combo_val, comboORtext_val;
    // console.log("filters_array"+filters_array);
    if (filters_array !== undefined && flength !== 0) {
        //Parse filters object
        var flength = filters_array.length;

        for (var i = 0; i < flength; i++) {
            var sel_val = filters_array[i].toString();
            deleq = sel_val.indexOf("=");

            combo_val = sel_val.substring(0, deleq);
            comboORtext_val = sel_val.substring(deleq + 1);
            comboORtext_val = comboORtext_val.replace(/'|<|>/g, ""); //replaces ' or < or > //JCH:replace three characters at once using OR
            //console.log($("#"+comboORtextID).html());
            //alert(combo_val+" | "+comboORtext_val);
            //alert("...."+flength);

            if (flength === 1 || i === 0) {

                //JCH: ebal trim giati sto Ideagarden oi times erxotan me kena

                $('#' + comboID).val(combo_val.trim());
                //console.log("Setting to:" + comboORtextID + "|val|" + comboORtext_val + "|");
                //alert("Setting to:" + comboORtextID + "|val|" + comboORtext_val + "|");
                //alert( $('#' + comboORtextID).html());
                $('#' + comboORtextID).val(comboORtext_val.trim());
            }
            else {
                if (selfiltercase) {
                    addSelFilterBlock(selected_change, selected_change_alt_name, selected_change_full_name, combo_val, comboORtext_val);
                }
                else {
                    addJoinFilterBlock(selected_change, selected_change_alt_name, selected_change_full_name, combo_val, comboORtext_val);
                    //$('#' + comboORtextID).val(comboORtext_val.trim());
                }
            }

        }
    }

}

function addJoinFilterBlock(selected_change, selected_change_alt_name, selected_change_full_name, join_combo_val, join2_combo_val) {

    jfno++;

    var joinfilterID1 = "join_combo" + jfno + selected_change_alt_name;
    var joinfilterID2 = "join_combo2" + jfno + selected_change_alt_name;
    var selfilterID = "select_combo" + sfno + selected_change_alt_name;
    var jpno = jfno + 1;
    var pbuttonID = "plusbutton" + jpno + selected_change_alt_name;

    var remove_button = "<input class=\"AddRemoveButton\" type=\"button\"  name=\"removeSJF\" value=\"-\" onclick=\" $(this).parent().remove(); disableLastJoinFilter();\">";
    var join_add_button = "<input class=\"AddRemoveButton\" type=\"button\" id=\"" + pbuttonID + "\" value=\"+\" onclick=\"addJoinFilterBlock('" + selected_change + "'" + "," + "'" + selected_change_alt_name + "'" + "," + "'" + selected_change_full_name + "'" + ")\">\n" + remove_button;
    var join_combo_left = "<label for=\"" + joinfilterID1 + "\" class=\"hidden\">''</label><select onchange=\"this.title=this.value;\" style=\"width:140px\" name=\"join_combo" + jfno + "\" id=\"" + joinfilterID1 + "\" class=\"join_combo" + selected_change_alt_name + "\">\n";
    var join_combo_right = "<label for=\"" + joinfilterID2 + "\" class=\"hidden\">''</label><select onchange=\"this.title=this.value;\" style=\"width:180px\" name=\"join_combo2" + jfno + "\" id=\"" + joinfilterID2 + "\" class=\"join_combo" + selected_change_alt_name + " dynamic_combo" + "\">\n";
    var join_filter_html = "<div><p>Join Filter</p>\n" +
            join_combo_left +
            "    <option value=\"\"></option>\n" +
            "    <option value=\"Select a parameter\">Select a parameter</option>\n" +
            "  </select>\n" +
            join_combo_right +
            "    <option value=\"\"></option>\n" +
            "    <option value=\"Select a parameter\">Select a parameter</option>\n" +
            "  </select>\n" +
            join_add_button;

    $("#join_filters" + selected_change_alt_name).append(join_filter_html);


    getSimpleChangeParameters(selected_change, selected_change_full_name, joinfilterID1, selfilterID);
    getSimpleChangeSpecifiedParameters(selected_change, selected_change_full_name, joinfilterID2, "", true);
    //setParamFieldStatus();

    if (join_combo_val !== undefined && join2_combo_val !== undefined) //on_edit case
    {
        $('#' + joinfilterID1).val(join_combo_val);
        $('#' + joinfilterID2).val(join2_combo_val);
    }

}

//used to add a selection filter (last two args used on edit case of advanced definition)
function addSelFilterBlock(selected_change, selected_change_alt_name, selected_change_full_name, sel_combo_val, sel_text_val) {

    sfno++;
    var selfilterID1 = "select_combo" + sfno + selected_change_alt_name;
    var selTvalueID = "select_tvalue" + sfno + selected_change_alt_name;


    var sel_filter_html = "<div><p><label for=\"" + selfilterID1 + "\">Selection Filter</label></p>\n" +
            "  <select onchange=\"this.title=this.value;\" style=\"width:140px\" name=\"" + selfilterID1 + "\" id=\"" + selfilterID1 + "\" class=\"select_combo" + selected_change_alt_name + "\">\n" +
            "    <option value=\"\"></option>\n" +
            "    <option value=\"Select a parameter\">Select a parameter</option>\n" +
            "  </select>\n" +
            "<label for=\"" + selTvalueID + "\" class=\"hidden\">''</label><input onchange=\"this.title=this.value;\" style=\"width:180px\" name=\"" + selTvalueID + "\" id=\"" + selTvalueID + "\" type=\"text\" class=\"smallsize select_combo" + selected_change_alt_name + "\">\n" +
            "<input class=\"AddRemoveButton\" type=\"button\" value=\"+\" onclick=\"addSelFilterBlock('" + selected_change + "'" + "," + "'" + selected_change_alt_name + "'" + "," + "'" + selected_change_full_name + "'" + ")\">\n" +
            "<input class=\"AddRemoveButton\" type=\"button\"  name=\"removeSF\" value=\"-\" onclick=\" $(this).parent().remove()\"></div>";

    //alert('append to' +"#sel_filters"+scno);
    $("#sel_filters" + selected_change_alt_name).append(sel_filter_html);

    getSimpleChangeParameters(selected_change, selected_change_full_name, '', selfilterID1);

    if (sel_combo_val !== undefined && sel_text_val !== undefined) //on_edit case
    {
        $('#' + selfilterID1).val(sel_combo_val);
        $('#' + selTvalueID).val(sel_text_val);
    }

}

function updateDynamicCombos(excludeMe, on_edit) {
    //JCH: currently on_edit arg is not used   
    //alert('updateDynamicCombos:'+excludeMe +":"+on_edit)   
    $.ajaxSetup({
        async: false
    });
    var currentval = '';
    var currentnam = '';
    var prefnam = '';
    var selected_change = '';  //ADD_SUPERCLASS
    var selected_change_full_name = ''; //1:ADD_SUPERCLASS


    $('.dynamic_combo').each(function() { //JCH:get all IDs of a class
        currentval = $("#" + this.id).val();
        currentnam = $("#" + this.id).attr('name'); //JCH:get name of specified id

        //console.log('currentnam'+currentnam);
        if (excludeMe) { //simple changes combo - exlude myself

            selected_change_full_name = currentnam;
            selected_change = currentnam.substring(2);
            //.console.log("selected_change:"+selected_change);
            //alert("checking......"+this.id);
            //console.log('currentval'+currentval);
            getSimpleChangeSpecifiedParameters(selected_change, selected_change_full_name, this.id, currentval, true, on_edit);

        }
        else {
            // selected_change = currentnam; //paremeters combo  

            //console.log('currentval NOW:'+currentval);
            getSimpleChangeSpecifiedParameters("", "", this.id, currentval, false, on_edit);

        }
    });

    $.ajaxSetup({
        async: true
    });
}

//Called to include param names in dynamic combo that are built in version filters
function getParamNameOptionVals() {
    //for each paramnames
    var append_values_html = "";
    $(".paramnames").each(function() {
        var paramnam = $("#" + this.id).val();
        if (paramnam !== '' && paramnam !== null) {
            append_values_html = append_values_html + "<option value=\"" + paramnam + "\">" + paramnam + "</option>\n";
        }
    });
    return append_values_html;
}

//fills-in join combo2 (right) and cc parameter combo  
function getSimpleChangeSpecifiedParameters(chname, chfullname, comboID, selectedVal, excludeme, on_edit) { // called in the building of add advanced definition

//alert('comboID'+comboID+" EXC:" +excludeme);

    var excludechange = 'no';


    if (excludeme) {
        excludechange = 'yes';
    }
//console.log('excludeMe'+excludeme);
//console.log('comboID'+comboID);
    var assignedscnames = "";
    var comboprefix = comboID.substring(0, 4);
    var changesontology = $('#sel_dataset').val();
    var datasetURI = getDatasetURI(changesontology);

    $(".simplechangeclass").each(function() {
        //console.log($(this).html());
        assignedscnames = assignedscnames + "$" + $(this).html(); //stores all assigned sc in this var
        // alert(assignedscnames);
    });

    $.ajaxSetup({
        async: false
    });
    $.get('OntologyQueryServlet', {qtype: 'scspecparams', datasetversions: datasetURI, dataset: changesontology, chname: chname, chfullname: chfullname, excludeme: excludechange, schanges: assignedscnames}).done(function(responseText) {


        $('#' + comboID).html(responseText); //<- all params except mine

        if (comboprefix === 'para') { //belongs to a combo of parameter filters

            $('#' + comboID).append("<option value=\"\">Assigned from version filter</option");
            //console.log("ID:"+comboID+' :selectedVal:'+selectedVal);

        }

        if (comboprefix === 'vfil') { //belongs to a combo of version filters

            $('#' + comboID).append(getParamNameOptionVals());

        }

        // if(selectedVal !== null){

        $('#' + comboID).val(selectedVal);

        // }

        //console.log(comboID+'=>Setting selectedVal:'+selectedVal);
        //console.log("HTML"+$('#'+comboID).html());


    });
    $.ajaxSetup({
        async: true
    });

}

//used when assigned simple changes are mandatory and addparambutton has been disabled on default
function setParamFieldStatus() {
    if ($('.scblock').length < 1) {
        $('#addparambutton').attr('disabled', 'disabled'); //JCH: disable button with specified id
    }
    else {
        $('#addparambutton').removeAttr('disabled'); // JCH: enable + button
    }
}
function deleteSCBlock(blockID) { // removes simple change filter block
    //scno--; Den meiwnw giati meta prepei na kanw epanadiataksh arithmon

    //alert(blockID);
    $("#" + blockID).remove();
    //setParamFieldStatus();
    //$(this).closest(".scblock").remove();

    updateDynamicCombos(true);
    disableLastJoinFilter();




}

// disable last join filter if it is the last one (called in each delete sc or delete join filter block)
function disableLastJoinFilter() {

//JCH:disabled1
    var lastJoinFilterID;
    var plusbuttonID;
    if ($('.scblock').length < 2) { //if only one assigned sc

        $(".scblock").each(function() {

            if ($('.dynamic_combo').length < 2) { // if only one join filter exists
                lastJoinFilterID = $('.dynamic_combo').attr('id');
                $("#" + lastJoinFilterID).prop('disabled', true);
                plusbuttonID = $("#" + lastJoinFilterID).next().attr('id');
                //alert('disabled:'+lastJoinFilterID);
                $("#" + plusbuttonID).prop('disabled', true);
                //alert('disabled:'+plusbuttonID);
            }
        }
        );

    }
}

//add a complex change parameter in extended def
function addExtCCParams(on_edit, param_name, param_value) {
    var param_html = "";
    var schange;
    paramno++;
    if (!on_edit) { //create new param
        param_name = $('#ccparam').val();

        if (checkUniqueParamName(param_name)) {
            schange = $('#assigned_sc').val();
            param_html = getParamHTML(schange, param_name, "");

            $('#params-filters').append(param_html);
            $('#ccparam').val(''); //clear the new param
            updateDynamicCombos(false, on_edit);
        }
        addUpdateParamButton();
    }
    else { // on edit
        //alert(param_value);
        /*if (param_value === '') {
         param_value = 'Assigned from version filter';
         }*/
        param_html = getParamHTML("", param_name, param_value);
        //alert(param_html);
        $('#params-filters').append(param_html);
        updateDynamicCombos(false, on_edit);

    }

//checkExtMandatoryFields (ccName, ccPriority);
    paramNames.push(param_name);
}

function getParamHTML(schange, param_name, param_value) {
    var removeBtn = "<input class=\"AddRemoveButton\" type=\"button\"  name=\"rem\" value=\"-\" onclick=\" $(this).parent().remove()\"></div>\n";
    var selected_op_val = "";
    if (param_value !== '') { //comes from edit
        selected_op_val = "<option selected value=\"" + param_value + "\">" + param_value + "</option>\n";
    }
    else {
        selected_op_val = "<option selected value=\"\">Assigned from version filter</option>\n";
    }

    //todo check how to set values and to fetch at once all other values
    //console.log("name/value"+param_name+"/"+param_value);
    var param_html =
            "<div id=\"div_param_" + paramno + "\"><span class=\"span_param_labels\"><label for=\"param_name" + paramno + "\"> Parameter </label> </span>" +
            "<span class=\"span_param_labels\"> <label for=\"param_filter" + paramno + "\"> Value:</label></span>\n" +
            "<input style=\"width:200px\" name=\"param_name" + paramno + "\" id=\"param_name" + paramno + "\" type=\"text\" class=\"smallsize paramnames\" value=\"" + param_name + "\">\n" +
            "<select class=\"dynamic_combo\" style=\"width:200px\" id=\"param_filter" + paramno + "\" name=\"" + schange + "\">\n" +
            selected_op_val +
            " </select>\n" +
            removeBtn +
            "\n";
    return param_html;

}

function checkUniqueParamName(paramname) {

    var retval = true;
    var msg = '<p>Cannot add a parameter with the same name!</p>';
    $(".paramnames").each(function() {

        if (($("#" + this.id).val()) === paramname) {
            showDialog('dialogmsg', msg);
            retval = false; //JCH: Always return a value in a jquery loop through a variable

        }
    });
    return retval;
}



function buildCCTypeMenu(selectedtd) { //called in create change option (link)
    resetTables();
    var js_json = "<script type=\"text/javascript\" src=\"js/jquery.form.js\" ></script>\n" + "<script src=\"js/jsonFileUploadScript.js\"></script>";

    $('.clickbuttons1').removeClass('fillmarktd');
    $(selectedtd).addClass('fillmarktd');

    if (checkDataset()) {
        var html_code = "<table width=\"180\" cellspacing='0' class=\"inside\"> <!-- cellspacing='0' is important, must stay -->\n" +
                "    <tr>\n" +
                "	  <th width=\"174\">1.Define by using a template of type</th></tr>\n" +
                "    \n" +
                "<tr>\n" +
                "  <td class=\"clickable  clickbuttons2\" onclick=\"getSimpleChangesTemplates('add',this);\"><a href=\"#\"><div class=\"menu\">Addition</div><!--img src=\"images/add.png\" width=\"27\" height=\"27\" /--></a></td></tr>\n" +
                "	<tr class='even'>\n" +
                "  <td class=\"clickable  clickbuttons2\" onclick=\"getSimpleChangesTemplates('del',this);\"><a href=\"#\"><div class=\"menu\">Deletion</div><!--img src=\"images/del.png\" width=\"27\" height=\"27\" /--></a></td></tr>\n" +
                "\n" +
                "	<tr>\n" +
                "  <td class=\"clickable  clickbuttons2\" onclick=\"getSimpleChangesTemplates('edit', this);\"><a href=\"#\"><div class=\"menu\">Update</div><!--img src=\"images/edit.png\" width=\"27\" height=\"27\" /--></a></td></tr>\n" +
                "\n" +
                "</table>\n";


        var json_code = "<div id=\"jsonf\"><table width=\"180\" cellspacing='0'> <!-- cellspacing='0' is important, must stay -->\n" +
                "    <tr>\n" +
                "	  <th width=\"174\">2. Define by using a JSON format</th></tr>\n" +
                "<tr><td>" +
                "<label for=\"myfile2\">Upload JSON:</label><form class=\"uploadform2\" id=\"UploadForm2\" action=\"UploadFile\" method=\"post\"\n" +
                "		enctype=\"multipart/form-data\">\n" +
                "		<input type=\"file\" size=\"60\" id=\"myfile2\" name=\"myfile2\"> <input\n" +
                "			type=\"submit\" value=\"Upload\">\n" +
                "\n" +
                "		<div id=\"message2\"></div>\n" +
                "	</form><br>" +
                "<label for=\"jsonstring\">Or enter the JSON definition here:</label><textarea id=\"jsonstring\" rows=\"8\" cols=\"70\"></textarea>" +
                "<br><br><input type=\"button\" class=\"Button\" value=\"Save\" onclick=\"saveCCDefByJSON();\">" +
                "</td></tr>" +
                "</table>" +
                "</div>\n";


        $('#cc_menu').html(js_json + html_code + json_code);





    }
}

function showCredits() {
    var copyright =
            "<table border=\"1\" cellpadding=\"10\" width=\"550\">\n" +
            "<tr>\n" +
            "<td style=\"text-align:justify\">\n" +
            "\n" +
            "\n" +
            "<img alt=\"forth\" src=\"images/forth_logo.png\" align=\"left\" width=\"60\" height=\"60\">" +
            "<strong><p align=\"center\">Copyright 2015, FORTH-ICS, All Rights Reserved.<br></p></strong>\n" +
            "<strong>Yannis Roussakis, Ioannis Chrysakis, Kostas Stefanidis and Giorgos Flouris.</strong>" + "<br>" +
            "<strong>Foundation for Research and Technology Hellas, Institute of Computer Science.</strong>" +
            "</p>\n" +
            "<br>" +
            "<br><p><a target=\"blank\" href=\"video.html\">Click here</a> for a <strong>video</strong> showing the basic functionalities.</p>" +
            "<br><p> More details can be found at the following <strong>publications</strong>:<br><br>\n\
            <ul><li>Yannis Roussakis, Ioannis Chrysakis, Kostas Stefanidis, Giorgos Flouris, Yannis Stavrakas. \"A Flexible Framework for Understanding the Dynamics of Evolving RDF Datasets\". In Proceedings of the 14th International Semantic Web Conference (ISWC-15), 2015. Best Student Research Paper Award. <a target=\"blank\" href=\"http://users.ics.forth.gr/~hrysakis/papers/iswc15\">[pdf]</a> </li> \n\
            <li>Yannis Roussakis, Ioannis Chrysakis, Kostas Stefanidis, Giorgos Flouris. \"D2V: A Tool for Defining, Detecting and Visualizing Changes on the Data Web\". In Proceedings of the 14th International Semantic Web Conference, Posters and Demonstrations Track (ISWC-15), 2015. <a target=\"blank\" href=\"http://users.ics.forth.gr/~hrysakis/papers/iswc15Demo.pdf\">[pdf]</a></li></p>" +
            "</ul>" +
            "<br><p>For any information about the D2V system please contact <strong>Ioannis Chrysakis</strong> via e-mail: hrysakis@ics.forth.gr</p>" +
            "</tr>\n" +
            "</tbody></table>\n";

    resetTables();
    $('#cc_types').html(copyright);
}


function showDatasetOptions() {
    var html_str = "<table width=\"218\" border=\"1\" cellpadding=\"10\">\n" +
            "  <tbody>\n" +
            "    <tr>\n" +
            "      <th width=\"174\">Manage datasets</th>\n" +
            "    </tr>\n" +
            "    <tr>\n" +
            "      <td width=\"150\"><p><label for=\"dsoptions-init" + "\">Choose action:</label></p>\n" +
            "        <div class=\"ds_actions\">\n" +
            "          <select name=\"dsoptions\" id=\"dsoptions-init\" onchange=\"retrieveDatasetOptions(this.value);\" >\n" +
            "            <option value=\"nop\"></option>\n" +
            "            <option value=\"add\">Add new dataset</option>\n" +
            "            <option value=\"addversion\">Add version to dataset</option>\n" +
            "            <option value=\"del\">Delete dataset</option>\n" +
            "            <option value=\"delversion\">Delete version from dataset</option>\n" +
            "          </select>\n" +
            "<div id=\"dscombo\"> </div>" +
            "<div id=\"dsmore\"> </div>" +
            "<div id=\"dsversions\"> </div>" +
            "        </div>\n" +
            "      \n" +
            "     \n" +
            "      </td>\n" +
            "    </tr>\n" +
            "  </tbody>";

    resetTables();
    $('#cc_menu').html(html_str);
}


function includeUploadScripts() {
    var html = "<script type=\"text/javascript\" src=\"js/jquery.form.js\" ></script>\n" +
            "<script type=\"text/javascript\" src=\"js/dsVersionUploadScript.js\"></script>    " +
            "<script type=\"text/javascript\" src=\"js/configFileUploadScript.js\"></script>    ";
    return html;
}

function retrieveDatasetOptions(value) {

    var up_str = includeUploadScripts();
    var html_str = "";
    var html_ds = " <p><label for=\"choose_dataset" + "\">Choose dataset*:</label></p><select name=\"choose_dataset\" id=\"choose_dataset\">\n" + " </select>";
    var config_str = "";
    var addbtn_str = "<input type=\"button\" class=\"Button\" value=\"Add\" onclick=\"addDataset(true);\">\n";

    var upload_str = "<p><label for=\"dvlabel\">Enter dataset version label*:</label></p><input name=\"dvlabel\" id=\"dvlabel\" type=\"text\"/>" + "<p>\n\
<br> <label for=\"myfile\">Upload dataset version from disk*:</label></p>\n\
<form class=\"uploadform\" id=\"UploadForm\" action=\"UploadFile\" method=\"post\"\n" +
            "		enctype=\"multipart/form-data\">\n" +
            "		<input type=\"file\" size=\"60\" id=\"myfile\" name=\"myfile\"> <input\n" +
            "			type=\"submit\" value=\"Upload\">\n" +
            "\n" +
            "		<div id=\"progressbox\">\n" +
            "			<div id=\"progressbar\"></div>\n" +
            "			<div id=\"percent\">0%</div>\n" +
            "		</div>\n" +
            "		<br />\n" +
            "\n" +
            "		<div id=\"message\"></div>\n" +
            "	</form>";


    //alert(value);
    $('#dsmore').html('');
    $('#dsversions').html('');
    if (value === 'add') {
        //JCH:Note to fullpath den douleuei ston Firefox gia input type file (security issue)
        html_str = "<br>\n" +
                "<p><label for=\"dlabel\">Enter dataset label*</label></p><input name=\"dlabel\" id=\"dlabel\" type=\"text\"/>" + "<br><br>" +
                upload_str;
        config_str = "<label for=\"myfile2\">Upload custom configuration file for this dataset from disk:</label><br><br>" +
                //fetch from second form and respective script
                "<form class=\"uploadform2\" id=\"UploadForm2\" action=\"UploadFile\" method=\"post\"\n" +
                "		enctype=\"multipart/form-data\">\n" +
                "		<input type=\"file\" size=\"60\" id=\"myfile2\" name=\"myfile2\"> <input\n" +
                "			type=\"submit\" value=\"Upload\">\n" +
                "\n" +
                "		<div id=\"message2\"></div>\n" +
                "	</form><br>";
        $('#dsmore').html(html_str + up_str + config_str + addbtn_str);

    }

    else if (value === 'addversion') {

        html_str = "<br>\n" +
                html_ds + "<br><br>\n" +
                upload_str +
                // "<p>Load dataset version from disk:</p><input name=\"browse\" id=\"filebrowse\" type=\"file\"/>" +

                /// "<input type=\"button\" class=\"Button\" value=\"Add\" onclick=\"alert($('#filebrowse').val())\">\n";
                "<input type=\"button\" class=\"Button\" value=\"Add\" onclick=\"addDataset(false);\">\n";
        getDatasetsCombo('choose_dataset');
        $('#dsmore').html(html_str + up_str);
    }

    else if (value === 'del') {
        html_str = "<br>\n" + html_ds + "<br><br>\n" +
                "<input type=\"checkbox\" id=\"delRelatedVersionsInfo\" name=\"delRelatedVersionsInfo\"><label for=\"delRelatedVersionsInfo\">Delete associated version(s) information for this dataset and from ALL users?</label><br>" +
                "<br><input type=\"button\" class=\"Button\" value=\"OK\" onclick=\"delDataset();\">";
        getDatasetsCombo('choose_dataset');
        $('#dsmore').html(html_str);
    }

    else if (value === 'delversion') {

        html_str = "<br>\n" + html_ds + "<br><br>\n";
        getDatasetsCombo('choose_dataset');
        $('#dsmore').html(html_str);
        $('#choose_dataset').change(function() {
            fetchDSVersions();
        });
    }



}

function delVersionDataset() {
    var nowchangesontology = $('#choose_dataset').val();
    var nowdatasetURI = getDatasetURI(nowchangesontology);
    var message = "The selected dataset version has been erased from the selected dataset.";
    var versionURI = $('input[name=version]:radio:checked').val();
    var msg = 'Please a select a version via a specified radio button, in order this version to be deleted';
    var userDatasetLabel = $('#choose_dataset option:selected').text();
    var deleteVersions = $("input[name='delRelatedVersionsInfo']:checked").val(); //on if selected

    if (versionURI === undefined || versionURI === 'undefined') {
        showDialog('dialogmsg', msg);
    }

    else {
        startAction(true, "actions");

        $.get('ActionServlet', {action: "ds_delversion", versionURI: versionURI, selectedDatasetURI: nowdatasetURI, datasetLabel: userDatasetLabel, deleteVersions: deleteVersions}).done(function(responseText) {
            if (responseText === "success")
            {
                finishAction();
                displayMessage(message);

            }

            else {
                showDialog('dialogmsg', "<p>" + responseText + "</p>"); //error-alert on response
                finishAction();
            }


        });
    }
}

function delDataset() {
    var nowchangesontology = $('#choose_dataset').val();
    var nowdatasetURI = getDatasetURI(nowchangesontology);
    //alert('DEL DATASET::'+nowdatasetURI);

    var nowchangesontology = $('#choose_dataset').val();
    var nowdatasetURI = getDatasetURI(nowchangesontology);
    var userDatasetLabel = $('#choose_dataset option:selected').text();
    var message = "The selected dataset and all its assigned version(s) has been just erased.";
    var deleteVersions = $("input[name='delRelatedVersionsInfo']:checked").val(); //on if selected

    if (nowchangesontology === '' || nowchangesontology === 'nop') {
        showDialog('dialogmsg', "Please select a dataset from the drop down menu!");
    }

    else {
        startAction(true, "actions");



        $.get('ActionServlet', {action: "ds_del", selectedDatasetURI: nowdatasetURI, datasetLabel: userDatasetLabel, deleteVersions: deleteVersions}).done(function(responseText) {
            if (responseText === "success")
            {
                finishAction();
                displayMessage(message);
                getDatasetsCombo('sel_dataset');
                enableDatasetOption('sel_dataset');

            }

            else {
                showDialog('dialogmsg', "<p>" + responseText + "</p>"); //error-alert on response
                finishAction();
            }


        });
    }
}

function addDataset(newdataset) {
    var action = '';
    var dlabel = $('#dlabel').val();
    var dvlabel = $('#dvlabel').val();
    //$('#myfile').val(); SOS: appears C:\fakepath from value
    var versionfile = document.getElementById("myfile").files[0].name;

    var configfile = $('#myfile2').val();
    var userDatasetLabel;

    if (newdataset) {
        action = "ds_add";
        userDatasetLabel = $("#dlabel").val();
    }
    else {
        action = "ds_addversion";
        dlabel = dvlabel;
        userDatasetLabel = $('#choose_dataset option:selected').text();
    }

    var nowchangesontology = $('#choose_dataset').val();
    var nowdatasetURI = getDatasetURI(nowchangesontology);

    var message = "The new dataset with label " + dlabel + " has just been added.";

    startAction(true, "actions");
    var intuser = getURLParameters("intenrnaluser");

    $.get('ActionServlet', {action: action, dslabel: dlabel, dvlabel: dvlabel, datasetLabel: userDatasetLabel, versionFilename: versionfile, configFilename: configfile, username: intuser, intuser: intuser, selectedDatasetURI: nowdatasetURI}).done(function(responseText) {
        if (responseText === "success")
        {
            finishAction();
            displayMessage(message);
            getDatasetsCombo('sel_dataset');
            enableDatasetOption('sel_dataset');

        }

        else {
            finishAction();
            showDialog('dialogmsg', "<p>" + responseText + "</p>"); //error-alert on response
        }


    });
}

function fetchDSVersions() {
    var OKButton = "<br>\n" + "<input type=\"button\" class=\"Button\" value=\"OK\" onclick=\"delVersionDataset();\">\n";
    var changesontology = $('#choose_dataset').val();
    var del_versions_box = "<input type=\"checkbox\" id=\"delRelatedVersionsInfo\" name=\"delRelatedVersionsInfo\"> <label for=\"delRelatedVersionsInfo\">Delete associated version's contents for this dataset and potentially assined in other users?</label><br>";
    var datasetURI = getDatasetURI(changesontology);
    $.get('OntologyQueryServlet', {qtype: 'versions', datasetversions: datasetURI, dataset: changesontology, valuestype: "radio"}, function(responseText) {

        $('#dsversions').html(responseText);
        $('#dsversions').append(del_versions_box);
        $('#dsversions').append(OKButton);
    });


}

function showVisionOptions(selectedtd) { //called in visualize changes option (link)


    $('.clickbuttons1').removeClass('fillmarktd');
    $(selectedtd).addClass('fillmarktd');

//Default options for visualization
    var init_selected_radio = 'historyradio';
    var init_coption = 'allcbox';
    var init_vfrom = '';
    var init_vto = '';
    var init_sclist = '';
    var init_cclist = '';


    var init_buttons = "<input name=\"getvis\" id=\"getvis\" onclick=\"getVisualisations('" + init_selected_radio + "'" + "," + "'" + init_coption + "'" + "," + "'" + init_vfrom + "'" + "," + "'" + init_vto + "'" + "," + "'" + init_sclist + "'" + "," + "'" + init_cclist + "'" + "); return false; \"class=\"Button\" value=\"OK\" type=\"submit\">\n"
            + "<input name=\"clear\" class=\"Button\" value=\"Reset\" onclick=\"resetTables()\" type=\"submit\"></td>\n";

    var version_combos = " <p><label for=\"vfrom" + "\">From version:</label></p>\n" +
            "<div class=\"versions_combo\">\n" +
            "<select id=\"vfrom\" class=\"versions_val\">\n" +
            " </select> \n" +
            "</select></div>\n" +
            "<p><label for=\"vfrom" + "\">To version:</label></p>\n" +
            "<div class=\"versions_combo\">\n" +
            " <select id=\"vto\" class=\"versions_val\">\n" +
            " </select>\n" +
            " </div>\n";

    if (checkDataset()) {
        var html_str = "<form id=\"visform\" name=\"visform\" method=\"post\" action=\"\"><fieldset><table border=\"1\" cellpadding=\"10\" width=\"420\">\n" +
                " <tbody><tr>\n" +
                "<th width=\"174\">Visualization Options</th></tr><tr>\n" +
                "<td width=\"150\"><p>\n" +
                "  <input checked name=\"visradio\" value=\"historyradio\" id=\"historyradio\" type=\"radio\" class=\"radioclass\"/>\n" +
                "  <label for=\"historyradio" + "\">View evolution history</label>\n" +
                "  </p>\n" +
                "<div><br><p id=\"evobetween\"><p></div>" +
                "</td></tr><tr><td>" +
                "  <p>\n" +
                "  <input name=\"visradio\" value=\"compareradio\" id=\"compareradio\" type=\"radio\" class=\"radioclass\"/>\n" +
                "  <label for=\"compareradio" + "\">Compare custom versions</label>\n" +
                "</p>\n" +
                "<br>" +
                "<div id=\"vcombos\"></div>" +
                //version_combos +         
                "</p>\n" +
                "<br>" +
                //"<br>" +
                "<div id=\"vis_buttons\">  \n" +
                init_buttons +
                "</div>\n" +
                "</tr>\n" +
                "<tr>\n" +
                "\n" +
                "</tr>\n" +
                "</tbody></table>\n" +
                "</fieldset></form>";

        resetTables();
        $('#cc_menu').html(html_str);


        //var coption = $('.choption:checked').attr('name'); //get checkbox name
        selected_radio = $("input[name='visradio']:checked").val();
        var changesontology = $('#sel_dataset').val();
        var datasetURI = getDatasetURI(changesontology);

        $(document).ready(function() {
            $.get('OntologyQueryServlet', {qtype: 'evoversions', datasetversions: datasetURI, dataset: changesontology, valuestype: "str"}, function(responseText) {
                //alert('evoversions'+responseText);
                $('#evobetween').html(responseText);
            });

            $('.radioclass').click(function() { //get type of visualization according to combo
                selected_radio = $("input[name='visradio']:checked").val();
                if (selected_radio === 'compareradio') {
                    $('#vcombos').html(version_combos);

                    //filling-combos
                    $.get('OntologyQueryServlet', {qtype: 'versions', datasetversions: datasetURI, dataset: changesontology, valuestype: "option"}, function(responseText) {
                        $('.versions_val').html(responseText);
                        vfrom = $('#vfrom option:selected').val();
                        vto = $('#vto option:selected').val();
                        updateVisButtons();
                    });
                    //geting v1,v2
                    $('.versions_combo').change(function() {
                        vfrom = $('#vfrom option:selected').val();
                        vto = $('#vto option:selected').val();
                        //alert('vfrom' +vfrom);
                        updateVisButtons();

                    });
                }
                else {
                    $('#vcombos').html('');
                }

                updateVisButtons();
            });

            handleChangesInteraction(datasetURI, changesontology);
            updateVisButtons();
        });  //end-of document ready  

    } //end-of valid dataset

}


function handleChangesInteraction(datasetURI, changesontology) {
//var changesontology =  $('#sel_dataset').val();
//var datasetURI = getDatasetURI (changesontology);

    $('.choption').click(function() { //get changes options
        ///   $('.choption').not($(this)).prop('checked', false); //JCH: on click disable the other of same class
        var coption = $(this).attr('name');//JCH: get name of selected class of checkboxes
        // alert("NEWcoption::"+coption);

        //alert(coption);
        if (coption === 'sccbox') {
            $('#allcbox').prop('checked', false);
            //qtype:'simplechanges', valuestype:'option'
            $.get('OntologyQueryServlet', {qtype: 'Simple_Change', valuestype: 'cbox', datasetversions: datasetURI, dataset: changesontology}, function(responseText) {
                //alert(responseText);
                $('#simple_changes_list').html(responseText);
                sclist = null;

                fetchChangesList();

            });


        } //end of simple changes option
        else if (coption === 'cccbox') {
            $('#allcbox').prop('checked', false);
            $.get('OntologyQueryServlet', {qtype: 'Complex_Change', valuestype: 'cbox', datasetversions: datasetURI, dataset: changesontology}, function(responseText) {
                //alert(responseText);
                $('#complex_changes_list').html(responseText);

                cclist = null;
                fetchChangesList();
            });
        }
        else {
            $('#cccbox').prop('checked', false);
            $('#sccbox').prop('checked', false);
            $('#simple_changes_list').html('');
            $('#complex_changes_list').html('');
            sclist = null;
            cclist = null;
            fetchChangesList();
        }

    });
    checkChangesOptions();
    fetchChangesList();
}


//finds selected changes (simple/complex) and update button that includes the action..
//Note that clist is the merged list of both sclist and cclist
function fetchChangesList() {

    $('.tabright').click(function() {
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

        }
        );// end of each  



        sclist = normalizeJSArray(schangesJSArray);
        cclist = normalizeJSArray(ccchangesJSArray);

        checkChangesOptions();
        updateVisButtons();
    });

    updateVisButtons();
}


//check changes options and fills-in the appropriate lists
function checkChangesOptions() {
    var coptions;
    $(".choption:checked").each(function() {

        coptions = $(this).attr('name');
        if (coptions === 'sccbox' && sclist === '') {
            sclist = null;
        }
        else if (coptions === 'cccbox' && cclist === '') {
            cclist = null;
        }

        else if (coptions === 'allbox') {
            sclist = null;
            cclist = null;
        }
    });

    //alert("SCLIST-NOW:"+ sclist);
    //alert("CCLIST_NOW:"+ cclist);
}


function getViewType() {
    if (sclist === null && cclist === '') {
        return "Simple_Change";
    }
    else if (cclist === null && sclist === '') {
        return "Complex_Change";
    }
    else {
        return "all";
    }
}

//OBSOLETE:set default pair of comparing versions
function setDefaultVersions(selected_radio, def_val_vfrom, def_val_vto) {

    if (selected_radio === 'historyradio') { //setting default values for v1,v2
        $("#vfrom").val(def_val_vfrom);
        $("#vfrom").prop("disabled", true);
        $("#vto").val(def_val_vto);
        $("#vto").prop("disabled", true);

    }
    else {
        $("#vfrom").val('');
        $("#vto").val('');
        $("#vfrom").prop("disabled", false);
        $("#vto").prop("disabled", false);
    }
}

//called to update OK button and fill-in parameters of getVisualisations (selected_radio,coption,vfrom,vto,sclist,cclist)
function updateVisButtons() {

    //merge clist here used in evolution as one list


    var clist = "";
    if (sclist === null && cclist !== null) {
        clist = cclist; //(+ all simple changes)
    }
    else if (cclist === null && sclist !== null) {
        clist = sclist; // (+ all complex changes)
    }
    else if (cclist !== null && sclist !== null) {
        clist = sclist + "," + cclist;
    }


    vfrom = $('#vfrom option:selected').val();
    vto = $('#vto option:selected').val();

    var newbuttons = "<input name=\"getvis\" id=\"getvis\" class=\"Button\" value=\"OK\" onclick=\"getVisualisations('" + selected_radio + "'" + "," + "'" + vfrom + "'" + "," + "'" + vto + "'" + "," + "'" + clist + "'" + "," + "'" + sclist + "'" + "," + "'" + cclist + "'" + "); return false; \" type=\"submit\">\n"
            + "<input name=\"clear\" class=\"Button\" value=\"Reset\" onclick=\"resetTables()\" type=\"submit\"></td>\n";
    //alert("MERGED:::"+clist);
    $("#vis_buttons").html(newbuttons);
    // alert(newbuttons);
//JCH:Sos return false after on click function was nececcary because Chrome refreshes
}

//selects frame of visualization, perfom init and detect actions and finally opens the frame
function getVisualisations(selected_radio, vfrom, vto, clist, sclist, cclist) {

    var vwmsg = "Wrong selection of versions. Please select a valid pair of versions!";
    var viewtype = getViewType(); //used for case without selecting specified changes


    var userChangesOntology = $('#sel_dataset').val();
    var userDatasetURI = getDatasetURI(userChangesOntology);


    var display_frame = "<iframe scrolling=\"yes\" style=\"overflow:hidden;height:200%;width:1000px;\"  id=\"iframe2\" src=\"vision.html?userChangesOntology=" + userChangesOntology + "&tempontology=no" + "&sradio=" + selected_radio + "&userDatasetURI=" + userDatasetURI + "&viewtype=" + viewtype + "&vfrom=" + vfrom + "&vto=" + vto + "&clist=" + clist + "&sclist=" + sclist + "&cclist=" + cclist + "\"></iframe>";
    var temp_frame = "<iframe style=\"overflow:hidden;height:200%;width:1000px;\"id=\"iframe2\" src=\"vision.html?userChangesOntology=" + userChangesOntology + "&tempontology=yes" + "&sradio=" + selected_radio + "&userDatasetURI=" + userDatasetURI + "&viewtype=" + viewtype + "&vfrom=" + vfrom + "&vto=" + vto + "&clist=" + clist + "&sclist=" + sclist + "&cclist=" + cclist + "\"></iframe>";
    //identical to display for (evo or cc_seq case)
    //var overwrite_frame = "<iframe style=\"overflow:hidden;height:105%;width:1000px;\"id=\"iframe2\" src=\"vision.html?dataset=" + dataset + "&tempontology=no" + "&sradio=" + selected_radio + "&versions=" + datasetversions + "&viewtype=" + viewtype + "&vfrom=" + vfrom + "&vto=" + vto + "&clist=" + clist +"&sclist=" + sclist + "&cclist=" + cclist + "\" style=\"min-width:1000px; min-height:850px;\"></iframe>";
    var vwcheck;
    //alert(temp_frame);
    resetTables();

    if (selected_radio === 'historyradio') {
        $('#cc_menu').append(display_frame);
    }


    else {
        var changeType = viewtype;
        vwcheck = checkWrongVersionRange(vfrom, vto, false);

        if (vwcheck) {
            showDialog('dialogmsg', vwmsg);
            showVisionOptions();
        }

        else {


            initAndDetectVisualizations(vfrom, vto, temp_frame, display_frame, sclist, cclist, changeType);

        }
    }


}

//called when I want to compare two versions and cleans tmp graph results when they are not sequential
function initAndDetectVisualizations(v1, v2, temp_frame, display_frame, sclist, cclist, changetype) {
    var changesontology = $('#sel_dataset').val();
    var userDatasetLabel = $('#sel_dataset option:selected').text();
    //alert ('userDatasetLabel:'+userDatasetLabel);
    var datasetURI = getDatasetURI(changesontology);
    var vset = v1.substr(v1.lastIndexOf('/') + 1) + "-" + v2.substr(v2.lastIndexOf('/') + 1);
    var changesgraph = changesontology + "/" + vset;
    $.get('OntologyQueryServlet', {qtype: 'graphexistance', changesgraph: changesgraph}, function(responseText) {

        if (responseText === 'exists') {
            startAction(false, "actions");
            // alert('No need for live detection....'); //TO-CHECK THIS if we have users
            finishAction();

            $('#cc_menu').append(display_frame);
        }
        else {
            {
                startAction(true, "cc_menu"); //cc_menu previously
                $.get('ActionServlet', {action: "initvision", userDatasetURI: datasetURI, datasetLabel: userDatasetLabel, userChangesOntology: changesontology, sclist: sclist, cclist: cclist, changetype: changetype, vold: v1, vnew: v2}).done(function(data) {
                    //alert( "New Detection Completed!" + data ); //JCH: load after servlet finishes
                    //showDialog('dialogmsg',"Warning",msg);
                    finishAction();
                    $('#cc_menu').html(temp_frame);

                });

            }
        }
    });





}


function resetTables() {

    if ($('#sel_dataset').val() !== 'nop') {
        disableDatasetOption();
    }
    scno = 0;
    jfno = 1;
    sfno = 1;
    clist = '';
    sclist = '';
    cclist = '';
    $('#cc_menu').html('');
    $('#cc_types').html('');  //resets previous code
    $('#cc_params').html('');  // resets previous code
    username = getUserName();

}

function enableDatasetOption() {
    $('#sel_dataset').prop('disabled', false);
}


function disableDatasetOption() {

    $('#sel_dataset').prop('disabled', true);
}

function displayMessage(message) { // called after detection has been completed or after saving a definition


    var html_code = "<table cellspacing='0' class=\"inside\">\n" +
            "<tr><td>\n" +
            "<p class=\"sucmessage\">" + message + "</p>"
            + "</td></tr>\n" +
            "</table>\n";
    resetTables();
    $('#cc_menu').html(html_code);
    // alert("Call visualization page");
}

//Checks for wrong selection at version combos selections
function checkWrongVersionRange(vfrom, vto, evo_case) {
    //at the evolution

    if (vto !== '' && vfrom !== '') {
        var vold_cut = vfrom.substr(vfrom.lastIndexOf('/') + 1);
        //alert('vold_cut'+vold_cut);
        var vnew_cut = vto.substr(vto.lastIndexOf('/') + 1);
        //alert('vnew_cut'+vnew_cut);
    }
    if ((evo_case && vold_cut >= vnew_cut) || (!evo_case && vold_cut === vnew_cut)) {
        showDialog('vision_title', 'Wrong selection of versions. The "From version" selection should be older than the "To Version"');
        $('#loading').remove();
        return true;
    }
}

function checkDataset() {

    var msg = 'Please choose dataset from the leftside menu!';
    var msg2 = 'Please wait. Another operation is in progress...';

    if ($('#sel_dataset option:selected').text() === ' ') {
        showDialog('dialogmsg', msg);
        return false;
    }

    if ($("body").hasClass("liveaction")) {
        showDialog('dialogmsg', msg2);
        return false;
    }

    return true;
}


function getDatasetURI(changesontology) { //New version, does not requires query to store
    var dataseturi = "n/a";
    if (changesontology !== undefined && changesontology !== "undefined") { //when comes from vis
        dataseturi = changesontology.replace("/changes", " ").trim();
        //alert('dataseturi>>' +dataseturi+"<<");
    }
    return dataseturi;
}


function getSimpleChanges() { // called in the building of add advanced definition
    var changesontology = $('#sel_dataset').val();

    var datasetURI = getDatasetURI(changesontology);

    $.get('OntologyQueryServlet', {qtype: 'Simple_Change', datasetversions: datasetURI, dataset: changesontology, valuestype: 'option'}, function(responseText) {
        //alert(responseText);
        $('#assigned_sc').html(responseText);
    });

}
//fills in join combo1 (left) and selection combo (left)
function getSimpleChangeParameters(chname, chfullname, joinfilterID1, selfilterID) { // called in the building of add advanced definition

    //fills-in selfilter and joincombo(left)

    var changesontology = $('#sel_dataset').val();
    var datasetURI = getDatasetURI(changesontology);
    //JCH:force sychronous call 
    $.ajaxSetup({
        async: false
    });
    $.get('OntologyQueryServlet', {qtype: 'scparams', username: username, chname: chname, chfullname: chfullname, datasetversions: datasetURI, dataset: changesontology}).done(function(responseText) {

        if (joinfilterID1 !== '') {
            $('#' + joinfilterID1).html(responseText);
        }
        if (selfilterID !== '') {
            $('#' + selfilterID).html(responseText);
            //alert(responseText);
        }

        //JCH:make it again asychronous for other calls
        $.ajaxSetup({
            async: true
        });
    }

    );

}

function getDefinedCC(action, selectedtd) {

    $('.clickbuttons1').removeClass('fillmarktd');
    $(selectedtd).addClass('fillmarktd');
    var user = getURLParameters("username");
    var sel_dataset_label = $('#sel_dataset option:selected').text();

    var msg = "<p>No complex change has been defined yet " + "for dataset " + sel_dataset_label + " and user " + user + '.</p>';

    var delbutton = "<input type=\"button\" name=\"actionbutton_del\" id=\"actionbutton\" class=\"Button\" value=\"Delete\" onclick=\"deleteCC(); return false;\"/>\n";
    var editbutton = "<input type=\"button\" name=\"actionbutton_edit\" id=\"actionbutton\" class=\"Button\" value=\"Edit\" onclick=\"editCC(); return false;\"/>\n";
    var actionbutton = delbutton;
    var html_str = "";



    html_str = "<form id=\"delform\" name=\"delform\" method=\"post\" action=\"\">"
            + ""
            + "<table width=\"400\" cellspacing='0'>\n"
            + "<tr>\n"
            + "<th width=\"174\">Defined Complex Changes</th></tr>\n"
            + "<tr>\n"
            + "<td colspan=\"2\">\n"
            + "<fieldset><div id=\"chboxes\"></div>"
            + "<input type=\"submit\" name=\"clear\" class=\"Button\" value=\"Reset\" onclick=\"resetTables()\"></fieldset>"
            + "</td></tr>"
            + "</table>"
            + "</form>";

    var changesontology = $('#sel_dataset').val();
    var datasetURI = getDatasetURI(changesontology);
    if (checkDataset()) {

        if (action === 'del') {
            resetTables();
            $.get('OntologyQueryServlet', {qtype: 'defchanges', action: "delete", changetype: 'Complex_Change', valuestype: "radio", datasetversions: datasetURI, dataset: changesontology}, function(responseText) {

                if (responseText === '') { //empty result: no defined changes
                    showDialog('dialogmsg', msg);
                }
                else {
                    $('#cc_menu').html(html_str);
                    //alert(html_str);
                    $('#chboxes').html(responseText).after(delbutton);
                    ;
                }
            });
        }

        else { //edit
            resetTables();

            $.get('OntologyQueryServlet', {qtype: 'defchanges', datasetversions: datasetURI, dataset: changesontology, action: "edit", changetype: 'Complex_Change', valuestype: "radio"}, function(responseText) {

                if (responseText === '') { //empty result: no defined changes
                    showDialog('dialogmsg', msg);
                }
                else {
                    $('#cc_menu').html(html_str);
                    $('#chboxes').html(responseText).after(editbutton);

                }
            });

        }
    }

}


function editCC() {

    var msg = '<p>No any selected complex change!</p>';

    var changesontology = $('#sel_dataset').val();
    var datasetURI = getDatasetURI(changesontology);


    var cc_name;
    var cc_priority;
    var cc_params;
    var cc_assigned_changes;
    var ver_filters;
    var cc_desc;

    var cclength = $('.cc_checkbox:checked').size();
    if (cclength < 1) {
        showDialog('dialogmsg', msg);
    }
    else {
        cc_name = $('.cc_checkbox:checked').val();
        //alert(selectedcc);
        createCCTables(true, cc_name);
        $.get('OntologyQueryServlet', {qtype: "ccjson", chname: cc_name, datasetversions: datasetURI, dataset: changesontology}, function(responseText) {

            json_string = responseText;


            var obj = jQuery.parseJSON(json_string);

            //console.log('JSON OBJ:'+obj);

            if (obj !== null) {
                cc_priority = obj.Priority;
                cc_params = obj.Complex_Change_Parameters;
                cc_desc = obj.Description;
                cc_assigned_changes = obj.Simple_Changes;
                ver_filters = obj.Version_Filters;
                retrieveCCBasicInfo(cc_name, cc_priority, cc_desc);

                //alert('Retrieving...');

                retrieveSCInfo(cc_assigned_changes);
                retrieveParamInfo(cc_params);
                retrieveVerFilter(ver_filters);
                //viewJSONButton

                var vsparqlbytton = "<input name=\"viewJSONButton\" id=\"viewJSONButton\" class=\"qvbutton\" value=\"View JSON\" type=\"button\" onclick=\"viewJSON();\">\n";
                $("#viewQ").append(vsparqlbytton);
            }
            else {
                showDialog('dialogmsg', "Cannot retrieve any information!");
            }
        });
    }

}


function viewJSON() {
    //alert(json_string);  
    showJSON('dialogmsg', "'" + json_string + "'");


}

function retrieveSCInfo(cc_assigned_changes) {

    var param_val;
    var sc, sc_uri, sel_filter, isopt, join_filter;

    //console.log("cc_assigned_changes:"+cc_assigned_changes);
    for (var i = 0; i < cc_assigned_changes.length; i++) {
        param_val = cc_assigned_changes[i];
        //for(var key in param_val){
        sc = param_val["Simple_Change"];

        sc_uri = param_val["Simple_Change_Uri"];
        sel_filter = param_val["Selection_Filter"];
        join_filter = param_val["Join_Filter"];
        isopt = param_val["Is_Optional"];

        //console.log("SF:::::::::" + sel_filter);
        //console.log("JF:::::::::" + join_filter);

        addSCBlock(true, sc, sc_uri, sel_filter, join_filter, isopt);

    }

}


function retrieveParamInfo(cc_params) {
    var param_val;

//console.log(cc_params);
    for (var i = 0; i < cc_params.length; i++) {
        param_val = cc_params[i];
        for (var key in param_val) {
            //console.log(key + ": " + param_val[key]);
            addExtCCParams(true, key, param_val[key]);
            //// paramNames.push(key);

        }
    }
    addUpdateParamButton();

}

function addUpdateParamButton() {
    var update_params_button = "<br><input class=\"UpdateVfiltersBtn button\" type=\"button\" onclick=\"updateParamsOnVerFilters();\" value=\"Update version filters\">";
    $("#update-params-btn").html(update_params_button);
}

function updateParamsOnVerFilters() {
    var currentParams = [];

//Retrieving current param names
    $(".paramnames").each(function() {

        var live_val = $(this).val();
        currentParams.push(live_val);
    });

    for (var i = 0; i < currentParams.length; i++) {
        if (currentParams[i] !== paramNames[i]) {
            //alert('Must replace:' +paramNames[i].toString()+" with:" +currentParams[i].toString());
            var old_value = paramNames[i];
            var new_value = currentParams[i];
            paramNames[i] = new_value; //hold the newest value of user for future update
            $('#version-filters option,#version-filters input').each(function() {
                if (($(this).html() === old_value) || ($(this).val() === old_value)) {
                    $(this).html(new_value);
                    $(this).val(new_value);
                }
            });

        }

    }

}

function replaceall(str, replace, with_this)
{
    var str_hasil = "";
    var temp;

    for (var i = 0; i < str.length; i++) // not need to be equal. it causes the last change: undefined..
    {
        if (str[i] == replace)
        {
            temp = with_this;
        }
        else
        {
            temp = str[i];
        }

        str_hasil += temp;
    }

    return str_hasil;
}

function retrieveCCBasicInfo(cc_name, cc_priority, cc_desc) {
    $('#cc_name').val(cc_name);
    $('#priority').val(cc_priority);
    $('#cc_desc').val(cc_desc);
}

function deleteCC() {

    //JCH:eixa grapsei kwdika gia na sbhnei panw apo 1 complex change alla apofasisame na mporei o xrhsths na sbhnei mono mia
    var todeleteChanges = [];
    var message = '';
    var changesontology = $('#sel_dataset').val();
    var userDatasetLabel = $('#sel_dataset option:selected').text();
    var datasetURI = getDatasetURI(changesontology);



    $(".cc_checkbox:checked").each(function() {
        //alert(this.value);
        todeleteChanges.push(this.value);
    });// end of each  


    if (todeleteChanges.length > 0) {
        var array_to_send = JSON.stringify(todeleteChanges);
        array_to_send = array_to_send.replace(/[[\]]/g, ""); //remove brackets
        array_to_send = array_to_send.replace(/"/g, "");   //removes "
        array_to_send = array_to_send.replace(/,/g, "$");

        message = 'The selected complex change(s) have been successfully deleted.';
        startAction(true, "actions");



        $.get('ActionServlet', {action: "delete", userDatasetURI: datasetURI, datasetLabel: userDatasetLabel, userChangesOntology: changesontology, dellist: array_to_send}).done(function(responseText) {

            $('#actions').html(responseText);
            finishAction();


            if (responseText === 'Cannot delete complex change!') {
                showDialog('dialogmsg', "<p>" + responseText + "</p>"); //error-alert on response  
            }

            else {
                displayMessage(message);
            }
        });


    }
    else {
        var msg = '<p>No any selected complex change!</p>';
        showDialog('dialogmsg', msg);
    }


}

function getSimpleChangesTemplates(complexChangeType, selectedtd) { //called in each selection of template
    var html_code;

    $('#jsonf').html('');

    $('.clickbuttons2').removeClass('fillmarktd');
    $(selectedtd).addClass('fillmarktd');
    if (complexChangeType === 'add') {

        html_code = "<table width=\"258\" border=\"1\" cellpadding=\"10\">\n" +
                " <tr>\n" +
                "	  <th width=\"174\" colspan=\"2\">Template <br><br><p class=\"template_decr\">(The template determines a family of similar complex changes which usually differ on parameter value)</p></th></tr>" +
                "  <tr class=\"show-option\" title=\"Use this template to define a complex change capturing the addition of a super-class on top of a selected class which is determined through a user-defined value\">\n" +
                "    <td colspan=\"2\" class=\"clickable clickbuttons3\" onclick=\"getParamChangeVal('Add Generalization Of','subclass','add',this)\">Add Generalization Of</td>\n" +
                " </tr>\n" +
                "</a>" +
                " <tr class=\"show-option\" title=\"Use this template to define a complex change capturing the addition of an instance of a selected property which is determined through a user-defined value\">\n" +
                " <td colspan=\"2\" class=\"clickable clickbuttons3\" onclick=\"getParamChangeVal('Add Instance Of','property','add',this)\"> Add Property Instance Of</td>\n" +
                "</td>\n" +
                "  </tr>\n" +
                "  <tr class=\"show-option\" title=\" Use this template to define a complex change capturing the addition of a selected type to a class which is determined through a user-defined value\">\n" +
                " <td colspan=\"2\" class=\"clickable clickbuttons3\" onclick=\"getParamChangeVal('Add Object Of Type','type','add',this)\">Add Class Instance</td>\n" +
                "  </tr>\n" +
                " <tr class=\"show-option\" title=\"Use this template to define a complex change capturing the addition of a sub-class on top of a selected class which is determined through a user-defined value\">\n" +
                " <td colspan=\"2\" class=\"clickable clickbuttons3\"  onclick=\"getParamChangeVal('Add Specialization Of','superclass','add',this)\">Add Specialization Of</td>\n" +
                "  </tr>\n" +
                "  <tr>\n" +
                "   \n" +
                "  </tr>\n" +
                "</table>";

        $('#cc_types').html(html_code);
    }
    else if (complexChangeType === 'del') {

        html_code = "<table width=\"258\" border=\"1\" cellpadding=\"10\">\n" +
                " <tr>\n" +
                " <tr>\n" +
                "	  <th width=\"174\" colspan=\"2\">Template <br><br><p class=\"template_decr\">(The template determines a family of similar complex changes which usually differ on parameter value)</p></th></tr>" +
                "  <tr class=\"show-option\" title=\"Use this template to define a complex change capturing the detaching of a super-class from the top of a selected class which is determined through a user-defined value\">\n" +
                " <td colspan=\"2\" class=\"clickable clickbuttons3\"  onclick=\"getParamChangeVal('Delete Generalization From','subclass','delete',this)\">Delete Generalization From</td>\n" +
                "  </tr>\n" +
                "  <tr class=\"show-option\" title=\"Use this template to define a complex change capturing the deletion of an instance of a selected property which is determined through a user-defined value\">\n" +
                " <td colspan=\"2\" class=\"clickable clickbuttons3\" onclick=\"getParamChangeVal('Delete Instance Of','property','delete',this)\">Delete Property Instance Of</td>\n" +
                "  </tr>\n" +
                "  <tr class=\"show-option\" title=\"Use this template to define a complex change capturing the deletion of a selected type from a class which is determined through a user-defined value\">\n" +
                " <td colspan=\"2\" class=\"clickable clickbuttons3\" onclick=\"getParamChangeVal('Delete Object Of Type','type','delete',this)\">Delete Class Instance</td>\n" +
                "  </tr>\n" +
                "  <tr class=\"show-option\" title=\"Use this template to define a complex change capturing the detaching of a sub-class from the top of a selected class which is determined through a user-defined value\">\n" +
                " <td colspan=\"2\" class=\"clickable clickbuttons3\" onclick=\"getParamChangeVal('Delete Specialization From','superclass','delete',this)\">Delete Specialization From</td>\n" +
                "  </tr>\n" +
                "  <tr>\n" +
                "   \n" +
                "  </tr>\n" +
                "</table>";
        $('#cc_types').html(html_code);
    }
    else if (complexChangeType === 'edit') {
        html_code = "<table width=\"258\" border=\"1\" cellpadding=\"10\">\n" +
                " <tr>\n" +
                " <tr>\n" +
                "	  <th width=\"174\" colspan=\"2\">Template <br><br><p class=\"template_decr\">(The template determines a family of similar complex changes which usually differ on parameter value)</p></th></tr>" +
                " <td colspan=\"2\" class=\"clickable clickbuttons3\" onclick=\"getParamChangeVal('Update Comment','URI','update',this)\">Update Comment</td>\n" +
//"    <td width=\"20\"><input name=\"Update Comment\" class=\"checkboxgroup\" type=\"radio\" value=\"URI\" /></td>\n" +
                "  </tr>\n" +
                "  <tr>\n" +
                " <td colspan=\"2\" class=\"clickable clickbuttons3\" onclick=\"getParamChangeVal('Update Domain','URI','update',this)\">Update Domain</td>\n" +
//"    <td><input name=\"Update Domain\" class=\"checkboxgroup\" type=\"radio\" value=\"URI\"/></td>\n" +
                "  </tr>\n" +
                "  <tr>\n" +
                " <td colspan=\"2\" class=\"clickable clickbuttons3\" onclick=\"getParamChangeVal('Update Label','URI','update',this)\">Update Label</td>\n" +
//"    <td><input name=\"Update Label\" class=\"checkboxgroup\" type=\"radio\" value=\"URI\"/></td>\n" +
                "  </tr>\n" +
                "  <tr>\n" +
                " <td colspan=\"2\" class=\"clickable clickbuttons3\" onclick=\"getParamChangeVal('Update Property','property','update',this)\">Update Property</td>\n" +
//"    <td><input name=\"Update Property\" class=\"checkboxgroup\" type=\"radio\" value=\"property\"/></td>\n" +
                "  </tr>\n" +
                "  <tr>\n" +
                "    <tr>\n" +
                " <td colspan=\"2\" class=\"clickable clickbuttons3\" onclick=\"getParamChangeVal('Update Range','URI','update',this)\">Update Range</td>\n" +
//"    <td><input name=\"Update Range\" class=\"checkboxgroup\" type=\"radio\" value=\"URI\"/></td>\n" +
                "  </tr>\n" +
                "  <tr>\n" +
                "  </tr>\n" +
                "</table>";
        $('#cc_types').html(html_code);
    }

    /*$('.checkboxgroup').click(function() {
     $('.checkboxgroup').not($(this)).prop('checked', false);
     getParamChangeVal();
     });*/
//createTooltips();
}


//called on click of each checkbox to build third table-module of simple add definition
function getParamChangeVal(selectedchange, selected_typeval, ttype, selectedtd) {
    $('.clickbuttons3').removeClass('fillmarktd');
    $(selectedtd).addClass('fillmarktd');
    var changesontology = $('#sel_dataset').val();
    var datasetURI = getDatasetURI(changesontology);


    selected_change = selectedchange;
    $.get('OntologyQueryServlet', {qtype: 'templates', datasetversions: datasetURI, dataset: changesontology, paramvaltype: selected_typeval, sc: selectedchange}, function(responseText) {
        $('#cc_params').html(responseText);
        $("#param_val").change(function() { //333
            //console.log(":" + $(this).val());
            $(this).attr('title', $(this).val()); //display selected on mouse over

            if ($(this).val() === 'Enter a user-defined value') {

                $('#user_param_value').prop('disabled', false);
            }
            else {
                $('#user_param_value').prop('disabled', true);
            }



        });
        if ($('#param_val').is(':disabled') && ttype !== 'update') {
            /// showDialog('dialogmsg', 'N/A - Please select another template');

            //showDialog('dialogmsg', 'Only user defined values are allowed for this template');
            $('#user_param_value').prop('disabled', false);
        }
    });


}

function checkExtMandatoryFields(ccName, ccPriority) {
    var msg = "<p>Please enter value(s) for mandatory field(s) marked with *</p>";
    if (ccName === '' || ccPriority === '' /*|| $('.scblock').length<1*/ || $(".paramnames").length < 1) {

        showDialog('dialogmsg', msg);
        return false;
    }
    else {
        //$("#saveExtButton").removeAttr('disabled');
        return true;
    }


}

//called in add advanced definition of a complex change
function saveExtCCDef(on_edit, prev_ccname) {

    var sub_combo;
    var sub_tex;
    var pred_combo;
    var pred_tex;
    var obj_combo;
    var obj_tex;
    var pres;
    var rows = '';

    var ccName = $('#cc_name').val();
    var ccPriority = $('#priority').val();

    var ccDescription = $('#cc_desc').val();
    var ccParams = ''; //name1:parameter1$name2:parameter2
    var selects = '';
    var joins = '';
    var opt_changes = '';
    var assignedscnames = "";

    //Get cc parameters
    $(".paramnames").each(function() {
        ccParams = ccParams + "$" + $(this).val() + ":" + $(this).next().val(); //name1:value1$name2:value2
    });


    //Get assined simple changes uris
    $(".simplechangeclass").each(function() {
        //console.log($(this).html());
        assignedscnames = assignedscnames + "$" + $(this).html(); //stores all assigned sc in this var
        // alert(assignedscnames);
    });

    //alert('NAMES:'+assignedscnames);
    if (checkExtMandatoryFields(ccName, ccPriority)) {


//Get selects/join filters for assigned SC
        var currentId = '';
        $(".scblock").each(function() {
            currentId = $(this).attr('id');
            //to currentID ksexwrizei gia poia allagh milame (*_No_ChangeType) ISWS na balw delimeter

            var select_class_id = "select_combo" + currentId;
            var join_class_id = "join_combo" + currentId;

            $("." + select_class_id).each(function() {
                //alert('next val:' +$(this).next().val());
                if ($(this).next().val() && $(this).next().val() !== '+') { //mhn pareis epomena zeygh pou den yparxoun
                    selects = selects + "$" + $(this).val() + "=" + $(this).next().val();

                }
            }
            );

            $("." + join_class_id).each(function() {

                if ($(this).next().val() && $(this).next().val() !== '+') {
                    joins = joins + "$" + $(this).val() + "=" + $(this).next().val();
                }
            }

            );

        } //For all blocks
        );

        //alert('SELECTS:' + selects);
        //alert('JOINS:'+joins);

        $('.optionalclass:checked').each(function() {  //JCH: For all checked checkboxes of a class:


            if (opt_changes !== '') {

                opt_changes = opt_changes + "$" + $("#" + this.id).attr('name');
            }
            else {
                opt_changes = $("#" + this.id).attr('name');
            }
        }
        );


//get version filter combo values
        $(".vfilter").each(function() {

            var filterid = ($(this).attr('id'));
            //alert(filterid);
            var $firstChild = $("#" + filterid + " > select:first-child");
            //alert("#"+filterid + " > select:first");
            //alert($firstChild.html() +"-"+$firstChild.length +"-"+$firstChild.val());
            if ($firstChild.next()) {


                $("#" + filterid + " > select:first").each(function() {
                    sub_combo = $(this);
                    //console.log('sub_combo_value:' + sub_combo.val());
                    sub_tex = sub_combo.next();
                    //console.log('sub_text_value:' + sub_tex.val());
                    pred_combo = sub_tex.next();
                    //console.log('pred_combo_value:' + pred_combo.val());
                    pred_tex = pred_combo.next();
                    //console.log('pred_text:' + pred_tex.val());
                    obj_combo = pred_tex.next();
                    //console.log('obj_combo_val:' + obj_combo.val());
                    obj_tex = obj_combo.next();
                    //console.log('obj_text:' + obj_tex.val());
                    pres = obj_tex.next();
                    //console.log('pres_val'+pres.val());

                    rows = rows.trim() + "*" + sub_tex.val() + "$" + pred_tex.val() + "$" + obj_tex.val() + "$" + pres.val();
                    //alert(rows);
                }
                );
            } //end-of vfilter each loop
        }
        );



        var message = "The advanced definition has been saved to the ontology.";

        var changesontology = $('#sel_dataset').val();
        var userDatasetLabel = $('#sel_dataset option:selected').text();
        var datasetURI = getDatasetURI(changesontology);
        startAction(true, "actions");


        $.get('ActionServlet', {action: "saveExt", update: on_edit, userDatasetURI: datasetURI, datasetLabel: userDatasetLabel, userChangesOntology: changesontology, vfilters: rows, name: ccName, prevname: prev_ccname, schanges: assignedscnames, opt_changes: opt_changes, priority: ccPriority, ccParams: ccParams, ccDescription: ccDescription, selects: selects, joins: joins}).done(function(responseText) {
            if (responseText === "success")
            {
                displayMessage(message);
            }

            else {
                showDialog('dialogmsg', "<p>" + responseText + "</p>"); //error-alert on response
            }
            finishAction();
        });
        //end-of valid checks         
    }
}


function saveCCDefByJSON() {
    var message = "The definition has been saved to the ontology.";
    var json_str = $('#jsonstring').val();

    //How to handle if parsing fails.
    try {
        var obj = jQuery.parseJSON(json_str);
        var ccName = obj.Complex_Change;

    }
    catch (err) {
        alert("JSON parsing error. Please check the json format and retry!");
    }

    //alert('ccName-------------->'+ccName);
    startAction(true, "actions");
    var changesontology = $('#sel_dataset').val();
    var datasetURI = getDatasetURI(changesontology);

    $.get('ActionServlet', {action: "saveByJSON", userDatasetURI: datasetURI, userChangesOntology: changesontology, name: ccName, ccJson: json_str}).done(function(responseText) {

        if (responseText === "success")
        {
            displayMessage(message);

        }
        else {
            showDialog('dialogmsg', "<p>" + responseText + "</p>"); //error-alert on response

        }

        finishAction();
    });

}



//called in add definition of a complex change from template (not advanced)
function saveCCDef() {
    var message = "The definition has been saved to the ontology.";
    var ccTemplate = selected_change;
    var user_val = $('#user_param_value').val();
    var selectedURI = user_val;
    if (!user_val) {
        selectedURI = $("#param_val option:selected").text();
        if (selectedURI === 'ALL(*)') {
            selectedURI = null; //enables ALL cases of URIs
        }
    }

    var ccName = $('#cc_name').val();
    var ccPriority = $('#priority').val();
    var userDatasetLabel = $('#sel_dataset option:selected').text();

    if (checkTemplateMandatoryFields(ccName, ccPriority)) {


        startAction(true, "actions");
        var changesontology = $('#sel_dataset').val();
        var datasetURI = getDatasetURI(changesontology);

        $.get('ActionServlet', {action: "save", userDatasetURI: datasetURI, userChangesOntology: changesontology, datasetLabel: userDatasetLabel, template: ccTemplate, name: ccName, priority: ccPriority, param_value_uri: selectedURI}).done(function(responseText) {

            if (responseText === "success")
            {
                displayMessage(message);

            }
            else {
                showDialog('dialogmsg', "<p>" + responseText + "</p>"); //error-alert on response

            }

            finishAction();
        });



    }
}

function checkTemplateMandatoryFields(ccName, ccPriority) {
    var msg = "<p>Please enter name and priority!</p>";
    if (ccName === '' || ccPriority === '') {
        showDialog('dialogmsg', msg);
        return false;
    }
    return true;
}


function startAction(requiresDetection, messagedivID /*, optionalmessage*/) {
    var patient_msg = "Reorganizing of changes ontology ... Please wait...";
    if (requiresDetection) {
        $("#" + messagedivID).html('<center><p>' + patient_msg + "</p><br></center>");
    }
    $("#" + messagedivID).append(loadingimg);
    //if on vision move message to top

    $('body').addClass("liveaction");

}

function finishAction() {

    $('body').removeClass("liveaction");
    $('#loading').remove();
    $('#actions').html('');


    /*$('#mainmenu').find('div,td,tr,tbody,table')
     .bind('click');
     console.log('ACTION FINISHED!');*/
}
//NOT-USED
function getUserName() {
    var user_info = $('.userinfo').html();
    //alert(user_info);
    if (user_info) {
        username = user_info.substring(8, user_info.indexOf("!"));
        //username = user_info.substring(user_info.indexOf("Welcome"),user_info.indexOf("!"));
    }
    //alert("%"+username+"%");
    return username;
}


function changeDataset() {

    $('.clickbuttons1').removeClass('fillmarktd');
    $('.clickbuttons2').removeClass('fillmarktd');
    $('.clickbuttons3').removeClass('fillmarktd');

    resetTables();
    $('#sel_dataset').val('');
    $('#sel_dataset').prop('disabled', false);
    //window.location = getContextPath();
}

function addUser() {
    var html = "<p> New user</p>\n" +
            "        <input type=\"text\" id=\"username\" name=\"username\" placeholder=\"enter a username\"> \n";
//"        <input type=\"button\" value=\"Login\" onclick=\"checkLogin($('#username').val(),true);\">";
}

function checkUser(user, loginoption) {
    var msg = "Please add an alias which should be used as a username for registered users.";
    var exists_msg = "This user has been already registered previously.\n Please select a different alias.";
    if (user === '') {
        showDialog('loginmsg', msg);
    }
    else {
        if (user === 'undefined' || user === undefined) {
            user = "guest";
        }
        username = user;
        $('#loginmsg').html(loadingimg);

        if (loginoption === "user-clean") {
            loginoption = "user";
        }
        else if (loginoption === "user") {
            loginiption = "user-clean";
        }
        else if (loginoption === "reg-user") {
            loginiption = "reg-user";
        }

        $.get('ActionServlet', {action: 'userexists', username: user, loginoption: loginoption}, function(responseText) {

            if (responseText === 'userexists') {
                showDialog('loginmsg', exists_msg);
            }
            else {

                checkLogin(user, loginoption);
            }
        });
    }

    function checkLogin(user, loginoption) {
        console.log('checkLogin user:' + user + " opt:" + loginoption);
        var no_user_msg = "Cannot found user with this alias. Please retry or create a new one.";

        if (username !== "guest") {


            var internal_uname = username;


            $.get('ActionServlet', {action: 'getInternalUserName', username: username, loginoption: loginoption}, function(responseText) {
                //alert('responseText'+responseText);
                internal_uname = responseText;

                if (responseText === 'nouser') {
                    showDialog('loginmsg', no_user_msg);
                }
                else
                {
                    var url = "main.jsp?username=" + user + "&loginoption=" + loginoption + "&intenrnaluser=" + internal_uname;
                    $(window.location).attr('href', url);

                    //Adds next user if needed (Asynchronously)
                    $.get('ActionServlet', {action: 'addnextuser', intenrnaluser: internal_uname, loginoption: loginoption}, function(responseText) {
                        //alert('addnextuser completed.......');
                    });
                }
            });

        }

        else { //guest-user login
            var url = "main.jsp?username=" + user;
            $(window.location).attr('href', url);
        }
    }
}



function getDatasetsCombo(divID) {//sel_dataset
    var intenrnaluser = getURLParameters("intenrnaluser");
    //var userDatasetURI = getURLParameters(userDatasetURI);

    $.get('OntologyQueryServlet', {qtype: 'changesontologies', intenrnaluser: intenrnaluser}, function(responseText) {
        //alert(responseText);
        $("#" + divID).html(responseText);
        //alert(responseText);

        // $("#" + divID).val($("#" + divID+" option:eq(1)").val());
        //$('#sel_dataset').prop('disabled', true);
    });
}

//checks if datasets options should be enabled via the "Options" button
function checkDatasetOptions() {
    var options_button = "<input class=\"OptionsButton button\" type=\"button\" value=\"Options\" onclick=\"showDatasetOptions();\">";
    $.get('OntologyQueryServlet', {qtype: 'dsoptions'}, function(responseText) {
        //JCH2018
       
        if (responseText === 'enabled') {
            $('#dsoptions').html(options_button);
        }

    });

}