/*
 *@author: Yannis Roussakis, Ioannis Chrysakis
 */

package store;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import org.diachron.detection.complex_change.CCDefinitionError;
import org.diachron.detection.complex_change.CCManager;
import org.diachron.detection.complex_change.SCDefinition;
import org.diachron.detection.complex_change.VersionFilter;
import org.diachron.detection.repositories.JDBCVirtuosoRep;
import org.diachron.detection.utils.ChangesDetector;
import org.diachron.detection.utils.ChangesManager;
import org.diachron.detection.utils.DatasetsManager;
import org.diachron.detection.utils.JSONMessagesParser;
import org.openrdf.repository.RepositoryException;


public class MCDUtils {

    private Properties propFile;
    private String changesOntologySchema;
    private String datasetUri;
    private List<String> changesOntologies;
    private ChangesDetector detector;
    private String associations;

    /**
     * Constructor of MCDUtils. Always use terminate after the usage.
     * @param prop the properties file that includes connection's credentials to the store
     * @param changesOntologySchema the changes ontology schema
     * @param datasetUri the selected dataset URI
     * @param assoc the named graph which contains optionally associations
     * @throws ClassNotFoundException in case of internal problem with the store
     * @throws SQLException in case of connection problem with the store
     * @throws IOException in case of no any properties file found
     * @throws RepositoryException in case of internal problem with the store
     */
    public MCDUtils(String prop, String changesOntologySchema, String datasetUri, String assoc) throws IOException, ClassNotFoundException, SQLException, RepositoryException {
        propFile = new Properties();
        InputStream inputStream;
        inputStream = new FileInputStream(prop);
        propFile.load(inputStream);
        inputStream.close();
        this.changesOntologySchema = changesOntologySchema;
        this.datasetUri = datasetUri;
        this.detector = new ChangesDetector(prop, null, changesOntologySchema);  //the changes ontology is null initially
        initChangesOntologies();
        associations = assoc;
    }


    /**
     * Saves a complex change definition based on a template
     * @param ccName the complex change name
     * @param ccPriority the complex change priority
     * @param ccJson the complex change definition in a JSON string representation
     * @param selectedURI a selected URI to be included in the internal selection filter of definition
     * @return a CCDefinitionError object which contains an enumeration of error codes in the case of error,
     * otherwise the error code returned as null
     */
    public CCDefinitionError saveTemplateCCDefinition(String ccName, double ccPriority, String ccJson, String selectedURI) {
        ccJson = ccJson.replace("[cc_name]", ccName);
        ccJson = ccJson.replace("[cc_priority]", "" + ccPriority);
        if (isURI(selectedURI)) {
            selectedURI = "<" + selectedURI + ">";
        } else {
            selectedURI = "'" + selectedURI + "'";
        }
        if (selectedURI != null && !selectedURI.equals("")) {
            ccJson = ccJson.replace("[select_filter]", selectedURI);
        } else {
            String filter = "\"Selection_Filter\": ";
            int start = ccJson.indexOf(filter) + filter.length();
            int end = ccJson.substring(start + 1).indexOf("\"");
            String replaced = ccJson.substring(start + 1, start + end + 1);
            ccJson = ccJson.replace(replaced, "");
        }
        CCManager ccDef = null;
        CCDefinitionError result;
        try {
            System.out.println(ccJson);
            ccDef = JSONMessagesParser.createCCDefinition(propFile, ccJson, changesOntologySchema);
        } catch (Exception ex) {
            System.out.println("Exception: " + ex.getMessage() + " occured .");
            result = ccDef.getCcDefError();
        }
        if (ccDef.getCcDefError().getErrorCode() == null) {
            ccDef.insertChangeDefinition();
            System.out.println("Complex Change definition was stored successfully.");
            result = ccDef.getCcDefError();
            for (String ontology : getChangesOntologies()) {
                ccDef.deleteComplexChangeInstWithLessPr(ontology, ccName); //update the changes ontology schema accordingly 
            }
        } else {
            System.out.println("Error: " + ccDef.getCcDefError().getDescription());
            result = ccDef.getCcDefError();
        }
        ccDef.terminate();
        if (result.getErrorCode() == null) {
            detectDatasets(true);
        }
        return result;
    }
    /**
     * Saves a complex change definition including all extended functionalities
     * @param name the complex change name
     * @param priority the complex change priority
     * @param description the complex change description
     * @param scDefinitions a list of simple change definitions 
     * @param ccParameters a list of the complex change parameters
     * @param vFilters a list of version filter definitions
     * @return a CCDefinitionError object which contains an enumeration of error codes in the case of error,
     *  otherwise the error code returned as null.
     */
    public CCDefinitionError saveCCExtendedDefinition(String name, Double priority, String description, List<SCDefinition> scDefinitions, Map<String, String> ccParameters, List<VersionFilter> vFilters) {
        CCManager ccDef = null;
        try {
            ccDef = new CCManager(propFile, changesOntologySchema);
        } catch (Exception ex) {
            System.out.println("Exception: " + ex.getMessage() + " occured .");
        }
        CCDefinitionError result = ccDef.saveCCExtendedDefinition(name, priority, description, scDefinitions, ccParameters, vFilters);
        if (result.getErrorCode() == null) {
            for (String ontology : getChangesOntologies()) {
                ccDef.deleteComplexChangeInstWithLessPr(ontology, name); //update the changes ontology schema accordingly 
            }
            ccDef.terminate();
            detectDatasets(true);
        } else {
            ccDef.terminate();
            System.out.println(result.getErrorCode());
        }
        return result;
    }

    public void detectDatasets(boolean complexOnly) {
        try {
            DatasetsManager dManager = new DatasetsManager(getJDBCRepository(), datasetUri);
            List<String> versions = new ArrayList(dManager.fetchDatasetVersions().keySet());
            String[] complexChanges = {};
            for (int i = 1; i < versions.size(); i++) {
                String v1 = versions.get(i - 1);
                String v2 = versions.get(i);
                ChangesManager cManager = new ChangesManager(getJDBCRepository(), datasetUri, v1, v2, false);
                String changesOntology = cManager.getChangesOntology();
                detector.setChangesOntology(changesOntology);
                if (!complexOnly) {
                    detector.detectAssociations(v1, v2, associations);
                    detector.detectSimpleChanges(v1, v2, null);
                }
                detector.detectComplexChanges(v1, v2, null);
                System.out.println("-----");
            }
        } catch (Exception ex) {
            detector.terminate();
            System.out.println("Exception: " + ex.getMessage());
        }
    }

    /**
     * Deletes one or more complex change(s)
     * @param names a list of complex change name(s) to be deleted
     * @return true on successful deletion
     */
    public boolean deleteMultipleCC(List<String> names) {
        CCManager ccDef = null;
        boolean result = false, retVal = false;
        changesOntologies = getChangesOntologies();
        try {
            ccDef = new CCManager(propFile, changesOntologySchema);
            for (String changesOntology : changesOntologies) {
                retVal = ccDef.deleteComplexChanges(changesOntology, names, true);
                if (retVal) {
                    result = retVal;
                }
            }
            retVal = ccDef.deleteComplexChanges(changesOntologySchema, names, false);
            if (retVal) {
                result = retVal;
            }
            if (result) {
                detectDatasets(true);
            }
        } catch (Exception ex) {
            System.out.println("Exception: " + ex.getMessage() + " occured .");
        }
        if (ccDef != null) {
            ccDef.terminate();
        }
        return result;
    }

    /**
     * Deletes one  more complex change based on its name
     * @param name the complex change name to be deleted
     * @return true on successful deletion
     */
    public boolean deleteCC(String name) {
        boolean result = false, retVal = false;
        CCManager ccDef = null;
        changesOntologies = getChangesOntologies();
        try {
            ccDef = new CCManager(propFile, changesOntologySchema);
            for (String changesOntology : changesOntologies) {
                retVal = ccDef.deleteComplexChange(changesOntology, name, true);
                if (retVal) {
                    result = retVal;
                }
            }
            retVal = ccDef.deleteComplexChange(changesOntologySchema, name, false);
            if (retVal) {
                result = retVal;
            }
        } catch (Exception ex) {
            System.out.println("Exception: " + ex.getMessage() + " occured .");
        }
        if (ccDef != null) {
            ccDef.terminate();
        }
        if (result) {
            detectDatasets(true);
        }
        return result;
    }

    /**
     * Returns true if the selected string corresponds to valid URI based on its prefix
     * @param s the selected string
     * @return true if the string corresponds to valid URI based on its prefix
     */
    public static boolean isURI(String s) {
        return s.startsWith("http") || s.startsWith("md5");
    }

    /**
     * Returns true if the selected string corresponds to a valid complex change parameter
     * @param s the selected string
     * @return true if the selected string corresponds to a valid complex change parameter
     */
    public boolean isValidCCParam(String s) {
        if (s.contains(":-") || s.startsWith("<") || !s.startsWith("'")) {
            return true;
        }
        return false;
    }

    private void initChangesOntologies() {
        StringBuilder datasetChanges = new StringBuilder();
        if (datasetUri.endsWith("/")) {
            datasetChanges.append(datasetUri.substring(0, datasetUri.length() - 1));
        } else {
            datasetChanges.append(datasetUri);
        }
        datasetChanges.append("/changes");
        this.changesOntologies = new ArrayList<>();
        String query = "select ?ontol from <http://datasets> where {\n"
                + "<" + datasetChanges + "> rdfs:member ?ontol.\n"
                + "?ontol co:old_version ?v1.\n"
                + "}  order by ?v1";
        try {
            JDBCVirtuosoRep jdbc = getJDBCRepository();
            ResultSet results = jdbc.executeSparqlQuery(query, false);
            if (results.next()) {
                do {
                    this.changesOntologies.add(results.getString(1));
                } while (results.next());
            }
        } catch (Exception ex) {
            System.out.println("Exception: " + ex.getMessage() + " occured .");
        }

    }

    private List<String> getChangesOntologies() {
        return this.changesOntologies;
    }
    
    /**
     * Returns the JDBCRepository connection object 
     * @return the JDBCRepository connection object 
     */
    public JDBCVirtuosoRep getJDBCRepository() {
        return detector.getJdbc();
    }

    /**
     * Terminates connection to the Virtuoso RDF store.
     */
    public void terminate() {
        this.detector.terminate();
    }

}
