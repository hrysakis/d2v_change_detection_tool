/**
*
* @author Ioannis Chrysakis (hrysakis@ics.forth.gr)
*/
package servlets;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.LineNumberReader;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.apache.commons.io.FileUtils;
import org.diachron.detection.associations.AssocManager;
import org.diachron.detection.complex_change.CCDefinitionError;
import org.diachron.detection.complex_change.Presence;
import org.diachron.detection.complex_change.SCDefinition;
import org.diachron.detection.complex_change.VersionFilter;
import org.diachron.detection.repositories.SesameVirtRep;
import org.diachron.detection.utils.ChangesManager;
import org.diachron.detection.utils.DatasetsManager;
import org.diachron.detection.utils.IOOps;
import org.diachron.detection.utils.OntologicalSimpleChangesType;
import org.openrdf.rio.RDFFormat;
import store.MCDUtils;
import store.SCDUtils;
import store.User;
import utils.SortBasedOnName;
import utils.VirtuosoUploader;

/**
* This servlet used to process requests that perform actions (i.e save
* definition,delete definition, detection, etc) to the RDF store.
*/
public class ActionServlet extends HttpServlet {

private String contextPath = "";
private String defaultuserspath = "";
private String defaultuserscleanpath = "";
private String configFilePath = "";
private String genericConfigFilePath;
private String simpleChangesFolder = "";

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
    MCDUtils cul = null;
    try (PrintWriter out = response.getWriter()) {

        String changes_ontology = "", changes_ontology_schema = "", datasetURI = "";

        String[] complexChanges = {}; // if empty detect all complex changes
        Map<String, String> selFiltersTable = new LinkedHashMap<>();
        Map<String, String> joinFiltersTable = new LinkedHashMap<>();

        String action = request.getParameter("action");
        System.out.println("ACTION:" + action);

        ServletContext servletContext = getServletContext();
        contextPath = servletContext.getRealPath("/");
        defaultuserspath = contextPath + "/config/" + "users_predefined.properties";
        defaultuserscleanpath = contextPath + "/config/" + "users_clean.properties";
        changes_ontology = request.getParameter("userChangesOntology");
        String dataset_label = request.getParameter("datasetLabel");
        //System.out.println("dataset_label:::::::"+dataset_label);
        configFilePath = OntologyQueryServlet.getConfigFilePath(servletContext, dataset_label);
        genericConfigFilePath = OntologyQueryServlet.getConfigFilePath(servletContext, null);

        Properties prop = new Properties();
        InputStream inputStream;
        try {
            inputStream = new FileInputStream(configFilePath);
            prop.load(inputStream);
            inputStream.close();
            simpleChangesFolder = contextPath + OntologyQueryServlet.getPropertyFromFile(configFilePath, "Simple_Changes_Folder");
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

            boolean newDataset = true;
            String constructed_datasetURI = "";
            String internaluser = request.getParameter("intuser");

            if (action.equals("ds_add") || action.equals("ds_addversion")) { //add dataset and one version

                if (action.equals("ds_addversion")) {
                    newDataset = false;
                }
                DatasetsManager dmgr = null;

                String datasetLabel = request.getParameter("dslabel");
                String datasetVersionLabel = request.getParameter("dvlabel");
                RDFFormat defaultSchemaFileFormat = null;
                RDFFormat versionFileFormat = null;

                String graphDest = "";
                String versionFilename = request.getParameter("versionFilename");
                String configFilename  = request.getParameter("configFilename");

                String constructed_versionNamedgraph = "";
                String dsfiles = OntologyQueryServlet.getPropertyFromFile(genericConfigFilePath, "Dataset_Files_Folder");
                String defaultSchemafilename = OntologyQueryServlet.getPropertyFromFile(genericConfigFilePath, "Dataset_Default_Schema");

                defaultSchemaFileFormat = (RDFFormat) VirtuosoUploader.getFileFormatByFilename(defaultSchemafilename);
                versionFileFormat = (RDFFormat) VirtuosoUploader.getFileFormatByFilename(versionFilename);
                String cfmsg = "";

                if (internaluser!= null) { //Fetch the appropriate named graphs according to internal users
                    username = internaluser;
                }
                else{
                    username ="guest";
                }

                if (dsfiles == null || defaultSchemafilename == null) {
                    getOutputError("Error in the properties file. Please check the properties: 'Dataset_Files_Folder' and 'Dataset_Default_Schema'.", out);
                } else if (defaultSchemaFileFormat == null) {
                    getOutputError("Invalid file format for default schema. Please check again the properties file and retry.", out);
                } else if (versionFileFormat == null) {
                    getOutputError("Invalid file format for version. Please select another format or check your file extension.", out);
                } else {

                    if (datasetLabel != null || !newDataset) {
                        try {

                            if (action.equals("ds_addversion")) {
                                String selectedDatasetURI = request.getParameter("selectedDatasetURI");
                                constructed_datasetURI = selectedDatasetURI;
                            } else {

                                constructed_datasetURI = "http://" + datasetLabel.replaceAll(" ", "-") + "/" + username;
                            }
                            dmgr = new DatasetsManager(prop, constructed_datasetURI);
                            constructed_versionNamedgraph = constructed_datasetURI + "/" + OntologyQueryServlet.getNextVersionNumber(dmgr); // check slash
                            System.out.println("constructed_versionNamedgraph:::" + constructed_versionNamedgraph);
                            versionFilename = contextPath + dsfiles + versionFilename;

                            if (newDataset) { //adds the dataset if it does not exist
                                //Assign config file (optional)

                               // updateGenericFileDatasets(constructed_datasetURI);

                                if (configFilename!= null && !configFilename.equals("")){
                                   cfmsg=adjustConfigFile(contextPath,configFilePath,configFilename,datasetLabel);

                                }
                                //1. associate dataset through generic named graph

                                if (cfmsg!= null && cfmsg.equals("")) {//succesfull case of configfile
                                 dmgr.insertDataset(constructed_datasetURI, datasetLabel);
                                //2. loads a default RDF schema
                                SesameVirtRep sesame = new SesameVirtRep(prop);
                                graphDest = constructed_datasetURI + "/changes/schema";
                                sesame.importFile(contextPath+defaultSchemafilename, defaultSchemaFileFormat, graphDest);
                                sesame.terminate();

                                System.out.println("Dataset schema importing completed!" +defaultSchemafilename);
                                }
                            }

                           if (cfmsg!= null && cfmsg.equals("")) {
                            //ADDS one version assigned to the selected/constructed dataset URI   
                            dmgr.insertDatasetVersion(versionFilename, versionFileFormat, constructed_versionNamedgraph, datasetVersionLabel, constructed_datasetURI);
                            Map prev_version_map = dmgr.fetchDatasetPrevVersion(constructed_versionNamedgraph);
                            String prev_version = null;

                            //3. custom compare of added version with the previous one if exists
                            if (prev_version_map != null) {
                                for (Object key : prev_version_map.keySet()) {

                                    prev_version = (String) key;
                                    break;

                                }
                                graphDest = constructed_datasetURI + "/changes/schema";
                                customCompare(prop, constructed_datasetURI, graphDest, null, null, prev_version, constructed_versionNamedgraph, false);
                            }
                                 out.print("success");
                             }
                           else{
                               getOutputError(cfmsg,out);
                           }
                            dmgr.terminate();


                        } catch (Exception ex) {
                            getOutputError("Cannot add dataset:" + ex.getMessage() +" Please check your configuration file or your connection!", out);
                        }
                    }
                }
            } else if (action.equals("ds_del")) {
                try {
                    String selectedDatasetURI = request.getParameter("selectedDatasetURI");
                    String deleteVersions = request.getParameter("deleteVersions");
                    boolean deleteVersionContents = false;
                    if (deleteVersions!= null && deleteVersions.equals("on")){
                        deleteVersionContents = true;
                    }
                    DatasetsManager dmgr = new DatasetsManager(prop, selectedDatasetURI);
                    dmgr.deleteDataset(deleteVersionContents, true); // JCH:Do not delete version contents because this would be result in deleting of
                    //shared versions that are described in properties files for each new user
                    //Otherwise use deleteDataset() from VirtuosoUploader
                    out.print("success");

                } catch (Exception ex) {
                    getOutputError("Cannot delete dataset:" + ex.getMessage() +" Please check your configuration file or your connection!", out);
                }
            } else if (action.equals("ds_delversion")) {
                try {
                    String selectedDatasetURI = request.getParameter("selectedDatasetURI");
                    String deleteVersions = request.getParameter("deleteVersions");
                    boolean deleteVersionContents = false;
                    if (deleteVersions!= null && deleteVersions.equals("on")){
                        deleteVersionContents = true;
                    }
                    String versionURI = request.getParameter("versionURI");
                    DatasetsManager dmgr = new DatasetsManager(prop, selectedDatasetURI);
                    dmgr.deleteDatasetVersion(versionURI, deleteVersionContents, true); //JCH-similarly to above comment 
                    out.print("success");
                } catch (Exception ex) {
                    getOutputError("Cannot delete version:" + ex.getMessage(), out);
                }
            }
        ///////////////// MANAGE USERS /////////////////////
        } else if (action.equals("getInternalUserName")) {
            String loginoption = request.getParameter("loginoption");
            String intenal_user = getInternalName(username, loginoption);
            if (intenal_user != null && !intenal_user.equals("")) { //Yparxei o xrhsths sto arxeio
                out.print(intenal_user);
            } 

            else { //this user is not registered
                if (loginoption!= null && loginoption.equals("reg-user")){
                out.print("nouser");    
                }
                else{ //general false case login as guest
                out.print("guest");
                }
            }

        } else if (action.equals("userexists")) {

            String loginoption = request.getParameter("loginoption");
            if (loginoption != null && loginoption.equals("user")) {
                if (this.userExists(defaultuserspath, username)) {
                    out.print("userexists");
                }
            } else if (loginoption != null && loginoption.equals("user-clean")) {
                if (this.userExists(defaultuserscleanpath, username)) {
                    out.print("userexists");
                }
            } 
            //No problem for create a new user with the desired alias
            else {
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
                dataseturis = OntologyQueryServlet.getPropertyFromFile(genericConfigFilePath, "Dataset_URIs");
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
        }

     

        else if (action.equals("adduser")) {
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

            }
        
           else if (action.equals("parseJSON")) {
            // send back a json string as parsed from the upload directory
            String UPLOAD_DIRECTORY = OntologyQueryServlet.getPropertyFromFile(genericConfigFilePath, "Dataset_Files_Folder");
            String jsonfilename = request.getParameter("jsonfilename");
            //String json_string = this.readFile(UPLOAD_DIRECTORY + jsonfilename);

            String json_string2 = new String(Files.readAllBytes(Paths.get(UPLOAD_DIRECTORY + jsonfilename)));
            //System.out.println("-------------JSON_STRING" +json_string2);
            out.print(json_string2);
        }

      else { ///////////////// BASIC ACTIONS ///////////////////////////////////////

         
        cul = new MCDUtils(configFilePath, changes_ontology_schema, datasetURI, null);

        //actions: (initvision-detection),save, delete (embedded detection)
        if (cul != null) {

             if (action.equals("initvision")) {

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
                customCompare(prop, datasetURI, changes_ontology_schema, scarray, ccarray, vold, vnew, true);
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
            } 

            //SAVING (from JSON) DEFINITION 
            else if (action.equals("saveByJSON")) {

             String ccName = request.getParameter("name");
             System.out.println("ccName:" + ccName);
             String ccJson = request.getParameter("ccJson");
             System.out.println("ccJson:" + ccJson);

             CCDefinitionError result = cul.saveJSONCCDefinition(ccJson, ccName);
             getOutputError(result, out);

            }


            //SAVING (EXTENDED) DEFINITION 
            else if (action.equals("saveExt")) {

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

                             //V.4.6 new merge set of keys to include unique-list of keys on joins/select
                            Set mergeset = new HashSet(selFiltersTable.keySet());
                            mergeset.addAll(joinFiltersTable.keySet());


                           /* Set set = selFiltersTable.keySet(); // get set-view of keys
                            //SOS: Ean den exw sel_filter alla join filter
                            if (set.isEmpty()) {
                                set = joinFiltersTable.keySet(); //Periptwsh mono me join filters, painrw ta kleidia apo jointables

                            }*/
                            Iterator itr = mergeset.iterator();

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
    }
        if (cul != null) {
            cul.terminate();
        }
    } catch (Exception ex) {
        //this.getOutputError("Invalid priority, please enter a double number",out);
          if (cul != null) {
            cul.terminate();
        }
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

//used to create empty sc defs (withous any sel filter, join_filter 
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

private void customCompare(Properties prop, String datasetURI, String changes_ontology_schema, String[] scarray, String[] ccarray, String vold, String vnew, boolean tempOntology) {
    ChangesManager cManager;
    String changesOntology;
    simpleChangesFolder = contextPath +prop.getProperty("Simple_Changes_Folder");
    try {
        cManager = new ChangesManager(prop, datasetURI, vold, vnew, tempOntology);
        changesOntology = cManager.getChangesOntology();

        System.out.println("custom---changesOntology-----------" + changesOntology);
        //cul.getJDBCRepository()
        boolean tempgraph_exists = OntologyQueryServlet.namedGraphExists(cManager.getJDBC(), changesOntology);
      if (tempgraph_exists) { //JCH: could be moved this at logout 
            cManager.deleteChangesOntology();
            //cManager.getJDBC().clearGraph(changesOntology, true);
        }
        cManager.terminate();
        String assoc = this.createAssociations(prop, datasetURI, vold, vnew);
        SCDUtils scd = new SCDUtils(prop, changesOntology, changes_ontology_schema, datasetURI, assoc);
        scd.customCompareVersions(simpleChangesFolder,vold, vnew, scarray, ccarray);

    } catch (Exception ex) {

        System.out.println("customCompare Exception:" + ex.getMessage());
    }
}

 //V4.2
 private String createAssociations(Properties prop,String datasetURI, String v1, String v2) throws Exception {
    String assocNamedGraph = "";
    AssocManager assoc = new AssocManager(prop, datasetURI, true);
    assocNamedGraph = assoc.createAssocGraph(v1, v2, true);
    assoc.terminate();
    return assocNamedGraph;
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

/*private String normalizeVerFilter(Map paramsMap, String sfilter) {
    String clean_filter ="";
    if (!nameBelongsToParameter(paramsMap, sfilter) && !sfilter.contains(":-")) {// <uri>, 'user_defined_value' case{


        clean_filter = sfilter.substring(1, sfilter.length() - 1); //removes ' '
        if (MCDUtils.isURI(clean_filter)){
            sfilter = clean_filter;
        }
        //sfilter = normalizeFilter(sfilter);
        return sfilter;
    } else {
        return sfilter;
    }

}*/

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

    //patch for already inserted the symbols of uri (Ideagarden)
   if(sfilter.startsWith("<") && sfilter.endsWith(">")){
       return sfilter;
   } 

   else if (MCDUtils.isURI(sfilter)) {

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
    } 

    else if (loginoption != null && loginoption.equals("reg-user")) {
        userspath = defaultuserspath;
        internal_name = OntologyQueryServlet.getPropertyFromFile(userspath, username);
        if (internal_name == null || internal_name.equals("")){
            userspath = defaultuserscleanpath;
            internal_name = OntologyQueryServlet.getPropertyFromFile(userspath, username);
        }
    } 
    else {
        userspath = defaultuserspath;
        internal_name = OntologyQueryServlet.getPropertyFromFile(userspath, username);
    }

    if (loginoption != null && !loginoption.equals("reg-user") && internal_name == null) { //NEW USER
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

//Note: not used because the constructed datasetURI contains username and is visible to specific
//user       
private void updateGenericFileDatasets (String datasetURI) throws IOException{

   ///  String datasetURIwithoutUsername = datasetURI.substring(0,datasetURI.lastIndexOf("/"));

     String dataseturis = OntologyQueryServlet.getPropertyFromFile(genericConfigFilePath, "Dataset_URIs");
  ///   dataseturis = dataseturis +"," +datasetURIwithoutUsername;
     OntologyQueryServlet.setPropertyToFile(genericConfigFilePath, "Dataset_URIs", dataseturis);

}



private String adjustConfigFile(String contextPath,String configFilePath,String configFilename,String datasetLabel){
    //copy uploaded config file to config/folder
    String ds_files_folder = OntologyQueryServlet.getPropertyFromFile(configFilePath, "Dataset_Files_Folder");
    File source = new File(contextPath + ds_files_folder+configFilename);
    ///datasetLabel = datasetLabel.replaceAll(" ", "-");
    String destPath = contextPath+"config/" +"config_"+datasetLabel +".properties";
    File dest = new File(destPath);
    Path path = Paths.get(destPath); 
    String msg = "";
    if (Files.exists(path)){ //check if exists config file with the specified label
        msg = "This label has been already assigned to a dataset. Please choose another one.";
        return msg;
    }
    try {
        FileUtils.copyFile(source, dest);


    } catch (IOException ex) {
        System.out.println("adjustConfigFile Exception:" +ex.getMessage());
        msg ="Error while setting the specified configuration file";

    }
    return msg;
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
