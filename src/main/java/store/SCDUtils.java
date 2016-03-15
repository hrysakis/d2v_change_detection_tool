/*
 *@author: Yannis Roussakis, Ioannis Chrysakis
 */
package store;

import java.io.File;
import java.io.IOException;
import java.sql.SQLException;
import java.util.Properties;
import org.diachron.detection.repositories.JDBCVirtuosoRep;
import org.diachron.detection.utils.ChangesDetector;
import org.diachron.detection.utils.IOOps;

public class SCDUtils {

    private Properties propFile;
    private String changesOntology;
    private String changesOntologySchema;
    private String associations;
    
    private String[] simpleChanges;

    /**
     * Constructor of SCDUtils.
     *
     * @param prop the properties file that includes connection's credentials to
     * the store
     * @param changesOnt the changes ontology
     * @param changesOntSchema the changes ontology schema
     * @param datasetUri the dataset URI
     * @param assoc the named graph which contains optionally associations
     * @throws ClassNotFoundException in case of internal problem with the store
     * @throws SQLException in case of connection problem with the store
     * @throws IOException in case of no any properties file found
     */
    public SCDUtils(Properties prop, String changesOnt, String changesOntSchema, String datasetUri, String assoc) throws IOException, ClassNotFoundException, SQLException {
        propFile = prop;
        this.changesOntology = changesOnt;
        this.changesOntologySchema = changesOntSchema;
        associations = assoc;
        this.simpleChanges = prop.getProperty("Simple_Changes").split(",");
    }

    /**
     * Deletes a changes ontology
     *
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
     *
     * @param simpleChangesFolder the folder that keeps the simple changes file
     * @param oldV the selected old version
     * @param newV the selected new version
     * @param simpleChanges an array of strings corresponding to simple change
     * names to eliminate results, otherwise set to null
     * @param complexChanges an array of strings corresponding to complex change
     * names to eliminate results, otherwise set to null
     * @throws Exception general exception
     */
    public void customCompareVersions(String simpleChangesFolder, String oldV, String newV, String[] simpleChanges, String[] complexChanges) throws Exception {
        
        ChangesDetector detector = new ChangesDetector(propFile, changesOntology, changesOntologySchema,associations);     
        //detector.detectSimpleChanges(oldV, newV, simpleChanges);
        detectSimpleChanges(simpleChangesFolder, oldV, newV, this.simpleChanges);
        detector.detectAssociations(oldV, newV);
        detector.detectComplexChanges(oldV, newV, complexChanges);
        detector.terminate();
    }
    
   /**
     * Detects the Simple Changes among two named graph versions and stores the
     * detected changes into the changes ontology.
     *
     * @param simpleChangesFolder the folder where Simple Changes has been stored
     * @param oldVersion The old version named graph.
     * @param newVersion The new version named graph.
     * @param simpleChanges An array of simple changes which may be potentially
     * be considered instead of all the defined simple changes.
     * @throws java.lang.Exception in case of error on detection
     */
    public void detectSimpleChanges(String simpleChangesFolder, String oldVersion, String newVersion, String[] simpleChanges) throws Exception {
        
        JDBCVirtuosoRep jdbc = new JDBCVirtuosoRep(propFile);
        System.out.println("-------------");
        System.out.println("Simple Change Detection among versions:");
        System.out.println(oldVersion + " (" + jdbc.triplesNum(oldVersion) + " triples)");
        System.out.println(newVersion + " (" + jdbc.triplesNum(newVersion) + " triples)");
        System.out.println("-------------");
        long oldSize = jdbc.triplesNum(changesOntology);
        System.out.print("Detecting simple changes...");
        long start = System.currentTimeMillis();
        String query = null;
        for (String simpleChange : simpleChanges) {
            StringBuilder prefixes = new StringBuilder("PREFIX diachron: <http://www.diachron-fp7.eu/resource/>\n"
                    + "PREFIX efo:<http://www.ebi.ac.uk/efo/>\n"
                    + "PREFIX co:<http://www.diachron-fp7.eu/changes/>"
                    + "PREFIX qb:<http://purl.org/linked-data/cube#>\n");
            prefixes.append(IOOps.readData(simpleChangesFolder + File.separator + simpleChange));
            query = prefixes.toString();
            query = query.replaceAll("changesOntology", changesOntology);
            query = query.replaceAll("'v1'", "'" + oldVersion + "'");
            query = query.replaceAll("'v2'", "'" + newVersion + "'");
            query = query.replaceAll("<v1>", "<" + oldVersion + ">");
            query = query.replaceAll("<v2>", "<" + newVersion + ">");
            query = query.replaceAll("<assoc>", "<" + this.associations + ">");
//            System.out.println("Detecting " + simpleChange);
            jdbc.executeUpdateQuery("sparql " + query, false);
        }
        System.out.println("DONE in " + (System.currentTimeMillis() - start));
        System.out.println("Simple change triples size: " + (jdbc.triplesNum(changesOntology) - oldSize));
        jdbc.terminate();
    }
   

}
