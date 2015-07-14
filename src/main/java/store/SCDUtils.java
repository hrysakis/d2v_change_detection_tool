/*
 *@author: Yannis Roussakis, Ioannis Chrysakis
 */

package store;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.sql.SQLException;
import java.util.Properties;
import org.diachron.detection.repositories.JDBCVirtuosoRep;
import org.diachron.detection.utils.ChangesDetector;


public class SCDUtils {

    private Properties propFile;
    private String changesOntology;
    private String changesOntologySchema;
    private String associations;
    
    
    /**
     * Constructor of SCDUtils.
     * @param prop the properties file that includes connection's credentials to the store
     * @param changesOnt the changes ontology
     * @param changesOntSchema the changes ontology schema
     * @param datasetUri the dataset URI
     * @param assoc the named graph which contains optionally associations
     * @throws ClassNotFoundException in case of internal problem with the store
     * @throws SQLException in case of connection problem with the store
     * @throws IOException in case of no any properties file found
     */
    public SCDUtils(String prop, String changesOnt, String changesOntSchema, String datasetUri, String assoc) throws IOException, ClassNotFoundException, SQLException {
        propFile = new Properties();
        InputStream inputStream;
        inputStream = new FileInputStream(prop);
        propFile.load(inputStream);
        this.changesOntology = changesOnt;
        this.changesOntologySchema = changesOntSchema;
        associations = assoc;
        inputStream.close();
    }

    /**
     * Deletes a changes ontology
     * @throws ClassNotFoundException in case of internal problem with the store
     * @throws SQLException in case of connection problem with the store
     * @throws IOException in case of no any properties file found
     */
    public void deleteChangesOntology() throws ClassNotFoundException, SQLException, IOException {
        JDBCVirtuosoRep rep = new JDBCVirtuosoRep(propFile);
        rep.clearGraph(changesOntology, false);
        rep.terminate();
    }

    /**
     * Compares a custom pair of versions, performing live detection
     * @param oldV the selected old version
     * @param newV the selected new version
     * @param simpleChanges an array of strings corresponding to simple change names to eliminate results, otherwise set to null
     * @param complexChanges an array of strings corresponding to complex change names to eliminate results, otherwise set to null
     * @throws Exception general exception
     */
    public void customCompareVersions(String oldV, String newV, String[] simpleChanges, String[] complexChanges) throws Exception {
        ChangesDetector detector = new ChangesDetector(propFile, changesOntology, changesOntologySchema);
        detector.detectSimpleChanges(oldV, newV, simpleChanges);
        detector.detectAssociations(oldV, newV, associations);
        detector.detectComplexChanges(oldV, newV, complexChanges);
        detector.terminate();
    }

}
