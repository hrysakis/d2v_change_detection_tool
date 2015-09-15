/**
 *
 * @author Ioannis Chrysakis (hrysakis@ics.forth.gr)
 */
package utils;

import info.aduna.lang.FileFormat;
import java.io.FileInputStream;
import java.io.InputStream;
import java.util.Properties;
import org.diachron.detection.repositories.SesameVirtRep;
import org.diachron.detection.utils.ChangesManager;
import org.diachron.detection.utils.DatasetsManager;
import org.openrdf.rio.RDFFormat;
import store.SCDUtils;

/**
 *
 * This class gives the opportunity to manage datasets in Virtuoso
 * exploiting DIACHRONFORTH library. Also used in IDEAGARDEN project.
 */
public class VirtuosoUploader {

   
    private Properties propertiesFile = null;
    private static DatasetsManager dmgr = null;
    private static String datasetURI = null;
    private String previousNamegraph = null;
     private String graphDest = null;
    
    /**
     * Constructor which sets the datasetURI and the assigned user. 
     * Note that the default user is "guest".
     * @param prop the properties file containing configuration parameters
     * @param datasetURI the specified dataset URI
     * @param user the username used for creating separate names graphs inside store
     * @throws Exception 
     */
    public VirtuosoUploader(Properties prop, String datasetURI, String user) throws Exception {
        if(datasetURI.endsWith("/")){ //removes last slash
            datasetURI = datasetURI.substring(0,datasetURI.length()-1);
        }
        this.propertiesFile = prop;
        this.datasetURI = datasetURI + "/" + user;
        this.graphDest = this.datasetURI + "/changes/schema";
        
       
    }

    /**
     * Ingest a dataset with specified label to Virtuoso and assigned it with a user and a datasetURI
     * @param datasetLabel the specified label
     * @return true on successful ingest otherwise it returns false
     */
    public boolean insertDataset(String datasetLabel) {
        
        try {
            this.dmgr = new DatasetsManager(propertiesFile, this.datasetURI);
            dmgr.insertDataset(this.datasetURI, datasetLabel);
            dmgr.terminate();
            return true;
        } catch (Exception ex) {
            System.out.println("insertDataset Exception:"+ex.getMessage());
            dmgr.terminate();
            return false;
        }
            
    }

    /**
     * Ingests a dataset version to Virtuoso from a file path with specified label and version number.
     * @param datasetVersionFilePath
     * @param versionNumber
     * @param datasetVersionLabel
     * @return true on successful ingest otherwise it returns false
     */
    public boolean insertDatasetVersion(String datasetVersionFilePath, String versionNumber, String datasetVersionLabel) {
        try {
            String constructed_versionNamedgraph = datasetURI + "/" + versionNumber;
            this.dmgr = new DatasetsManager(propertiesFile, this.datasetURI);
            dmgr.insertDatasetVersion(datasetVersionFilePath, RDFFormat.RDFXML, constructed_versionNamedgraph, datasetVersionLabel, datasetURI);
            
            if(previousNamegraph != null){
                this.customCompare(propertiesFile, datasetURI, graphDest, null, null, previousNamegraph, constructed_versionNamedgraph, false);
            }
            
            previousNamegraph = constructed_versionNamedgraph;
            dmgr.terminate();
            return true;
        } catch (Exception ex) {
            
            dmgr.terminate();
            System.out.println("insertDatasetVersion Exception:"+ex.getMessage());
            return false;
        }
    }

    /**
     * Imports a schema after the ingestion of a datasetURI
     * @param schemaFilePath the file path of the schema
     * @return true on successful import otherwise it returns false
     */
    public boolean insertSchema(String schemaFilePath) {
        try {
            SesameVirtRep sesame = new SesameVirtRep(propertiesFile);
            sesame.importFile(schemaFilePath, (RDFFormat) this.getFileFormatByFilename(schemaFilePath), graphDest);

            return true;
        } catch (Exception ex) {
            System.out.println("insertSchema Exception:"+ex.getMessage());
            return false;
        }
    }

    /**
     * Performs a change detection between two selected versions of a datasetURI
     * @param prop the properties file
     * @param datasetURI the specified dataset URI
     * @param changes_ontology_schema the graph destination of the loaded schema
     * @param scarray a string array of simple change name(s) to be included in the detection 
     * If it is null ALL Simple Changes should be taken into account.
     * @param ccarray a string array of complex change name(s) to be included in the detection 
     * If it is null ALL Simple Changes should be taken into account.
     * @param vold the old version URI 
     * @param vnew the new version URI
     * @param tempOntology a flag that denotes it the detection results should be stored in a temp named graph 
     */
    private void customCompare(Properties prop, String datasetURI, String changes_ontology_schema, String[] scarray, String[] ccarray, String vold, String vnew, boolean tempOntology) {
        ChangesManager cManager;
        String changesOntology;

        try {
            cManager = new ChangesManager(prop, datasetURI, vold, vnew, tempOntology);
            changesOntology = cManager.getChangesOntology();
            cManager.terminate();
            SCDUtils scd = new SCDUtils(prop, changesOntology, changes_ontology_schema, datasetURI, null);
            scd.customCompareVersions(vold, vnew, scarray, ccarray);
        } catch (Exception ex) {
            System.out.println("customCompare Exception:"+ex.getMessage());
        }
    }
    
    /**
     * Returns the RDFFormat based on a  specified filename
     * @param filename the specified filename
     * @return the RDFFormat based on a  specified filename
     */
    public static FileFormat getFileFormatByFilename(String filename){
        if (filename !=null){
            String ext = filename.substring(filename.lastIndexOf(".")+1);
            switch (ext) {
                case "rdf":
                case "xml":
                case "rdfs":
                    return RDFFormat.RDFXML;
                case "n3":
                    return RDFFormat.N3;
                case "nt":
                    return RDFFormat.NTRIPLES;
                case "nq":
                    return RDFFormat.NQUADS;
                case "ttl":
                    return RDFFormat.TURTLE;
            }
        }
        
        return null;
    }
    
    
   
    
    private  void deleteDataset(Properties prop, String datasetURI, boolean deleteVersionContents, boolean deleteChangeOntologies){
        try {
            dmgr = new DatasetsManager(propertiesFile, this.datasetURI);
            dmgr.deleteDataset(deleteVersionContents, deleteChangeOntologies);
            dmgr.terminate();
        } catch (Exception ex) {           
             dmgr.terminate();
             System.out.println("deleteDataset Exception:"+ex.getMessage());
        }
        
       
    }
    
    /**
     * Closes the connection initiated by the constructor of DatasetsManager.
     * It is important to close connection after insertion to database.
     */
    public void closeConnection() {
        if (dmgr != null) {
            dmgr.terminate();
        }
    }
    /**
     * Main test case of full uploading a dataset its schema and two assigned versions.
     * @param args 
     */
    private static void main(String[] args) {
        String propertiesFilepath = "C:/Users/hrysakis/WORK/DIACHRON/DEV/D2VBeta/web/config/config_generic.properties";
          try {
        
            Properties  propertiesFile = new Properties();
            InputStream inputStream = new FileInputStream(propertiesFilepath);
            propertiesFile.load(inputStream);
            String dataset ="http://DataMarkNS"; //without username (guest,user1 etc)
            VirtuosoUploader virtuosoUploader = new VirtuosoUploader(propertiesFile,datasetURI, "guest");
            virtuosoUploader.deleteDataset(propertiesFile,datasetURI,true,true);
            /*
            virtuosoUploader.insertDataset("Diachron Dataset");
            virtuosoUploader.insertSchema("C:/datasets/schema.nt");
            virtuosoUploader.insertDatasetVersion("C:/datasets/v1.rdf", "1", "ds ver. 1");
            virtuosoUploader.insertDatasetVersion("C:/datasets/v2.rdf", "2", "ds ver. 2");
            */
            //virtuosoUploader.closeConnection();
        } catch (Exception ex) {
            System.out.println("Exception in VirtuosoUploader:" +ex.getMessage());
        }
    }
}
