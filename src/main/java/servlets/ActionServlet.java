package servlets;

import java.io.FileInputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.LineNumberReader;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import javax.servlet.ServletContext;
/**
 *
 * @author Ioannis Chrysakis (hrysakis@ics.forth.gr)
 */

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import store.MCDUtils;
import store.SCDUtils;
import store.User;
import org.diachron.detection.complex_change.CCDefinitionError;
import org.diachron.detection.complex_change.Presence;
import org.diachron.detection.complex_change.SCDefinition;
import org.diachron.detection.complex_change.VersionFilter;
import org.diachron.detection.utils.ChangesManager;
import org.diachron.detection.utils.DatasetsManager;
import org.diachron.detection.utils.IOOps;
import org.diachron.detection.utils.OntologicalSimpleChangesType;
import org.openrdf.rio.RDFFormat;
import utils.SortBasedOnName;

/**
 * This servlet used to process requests that perform actions (i.e save
 * definition,delete definition, detection, etc) to the RDF store.
 */
public class ActionServlet extends HttpServlet {

    private String contextPath = "";
    private String defaultuserspath = "";
    private String defaultuserscleanpath = "";

    /**
     * Processes the requests (get/post) coming from UI, accordingly to user
     * options.
     *
     * @param request the servlet's request object
     * @param response the servlet's response object
     */
    protected void processRequest(HttpServletRequest request, HttpServletResponse response) {

        response.setContentType("text/html;charset=UTF-8");
        response.setHeader("Access-Control-Allow-Origin", "*");
        try (PrintWriter out = response.getWriter()) {

            String changes_ontology = "", changes_ontology_schema = "", datasetURI = "";
            MCDUtils cul = null;
            String[] complexChanges = {}; // if empty detect all complex changes
            Map<String, String> selFiltersTable = new LinkedHashMap<>();
            Map<String, String> joinFiltersTable = new LinkedHashMap<>();

            String action = request.getParameter("action");
            System.out.println("ACTION:" + action);

            ServletContext servletContext = getServletContext();
            contextPath = servletContext.getRealPath("/");
            defaultuserspath = contextPath + "\\config\\" + "\\users_predefined.properties";
            defaultuserscleanpath = contextPath + "\\config\\" + "\\users_clean.properties";
            changes_ontology = request.getParameter("userChangesOntology");
            String configPath = OntologyQueryServlet.getConfigFilePath(servletContext, changes_ontology);

            Properties prop = new Properties();
            InputStream inputStream;
            try {
                inputStream = new FileInputStream(configPath);
                prop.load(inputStream);
            } catch (IOException ex) {
                System.out.println("Exception with prop file: " + ex.getMessage() + " occured .");
                //return;
            }

            changes_ontology_schema = changes_ontology + "/schema";
            //System.out.println("changes_ontology_schema:" + changes_ontology_schema);

            datasetURI = request.getParameter("userDatasetURI");
            //System.out.println("dataset_uri:" + datasetURI);
            String username = request.getParameter("username");
            if (username == null) {
                username = "guest";
            }

            if (action.startsWith("ds")) {

                ///////////////// MANAGE DATASETS /////////////////////
                if (action.equals("ds_add")) { //add dataset and one version

                    DatasetsManager dmgr = null;

                    String constructed_datasetURI = "";
                    String datasetLabel = request.getParameter("dslabel");
                    String datasetVersionLabel = request.getParameter("dvlabel");
                    String versionFilename = ""; //retrieve from Files directory
                    RDFFormat format = RDFFormat.RDFXML;// JCH: be a case to support other formats
                    String constructed_versionNamedgraph = ""; //JCH: retrieve list of versions and plus one (getLastVersionNumber)
                    //JCH-DO for all private methods of OntologyQueryServlet create a class on utils named GraphUtils

                    //dvlabel
                    if (datasetLabel != null) {
                        try {

                            constructed_datasetURI = "http://" + datasetLabel.replaceAll(" ", "-");

                            dmgr = new DatasetsManager(prop, constructed_datasetURI);

                            dmgr.insertDataset(constructed_datasetURI, datasetLabel);
                            dmgr.insertDatasetVersion(versionFilename, format, constructed_versionNamedgraph, datasetVersionLabel, constructed_datasetURI);
                            out.print("success");
                            dmgr.terminate();
                        } catch (Exception ex) {
                            getOutputError("Cannot add dataset:" + ex.getMessage(), out);
                        }
                    }
                }

            } else if (action.equals("getInternalUserName")) {
                String loginoption = request.getParameter("loginoption");
                String intenal_user = getInternalName(username, loginoption);
                if (intenal_user != null && !intenal_user.equals("")) { //Yparxei o xrhsths sto arxeio
                    out.print(intenal_user);
                } else { //false case login as guest
                    out.print("guest");

                }

            } else if (action.equals("userconflict")) {

                String loginoption = request.getParameter("loginoption");
                if (loginoption != null && loginoption.equals("user-clean")) {
                    if (this.userExists(defaultuserspath, username)) {
                        out.print("userconflict");
                    }
                } else if (loginoption != null && loginoption.equals("user")) {
                    if (this.userExists(defaultuserscleanpath, username)) {
                        out.print("userconflict");
                    }
                } else {
                    out.print("ok");
                }
            } else if (action.equals("addnextuser")) { //Each user prepares the next one
                boolean cleanspace = false;
                String userspath = defaultuserspath;
                String loginoption = request.getParameter("loginoption");
                String default_duri = "";
                String dataseturis = "";

                if (loginoption != null && loginoption.equals("user-clean")) {
                    cleanspace = true;
                    userspath = defaultuserscleanpath;
                }

                String internal_name = OntologyQueryServlet.getPropertyFromFile(userspath, "newuser");

                //Case for creation of new user
                if (internal_name.equals("toupdate")) {
                    username = getNextUser(userspath); //updates newuser value
                    //Loading default dataset uris from file
                    dataseturis = OntologyQueryServlet.getPropertyFromFile(configPath, "Dataset_URIs");
                    String[] duris = dataseturis.split("\\,");

                    for (int i = 0; i < duris.length; i++) {
                        if (!duris[i].equals("")) {
                            default_duri = duris[i];

                            //Fixing dataset uri if corresponding to clean state    
                            if (cleanspace) {
                                if (default_duri.endsWith("/")) {
                                    default_duri += "clean";
                                } else {
                                    default_duri += "/clean";
                                }
                            }

                            User user = new User(prop, username, default_duri);
                            System.out.println("User with internal_name (" + username + ") just added to:" + default_duri);
                            user.terminate();
                        }
                    }

                }
            } else { ///////////////// BASIC ACTIONS ///////////////////////////////////////

                //System.out.println("config_path///////////////////////->" + configPath);
                cul = new MCDUtils(configPath, changes_ontology_schema, datasetURI, null);

                //Retrieving CC-Elements
            }
            //actions: save, detect, delete
            if (cul != null) {

                if (action.equals("adduser")) {
                    //loadedDatasetURI
                    String loadedDatasetURI = request.getParameter("loadedDatasetURI");
                    //loadedDatasetURI = "http://geneontology.org/";

                    System.out.println("USERNAME:" + username);
                    //live-adding of user
                    User user = new User(prop, username, loadedDatasetURI);
                    System.out.println("USER-DATASETURI:" + user.getUserDatasetUri());
                    System.out.println("USER-ONTOLOGY-SCHEMA:" + user.getUserOntologySchema());

                    user.terminate();
                    System.out.println("user:" + username + " just added to::" + loadedDatasetURI);

                } else if (action.equals("initvision")) {

                    String vold = request.getParameter("vold");
                    System.out.println("custom---vold------------" + vold);
                    String vnew = request.getParameter("vnew");
                    System.out.println("custom---vnew------------" + vnew);
                    String changetype = request.getParameter("changetype");
                    String sclist = request.getParameter("sclist");
                    if (sclist != null && sclist.equals("null")) { //JCH:Oloi oi parametroi metatrepontai se string kata thn metafora tous apo request
                        sclist = null;
                    }
                    String cclist = request.getParameter("cclist");
                    if (cclist != null && cclist.equals("null")) {
                        cclist = null;
                    }

                    String scarray[] = null;
                    String ccarray[] = null;
                    if (changetype != null && !changetype.equals("all")) {

                        if (sclist != null) {
                            if (sclist.equals("")) {
                                //empty string (don't detect them)
                                scarray = new String[0];
                            } else {
                                scarray = sclist.split("\\,");
                            }
                        }

                        if (cclist != null) {
                            if (cclist.equals("")) {
                                //empty string (don't detect them)
                                ccarray = new String[0];
                            } else {
                                ccarray = cclist.split("\\,");
                            }
                        }
                        System.out.println("SCLIST:" + sclist);
                        System.out.println("CCLIST:" + cclist);
                    }
                    ChangesManager cManager;
                    String changesOntology;
                    try {
                        cManager = new ChangesManager(prop, datasetURI, vold, vnew, true);
                        changesOntology = cManager.getChangesOntology();

                        System.out.println("custom---changesOntology-----------" + changesOntology);
                        boolean tempgraph_exists = OntologyQueryServlet.namedGraphExists(cul.getJDBCRepository(), changesOntology);
                        if (tempgraph_exists) { //JCH: could be moved this at logout 
                            cManager.deleteChangesOntology();
                            //cManager.getJDBC().clearGraph(changesOntology, true);
                        }

                        SCDUtils scd = new SCDUtils(configPath, changesOntology, changes_ontology_schema, datasetURI, null);
                        scd.customCompareVersions(vold, vnew, scarray, ccarray);
                        cManager.terminate();
                    } catch (Exception ex) {

                        System.out.println("initvision option exception:" + ex.getMessage());
                    }
                } //SAVING (TEMPLATE) DEFINITION
                else if (action.equals("save")) {

                    String ccPriority_str = request.getParameter("priority");
                    double ccPriority = 0;

                    try {

                        if (ccPriority_str != null) {
                            ccPriority = Double.parseDouble(ccPriority_str);
                            System.out.println("ccPriority:" + ccPriority);
                            String ccTemplate = request.getParameter("template");
                            System.out.println("ccTemplate:" + ccTemplate);

                            String ccJson = IOOps.readData(contextPath + "complex_changes/" + ccTemplate);
                            //IOOps.readData("complex_changes/Add_Instance_Of");
                            System.out.println("ccJson:" + ccJson);
                            String selectedURI = request.getParameter("param_value_uri");
                            System.out.println("selectedURI:" + selectedURI);
                            String ccName = request.getParameter("name");
                            System.out.println("ccName:" + ccName);
                            CCDefinitionError result = cul.saveTemplateCCDefinition(ccName, ccPriority, ccJson, selectedURI);
                            getOutputError(result, out);

                        }
                    } catch (java.lang.NumberFormatException ex) {

                        this.getOutputError("Invalid priority, please enter a double number", out);
                    }

                    //SAVING (EXTENDED) DEFINITION  
                } else if (action.equals("saveExt")) {

                    boolean errorcase = false;
                    // name:ccName, priority:ccPriority,ccParams:ccParams,selects:selects,joins:joins
                    //prevname
                    String prevname = request.getParameter("prevname");
                    String ccPriority_str = request.getParameter("priority");
                    System.out.println("ccPriority_str:" + ccPriority_str);
                    String ccParams_str = request.getParameter("ccParams");
                    System.out.println("ccParams_str:" + ccParams_str);
                    //ccDescription
                    String description = request.getParameter("ccDescription");
                    System.out.println("description:" + ccParams_str);
                    if (description != null && description.equals("")) {
                        description = null;
                    }
                    String selects = request.getParameter("selects");
                    String opt_changes_str = request.getParameter("opt_changes");
                    System.out.println("opt_changes_str:" + opt_changes_str);
                    String vfilters_str = request.getParameter("vfilters");
                    System.out.println("vfilters_str:" + vfilters_str);
                    System.out.println("SELECTS:" + selects);
                    String joins = request.getParameter("joins");
                    System.out.println("JOINS:" + joins);
                    //schanges
                    String schanges = request.getParameter("schanges");
                    //used to distinguish case of assignes changes with at least one filter (version,select, join)
                    //in this case we create only the definitions
                    System.out.println("schanges:" + schanges);
                    boolean update = Boolean.valueOf(request.getParameter("update"));
                    System.out.println("update:" + update);

                    double ccPriority = 0;
                    Hashtable existing_scdefs = new Hashtable();
                    try {

                        if (ccPriority_str != null) {
                            ccPriority = Double.parseDouble(ccPriority_str);
                            List<SCDefinition> scDefs = new ArrayList<>();
                            String clearchangename = "";
                            String selparts[] = selects.split("\\$");
                            String joinparts[] = joins.split("\\$");

                            String changeID = "";
                            String tmp_name = ""; //used to extract name from 
                            String existing_val = "";
                            boolean isOptional = false;
                            //Extracting select information anf put it to hashtable {id,select_filter}
                            //Case: assined SC without sel/join

                            if (selects.equals("") && joins.equals("") && schanges != null && !schanges.equals("") && vfilters_str != null && !vfilters_str.equals("*$$$nop")) {
                                //iterate through assigned scanges of as_sc
                                String as_sc[] = schanges.split("\\$");
                                for (String schange : as_sc) {
                                    createEmptySCDef(schange);
                                }
                            } //Case assigned SC with at least one filter (sel,join,version)
                            else if ((!selects.equals("") || !joins.equals("")) || (vfilters_str != null && !vfilters_str.equals("*$$$nop"))) {
                                for (int i = 0; i < selparts.length; i++) {

                                    if (!selparts[i].equals("") && !selparts[i].contains("nop") && !selparts[i].contains("*")) {
                                        tmp_name = selparts[i];
                                        tmp_name = tmp_name.replaceFirst(":", "*");
                                        changeID = tmp_name.substring(0, tmp_name.indexOf(":"));
                                        changeID = changeID.replaceFirst("\\*", "\\:");
                                        if (selFiltersTable.containsKey(changeID)) {//support of multiple selection filters
                                            existing_val = (String) selFiltersTable.get(changeID);
                                            selFiltersTable.put(changeID, existing_val + "$" + selparts[i]); //{id,sel_filter1$sel_filter2}
                                        } else {
                                            selFiltersTable.put(changeID, selparts[i]); //id,sel_filter
                                        }
                                    }

                                }
                                changeID = "n/a";
                                //Extracting join information anf put it to hashtable {id,join_filter}
                                for (int j = 0; j < joinparts.length; j++) {
                                    //id,joinfilter
                                    if (!joinparts[j].equals("") && !joinparts[j].contains("nop") && !joinparts[j].contains("*")) {
                                        tmp_name = joinparts[j];
                                        tmp_name = tmp_name.replaceFirst(":", "*");
                                        changeID = tmp_name.substring(0, tmp_name.indexOf(":"));
                                        changeID = changeID.replaceFirst("\\*", "\\:");
                                        if (joinFiltersTable.containsKey(changeID)) {//support of multiple selection filters
                                            existing_val = (String) joinFiltersTable.get(changeID);
                                            joinFiltersTable.put(changeID, existing_val + "$" + joinparts[j]); //<---------------- TO BE MULTIPLE

                                        } else {
                                            joinFiltersTable.put(changeID, joinparts[j]);
                                        }
                                    }

                                }

                                Set set = selFiltersTable.keySet(); // get set-view of keys
                                //SOS: Ean den exw sel_filter alla join filter???? TOFIX
                                if (set.isEmpty()) {
                                    set = joinFiltersTable.keySet(); //Periptwsh mono me join filters, painrw ta kleidia apo jointables

                                }
                                Iterator itr = set.iterator();

                                while (itr.hasNext()) {

                                    String key = (String) itr.next(); //fullchangename
                                    isOptional = false;
                                    clearchangename = key.substring(2); //without number prefix
                                    String selfilters = (String) selFiltersTable.get(key);

                                    //check if optional
                                    if (opt_changes_str.contains(key)) {
                                        isOptional = true;
                                    }
                                    System.out.println("####################################################################################################");
                                    System.out.println("Creating new SC Definition of " + clearchangename + " with name:" + key + " and optional:" + isOptional);
                                    SCDefinition sc1 = new SCDefinition(OntologicalSimpleChangesType.fromString(clearchangename), key, isOptional);
                                    existing_scdefs.put(key, sc1);

                                    if (selfilters != null) {
                                        if (selfilters.contains("$")) { //multiple selection filters, split them and add set them to the current definition

                                            String sfilters[] = selfilters.split("\\$");
                                            for (String sfilter : sfilters) {
                                                System.out.println("\nSetting multiple selection filter for:" + key + " with value:" + sfilter);

                                                sfilter = this.normalizeSelFilter(sfilter);
                                                sc1.setSelectionFilter(sfilter);
                                            }

                                        } else {
                                            System.out.println("\nSetting selection filter for:" + key + " with value:" + selFiltersTable.get(key));
                                            String sfilter1 = (String) selFiltersTable.get(key);

                                            sfilter1 = this.normalizeSelFilter(sfilter1);
                                            sc1.setSelectionFilter(sfilter1); //single-value
                                        }
                                    }

                                    String join_filter_val = "";
                                    if (joinFiltersTable.containsKey(key)) {
                                        String joinfilters = (String) joinFiltersTable.get(key);
                                        if (joinfilters.contains("$")) { //multiple join case
                                            String jfilters[] = joinfilters.split("\\$");
                                            for (String jfilter : jfilters) {
                                                System.out.println("Setting multiple join filter for:" + key + " with value:" + jfilter);
                                                sc1.setJoinFilter(jfilter);
                                            }
                                        } else {

                                            join_filter_val = (String) joinFiltersTable.get(key);
                                            sc1.setJoinFilter(join_filter_val); ////<-------------
                                            System.out.println("Setting join filter for:" + key + " with value:" + join_filter_val);
                                        }

                                    }

                                    scDefs.add(sc1);
                                }
                                //check if needed to create empty Simple Change Definitions
                                List emptydefs = checkForEmptySCDefs(schanges, existing_scdefs);
                                if (emptydefs.size() > 0) {
                                    scDefs.addAll(emptydefs);
                                }
                                Collections.sort(scDefs, new SortBasedOnName());//JCH:Sos sort definitions based on their name
                            } //<---- check for empty SC 
                            //invalid case of filters
                            else {
                                errorcase = true;
                                this.getOutputError("Invalid usage of filters.\n Please check again the existance of at least one assigned simple change(s) or version filter(s).\n", out);
                            }
                            Map<String, String> ccParams = new LinkedHashMap<>();
                            String params[] = ccParams_str.split("\\$");
                            String paramname = "";
                            String paramvalue = "";
                            for (int k = 0; k < params.length; k++) {
                                if (!params[k].equals("")) {
                                    paramname = params[k].substring(0, params[k].indexOf(":"));
                                    paramvalue = params[k].substring(params[k].indexOf(":") + 1);
                                    ccParams.put(paramname, paramvalue);
                                    System.out.println("Setting parameter(" + paramname + "):" + paramvalue);
                                }
                            }
                            System.out.println("-----------SCDEFS----------");
                            for (SCDefinition sc1 : scDefs) {
                                System.out.println(sc1);
                            }
                            System.out.println("-------------------");

                            System.out.println("--------CC PARAMS ------------");
                            for (String val : ccParams.keySet()) {
                                System.out.println(val + " -> " + ccParams.get(val));
                            }
                            System.out.println("-------------------");

                            List<VersionFilter> versionFilters = new ArrayList<>();
                            //VersionFilter(String subject, String predicate, String object, Presence presence) 

                            String vfilters[] = vfilters_str.split("\\*"); //Split vfilters per row
                            if (vfilters.length > 0 && !vfilters_str.equals("*$$$nop")) { //empty verfilters
                                System.out.println("--------VERSION FILTERS ------------");
                                String subject = "";
                                String predicate = "";
                                String object = "";
                                for (int u = 0; u < vfilters.length; u++) {
                                    if (!vfilters[u].equals("")) {
                                        String row[] = vfilters[u].split("\\$"); //split filter 4 aruuments

                                        if (row[0] != null && row[1] != null && row[2] != null && getPresence(row[3]) != null) {
                                            if (!cul.isValidCCParam(row[0]) || !cul.isValidCCParam(row[1])) {
                                                errorcase = true;
                                                this.getOutputError("Subject and predicate should correspond to a URI!", out);

                                            } else {
                                                // 4 cases: <uri>, 'user_defined_value', cc param(param1), sc param (1:ADD_COMMENT:-subject)
                                                subject = normalizeVerFilter(ccParams, row[0]);
                                                predicate = normalizeVerFilter(ccParams, row[1]);
                                                object = normalizeVerFilter(ccParams, row[2]);
                                                versionFilters.add(new VersionFilter(subject, predicate, object, getPresence(row[3])));
                                                System.out.println("Setting VersionFilter(" + subject + "," + predicate + "," + object + "," + getPresence(row[3]) + ")");
                                            }
                                        } else {

                                            errorcase = true;
                                            this.getOutputError("Please check again the inserted or selected values in version filters!", out);

                                        }

                                    }
                                }
                            }
                            System.out.println("-------------------");
                            if (!errorcase) {
                                String ccName = request.getParameter("name");
                                System.out.println("ccName:" + ccName);
                                //
                                if (update && prevname != null && !prevname.equals("") && !prevname.equals("undefined")) { //delete previous version
                                    boolean success = cul.deleteCC(prevname);
                                    if (!success) {
                                        getOutputError("Edit of complex change failed: Cannot delete previous version of complex change!", out);
                                        return;
                                    } else {
                                        System.out.println("Deleting previous version of:" + ccName);
                                    }
                                }
                                CCDefinitionError result = cul.saveCCExtendedDefinition(ccName, ccPriority, description, scDefs, ccParams, versionFilters);
                                getOutputError(result, out);
                            }
                        }
                    } catch (java.lang.NumberFormatException ex) {
                        this.getOutputError("Invalid priority, please enter a double number", out);
                    }

                } //DELETE 
                else if (action.equals("delete")) {

                    String cclist = request.getParameter("dellist");
                    System.out.println("cclist:" + cclist);
                    String[] parts = cclist.split("\\$");
                    String ccname = "";

                    ArrayList<String> dlist = new ArrayList<>();
                    boolean success = false;
                    /* int delchangesNo = parts.length;
                     if (delchangesNo == 1){
                     cul.deleteCC(parts[0]);
                     }
                     else{ //multiple-deletion
                     */
                    for (int j = 0; j < parts.length; j++) {
                        ccname = parts[j];

                        if (!ccname.equals("")) {
                            dlist.add(ccname);

                        }
                    }
                    success = cul.deleteMultipleCC(dlist);
                    if (!success) {
                        getOutputError("Cannot delete complex change!", out);
                    }
                    //}
                } // DETECTION

            }
            if (cul != null) {
                cul.terminate();
            }
        } catch (Exception ex) {
            //this.getOutputError("Invalid priority, please enter a double number",out);
            System.out.println("EXCEPTION:" + ex.getMessage());
        }
    }

    private List<SCDefinition> checkForEmptySCDefs(String schanges, Hashtable existing_scdefs_names) {
        String as_sc[] = schanges.split("\\$");

        List<SCDefinition> scDefs = new ArrayList<>();
        for (int i = 0; i < as_sc.length; i++) {
            if (!as_sc[i].equals("")) {
                if (!existing_scdefs_names.containsKey(as_sc[i])) {
                    scDefs = createEmptySCDef(as_sc[i]);
                }
            }
        }
        return scDefs;
    }

//used to create empty sc defs (withous any sel filter, join_filter TOTEST
    private List<SCDefinition> createEmptySCDef(String schange) {
        String clearchangename = "";
        List<SCDefinition> scDefs = new ArrayList<>();

        if (!schange.equals("")) {
            //Yparxei assigned SC xwris sel/join filter
            clearchangename = schange.substring(2);
            SCDefinition sc_without_seljoins = new SCDefinition(OntologicalSimpleChangesType.fromString(clearchangename), schange, false);
            System.out.println("####################################################################################################");
            System.out.println("Creating EMPTY SC Definition of " + clearchangename + " with name:" + schange + " and optional:false");
            scDefs.add(sc_without_seljoins);
        }

        return scDefs;
    }

    private Presence getPresence(String pres_str_value) {
        if (pres_str_value != null && pres_str_value.equals("EXISTS_IN_V2")) {
            return Presence.EXISTS_IN_V2;
        } else if (pres_str_value != null && pres_str_value.equals("EXISTS_IN_V1")) {
            return Presence.EXISTS_IN_V1;
        } else if (pres_str_value != null && pres_str_value.equals("NOT_EXISTS_IN_V1")) {
            return Presence.NOT_EXISTS_IN_V1;
        } else if (pres_str_value != null && pres_str_value.equals("NOT_EXISTS_IN_V2")) {
            return Presence.NOT_EXISTS_IN_V2;
        }
        return null; // den prepei na symbei auto giati ena version filter panta prepei na efarmozetai se mia v1 h' v2
    }

    private String normalizeVerFilter(Map paramsMap, String sfilter) {
        if (!nameBelongsToParameter(paramsMap, sfilter) && !sfilter.contains(":-")) {// <uri>, 'user_defined_value' case{
            sfilter = normalizeFilter(sfilter);
            return sfilter;
        } else {
            return sfilter;
        }

    }

    private boolean nameBelongsToParameter(Map ccParams, String name) {
        return ccParams.containsKey(name);
    }

    private String normalizeSelFilter(String sfilter) {
        //1:ADD_COMMENT:-subject=http://test ->1:ADD_COMMENT:-subject=<http://tes> etc
        String sfilter_part1 = sfilter.substring(0, sfilter.lastIndexOf("="));
        String sfilter_part2 = sfilter.substring(sfilter.lastIndexOf("=") + 1);
        sfilter_part2 = this.normalizeFilter(sfilter_part2); //normalization on second part
        sfilter = sfilter_part1 + "=" + sfilter_part2;
        return sfilter;
    }

    private String normalizeFilter(String sfilter) {
        if (MCDUtils.isURI(sfilter)) {
            sfilter = "<" + sfilter + ">";
        } else {
            sfilter = "'" + sfilter + "'";
        }
        return sfilter;
    }

    private PrintWriter getOutputError(CCDefinitionError result, PrintWriter out) {

        if (result.getErrorCode() != null) //yparxei error{
        {
            if (result.getDescription() != null) {
                out.println("Error:" + result.getDescription());
            } else {
                out.println("Error:" + result.getErrorCode());

            }
        } else {
            out.print("success");
        }

        return out;
    }

    private PrintWriter getOutputError(String message, PrintWriter out) {

        if (message != null) //yparxei message
        {
            out.print(message);
        }

        return out;
    }

    private int countLines(String filename) throws IOException {
        LineNumberReader reader = new LineNumberReader(new FileReader(filename));
        int cnt = 0;
        String lineRead = "";
        while ((lineRead = reader.readLine()) != null) {
        }

        cnt = reader.getLineNumber() - 1;
        reader.close();
        System.out.println(filename + " COUNTLINES:::::" + cnt);
        return cnt;
    }

    private String getInternalName(String username, String loginoption) throws IOException {
        //String loginoption = request.getParameter("loginoption");
        String internal_name = "";
        String userspath = "";
        if (loginoption != null && loginoption.equals("user-clean")) {
            userspath = defaultuserscleanpath;
            internal_name = OntologyQueryServlet.getPropertyFromFile(userspath, username);
        } else {
            userspath = defaultuserspath;
            internal_name = OntologyQueryServlet.getPropertyFromFile(userspath, username);
        }

        if (internal_name == null) { //NEW USER
            internal_name = OntologyQueryServlet.getPropertyFromFile(userspath, "newuser");
            this.assignUser(username, internal_name, userspath);
            OntologyQueryServlet.setPropertyToFile(userspath, "newuser", "toupdate");

        }
        return internal_name;
    }

    private void assignUser(String username, String internal_name, String userspath) throws IOException {

        OntologyQueryServlet.setPropertyToFile(userspath, username, internal_name);
        System.out.println("Setting to:" + userspath + "with:" + username + "=" + internal_name);
    }

    private String getNextUser(String userspath) throws IOException {
        String retusr = "";
        int lineno = countLines(userspath);
        if (userspath.equals(this.defaultuserspath)) {
            retusr = "user" + lineno;
        } else {
            retusr = "user" + lineno + "clean";
        }
        OntologyQueryServlet.setPropertyToFile(userspath, "newuser", retusr);
        return retusr;
    }

    private boolean userExists(String userspath, String username) {
        if (OntologyQueryServlet.getPropertyFromFile(userspath, username) == null) {
            return false;
        } else {
            return true;
        }
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
