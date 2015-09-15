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
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;
import store.QueryUtils;

/**
 * This servlet used to create the data structures required for advanced on the
 * fly visualization analysis based on user options.
 */
public class ExtVisualiseServlet extends DataSourceServlet {

    /**
     * Generates the data table that required for each google chart
     *
     * @param query the google query source
     * @param request the request that comes from query
     */
    @Override
    public DataTable generateDataTable(Query query, HttpServletRequest request) {
        DataTable data = new DataTable(); // Create a data table in order to be an input for the google charts.

        String changesOntology = request.getParameter("changesgraph");
        String changesOntologySchema = changesOntology + "/schema";
        String datasetURI = request.getParameter("dataseturi");
        ServletContext servletContext = getServletContext();
        String configPath = OntologyQueryServlet.getConfigFilePath(servletContext, changesOntology);
        System.out.println("config_path===================>" + configPath);

        Properties prop = new Properties();
        InputStream inputStream;
        try {
            inputStream = new FileInputStream(configPath);
            prop.load(inputStream);
        } catch (IOException ex) {
            System.out.println("Exception with prop file: " + ex.getMessage() + " occured .");
            //return;
        }

        String groupby = request.getParameter("groupby"); //versionsradio vs. changesradio
        String sclist = request.getParameter("sclist"); // "null" or comma string simple changes
        String cclist = request.getParameter("cclist"); // "null" or comma string complex changes
        String vlist = request.getParameter("vlist"); //ALL or comma string newversions

        boolean tempOntology = false;
        String tempontology = request.getParameter("tempontology");
        String on_compare = request.getParameter("oncompare");
        boolean customCompare = false;

        if (tempontology != null && tempontology.equals("yes")) {
            tempOntology = true;
        }

        ArrayList<ColumnDescription> cd = new ArrayList<ColumnDescription>();
        TableRow row;

        String occurences = "";
        String vset;
        QueryUtils fur = null;

        List versions_list = new ArrayList();
        Map<String, String> vset_labels_map = null;
        String v1_label, v2_label;
        Set<String> versions = new LinkedHashSet<>();
        //Filling the versions and changes

        String vfrom = "", vto = "";
        if (on_compare != null && on_compare.equals("yes")) {
            customCompare = true;

            vfrom = request.getParameter("vfrom");
            vto = request.getParameter("vto");
            //put the temp graph on the versions list
            if (tempOntology) {
                try {
                    versions.add(OntologyQueryServlet.getTempOntology(prop, datasetURI, vfrom, vto));
                } catch (Exception ex) {
                    System.out.println("Cannot found temp ontology " + ex.getMessage());
                }
            } else {
                versions.add(vto);
            }

        } else {
            versions = getVersionsSet(vlist);
        }
        Set<String> changes = getChangesSet(sclist, cclist);
        System.out.println("VERSIONS:" + versions.toString());
        System.out.println("CHANGES:" + changes.toString());

        /////////////////////////////////////////////////////////////////////////////////////
        if (datasetURI != null && changesOntology != null) {
            try {
                fur = new QueryUtils(prop, datasetURI, changesOntologySchema);
            } catch (SQLException | IOException | ClassNotFoundException ex) {
                System.out.println("ExtVisServlet Exception:" + ex.getMessage());
            }
        }
        if (fur != null && groupby != null) {

            if (groupby.equals("versionsradio")) {

                cd.add(new ColumnDescription("vset", ValueType.TEXT, "Versions"));
                cd.add(new ColumnDescription("changetimes", ValueType.NUMBER, "Number of occurences"));
                data.addColumns(cd);

                LinkedHashMap<String, Long> results = (LinkedHashMap<String, Long>) fur.fetchVersionsForChanges(versions, changes);
                for (String key : results.keySet()) {

                    vset_labels_map = fur.fetchVersionLabels(key);
                    versions_list = new ArrayList(vset_labels_map.keySet()); // fixing vset displayable at pie to take its name from version labels
                    if (!versions_list.isEmpty()) {
                        v1_label = (String) versions_list.get(0);
                        v2_label = vset_labels_map.get(v1_label);
                        vset = v1_label + " VS. " + v2_label;
                    } else {
                        vset = key.substring(key.lastIndexOf("/") + 1); //vset name take as default its name from the changes graph
                        vset = vset.replaceAll("\\-", " VS. ");
                    }

                    occurences = results.get(key).toString();
                    if (!occurences.equals("0")) {
                        row = new TableRow();
                        row.addCell(vset);
                        row.addCell(Integer.parseInt(occurences));
                        try {
                            data.addRow(row);
                        } catch (TypeMismatchException ex) {
                            System.out.println("TypeMismatchException exception!");
                        }
                    }
                }

            } else if (customCompare || groupby.equals("changesradio")) {

                cd.add(new ColumnDescription("changename", ValueType.TEXT, "Change"));
                cd.add(new ColumnDescription("changenumber", ValueType.NUMBER, "Number of occurences"));
                data.addColumns(cd);

                LinkedHashMap<String, Long> results = (LinkedHashMap<String, Long>) fur.fetchChangesForVersions(versions, changes, tempOntology);

                for (String key : results.keySet()) {

                    occurences = results.get(key).toString();
                    if (!occurences.equals("0")) {
                        row = new TableRow();
                        row.addCell(key);
                        row.addCell(Integer.parseInt(occurences));
                        try {
                            data.addRow(row);
                        } catch (TypeMismatchException ex) {
                            Logger.getLogger(ExtVisualiseServlet.class.getName()).log(Level.SEVERE, null, ex);
                        }
                    }
                }

            }

        }

        if (fur != null) {
            fur.terminate();
        }
        return data;

    }

    /**
     * Filling in the set of changes required for visualization analysis
     *
     * @param sclist a comma string of selected simple changes
     * @param cclist a comma string of selected complex changes
     * @return the set of changes
     */
    public static Set<String> getChangesSet(String sclist, String cclist) {

        Set<String> changes = new LinkedHashSet<>();
        if (sclist != null && cclist != null) {

            if (sclist.equals("null") && cclist.equals("null")) {
                changes.add("ALL");
            } else {

                if (sclist.equals("null")) {
                    changes.add("Simple_Change");
                } else if (!sclist.equals("")) {
                    String schanges[] = sclist.split(",");
                    for (String schange : schanges) {
                        if (!schange.equals("")) {
                            changes.add(schange);

                        }
                    }
                }
                if (cclist.equals("null")) {
                    changes.add("Complex_Change");
                } else if (!cclist.equals("")) {
                    String cchanges[] = cclist.split(",");
                    for (String cchange : cchanges) {
                        if (!cchange.equals("")) {
                            changes.add(cchange);
                        }
                    }
                }

            }
        }

        return changes;

    }

    /**
     * Filling in the set of versions required for visualization analysis
     *
     * @param vlist a comma string of selected versions
     * @return the set of versions
     */
    public static Set<String> getVersionsSet(String vlist) {
        Set<String> versions = new LinkedHashSet<>();
        if (vlist != null && vlist.equals("ALL")) {
            versions.add("ALL");
        } else {
            String nversions[] = vlist.split(",");
            for (int i = 0; i < nversions.length; i++) {
                if (!nversions[i].equals("")) {
                    versions.add(nversions[i]);
                }
            }
        }
        return versions;
    }
}
