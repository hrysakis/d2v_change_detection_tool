/**
 *
 * @author Ioannis Chrysakis (hrysakis@ics.forth.gr)
 */
package servlets;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import store.QueryUtils;
import org.diachron.detection.exploit.DetChange;
import org.diachron.detection.repositories.JDBCVirtuosoRep;
import org.diachron.detection.utils.ChangesManager;
import org.diachron.detection.utils.DatasetsManager;
import utils.TranslationUtils;

/**
 * This servlet used to process all the query requests from the RDF store.
 */
public class OntologyQueryServlet extends HttpServlet {

    public static String configPath = "";
    public static String contextPath = "";
    private int initLimit = 3; //JCH:big limit to retrieve them all
    int largeLimit = 1000000; //In practise means retrieve them all

    /**
     * Processes query requests for both HTTP <code>GET</code> and
     * <code>POST</code> methods.
     *
     * @param request servlet request
     * @param response servlet response
     * @throws ServletException if a servlet-specific error occurs
     * @throws IOException if an I/O error occurs
     */
    protected void processRequest(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        response.setContentType("text/html;charset=UTF-8");
        response.setHeader("Access-Control-Allow-Origin", "*");
        String selected_change_template = request.getParameter("sc");
        String query_type = request.getParameter("qtype");
        System.out.println("OQS query_type:" + query_type);

        String changesOntology = request.getParameter("dataset");  //dataset ~> ontology named graph uri
        String changesOntologySchema = changesOntology + "/schema";
        String datasetURI = request.getParameter("datasetversions");
        String paramvaltype = request.getParameter("paramvaltype");
        PrintWriter out = response.getWriter();

        QueryUtils fur = null;
        DatasetsManager dmgr1 = null;
        boolean activeConnection = false;

        ServletContext servletContext = getServletContext();
        contextPath = servletContext.getRealPath("/"); //File.separator
        configPath = OntologyQueryServlet.getConfigFilePath(servletContext, changesOntology);
        System.out.println("config_path _______________________->" + configPath);
        Properties prop = new Properties();
        InputStream inputStream;
        try {
            inputStream = new FileInputStream(configPath);
            prop.load(inputStream);
        } catch (IOException ex) {
            System.out.println("Exception with prop file: " + ex.getMessage() + " occured .");
            //return;
        }
        try {
            dmgr1 = new DatasetsManager(prop, datasetURI);
        } catch (Exception ex) {
            System.out.println("Message:" + ex.getMessage());
            showErrorMessage(out, ex.getMessage());
            dmgr1 = null;
        }

        if (query_type != null && query_type.equals("changesontologies")) {

            try {
                String username = request.getParameter("intenrnaluser");
                //userDatasetURI (contains clean?)
                if (username == null) {
                    username = "guest";
                }
                Map<String, String> ds_map_init = dmgr1.fetchDatasets();
                Map<String, String> ds_map = new LinkedHashMap<>();
                for (String uri : ds_map_init.keySet()) {
                    if (uri.endsWith("/" + username)) {
                        ds_map.put(uri, ds_map_init.get(uri));
                    }
                }

                if (ds_map.isEmpty()) { //
                    showErrorMessage(out, "No any assigned dataset for this user. Please check your loaded datasets or logout and use guest account.");
                } else {

                    String html_str = "";
                    String empty_val = "<option value=\"nop\"> </option>";
                    String ds_label = "";
                    for (String key : ds_map.keySet()) {
                        ds_label = ds_map.get(key);
                        if (ds_label == null || ds_label.equals("")) {

                            ds_label = key;
                        }

                        if (key.endsWith("/")) {
                            key = key + "changes"; //the key is the datasetURI

                        } else {
                            key = key + "/" + "changes";
                        }
                        html_str = html_str.trim() + "<option value=\"" + key + "\">" + ds_label + "" + "</option>";
                    }

                    out.print(empty_val + html_str);
                }

            } catch (Exception ex) {
                dmgr1 = null;
            }

        } 

        
        else  if (query_type != null && query_type.equals("dsoptions")) {
            // checks for existing properties at config file
            
          String ds_files_folder = OntologyQueryServlet.getPropertyFromFile(configPath, "Dataset_Files_Folder");
          String ds_default_schema = OntologyQueryServlet.getPropertyFromFile(configPath, "Dataset_Default_Schema");
          if (ds_files_folder != null && ds_default_schema != null){
              out.print("enabled");
          }
          else{
              out.print("disabled");
          }
        
        }
        else if (query_type != null && query_type.equals("dataseturi")) {
            String selected_dataset_uri = "";
            String dlabel = request.getParameter("dslabel");
            try {
                if (dlabel != null && !dlabel.equals("")) {
                    dmgr1 = new DatasetsManager(prop,null);
                    selected_dataset_uri = dmgr1.fetchDatasetUri(dlabel);
                    System.out.println("DATASET_URI is:" + selected_dataset_uri);
                    out.print(selected_dataset_uri);

                } else {
                    dmgr1 = null;
                }
            } catch (Exception ex) {
                dmgr1 = null;
            }

        } //not currently used
        else if (query_type != null && query_type.equals("graphexistance")) {
            String changesgraph = request.getParameter("changesgraph");
            if (changesgraph != null && !changesgraph.equals("")) {
                try {
                    if (this.namedGraphExists(dmgr1.getJDBCVirtuosoRep(), changesgraph)) {
                        System.out.println(changesgraph + " NAMED-GRAPH EXISTS!!");
                        out.print("exists");
                    }
                } catch (SQLException ex) {
                    System.out.println("Internal problem:" + ex.getMessage());
                    showErrorMessage(out, "Internal problem:" + ex.getMessage());
                } catch (ClassNotFoundException ex) {
                    showErrorMessage(out, "Internal problem:" + ex.getMessage());
                }
            }

        } ////////////// READ FROM CONFIG FILE DATASET OPTIONS //////////////////////////////       
        //search in config file for changesontologies and build the left combo dataset
        else if (query_type != null && query_type.equals("changesontologies_fromfile")) {

            String chontologies = OntologyQueryServlet.getPropertyFromFile(configPath, "Ontologies");
            String datasetlabels = OntologyQueryServlet.getPropertyFromFile(configPath, "Dataset_labels");

            if (chontologies == null || datasetlabels == null) {
                out.print("ConfigFileError");
                System.out.println("Error on the configuration file.");

            } else {
                String[] chonts = chontologies.split("\\,");
                String[] dlabls = datasetlabels.split("\\,");
                String html_str = "";
                String empty_val = "<option value=\"nop\"> </option>";
                for (int i = 0; i < chonts.length && i < dlabls.length; i++) {
                    if (!chonts[i].equals("") && !dlabls[i].equals("")) {
                        html_str = html_str.trim() + "<option value=\"" + chonts[i] + "\">" + dlabls[i] + "</option>";
                    }
                }
                out.print(empty_val + html_str);
            }
        } //search in config file for datasetversion uri 
        else if (query_type != null && query_type.equals("dataseturi_fromfile")) {
            String dataseturis = OntologyQueryServlet.getPropertyFromFile(configPath, "Dataset_URIs");
            String chontologies = OntologyQueryServlet.getPropertyFromFile(configPath, "Ontologies");

            if (chontologies == null || dataseturis == null) {
                out.print("ConfigFileError");
                System.out.println("Error on the configuration file...");

            } else {
                String[] chonts = chontologies.split("\\,");
                String[] duris = dataseturis.split("\\,");
                String selected_dataset_uri = "";
                String selected_ontology = changesOntology;

                for (int i = 0; i < chonts.length && i < duris.length; i++) {
                    if (!duris[i].equals("") && !chonts[i].equals("") && selected_ontology != null && selected_ontology.contains(duris[i])) {
                        selected_dataset_uri = duris[i];
                        out.print(selected_dataset_uri);
                        //return;
                    }
                }
            }
        } ////////////////////////////////////////////////////////////////////////////////////////////// 
        else { //connect to Triple store
            //System.out.println("QS-Config-path:" + configPath);
            try {

                if (datasetURI != null && changesOntology != null) {

                    System.out.println("OQS changesOntology:" + changesOntology);
                    System.out.println("OQS datasetURI (versions):" + datasetURI);

                    fur = new QueryUtils(prop, datasetURI, changesOntologySchema);
                    activeConnection = true;
                }

            } catch (Exception ex) {

                activeConnection = false;
                showErrorMessage(out, ex.getMessage());

            }
            //////////////////////////////////////////////////////////////////    

            //System.out.println("CONNECTIONNNNNNNNNNNNNNNNNNNNNNNN:[" + activeConnection + "]");
            //different html results depending on the query_type    
            try {

                if (activeConnection && query_type != null && fur != null) {

                    if (query_type.equals("limitresults")) {
                        String nversion = request.getParameter("newversion");
                        String newversion = ""; //te be calculated if nversion= version label
                        String oldversion = request.getParameter("oldversion");
                        String changename = request.getParameter("changename");
                        String sradio = request.getParameter("sradio");
                        String groupby = request.getParameter("groupby");
                        String limit = request.getParameter("limit");
                        
                        int limitPerChange = initLimit;
                        if (limit != null && limit.equals("unlimited")){
                           limitPerChange = largeLimit;
                        }
                        
                        //Necessary for custom compare
                        System.out.println("oldversion:" + oldversion);
                        System.out.println("newversion(label):" + nversion);
                        //
                        System.out.println("changename:" + changename);
                        System.out.println("sradio:" + sradio);
                        System.out.println("groupby:" + groupby);

                        String sclist = request.getParameter("sclist"); // "null" or comma string simple changes
                        String cclist = request.getParameter("cclist"); // "null" or comma string complex changes
                        String vlist = request.getParameter("vlist"); //ALL or comma string newversions
                        Set<String> myversions = ExtVisualiseServlet.getVersionsSet(vlist);
                        Set<String> mychanges = ExtVisualiseServlet.getChangesSet(sclist, cclist);

                        Set<DetChange> changes = null;
                        String tempontology = "no";

                        boolean tempOntologyExists = false;

                        if (sradio != null && !sradio.equals("")) {

                            if (sradio.equals("compareradio")) {
                                myversions = new LinkedHashSet<>();

                                String tempOntology = OntologyQueryServlet.getTempOntology(prop, datasetURI, oldversion, nversion);

                                if (OntologyQueryServlet.namedGraphExists(dmgr1.getJDBCVirtuosoRep(), tempOntology)) {
                                    tempOntologyExists = true;
                                    tempontology = "yes";
                                    myversions.add(tempOntology); //
                                    vlist = tempOntology;
                                } else { //sequential case
                                    tempOntologyExists = false;

                                    myversions.add(nversion);
                                }
                                mychanges = new LinkedHashSet<>();
                                mychanges.add(changename);
                                sclist = changename;
                            } else {
                                //newversionlabel
                                if (groupby != null && groupby.equals("versionsradio")) {
                                    myversions = new LinkedHashSet<>();
                                    newversion = this.getVersionByLabel(dmgr1, datasetURI, nversion);
                                    myversions.add(newversion); //selected version from pie (chart)
                                    vlist = newversion;
                                } else {
                                    mychanges = new LinkedHashSet<>();
                                    mychanges.add(changename); //selected change from pie (chart)
                                    sclist = changename;
                                }
                            }

                            System.out.println("OQS_LR VERSIONS:" + myversions.toString());
                            System.out.println("OQS_LR CHANGES:" + mychanges.toString());
                            changes = fur.fetchDetChangesForVersions(myversions, mychanges, tempOntologyExists, limitPerChange);
                            // System.out.println("OQS DET CHANGES::::::::::::::"+changes.toString());
                            if (changes.isEmpty()) {
                                out.print("empty"); //den xrhsomopoiw showErrorMessage giati paizei sto dialogmsg DIV kai sto kentro
                            } else {
                                out.println(this.displayTableResults(changes, changesOntology, datasetURI, "", sclist, cclist, vlist, false, tempontology));
                            }
                        } else {
                            out.print("empty");
                        }

                    }

                    if (query_type.equals("termevolution")) {

                        String uriORliteral = request.getParameter("uri");
                        //param_value_ns = param_value_ns.replaceAll("\\+", "%20");

                        uriORliteral = URLDecoder.decode(uriORliteral, "UTF-8");

                        String sclist = request.getParameter("sclist"); // "null" or comma string simple changes
                        String cclist = request.getParameter("cclist"); // "null" or comma string complex changes
                        String vlist = request.getParameter("vlist"); //ALL or comma string newversions
                        String tempontology = request.getParameter("tempontology");
                        boolean tempOntology = false;
                        if (tempontology != null && tempontology.equals("yes")) {
                            tempOntology = true;
                        }

                        //System.out.println("uriORliteral::::::::::decoded::::::---" + uriORliteral);
                        String message = "No results to show for this term, please select another.";
                        Set<String> myversions = ExtVisualiseServlet.getVersionsSet(vlist);
                        Set<String> mychanges = ExtVisualiseServlet.getChangesSet(sclist, cclist);
                        System.out.println("OQS_TERM VERSIONS:" + myversions.toString());
                        System.out.println("OQS_TERM CHANGES:" + mychanges.toString());

                        Set<DetChange> changes = fur.fetchDetChangesContainValue(uriORliteral, myversions, mychanges, tempOntology);
                        if (changes.size() > 0) {

                            String new_html_str = this.displayTableResults(changes, changesOntology, datasetURI, uriORliteral, sclist, cclist, vlist, true, tempontology);

                            out.println("<!DOCTYPE html>");
                            out.println("<html>");
                            out.println("<head>");
                            out.println("<title>A Tool for Defining, Detecting and Visualizing Changes on the Data Web</title>");
                            out.println("<link href=\"css/mainstyle.css\" rel=\"stylesheet\" type=\"text/css\"> \n"
                                    + "<link href=\"http://ajax.googleapis.com/ajax/libs/jqueryui/1.10.3/themes/smoothness/jquery-ui.css\" rel=\"stylesheet\" type=\"text/css\">");
                            out.println("</head>");
                            out.println("<body>");

                            out.println("<h2> Term Evolution for: " + uriORliteral + "</h1>");
                            out.println(new_html_str);
                            out.println("</body>");
                            out.println("</html>");

                        } else {

                            out.println("<!DOCTYPE html>");
                            out.println("<html>");
                            out.println("<head>");
                            out.println("<script src=\"//code.jquery.com/jquery-1.11.2.min.js\"></script>\n"
                                    + "<script src=\"//code.jquery.com/ui/1.11.2/jquery-ui.js\"></script>");
                            out.println("<link href=\"css/mainstyle.css\" rel=\"stylesheet\" type=\"text/css\"> \n"
                                    + "<link href=\"http://ajax.googleapis.com/ajax/libs/jqueryui/1.10.3/themes/smoothness/jquery-ui.css\" rel=\"stylesheet\" type=\"text/css\">");
                            out.println("<script src=\"js/generic.js\"></script>");
                            //out.print("<script language='JavaScript'>alert('Hello');</script>");
                            out.println("</head>");
                            out.println("<body>");
                            out.println("<div id=\"dialogmsg2\">");
                            out.println("</div>");
                            out.println("<script type=\"text/javascript\">showDialog('dialogmsg2'," + "'" + message + "'" + ");</script>");
                            out.println("</body>");
                            out.println("</html>");
                            //showErrorMessage(out,message );
                        }
                    }

                    if (query_type.equals("templates")) {
                        //saveCCDef(ccTemplate, selectedURI, ccName, ccPriority )

                        int resultslimit = 10;
                        String param_value_str = "";
                        String select_def_str = "<select name=\"param_val\" id=\"param_val\">\n";
                        String enter_value = "<option value=\"Enter a user-defined value\">Enter a user-defined value</option>";
                        String all_value = "<option value=\"Not specified\">ALL(*)</option>";
                        String option_val_str = " ";
                        List<String> uris = null;

                        uris = fur.fetchURIsByChangeType(selected_change_template, resultslimit);

                        if (uris == null || uris.isEmpty()) { //ean einai keno kanw disable to select box
                            /// select_def_str = "<select disabled name=\"param_val\" id=\"param_val\">\n";
                            all_value = "";
                        } else {
                            for (String uri : uris) {
                                //System.out.println(uri);
                                option_val_str = option_val_str.trim() + "<option value=\"" + uri + "\">" + uri + "</option>";
                            }
                        }

                        if (selected_change_template != null /*&& !selected_change_template.startsWith("Update")*/) { // claims for Add_XXX and Delete_XXX
                            param_value_str = "Choose " + paramvaltype + ": [Most popular list]</p>\n"
                                    + "<p>\n"
                                    + select_def_str
                                    + "<option value=\"\"></option>"
                                    + option_val_str.trim()
                                    + all_value
                                    + enter_value
                                    + " </select>\n"
                                    + "</p>\n"
                                    + "<p>User defined\n"
                                    + paramvaltype + ":\n"
                                    //+ " parameter value:\n"
                                    + "<input disabled type=\"text\" name=\"user_param_value\" id=\"user_param_value\" />\n"
                                    + "</p>\n";
                        }
                        String parameters = "<form id=\"userform\" name=\"userform\" method=\"post\" action=\"\">"
                                + ""
                                + "<table width=\"218\" border=\"1\" cellpadding=\"10\">\n"
                                + " <tr>\n"
                                + "<th width=\"174\">User input</th></tr>"
                                + "<tr>\n"
                                + "<td width=\"150\"><p>\n"
                                + param_value_str
                                + "<p>Complex change name*:\n"
                                + "\n"
                                + "<input type=\"text\" name=\"cc_name\" id=\"cc_name\" />\n"
                                + " </p>\n"
                                + "<p>Priority*:</p>\n"
                                + "<p>\n"
                                + "\n"
                                + "<input type=\"text\" name=\"priority\" id=\"priority\" />\n"
                                + "</p> </br>"
                                + "<input type=\"submit\" name=\"save\" id=\"save\" class=\"Button\" value=\"Save\" onclick=\"saveCCDef(); return false;\"/>\n"
                                + "<input type=\"button\" name=\"clear\" class=\"Button\" value=\"Reset\" onclick=\"resetTables(); return false;\">"
                                + "</td>\n"
                                + "\n"
                                + "</tr>\n"
                                + "<tr>\n"
                                + "\n"
                                + "</tr>\n"
                                + "</table>\n"
                                + "</form>";

                        out.println(parameters);

                    } else if (query_type.equals("versions")) {

                        String valuesType = request.getParameter("valuestype");
                        String version_combos = buildVersionCombos(dmgr1, valuesType);

                        out.println(version_combos);

                    } else if (query_type.equals("evoversions")) {
                        //String vold = request.getParameter("vold");
                        //String vnew = request.getParameter("vnew");
                        String valuesType = request.getParameter("valuestype");
                        String version_combos = buildEvoVersionPairElements(dmgr1, "", "", valuesType);
                        out.println(version_combos);

                    } else if (query_type.equals("defchanges")) {

                        String changetype = request.getParameter("changetype");//changetype :complex h' simple h' null (all)
                        String action = request.getParameter("action");
                        String cc_src = "";

                        if (action != null && (action.equals("delete") || action.equals("edit"))) {
                            cc_src = buildChangesCBoxes(fur, changesOntologySchema, changetype, true); //return result in tr/td format
                        } else {
                            cc_src = buildChangesCBoxes(fur, changesOntologySchema, changetype, false);
                        }

                        out.print(cc_src);
                    } else if (query_type.equals("ccjson")) {
                        String selected_change = request.getParameter("chname");
                        String json_str = "";
                        if (selected_change != null) {
                            json_str = fur.fetchCCJson(selected_change);
                            //System.out.println("json_str:" + json_str);
                            out.print(json_str);
                        }

                    } else if (query_type.equals("scparams")) {
                        String sc_parameters = "";
                        String selected_change = request.getParameter("chname");
                        String chfullname = request.getParameter("chfullname");
                        sc_parameters = getSCParameterNamesComboValues(fur, selected_change, chfullname, "", false);
                        out.println(sc_parameters);

                    } else if (query_type.equals("scspecparams")) {
                        boolean excludeSelfChange = false;
                        String sc_parameters = "";//schanges
                        String scnames = request.getParameter("schanges");
                        String selected_change = request.getParameter("chname");
                        //chfullname
                        String chfullname = request.getParameter("chfullname");
                        String excludechange = request.getParameter("excludeme");
                        if (excludechange != null && excludechange.equals("yes")) {
                            excludeSelfChange = true;
                        }

                        sc_parameters = getSCParameterNamesComboValues(fur, selected_change, chfullname, scnames, excludeSelfChange);
                        out.println(sc_parameters);

                    } else if ((query_type.equals("Simple_Change") || query_type.equals("Complex_Change"))) {
                        String simple_changes = "";
                        String valuesType = request.getParameter("valuestype");
                        if (valuesType != null && valuesType.equals("option")) { //return values for select box(es)
                            String init_option_changes_val = "<option value=\"nop\"> </option>\n";
                            simple_changes = init_option_changes_val + getChangesValues(fur, query_type, valuesType, changesOntology + "/schema");
                        } else { //valueType //option,radio,str
                            simple_changes = getChangesValues(fur, query_type, valuesType, changesOntology + "/schema");
                        }
                        out.println(simple_changes);

                    }

                }

            } catch (Exception ge) { // general exception
                System.out.println("General Exception:" + ge.getMessage());
                showErrorMessage(out, "General problem:" + ge.getMessage());
            }
        }

        if (fur != null /*&& expl != null*/) {
            //expl.terminate();
            fur.terminate(); //CLOSES CONNECTION  
            //System.out.println("fur terminated...CLOSED connection");

        }
        if (dmgr1 != null) { //CLOSES CONNECTION
            dmgr1.terminate();
            //System.out.println("dmgr terminated...CLOSED connection");
        } else { //NO-CONNECTION

            if (dmgr1 == null || fur == null) { //no any connection through DatasetsManager/FetchUris
                System.out.println("Connection error with the Virtuoso!");
                showErrorMessage(out, "Connection error with the Virtuoso. Please try again later!");
                //out.println("<p><font size=\"2\" color=\"red\"> Connection error with the Virtuoso! </font> </p>");
            }
        }
    }

    private String buildChangesCBoxes(QueryUtils fur, String ontology_uri, String changeType, boolean deleteaction) {
        String cc_checkboxes = "";
        String ccname = "";
        String html_str = "";
        if (fur != null && ontology_uri != null && changeType != null) {
            List list_cc = fur.fetchChangesNames(changeType);

            for (Object list_name : list_cc) {
                ccname = (String) list_name;
                if (!ccname.equals("")) {
                    //System.out.println("CCNAME:"+ccname);
                    if (deleteaction) {

                        cc_checkboxes = cc_checkboxes.trim() + "<input name=\"" + "samename" + "\" type=\"radio\" class=\"cc_checkbox\"/ value=\"" + ccname + "\">" + ccname + "<br><br>";
                    } /*else { //periptwsh vision fere ta xwris na einai mesa se td

                     }*/

                }
            }

        }
        return cc_checkboxes;
    }

    private String getSCParameterNamesComboValues(QueryUtils fur, String sc_name, String changefullname, String alldefinedSC, boolean excludemyself) {

        String simplechanges_val = "<option value=\"nop\"> </option>\n";
        String cur_param = "";
        String clear_change = "";
        List list_params = fur.fetchSCParameterNames(sc_name);
        if (alldefinedSC.equals("")) {  //single case   
            for (int i = 0; i < list_params.size(); i++) {
                cur_param = (String) list_params.get(i);
                simplechanges_val = simplechanges_val.trim() + "<option value=\"" + changefullname + ":-" + cur_param + "\">" + cur_param + "</option>\n";
            }
        } else { //prepei na labw ypopsin tis dilomenes gia na bgalw ta antistoixa onomata parametrwn me arithmisi

            String[] parts = alldefinedSC.split("\\$"); //extract specified names of simple changes
            for (int j = 0; j < parts.length; j++) { //For all defined simple changes
                //System.out.println("Checking changefullname/parts:"+changefullname+"/"+parts[j]);
                //System.out.println("Excludeme:"+excludemyself);
                if (!parts[j].equals("") && !parts[j].contains("nop")) {
                    //if (!excludemyself /*&& !changefullname.equals(parts[j])*/) {
                    if (!excludemyself || (excludemyself && !changefullname.equals(parts[j]))) {
                        clear_change = parts[j].substring(2).trim(); //parts[j] contains full names
                        list_params = fur.fetchSCParameterNames(clear_change);

                        for (int i = 0; i < list_params.size(); i++) { //Find all parameters
                            cur_param = (String) list_params.get(i); // Assign values
                            //simplechanges_val = simplechanges_val.trim() + "<option value=\"" + clear_change + "." + cur_param + "\">" + parts[j] + ":-" + cur_param + "</option>\n";
                            simplechanges_val = simplechanges_val.trim() + "<option value=\"" + parts[j] + ":-" + cur_param + "\">" + parts[j] + ":-" + cur_param + "</option>\n";
                        }

                    }

                }
            }

            //System.out.println("alldefinedSC:" + alldefinedSC);
        }

        return simplechanges_val;
    }

    private String getChangesValues(QueryUtils fur, String changeType, String valuesType, String ontology_uri) {

        String changes_val = "";
        //String init_cbox_changes_val = "";
        String cur_sc = "";
        //"Simple_Change", option
        List list_schanges = fur.fetchChangesNames(changeType);
        String classbox = "noclass";
        if (changeType.equals("Simple_Change")) {
            classbox = "simclass";
        } else if (changeType.equals("Complex_Change")) {
            classbox = "coclass";
        }

        for (int i = 0; i < list_schanges.size(); i++) {
            cur_sc = (String) list_schanges.get(i);
            if (valuesType != null && valuesType.equals("option")) {
                changes_val = changes_val.trim() + "<option value=\"" + cur_sc + "\">" + cur_sc + "</option>\n";
            } else if (valuesType != null && valuesType.equals("str")) { //Returns a comma string of changes
                changes_val = changes_val.trim() + "," + cur_sc;
            } else {
                changes_val = changes_val.trim() + "<input class=\"tabright " + classbox + "\" type=\"checkbox\" value=\"" + cur_sc + "\">" + cur_sc + "<br>\n";
            }
        }

        return changes_val;
    }

    private String buildEvoVersionPairElements(DatasetsManager dmgr, String v1, String v2, String valuesType) {
        String version_ret_val = "";
        String version_key = "";
        String version_key_next = "";
        String version_ret_label = "";
        String version_label = "";
        String version_label_next = "";
        String vfirst, vlast = "";
        List versions = new ArrayList();

        if (dmgr != null) {

            Map<String, String> versions_map = dmgr.fetchDatasetVersions();
            versions = new ArrayList(versions_map.keySet());

            //Range query - eliminate versions (not-used)
           /* if (!v1.equals("") && !v1.equals("nop") && !v2.equals("") && !v2.equals("nop") && !(v1.equals(versions.get(0)) && v2.equals(versions.get(versions.size() - 1)))) {
             versions_map = dmgr.fetchDatasetRangeVersions(versionsuri, v1, v2);
             versions = new ArrayList(versions_map.keySet());
             }*/
            int j = 0;
            for (int i = 0; i < versions.size(); i++) {
                version_key = (String) versions.get(i);
                j = i + 1;
                if (j < versions.size()) {
                    version_key_next = (String) versions.get(j);
                    version_label = versions_map.get(version_key);
                    version_label_next = versions_map.get(version_key_next);

                    //Handling versions without labels
                    if (version_label == null || version_label.equals("")) {
                        version_label = version_key;
                    }
                    if (version_label_next == null || version_label_next.equals("")) {
                        version_label_next = version_key_next;
                    }
                    //
                    version_ret_label = version_label + " VS. " + version_label_next;
                    if (valuesType.equals("option")) {
                        version_ret_val = version_ret_val.trim() + "<option value=\"" + version_key_next + "\">" + version_ret_label + "</option>\n";
                    } else if (valuesType.equals("cbox")) {
                        version_ret_val = version_ret_val.trim() + "<p>\n" + "<input name=\"vcbox_" + version_ret_label + "\" id=\"vbox_" + version_ret_label + "\" type=\"checkbox\" value=\"" + version_key_next + "\" class=\"vsoption\">\n" + " " + version_ret_label + "</p> \n";
                    } else if (valuesType.equals("str")) {
                        vfirst = versions_map.get((String) versions.get(0));
                        vlast = versions_map.get((String) versions.get(versions.size() - 1));
                        version_ret_val = "(between versions " + vfirst + " - " + vlast + ")";
                    }
                }
            }

            //String opt_values = "<option value=\"nop\">Choose a pair</option>\n" + version_ret_val.trim();
            return version_ret_val;
        }
        return "";

    }

    private String buildVersionCombos(DatasetsManager dmgr, String valuesType) {
        String version_ret_val = "";
        String version = "";
        String version_label = ""; //e.g 2.31

        if (dmgr != null) {

            Map<String, String> versions_map = dmgr.fetchDatasetVersions();

            for (String key : versions_map.keySet()) {

                version = key;
                version_label = versions_map.get(key);
                if (version_label == null || version_label.equals("")) {
                    version_label = version;
                }
                if (valuesType != null) {
                    if (valuesType.equals("option")) {
                        version_ret_val = version_ret_val.trim() + "<option value=\"" + version + "\">" + version_label + "</option>\n";
                    } else if (valuesType.equals("radio")) {
                        version_ret_val = version_ret_val.trim() + "<input name=\"" + "version" + "\" type=\"radio\" value=\"" + version + "\">" + version_label + "<br><br>";
                    }
                }
            }
            if (valuesType != null && valuesType.equals("option")) {
                String opt_values
                        = "<option value=\"nop\"> </option>\n"
                        + version_ret_val.trim();
                return opt_values;
            } else { //radio buttons case
                return version_ret_val;
            }

        }
        return "";

    }

    private String displayTableResults(Set<DetChange> changes, String dataset_uri, String dataset_versions_uri, String uriORliteral, String sclist, String cclist, String vlist, boolean multiplechanges, String tempontology) {
        LinkedHashMap<String, String> cur_params;
        String html_str = "";
        String param_value = "";
        String vold = "";
        String vnew = "";
        String cutoversion = "";
        String cutnversion = "";
        String change_description ="";
        String change_name ="";
        TranslationUtils trutils = null; //JCH: if null translations are disabled
        boolean exactMatchTranslation = false; //JCH: if true only the exact matching terms should be translated

        try {
            String translations_path = contextPath + "lang/en.txt";
            trutils = new TranslationUtils(translations_path, exactMatchTranslation);

        } catch (IOException ex) {
            System.out.println("Could not read translations!");
        }

        int paramnum = 0;
        String param_info = "";
        for (DetChange cur_change : changes) {

            vold = cur_change.getOldVersion();
            vnew = cur_change.getNewVersion();
            change_description = cur_change.getChangeDescription();
            change_name = cur_change.getChangeName();
            if (change_description == null || change_description.equals("")){
                change_description = change_name;
            }
            cutoversion = vold.substring(vold.lastIndexOf("/") + 1);
            cutnversion = vnew.substring(vnew.lastIndexOf("/") + 1);
            cur_params = cur_change.getParameters();
            paramnum = cur_params.size();
            int cnt = 0;
            String param_names_row = "<tr><th colspan=\"" + "100%" + "\" style=\"text-align:left;\">" + "<font color=\"green\">" +"<span title=\""+change_description+"\"</span>"+
            "[" + vold + "-" + vnew + "] " + change_name + "</font></th></tr>";
            
            String param_values_row = "";
            String param_value_ns = "";
            String param_value_special = "";
            String param_value_enc = "";
            String transl_param_value = "";
            for (Object key : cur_params.keySet()) {

                param_value_ns = cur_params.get(key);
                // if(cul.isURI(param_value)){

                try { // encode the uri (to pass uris that contain # etc)
                    //System.out.println("param_value_ns::::::::::::::::---" +param_value_ns);
                    param_value_special = param_value_ns.replaceAll("\\+", "%2B");
                    param_value_enc = URLEncoder.encode(param_value_special, "UTF-8");
                    //param_value_enc = MyUrlEncode.URLencoding(param_value_ns, "UTF-8");
                    //System.out.println("param_value_ns:::::::::::::::: encoded:" +param_value_enc);
                } catch (UnsupportedEncodingException ex) {

                    System.out.println("UnsupportedEncodingException:" + ex.getMessage());
                }

                if (trutils != null && exactMatchTranslation) {
                    transl_param_value = trutils.getTranslation(param_value_ns);
                } else {
                    transl_param_value = param_value_ns;
                }

                param_value = "<a target=\"_blank\" class=\"bluelink\" href=\"OntologyQueryServlet?uri=" + param_value_enc + "&tempontology=" + tempontology + "&qtype=termevolution&sclist=" + sclist + "&cclist=" + cclist + "&vlist=" + vlist + "&dataset=" + dataset_uri + "&datasetversions=" + dataset_versions_uri + "\"" + "title=\"" + key + "\">" + transl_param_value + "</a>";
                //}
                if (multiplechanges) { //case of termevolution
                    param_names_row = param_names_row.trim() + "<td><b>" + key + "</b></td>";
                    if (param_value_ns.equals(uriORliteral)) {
                        param_values_row = param_values_row.trim() + "<td class=\"fillmarktd\"><b>" + param_value + "</b></td>";
                    } else {
                        param_values_row = param_values_row.trim() + "<td><b>" + param_value + "</b></td>";
                    }
                } else { //per change view
                    param_names_row = param_names_row.trim() + "<td><b>" + key + "</b></td>";
                    param_values_row = param_values_row.trim() + "<td>" + param_value + "</td>";
                }
                //colspan=\"100%
                param_info += param_value;
                cnt++;
                if (cnt < cur_params.keySet().size()) {
                    param_info += ", ";
                }
            }
            html_str = html_str.trim() + "<tr>" + param_names_row + "</tr>" + "<tr>" + param_values_row + "</tr>";

        }
        if (!multiplechanges) { //changes view
            //String header = "<h2> Partial list of changes for Versions (" + cutoversion + "-" + cutnversion + ")";
            String header = "<h2>Partial list of changes for selected version(s)</h2>";
            html_str = header + "<table id=\"resultstable\" class=\"resultstable\">" + html_str + "</table>";
        } else { //uri evolution view
            html_str = "<table id=\"resultstable\" class=\"resultstable\">" + html_str + "</table>";
        }
        //html_str = "<table class=\"resultstable\">" + html_str + "</table>";
        //System.out.println("********CHECK**************"+html_str.replaceAll("http://www.ics.forth.gr/Ontology/IdeaGarden/SSIS/", "SSIS:"));
        //http://www.iana.org/assignments/media-types/=PIOU
        //html_str = html_str.replaceAll("http://www.ics.forth.gr/Ontology/IdeaGarden/SSIS/", "SSIS:");s
        if(trutils!=null && !exactMatchTranslation){
            html_str = trutils.getTranslatedHTML(html_str);
        }
        return html_str;
    }

    /**
     * Checks if a named graph exists within Virtuoso RDF store
     *
     * @param jdbc the required object that creates a jdbc connection
     * @param graphname the name of the graph for checking its existence
     * @return true if the named graph exists
     * @throws java.sql.SQLException if connection problem appears
     * @throws java.lang.ClassNotFoundException if internal store problem
     * appears
     * @throws java.io.IOException if internal store problem appears
     */
    public static boolean namedGraphExists(JDBCVirtuosoRep jdbc, String graphname) throws SQLException, ClassNotFoundException, IOException {
        StringBuilder query = new StringBuilder();
        boolean result = false;
        //query.append(" ASK { GRAPH <"+graphname+"> {?s ?p ?o} } ");
        query.append("select * from <").append(graphname).append("> where  {?s ?p ?o} LIMIT 2");
        ResultSet res = jdbc.executeSparqlQuery(query.toString(), true);
        return res.next();
    }

    /**
     * Returns the temp ontology that has been created for specified custom
     * compare case (selected old and new version)
     *
     * @param prop the properties file object
     * @param datasetURI the selected dataset uri
     * @param oldversion the selected old version
     * @param newversion the selected new version
     * @return the namedgraph of the created temp ontology
     * @throws java.lang.Exception exception
     */
    public static String getTempOntology(Properties prop, String datasetURI, String oldversion, String newversion) throws Exception {

        ChangesManager cManager = new ChangesManager(prop, datasetURI, oldversion, newversion, true);
        String tempOntology = cManager.getChangesOntology();
        System.out.println("TEMP ontology:===============================>" + tempOntology);
        cManager.terminate();
        return tempOntology;
    }

    private static List getVersions(DatasetsManager dmgr) {
        Map versions_map = null;
        versions_map = dmgr.fetchDatasetVersions();
        
        //dmgr.terminate();
        List versions = new ArrayList(versions_map.keySet()); //YR new
        return versions;
    }

    //Douleuei otan exw evolution apo ola ta versions
    private String getVersionByIndex(DatasetsManager dmgr, int index) {
        List versions = OntologyQueryServlet.getVersions(dmgr);
        return (String) versions.get(index);
    }

    /**
     * Returns the configuration filepath based on the changes Ontology
     *
     * @param servletContext the servlet's context
     * @param changesOntology the selected changes Ontology
     * @return the configuration filepath based on the changes Ontology
     */
    //JCH-Note:We had different configuration paths for non-diachron rdf gereric approach
    public static String getConfigFilePath(ServletContext servletContext, String changesOntology) {

        String contextpath = servletContext.getRealPath("/"); //File.separator
        String filepath;
        if (changesOntology != null && changesOntology.contains("efo")) {
            filepath = contextpath + "config/" + "config_diachron.properties";
        } 
        /*else  if (changesOntology != null && changesOntology.contains("datamarket")) {
            filepath = contextpath + "config/" + "md_config.properties";
        } */
        
     else {
        
            filepath = contextpath + "config/" + "config_generic.properties";
        }

        return filepath;
    }

    /**
     * Returns the next number of a version to be created for selected dataset URI
     *
     * @param dmgr the DatasetsManager object that has set the dataset URI
     * @return the last number of a version for the selected dataset URI
     */
    public static int getNextVersionNumber(DatasetsManager dmgr) {
        List versions = OntologyQueryServlet.getVersions(dmgr);
        int lastno = 1;
        if (!versions.isEmpty()){
        
            String version = (String)versions.get(versions.size()-1);
            String versionNO = version.substring(version.lastIndexOf("/")+1);
            lastno = Integer.parseInt(versionNO);
            lastno++;
        }
        return lastno;
    }

    private String getVersionByLabel(DatasetsManager dmgr, String datasetURI, String label) {
        String version = "";
        Map versions_map = dmgr.fetchDatasetVersions();
        String value = "";
        for (Object key : versions_map.keySet()) {
            value = (String) versions_map.get(key);
            if (value.equals(label)) {
                version = (String) key;
            }
            //System.out.println("Key : " + key.toString() + " Value : "
            //+ versions_map.get(key));
        }
        return version;
    }

    private void showErrorMessage(PrintWriter out, String message) {
        out.print("<script language='JavaScript'>showDialog('dialogmsg'," + "'" + message + "'" + ");</script>");
        //out.println("<script type=\"text/javascript\" src=\"js/generic.js\">showDialog('dialogmsg',"+"'"+message+"'"+");");
        //out.print("<script language='JavaScript'>alert('Hello');</script>");
        //out.println("</script>");

    }

    /**
     * Sets for a property a specified value within a properties file
     *
     * @param propertiesFilePath the properties file path
     * @param propertyName the name of the property
     * @param value the value to be set
     * @throws java.io.IOException in case where the properties file cannot be
     * found
     */
    public static void setPropertyToFile(String propertiesFilePath, String propertyName, String value) throws IOException {
        Properties props;
        try (FileInputStream in = new FileInputStream(propertiesFilePath)) {
            props = new Properties();
            props.load(in);
        }

        FileOutputStream out = new FileOutputStream(propertiesFilePath);
        props.setProperty(propertyName, value);
        props.store(out, null);
        out.close();
    }

    /**
     * Gets from a property file the value of a specified property
     *
     * @param propertiesFilePath the properties file path
     * @param propertyName the name of the property
     * @return the value for the selected property
     */
    public static String getPropertyFromFile(String propertiesFilePath, String propertyName) {
        Properties prop = new Properties();
        InputStream inputStream;
        String propvalue = "";
        try {
            inputStream = new FileInputStream(propertiesFilePath);
            prop.load(inputStream);
            propvalue = prop.getProperty(propertyName);
        } catch (IOException ex) {
            System.out.println("Exception: " + ex.getMessage() + " occured .");

        }
        return propvalue;
    }

    // <editor-fold defaultstate="collapsed" desc="HttpServlet methods. Click on the + sign on the left to edit the code.">
    /**
     * Handles the HTTP <code>GET</code> method.
     *
     * @param request servlet request
     * @param response servlet response
     * @throws ServletException if a servlet-specific error occurs
     * @throws IOException if an I/O error occurs
     */
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        processRequest(request, response);
    }

    /**
     * Handles the HTTP <code>POST</code> method.
     *
     * @param request servlet request
     * @param response servlet response
     * @throws ServletException if a servlet-specific error occurs
     * @throws IOException if an I/O error occurs
     */
    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        processRequest(request, response);
    }

    /**
     * Returns a short description of the servlet.
     *
     * @return a String containing servlet description
     */
    @Override
    public String getServletInfo() {
        return "Short description";
    }// </editor-fold>

}
