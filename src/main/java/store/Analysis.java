/*
 *@author: Yannis Roussakis, Ioannis Chrysakis
 */
package store;

import java.sql.ResultSet;
import java.util.LinkedHashMap;
import java.util.Properties;
import org.diachron.detection.repositories.JDBCVirtuosoRep;
import org.diachron.detection.utils.ChangesManager;

/**
 * This class used for statistical analysis used in core visualizations
 * 
 */
public class Analysis {
    
    /** Provides statistical analysis by returning a map that contains the change name as key and the change occurrences as value
     * @param jdbc the JDBCVirtuosoRep objects that creates internally the connection to the store
     * @param changesOntolSchema the changes ontology schema
     * @param datasetUri the selected dataset URI
     * @param v1 the selected old version
     * @param v2 the selected new version
     * @param changeType the type of changes to analyze ("Simple_Change","Complex_Change" or null for both cases)
     * @param tempOntology determines if results are located in a temp ontology (custom compare case)
     * @return a map that contains the change name as key and the change occurrences as value
     * @throws Exception general exception
     */
    public static LinkedHashMap analyzeChanges(JDBCVirtuosoRep jdbc, String changesOntolSchema, String datasetUri, String v1, String v2, String changeType, boolean tempOntology) throws Exception {
        ChangesManager cManager = new ChangesManager(jdbc, datasetUri, v1, v2, tempOntology);
        String changesOntology = cManager.getChangesOntology();
        LinkedHashMap<String, Long> changeAnalysis = new LinkedHashMap<>();
        StringBuilder query = new StringBuilder();
        String type;
        if (changeType == null) {
            type = "?ct";
        } else {
            type = "co:" + changeType;
        }

        query.append(""
                + "select ?name count(*) as ?count where {\n"
                + "graph  <" + changesOntolSchema + ">  { \n"
                + "?sc rdfs:subClassOf " + type + "; \n"
                + "co:name ?name.\n"
                + "}\n");
        query.append("graph <" + changesOntology + ">  { \n"
                + "?dsc a ?sc.\n");
        query.append("filter not exists {?dcc co:consumes ?dsc}.\n}\n");

//        query.append("filter (?name = 'ADD_TYPE_CLASS').");
        query.append("}");
        ResultSet res = jdbc.executeSparqlQuery(query.toString(), false);
        while (res.next()) {
            String name = res.getString("name");
            long number = Long.parseLong(res.getString("count"));
            changeAnalysis.put(name, number);
        }
        return changeAnalysis;
    }
   
    /** Provides statistical analysis by returning a map that contains the change name as key and the change occurrences as value
     * @param prop the properties file that includes connection's credentials to the store
     * @param changesOntolSchema the changes ontology schema
     * @param datasetUri the selected dataset URI
     * @param v1 the selected old version
     * @param v2 the selected new version
     * @param changeType the type of changes to analyze ("Simple_Change","Complex_Change" or null for both cases)
     * @param tempOntology determines if results are located in a temp ontology (custom compare case)
     * @return a map that contains the change name as key and the change occurrences as value
     * @throws Exception general exception
     */
    public static LinkedHashMap analyzeChanges(Properties prop, String changesOntolSchema, String datasetUri, String v1, String v2, String changeType, boolean tempOntology) throws Exception {
        JDBCVirtuosoRep jdbc = new JDBCVirtuosoRep(prop);
        ChangesManager cManager = new ChangesManager(jdbc, datasetUri, v1, v2, tempOntology);
        String changesOntology = cManager.getChangesOntology();
        LinkedHashMap<String, Long> changeAnalysis = new LinkedHashMap<>();
        StringBuilder query = new StringBuilder();
        String type;
        if (changeType == null) {
            type = "?ct";
        } else {
            type = "co:" + changeType;
        }
        query.append(""
                + "select ?name count(*) as ?count where {\n"
                + "graph  <" + changesOntolSchema + ">  { \n"
                + "?sc rdfs:subClassOf " + type + "; \n"
                + "co:name ?name.\n"
                + "} \n");
        query.append("graph <" + changesOntology + ">  { \n"
                + "?dsc a ?sc.\n");
        query.append("filter not exists {?dcc co:consumes ?dsc}.\n}\n");
//        query.append("filter (?name = 'ADD_TYPE_CLASS').");
        query.append("} order by ?name");
        ResultSet res = jdbc.executeSparqlQuery(query.toString(), false);
        while (res.next()) {
            String name = res.getString("name");
            long number = Long.parseLong(res.getString("count"));
            changeAnalysis.put(name, number);
        }
        jdbc.terminate();
        return changeAnalysis;
    }

}
