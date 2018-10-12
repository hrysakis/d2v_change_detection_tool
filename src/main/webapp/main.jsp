
<!DOCTYPE HTML PUBLIC "-//W3C//DTD HTML 4.0 Transitional//EN""http://www.w3.org/TR/REC-html40/loose.dtd">
<html lang="en">
<head>
<meta name="description" content="FORTH-ICS">
<meta name="keywords" content="change detection tool, change detection, evolution, change management">
<meta name="author" content="Ioannis Chrysakis">    
<meta http-equiv="Content-Type" content="text/html; charset=utf-8">

<title>D2V:A Tool for Defining, Detecting and Visualizing Changes on the Data Web</title>

<link href="css/mainstyle.css" rel="stylesheet" type="text/css">

<link href="http://ajax.googleapis.com/ajax/libs/jqueryui/1.10.3/themes/smoothness/jquery-ui.css" rel="stylesheet" type="text/css">
<!--script src="http://code.jquery.com/jquery-latest.min.js"></script-->
<script type="text/javascript" src="//code.jquery.com/jquery-1.11.2.min.js"></script>
<script type="text/javascript" src="//code.jquery.com/ui/1.11.2/jquery-ui.js"></script>

<script type="text/javascript" src="js/d2v.js"></script>
<script type="text/javascript" src="js/generic.js"></script>


</head>



<body style="min-width:1150px;">
  
<div id="mainmenu"  class="inside">
    <p class="userinfo">Welcome
    <%=request.getParameter("username")%>
    !</p> 
    <a class="logout" href="index.html"><img alt="Logout" src="images/Logout-icon-01.png" height="25" width="25"></a>
    
<table style="width:180px; border-spacing:0px;"> <!-- cellspacing='0' is important,and equivalent to border-spacing-->
    <tr>
	  <th style="width:174px;">Menu</th></tr>
  <tr>
              <td><label for="sel_dataset">Select dataset:</label><select name="sel_dataset" id="sel_dataset" class="select-menu">
 </select> 
                  <br><br>
              <input class="ChangeButton button" type="button" value="Change dataset" onclick="changeDataset();">
              <div id="dsoptions"></div>
              </td></tr> 
<tr>
    <td class="clickable clickbuttons1" onclick="buildCCTypeMenu(this)"><a href="#"><div class="menu">Define complex change</div><img alt="add" class="menu_img" src="images/add.png" width="27" height="27"></a></td></tr>  
<tr>
    <td class="clickable clickbuttons1" onclick="createCCTables(false,'',this)"><a href="#"><div class="menu">Define complex change (advanced)</div><img alt="addplus" class="menu_img" src="images/add_plus.png" width="27" height="27"></a></td></tr>
<tr>
    <td class="clickable clickbuttons1" onclick="getDefinedCC('edit',this)"><a href="#"><div class="menu">Edit complex change</div><img class="menu_img" alt="edit" src="images/edit.png" width="27" height="27"></a></td>
</tr> 
<tr>
    <td class="clickable clickbuttons1" onclick="getDefinedCC('del',this)"><a href="#"><div class="menu">Delete complex change</div><img class="menu_img" alt="delete" src="images/delete.png" width="27" height="27"></a></td></tr> 


<tr>
    <td class="clickable clickbuttons1" onclick="showVisionOptions(this)"><a href="#"><div class="menu">Visualize changes</div><img class="menu_img" alt="visualisation" src="images/chart.png" width="25" height="25"></a></td></tr>

</table> 
    <p class="powered"> Powered and sponsored by:</p>
    <a class="logos" href="http://www.diachron-fp7.eu" target="_blank"><img alt="diachron" src="images/diachron_small.png"></a>
    <br>
    <a class="logo_idea" href="http://idea-garden.org/" target="_blank"><img alt="ideagarden" src="images/ideagarden.png" width="100"></a> 


    <br><br><p class="powered"> <a href="#" onclick="showCredits()"> <img alt="copyright" src="images/copyright.png" width="30" height="30">Copyright</a>,<a href="changeLOG.txt" target="_blank"> V6.3, </a><a href="http://www.ics.forth.gr" target="_blank">FORTH-ICS</a> </p>
    <br><p><a href="https://www.w3.org/TR/WCAG20/#a" target="_blank"><img alt="WCAG" src="images/w3c.png" style="margin-left:45px;"></a></p>
</div>   
    

    
    <div id="cc_menu" class="inside"></div>
    
    
    <div id="father" class="inside">
        <div id="cc_types" class="inside"></div>
        <div id="cc_ext_params"></div>
    </div>
    <div id="cc_params" class="inside" ></div>
    <div id="actions"></div>
    <div id="dialogmsg"></div>
    
    <script type="text/javascript">
     getDatasetsCombo('sel_dataset');
     
     
     <%String username=request.getParameter("username");
     %>
     var username="<%=username%>"; 

     if (username ==='undefined'){
         username = 'guest';
     }
    $("#userinfo").html("Welcome "+username+"...");
    /*
    $('#sel_dataset').change(function() {
    var loginoption = getURLParameters("loginoption");
     checkLogin(username,loginoption, false);
    });*/
    
    checkDatasetOptions();
    </script>  
    
    <!-- Start of StatCounter Code for Dreamweaver -->
<script type="text/javascript">
var sc_project=10416403; 
var sc_invisible=1; 
var sc_security="8af7adc9"; 
var scJsHost = (("https:" == document.location.protocol) ?
"https://secure." : "http://www.");
document.write("<sc"+"ript type='text/javascript' src='" +
scJsHost+
"statcounter.com/counter/counter.js'></"+"script>");
</script>
<noscript><div class="statcounter"><a title="shopify
analytics ecommerce tracking"
href="http://statcounter.com/shopify/" target="_blank"><img
class="statcounter"
src="http://c.statcounter.com/10416403/0/8af7adc9/1/"
alt="shopify analytics ecommerce
tracking"></a></div></noscript>
<!-- End of StatCounter Code for Dreamweaver -->
</body>
</html>
