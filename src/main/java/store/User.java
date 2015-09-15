/*
 *@author: Yannis Roussakis, Ioannis Chrysakis
 */

package store;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import org.diachron.detection.repositories.JDBCVirtuosoRep;
import org.diachron.detection.utils.DatasetsManager;
import org.openrdf.repository.RepositoryException;

/**
 * This class used to handle users  
 */
public class User {

    private JDBCVirtuosoRep jdbc;
    private String username;
    private String userDatasetUri;
    private String userOntologySchema;
    private DatasetsManager dManager;

    /**
     * The User constructor. Always use terminate after the usage.
     * @param propFile the properties file that includes connection's credentials to the store
     * @param username the username which could be used to construct the respective named graphs
     * @param defaultDatasetUri the default dataset URI which contains the default schema to be copied for user initialization
     */
    public User(Properties propFile, String username, String defaultDatasetUri) {
        this.username = username;
        String tmpUri = defaultDatasetUri;
        if (defaultDatasetUri.endsWith("/")) {
            tmpUri = defaultDatasetUri.substring(0, defaultDatasetUri.length() - 1);
        }
        this.userDatasetUri = tmpUri + "/" + username;
        this.userOntologySchema = this.userDatasetUri + "/changes/schema";
        try {
            this.jdbc = new JDBCVirtuosoRep(propFile);
            dManager = new DatasetsManager(jdbc, defaultDatasetUri);
            dManager.copyVersionsToDataset(userDatasetUri);
            dManager.copyChangeOntologies(defaultDatasetUri, userDatasetUri);
        } catch (Exception ex) {
            System.out.println("Exception: " + ex.getMessage());
        }
        if (!jdbc.graphExists(userOntologySchema)) {
            String defaultSchema = tmpUri + "/changes/schema";
            jdbc.copyGraph(defaultSchema, userOntologySchema);
        }
    }

   /**
    * Deletes a user
    */
    public void deleteUser() {
        jdbc.clearGraph(this.userOntologySchema, false);
        try {
            dManager.deleteDataset(false, true);
        } catch (SQLException ex) {
            System.out.println("Exception: " + ex.getMessage());
        }
    }

    /**
    * Returns the user associated dataset URI
    * @return the user associated dataset URI
    */
    public String getUserDatasetUri() {
        return userDatasetUri;
    }

    /**
    * Returns the user associated changes ontology schema
    * @return user associated changes ontology schema
    */
    public String getUserOntologySchema() {
        return userOntologySchema;
    }

    /**
    * Returns the JDBCRepository connection object 
    * @return the JDBCRepository connection object 
    */
    private JDBCVirtuosoRep getJdbc() {
        return jdbc;
    }
    
    /**
     * Terminates connection to the Virtuoso RDF store.
     */
    public void terminate() {
        jdbc.terminate();
    }
    
    /**
    *  Used to initialize e dataset for D2V tool users
    */
    private static void initDataset(String configPath, String datasetURI) throws ClassNotFoundException, SQLException, RepositoryException{
        Properties prop = new Properties();
        InputStream inputStream;
        String username;
        User user;
        try {
            //config.properties
            inputStream = new FileInputStream(configPath);
            prop.load(inputStream);
            
             //JCH: Coukld get Dataset all URIS from config file
            
            List<String> changesList = new ArrayList<>();
            if (!datasetURI.endsWith("/")){
                datasetURI = datasetURI +"/";
            }
            
            //Create guest account
            username = "guest";
            user = new User(prop, username, datasetURI);
            user.terminate();
            
            //Create user1 account
            username = "user1";
            user = new User(prop, username, datasetURI);
            user.terminate();
            
            //Create clean account
            username = "clean";
            user = new User(prop, username, datasetURI);
            user.terminate();
            
            //Clear potential complex changes in clean
            String cleanURI = datasetURI +"clean";
            String changesOntologySchema = cleanURI + "/changes/schema";
            QueryUtils qul = new QueryUtils(prop, cleanURI, changesOntologySchema);
            changesList.addAll(qul.fetchChangesNames("Complex_Change"));
            qul.terminate();
            
            MCDUtils mcduls = new MCDUtils(configPath, changesOntologySchema, cleanURI, null);
            if (!changesList.isEmpty()){
            mcduls.deleteMultipleCC(changesList);
            }
            mcduls.terminate();
            
            //Create user1clean
            username = "user1clean";
            user = new User(prop, username, cleanURI);
            user.terminate();
            
            
        } catch (IOException ex) {
            System.out.println("Exception: " + ex.getMessage() + " occured .");
            return;
        }
    }

    public static void main(String[] args) throws Exception {
        String efoDataset = "http://www.ebi.ac.uk/efo/";
        initDataset("config.properties",efoDataset);
        
   /*     String goDataset = "http://geneontology.org/";
        String ideaDataset = "http://idea-garden.org";
        Properties prop = new Properties();
        InputStream inputStream;
        try {
            inputStream = new FileInputStream("config.properties");
            prop.load(inputStream);
            
           
            
        } catch (IOException ex) {
            System.out.println("Exception: " + ex.getMessage() + " occured .");
            return;
        }
        
        String username = "user1clean";
        User user = new User(prop, username, efoDataset);
        //user.deleteUser();
        user.terminate();

        /*user = new User(prop, username, ideaDataset);
        user.deleteUser();
        user.terminate();

        user = new User(prop, username, goDataset);
        user.deleteUser();
        user.terminate();*/
        // }

//        String uri = user.getUserDatasetUri();
//        String schema = user.getUserOntologySchema();
//        IdeaGarden.DeleteAllDefinedComplexChanges(uri, schema);
//        System.out.println(user.getUserDatasetUri());
//        System.out.println(user.getUserOntologySchema());
//        user.deleteUser();
//        user.getJdbc().clearGraph("http://geneontology.org/changes/3-4/copy", true);
//        user.getJdbc().copyGraph("http://geneontology.org/changes/3-4", "http://geneontology.org/changes/3-4/copy");
//        user.getJdbc().renameGraph("http://geneontology.org/changes/3-4/copy", "http://geneontology.org/changes/3-4/copy/new");
        //System.out.println(user.getJdbc().triplesNum("http://geneontology.org/changes/3-4"));
    }
}
