/**
 *
 * @author Ioannis Chrysakis (hrysakis@ics.forth.gr)
 */
package servlets;

import com.google.visualization.datasource.DataSourceServlet;
import com.google.visualization.datasource.base.TypeMismatchException;
import com.google.visualization.datasource.datatable.ColumnDescription;
import com.google.visualization.datasource.datatable.DataTable;
import com.google.visualization.datasource.datatable.TableRow;
import com.google.visualization.datasource.datatable.value.ValueType;
import com.google.visualization.datasource.query.Query;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;
import store.QueryUtils;
import org.diachron.detection.utils.DatasetsManager;
import org.diachron.detection.repositories.JDBCVirtuosoRep;

/**
 * This servlet used to create the data structures required for the basic
 * visualization analysis.
 */
public class VisualizeServlet extends DataSourceServlet {

    /**
     * Generates the data table that required for each google chart
     *
     * @param query the google query source
     * @param request the request that comes from query
     */
    @Override
    public DataTable generateDataTable(Query query, HttpServletRequest request) throws TypeMismatchException {
        DataTable data = new DataTable(); // Create a data table in order to be an input for the google charts.

        JDBCVirtuosoRep jdbc = null;
        try {

            String changesOntology = request.getParameter("changesgraph");
            String changesOntologySchema = changesOntology + "/schema";

            ServletContext servletContext = getServletContext();
            //String configPath = OntologyQueryServlet.getConfigFilePath(servletContext, changesOntology);
            String genericConfigFilePath = OntologyQueryServlet.getConfigFilePath(servletContext, null);
            //System.out.println("config_path:::::::::::::::::::::::::::::>" + genericConfigFilePath);
            Properties prop = new Properties();
            InputStream inputStream;
            try {
                inputStream = new FileInputStream(genericConfigFilePath);
                prop.load(inputStream);
                inputStream.close();
            } catch (IOException ex) {
                System.out.println("Exception with prop file: " + ex.getMessage() + " occured .");
                //return;
            }

            System.out.println("Changesgraph::" + changesOntology);
            String v1 = request.getParameter("vold"); //JCH: what-if null
            System.out.println("Vfrom:" + v1);
            String v2 = request.getParameter("vnew");
            System.out.println("VTo:" + v2);
            String sradio = request.getParameter("sradio");
            System.out.println("sradio:" + sradio);

            String viewtype = request.getParameter("viewtype");
            System.out.println("viewtype:" + viewtype);
            if (viewtype == null || viewtype.equals("all")) {
                viewtype = null;
            }
            String tempontology = request.getParameter("tempontology");

            String sclist = request.getParameter("sclist");
            String cclist = request.getParameter("cclist");
            String datasetUri = request.getParameter("dataseturi");

            boolean tempOntology = false;
            String vset = "";
            String vdisplay = "";
            Hashtable allResults = new Hashtable(); //store all results about changes and version pairs
            ArrayList<ColumnDescription> cd = new ArrayList<ColumnDescription>(); //store all 
            cd.add(new ColumnDescription("change", ValueType.TEXT, "Change"));
            List versions = new ArrayList();

            QueryUtils fur = null;
            if (datasetUri != null && changesOntology != null) {
                fur = new QueryUtils(prop, datasetUri, changesOntologySchema);
            }

            ArrayList<String> filterChangesList = fetchChangesList(fur, sclist, cclist);
            System.out.println("filterChangesList:::::::::::::" + filterChangesList.toString());

            /* Connect with jdbc @ Virtuoso driver */
            jdbc = new JDBCVirtuosoRep(prop);
            System.out.println("Connecting to Virtuoso with JDBC...");
            Map<String, String> versions_map = null;
            //JDBCVirtuosoRep jdbc, String changesOntol, ArrayList<String> versions, String changeType

            if (sradio != null && v1 != null && v2 != null) {
                //compare just two versions 
                if (sradio.equals("compareradio")) {
                    tempOntology = true;
                    System.out.println("Comparing..." + v1 + "-" + v2);

                    versions.add(v1);
                    versions.add(v2);
                    vset = v1.substring(v1.lastIndexOf("/") + 1) + "-" + v2.substring(v2.lastIndexOf("/") + 1);

                    cd.add(new ColumnDescription(vset, ValueType.NUMBER, "Number of occurences"));
                    Set<String> versionset = new LinkedHashSet<>();
                    Set<String> changeset = new LinkedHashSet<>();
                    changeset.add("ALL");
                    //MAP example: MAP{ADD_LABEL=69, ADD_SUPERCLASS=95, DELETE_LABEL=15, DELETE_TYPE_CLASS=14, DELETE_SUPERCLASS=47, DELETE_PROPERTY_INSTANCE=20, ADD_TYPE_CLASS=68, ADD_PROPERTY_INSTANCE=87}
                    // overwrite case (no need for further detection)
                    if (tempontology != null && tempontology.equals("no")) {
                        tempOntology = false;
                        versionset.add(v2);
                    } else {
                        try {
                            versionset.add(OntologyQueryServlet.getTempOntology(prop, datasetUri, v1, v2));
                        } catch (Exception ex) {
                            System.out.println("Cannot found temp ontology " + ex.getMessage());
                        }
                    }
                    
                    
                    
                    
                    LinkedHashMap<String, Long> mappair = (LinkedHashMap<String, Long>) fur.fetchChangesForVersions(versionset, changeset, tempOntology);
                    allResults = fetchMapResults(mappair, allResults, v2);
          

                } else { //evolution history (all versions) 

                    //Iterate-through versions to fill-in columns for each set of versions
                    //used to add the comparing set of versions to the label of the column for stacked charts
                    System.out.println("datasetversions:" + datasetUri);

                    DatasetsManager dmgr = null;
                    try {
                        dmgr = new DatasetsManager(prop, datasetUri);

                    } catch (Exception ex) {
                        System.out.println("VisServlet Exception (Dmgr)" + ex.getMessage());
                        //showErrorMessage(out, ex.getMessage());
                    }
                    if (dmgr != null) {
                        versions_map = dmgr.fetchDatasetVersions();
                        versions = new ArrayList(versions_map.keySet()); //YR new

                        System.out.println("-----------------ALL-VERSIONS QUERY---------------------");

                        if (versions.size() > 0) {
                            for (int i = 1; i < versions.size(); i++) {

                                v1 = (String) versions.get(i - 1);
                                v2 = (String) versions.get(i);
                                vset = v1.substring(v1.lastIndexOf("/") + 1) + "-" + v2.substring(v2.lastIndexOf("/") + 1);

                                vdisplay = versions_map.get(v1) + " VS. " + versions_map.get(v2);

                                if (vdisplay == null || vdisplay.equals("")) {
                                    vdisplay = v2.substring(v2.lastIndexOf("/") + 1); //previous apprioch
                                }

                                Set<String> versionset = new LinkedHashSet<>();
                                versionset.add(v2);
                                Set<String> changeset = new LinkedHashSet<>();
                                changeset.add("ALL");
                                LinkedHashMap<String, Long> mappair = (LinkedHashMap<String, Long>) fur.fetchChangesForVersions(versionset, changeset, tempOntology);

                                allResults = fetchMapResults(mappair, allResults, v2);

                                //Add column for each comparing vset of versions
                                cd.add(new ColumnDescription(vset, ValueType.NUMBER, vdisplay));
                            }

                        }
                    }
                    // end of dmgr!= null & visualization process
                    if (dmgr != null) {
                        dmgr.terminate();
                    }
                }

                data = buildDataTable(data, cd, allResults, versions, filterChangesList);

                //System.out.println("Columns:"+data.getNumberOfColumns());
                //System.out.println("Rows:"+data.getNumberOfRows());
            } // end of sradio != null && v1 != null && v2!= null

            return data;

        } catch (ClassNotFoundException ex) {
            System.out.println("CrossVersionServlet ClassNotFoundException Exception:" + ex.getMessage());

        } catch (SQLException ex) {
            System.out.println("CrossVersionServlet SQLException Exception:" + ex.getMessage());

        } catch (IOException ex) {
            System.out.println("CrossVersionServlet IOException Exception:" + ex.getMessage());

        } catch (Exception ex) {
            System.out.println("CrossVersionServlet Exception:" + ex.getMessage());

        }
        if (jdbc != null) {
            jdbc.terminate();
        }
        return null;
    }

    private DataTable buildDataTable(DataTable data, ArrayList<ColumnDescription> cd, Hashtable allResults, List versions, ArrayList<String> filterChangesList) throws TypeMismatchException {
        data.addColumns(cd);
        allResults = this.setEmptyValues(allResults, versions);
        //System.out.println("allResults........." + allResults.toString());

        TableRow row;
        String changeName = "";
        String version_values = "";
        //Iterate-through changes Map to fill-in results for each set of versions

        // Fill the data table with column values:
        Enumeration enum1 = allResults.keys();
        while (enum1.hasMoreElements()) {
            changeName = (String) enum1.nextElement();
            Hashtable verResults = new Hashtable();
            verResults = (Hashtable) allResults.get(changeName);
            if (filterChangesList.contains(changeName) || filterChangesList.isEmpty()) {
                row = new TableRow();
                row.addCell(changeName);

                for (int i = 1; i < versions.size(); i++) {
                    String version_val = (String) verResults.get((String) versions.get(i));
                    //String current_version = (String) versions.get(i);
                    row.addCell(Integer.parseInt(version_val));
                    //System.out.println("Row Value:" +version_val );
                }

                data.addRow(row);
                //System.out.println("ROW Ended for:" + changeName);
            }

        }
        return data;
    }

    private ArrayList<String> fetchChangesList(QueryUtils fur, String sclist, String cclist) {
        ArrayList<String> simpleChangesList = new ArrayList<String>();
        ArrayList<String> complexChangesList = new ArrayList<String>();
        ArrayList<String> filterChangesList = new ArrayList<String>(); //Apothikeuw tis filterchanges se mia lista, future beltistopoihsh analyze changes   
        if (fur != null) {
            if (sclist != null && !sclist.equals("")) {

                if (sclist.equals("null"))//add all simple changes
                {
                    simpleChangesList = (ArrayList<String>) fur.fetchChangesNames("Simple_Change");
                    fur.terminate();
                    filterChangesList.addAll(simpleChangesList);
                } else { // add selected ones simple changes
                    String changes[] = sclist.split(",");
                    for (int i = 0; i < changes.length; i++) {
                        if (!changes[i].equals("")) {
                            filterChangesList.add(changes[i]);
                        }
                    }
                }
            }

            //System.out.println("CCLIST:::::::"+cclist);
            if (cclist != null && !cclist.equals("")) {
                if (cclist.equals("null"))//add all simple changes
                {
                    complexChangesList = (ArrayList<String>) fur.fetchChangesNames("Complex_Change");
                    filterChangesList.addAll(complexChangesList);
                } else {
                    String fchanges[] = cclist.split(",");
                    for (int i = 0; i < fchanges.length; i++) {
                        if (!fchanges[i].equals("")) {
                            filterChangesList.add(fchanges[i]);
                        }
                    }
                }
            }
        }
        return filterChangesList;
    }

    private Hashtable fetchMapResults(LinkedHashMap mappair, Hashtable allResults, String v2) {
        String change_val = "";
        for (Object key : mappair.keySet()) { //get all keys (change names) and values (number)

            Hashtable changeVersionVals = new Hashtable();
            Hashtable existVersionVals = new Hashtable();
            change_val = mappair.get(key).toString();
            //schange_val = change_val.

            if (allResults.containsKey(key)) {
                existVersionVals = (Hashtable) allResults.get(key);
                existVersionVals.put(v2, change_val);
                allResults.put(key, existVersionVals);

            } else {
                changeVersionVals.put(v2, change_val);
                allResults.put(key, changeVersionVals);
            }

        }
        return allResults;
    }

//Set zero to undetected changes for all version pairs
//Note that AllResults contains {id(changeame),Hashtable(v2,value of change as string))
    private Hashtable setEmptyValues(Hashtable allResults, List versionsList) {
        Enumeration enum1 = allResults.keys();
        Enumeration enum2 = null;
        String chname_key = "";
        String v2 = "";
        String cur_version = "";

        while (enum1.hasMoreElements()) {
            chname_key = (String) enum1.nextElement();
            Hashtable verResults = new Hashtable();
            verResults = (Hashtable) allResults.get(chname_key);
            enum2 = verResults.keys();
            while (enum2.hasMoreElements()) { //For each change Hashtable
                v2 = (String) enum2.nextElement();
                for (int i = 1; i < versionsList.size(); i++) { //except i=0 for first version because I store the first vset is v2
                    cur_version = (String) versionsList.get(i);
                    if (!verResults.containsKey(cur_version)) {
                        verResults.put(cur_version, "0");
                    }
                }

                allResults.put(chname_key, verResults);
            }
        }

        return allResults;

    }

    @Override
    protected boolean isRestrictedAccessMode() {
        return false;
    }
}
