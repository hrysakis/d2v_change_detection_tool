 /*
 *@author: Yannis Roussakis
 */
package store;

import java.io.IOException;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.TreeSet;
import org.diachron.detection.complex_change.CCManager;
import org.diachron.detection.complex_change.SCDefinition;
import org.diachron.detection.exploit.DetChange;
import org.diachron.detection.exploit.Parameter;
import org.diachron.detection.repositories.JDBCVirtuosoRep;

/**
 * This class contains various methods that retrieve information from Virtuoso
 * RDF store including changes definitions, changes templates, changes ontology
 * information etc.
 */
public class QueryUtils {

    private final JDBCVirtuosoRep rep;
    private List<String> changesOntologies;
    private String changesOntologySchema;
    private String datasetUri;
    private final String datasetsGraph = "http://datasets";

    /**
     * Constructor of FetchUris
     *
     * @param propFile the properties file containing Virtuoso credentials etc.
     * information
     * @param datasetUri the selected dataset URI
     * @param changesOntologySchema the selected changes ontology schema
     * @throws ClassNotFoundException in case of internal problem with the store
     * @throws SQLException in case of connection problem with the store
     * @throws IOException in case of no any properties file found
     */
    public QueryUtils(Properties propFile, String datasetUri, String changesOntologySchema) throws ClassNotFoundException, SQLException, IOException {
        rep = new JDBCVirtuosoRep(propFile);
        this.datasetUri = datasetUri;
        this.changesOntologies = new ArrayList<>();
        this.changesOntologySchema = changesOntologySchema;
        if (datasetUri.endsWith("/")) {
            datasetUri = datasetUri.substring(0, datasetUri.length() - 1);
        }
        String query = "select ?ontol from <" + datasetsGraph + "> where {\n"
                + "<" + datasetUri + "/changes> rdfs:member ?ontol.\n"
                + "?ontol co:old_version ?v1.\n"
                + "filter(!regex(?ontol,'/temp')).\n"
                + "}  order by ?v1";
        try {
            ResultSet results = rep.executeSparqlQuery(query, false);
            if (results.next()) {
                do {
                    changesOntologies.add(results.getString(1));
                } while (results.next());
            }
        } catch (Exception ex) {
            System.out.println("Exception: " + ex.getMessage() + " occured .");
        }
    }

    /**
     * Returns a list of top-added types
     *
     * @param topK an integer that denotes the top-k limit
     * @return a list of top-k added types
     */
    public List<String> fetchTopAddedTypes(int topK) {
        ArrayList<String> types = new ArrayList<>();
        StringBuilder query = new StringBuilder();
        StringBuilder graphs = new StringBuilder();
        graphs.append("from <" + changesOntologySchema + "> \n");
        for (String ontology : changesOntologies) {
            graphs.append("from <" + ontology + "> \n");
        }
        query.append("select ?type count(*) as ?count \n").
                append(graphs).
                append("where {\n").
                append("?sc rdfs:subClassOf co:Simple_Change; \n").
                append("co:name ?name. \n").
                append("filter (?name = 'ADD_TYPE_TO_INDIVIDUAL').\n").
                append("?dsc a ?sc;\n").
                append("co:atti_p2 ?type.\n").
                append("} order by desc(?count) limit " + topK);
        ResultSet results = rep.executeSparqlQuery(query.toString(), true);
        try {
            if (!results.next()) {
                return types;
            }
            do {
                types.add(results.getString(1));
            } while (results.next());
        } catch (SQLException ex) {
            System.out.println("Exception: " + ex.getMessage());
        }
        return types;
    }

    /**
     * Returns a list of top-deleted types
     *
     * @param topK an integer that denotes the top-k limit
     * @return a list of top-k-deleted types
     */
    public List<String> fetchTopDeletedTypes(int topK) {
        ArrayList<String> types = new ArrayList<>();
        StringBuilder query = new StringBuilder();
        StringBuilder graphs = new StringBuilder();
        graphs.append("from <" + changesOntologySchema + "> \n");
        for (String ontology : changesOntologies) {
            graphs.append("from <" + ontology + "> \n");
        }
        query.append("select ?type count(*) as ?count \n").
                append(graphs).
                append("where {\n").
                append("?sc rdfs:subClassOf co:Simple_Change; ").
                append("co:name ?name. ").
                append("filter (?name = 'DELETE_TYPE_FROM_INDIVIDUAL').").
                append("?dsc a ?sc;").
                append("co:dtfi_p2 ?type.").
                append("} order by desc(?count) limit " + topK);
        ResultSet results = rep.executeSparqlQuery(query.toString(), false);
        try {
            if (!results.next()) {
                return types;
            }
            do {
                types.add(results.getString(1));
            } while (results.next());
        } catch (SQLException ex) {
            System.out.println("Exception: " + ex.getMessage());
        }
        return types;
    }

    /**
     * Returns a list of top-added super classes
     *
     * @param topK an integer that denotes the top-k limit
     * @return a list of top-k added super classes
     */
    public List<String> fetchTopAddedSuperclasses(int topK) {
        ArrayList<String> types = new ArrayList<>();
        StringBuilder query = new StringBuilder();
        StringBuilder graphs = new StringBuilder();
        graphs.append("from <" + changesOntologySchema + "> \n");
        for (String ontology : changesOntologies) {
            graphs.append("from <" + ontology + "> \n");
        }
        query.append("select ?super count(*) as ?count \n").
                append(graphs).
                append("where {\n").
                append("?sc rdfs:subClassOf co:Simple_Change; \n").
                append("co:name ?name. \n").
                append("filter (?name = 'ADD_SUPERCLASS').\n").
                append("?dsc a ?sc;\n").
                append("co:asc_p2 ?super.\n").
                append("} order by desc(?count) limit " + topK);
        ResultSet results = rep.executeSparqlQuery(query.toString(), false);
        try {
            if (!results.next()) {
                return types;
            }
            do {
                types.add(results.getString(1));
            } while (results.next());
        } catch (SQLException ex) {
            System.out.println("Exception: " + ex.getMessage());
        }
        return types;
    }

    /**
     * Returns a list of top-deleted super classes
     *
     * @param topK an integer that denotes the top-k limit
     * @return a list of top-k deleted super classes
     */
    public List<String> fetchTopDeletedSuperclasses(int topK) {
        ArrayList<String> types = new ArrayList<>();
        StringBuilder query = new StringBuilder();
        StringBuilder graphs = new StringBuilder();
        graphs.append("from <" + changesOntologySchema + "> \n");
        for (String ontology : changesOntologies) {
            graphs.append("from <" + ontology + "> \n");
        }
        query.append("select ?super count(*) as ?count \n").
                append(graphs).
                append("where {\n").
                append("?sc rdfs:subClassOf co:Simple_Change; \n").
                append("co:name ?name. ").
                append("filter (?name = 'DELETE_SUPERCLASS').\n").
                append("?dsc a ?sc;\n").
                append("co:dsc_p2 ?super.\n").
                append("} order by desc(?count) limit " + topK);
        ResultSet results = rep.executeSparqlQuery(query.toString(), false);
        try {
            if (!results.next()) {
                return types;
            }
            do {
                types.add(results.getString(1));
            } while (results.next());
        } catch (SQLException ex) {
            System.out.println("Exception: " + ex.getMessage());
        }
        return types;
    }

    /**
     * Returns a list of top-updated labels
     *
     * @param topK an integer that denotes the top-k limit
     * @return a list of top-k updated labels
     */
    public List<String> fetchTopUpdatedLabels(int topK) {
        ArrayList<String> types = new ArrayList<>();
        StringBuilder query = new StringBuilder();
        StringBuilder graphs = new StringBuilder();
        graphs.append("from <" + changesOntologySchema + "> \n");
        for (String ontology : changesOntologies) {
            graphs.append("from <" + ontology + "> \n");
        }
        query.append("select ?uri count(*) as ?count \n").
                append(graphs).
                append("where {\n").
                append("?sc1 rdfs:subClassOf co:Simple_Change; \n").
                append("co:name ?name1. \n").
                append("?sc2 rdfs:subClassOf co:Simple_Change; \n").
                append("co:name ?name2. \n").
                append("filter (?name1 = 'ADD_LABEL'). \n").
                append("filter (?name2 = 'DELETE_LABEL'). \n").
                append("?dsc1 a ?sc1; \n").
                append("co:al_p1 ?uri. \n").
                append("?dsc2 a ?sc2; \n").
                append("co:dl_p1 ?uri. \n").
                append("} order by desc(?count) limit " + topK);
        ResultSet results = rep.executeSparqlQuery(query.toString(), false);
        try {
            if (!results.next()) {
                return types;
            }
            do {
                types.add(results.getString(1));
            } while (results.next());
        } catch (SQLException ex) {
            System.out.println("Exception: " + ex.getMessage());
        }
        return types;
    }

    /**
     * Returns a list of top-updated comments
     *
     * @param topK an integer that denotes the top-k limit
     * @return a list of top-k updated comments
     */
    public List<String> fetchTopUpdatedComments(int topK) {
        ArrayList<String> types = new ArrayList<>();
        StringBuilder query = new StringBuilder();
        StringBuilder graphs = new StringBuilder();
        graphs.append("from <" + changesOntologySchema + "> \n");
        for (String ontology : changesOntologies) {
            graphs.append("from <" + ontology + "> \n");
        }
        query.append("select ?uri count(*) as ?count \n").
                append(graphs).
                append("where {\n").
                append("?sc1 rdfs:subClassOf co:Simple_Change; ").
                append("co:name ?name1. ").
                append("?sc2 rdfs:subClassOf co:Simple_Change; ").
                append("co:name ?name2. ").
                append("filter (?name1 = 'ADD_COMMENT').").
                append("filter (?name2 = 'DELETE_COMMENT').").
                append("?dsc1 a ?sc1;").
                append("co:ac_p1 ?uri.").
                append("?dsc2 a ?sc2;").
                append("co:dc_p1 ?uri.").
                append("} order by desc(?count) limit " + topK);
        ResultSet results = rep.executeSparqlQuery(query.toString(), false);
        try {
            if (!results.next()) {
                return types;
            }
            do {
                types.add(results.getString(1));
            } while (results.next());
        } catch (SQLException ex) {
            System.out.println("Exception: " + ex.getMessage());
        }
        return types;
    }

    /**
     * Returns a list of top-updated domains
     *
     * @param topK an integer that denotes the top-k limit
     * @return a list of top-k updated domains
     */
    public List<String> fetchTopUpdatedDomains(int topK) {
        ArrayList<String> types = new ArrayList<>();
        StringBuilder query = new StringBuilder();
        StringBuilder graphs = new StringBuilder();
        graphs.append("from <" + changesOntologySchema + "> \n");
        for (String ontology : changesOntologies) {
            graphs.append("from <" + ontology + "> \n");
        }
        query.append("select ?uri count(*) as ?count \n").
                append(graphs).
                append("where {\n").
                append("?sc1 rdfs:subClassOf co:Simple_Change; ").
                append("co:name ?name1. ").
                append("?sc2 rdfs:subClassOf co:Simple_Change; ").
                append("co:name ?name2. ").
                append("filter (?name1 = 'ADD_DOMAIN').").
                append("filter (?name2 = 'DELETE_DOMAIN').").
                append("?dsc1 a ?sc1;").
                append("co:ad_p1 ?uri.").
                append("?dsc2 a ?sc2;").
                append("co:dd_p1 ?uri.").
                append("} order by desc(?count) limit " + topK);
        ResultSet results = rep.executeSparqlQuery(query.toString(), false);
        try {
            if (!results.next()) {
                return types;
            }
            do {
                types.add(results.getString(1));
            } while (results.next());
        } catch (SQLException ex) {
            System.out.println("Exception: " + ex.getMessage());
        }
        return types;
    }

    /**
     * Returns a list of top-updated ranges
     *
     * @param topK an integer that denotes the top-k limit
     * @return a list of top-k updated ranges
     */
    public List<String> fetchTopUpdatedRanges(int topK) {
        ArrayList<String> types = new ArrayList<>();
        StringBuilder query = new StringBuilder();
        StringBuilder graphs = new StringBuilder();
        graphs.append("from <" + changesOntologySchema + "> \n");
        for (String ontology : changesOntologies) {
            graphs.append("from <" + ontology + "> \n");
        }
        query.append("select ?uri count(*) as ?count \n").
                append(graphs).
                append("where {\n").
                append("?sc1 rdfs:subClassOf co:Simple_Change; ").
                append("co:name ?name1. ").
                append("?sc2 rdfs:subClassOf co:Simple_Change; ").
                append("co:name ?name2. ").
                append("filter (?name1 = 'ADD_RANGE').").
                append("filter (?name2 = 'DELETE_RANGE').").
                append("?dsc1 a ?sc1;").
                append("co:ar_p1 ?uri.").
                append("?dsc2 a ?sc2;").
                append("co:dr_p1 ?uri.").
                append("} order by desc(?count) limit " + topK);
        ResultSet results = rep.executeSparqlQuery(query.toString(), false);
        try {
            if (!results.next()) {
                return types;
            }
            do {
                types.add(results.getString(1));
            } while (results.next());
        } catch (SQLException ex) {
            System.out.println("Exception: " + ex.getMessage());
        }
        return types;
    }

    /**
     * Returns a list of top-updated properties
     *
     * @param topK an integer that denotes the top-k limit
     * @return a list of top-k updated properties
     */
    public List<String> fetchTopUpdatedProperties(int topK) {
        ArrayList<String> types = new ArrayList<>();
        StringBuilder query = new StringBuilder();
        StringBuilder graphs = new StringBuilder();
        graphs.append("from <" + changesOntologySchema + "> \n");
        for (String ontology : changesOntologies) {
            graphs.append("from <" + ontology + "> \n");
        }
        query.append("select ?uri count(*) as ?count \n").
                append(graphs).
                append("where {\n").
                append("?sc1 rdfs:subClassOf co:Simple_Change; ").
                append("co:name ?name1. ").
                append("?sc2 rdfs:subClassOf co:Simple_Change; ").
                append("co:name ?name2. ").
                append("?sc3 rdfs:subClassOf co:Simple_Change; ").
                append("co:name ?name3. ").
                append("?sc4 rdfs:subClassOf co:Simple_Change; ").
                append("co:name ?name4. ").
                append("filter (?name1 = 'ADD_RANGE').").
                append("filter (?name2 = 'DELETE_RANGE').").
                append("filter (?name3 = 'ADD_DOMAIN').").
                append("filter (?name4 = 'DELETE_DOMAIN').").
                append("?dsc1 a ?sc1;").
                append("co:ar_p1 ?uri.").
                append("?dsc2 a ?sc2;").
                append("co:dr_p1 ?uri.").
                append("?dsc3 a ?sc3;").
                append("co:ad_p1 ?uri.").
                append("?dsc4 a ?sc4;").
                append("co:dd_p1 ?uri.").
                append("} order by desc(?count) limit " + topK);
        ResultSet results = rep.executeSparqlQuery(query.toString(), false);
        try {
            if (!results.next()) {
                return types;
            }
            do {
                types.add(results.getString(1));
            } while (results.next());
        } catch (SQLException ex) {
            System.out.println("Exception: " + ex.getMessage());
        }
        return types;
    }

    /**
     * Returns a list of top-added subclasses
     *
     * @param topK an integer that denotes the top-k limit
     * @return a list of top-k added subclasses
     */
    public List<String> fetchTopAddedSubclasses(int topK) {
        ArrayList<String> types = new ArrayList<>();
        StringBuilder query = new StringBuilder();
        StringBuilder graphs = new StringBuilder();
        graphs.append("from <" + changesOntologySchema + "> \n");
        for (String ontology : changesOntologies) {
            graphs.append("from <" + ontology + "> \n");
        }
        query.append("select ?sub count(*) as ?count \n").
                append(graphs).
                append("where {\n").
                append("?sc rdfs:subClassOf co:Simple_Change; ").
                append("co:name ?name. ").
                append("filter (?name = 'ADD_SUPERCLASS').").
                append("?dsc a ?sc;").
                append("co:asc_p1 ?sub.").
                append("} order by desc(?count) limit " + topK);

        ResultSet results = rep.executeSparqlQuery(query.toString(), false);
        try {
            if (!results.next()) {
                return types;
            }
            do {
                types.add(results.getString(1));
            } while (results.next());
        } catch (SQLException ex) {
            System.out.println("Exception: " + ex.getMessage());
        }
        return types;
    }

    /**
     * Returns a list of top-deleted subclasses
     *
     * @param topK an integer that denotes the top-k limit
     * @return a list of top-k deleted subclasses
     */
    public List<String> fetchTopDeletedSubclasses(int topK) {
        ArrayList<String> types = new ArrayList<>();
        StringBuilder query = new StringBuilder();
        StringBuilder graphs = new StringBuilder();
        graphs.append("from <" + changesOntologySchema + "> \n");
        for (String ontology : changesOntologies) {
            graphs.append("from <" + ontology + "> \n");
        }
        query.append("select ?sub count(*) as ?count \n").
                append(graphs).
                append("where {\n").
                append("?sc rdfs:subClassOf co:Simple_Change; ").
                append("co:name ?name. ").
                append("filter (?name = 'DELETE_SUPERCLASS').").
                append("?dsc a ?sc;").
                append("co:dsc_p1 ?sub.").
                append("} order by desc(?count) limit " + topK);
        ResultSet results = rep.executeSparqlQuery(query.toString(), false);
        try {
            if (!results.next()) {
                return types;
            }
            do {
                types.add(results.getString(1));
            } while (results.next());
        } catch (SQLException ex) {
            System.out.println("Exception: " + ex.getMessage());
        }
        return types;
    }

    /**
     * Returns a list of top-added properties
     *
     * @param topK an integer that denotes the top-k limit
     * @return a list of top-k added properties
     */
    public List<String> fetchTopAddedProperties(int topK) {
        ArrayList<String> types = new ArrayList<>();
        StringBuilder query = new StringBuilder();
        StringBuilder graphs = new StringBuilder();
        graphs.append("from <" + changesOntologySchema + "> \n");
        for (String ontology : changesOntologies) {
            graphs.append("from <" + ontology + "> \n");
        }
        query.append("select ?prop count(*) as ?count \n").
                append(graphs).
                append("where {\n").
                append("?sc rdfs:subClassOf co:Simple_Change; ").
                append("co:name ?name. ").
                append("filter (?name = 'ADD_PROPERTY_INSTANCE').").
                append("?dsc a ?sc;").
                append("co:api_p2 ?prop.").
                append("} order by desc(?count) limit " + topK);
        ResultSet results = rep.executeSparqlQuery(query.toString(), true);
        try {
            if (!results.next()) {
                return types;
            }
            do {
                types.add(results.getString(1));
            } while (results.next());
        } catch (SQLException ex) {
            System.out.println("Exception: " + ex.getMessage());
        }
        return types;
    }

    /**
     * Returns a list of top-deleted properties
     *
     * @param topK an integer that denotes the top-k limit
     * @return a list of top-k deleted properties
     */
    public List<String> fetchTopDeletedProperties(int topK) {
        ArrayList<String> types = new ArrayList<>();
        StringBuilder query = new StringBuilder();
        StringBuilder graphs = new StringBuilder();
        graphs.append("from <" + changesOntologySchema + "> \n");
        for (String ontology : changesOntologies) {
            graphs.append("from <" + ontology + "> \n");
        }
        query.append("select ?prop count(*) as ?count \n").
                append(graphs).
                append("where {\n").
                append("?sc rdfs:subClassOf co:Simple_Change; ").
                append("co:name ?name. ").
                append("filter (?name = 'DELETE_PROPERTY_INSTANCE').").
                append("?dsc a ?sc;").
                append("co:dpi_p2 ?prop.").
                append("} order by desc(?count) limit " + topK);
        ResultSet results = rep.executeSparqlQuery(query.toString(), false);
        try {
            if (!results.next()) {
                return types;
            }
            do {
                types.add(results.getString(1));
            } while (results.next());
        } catch (SQLException ex) {
            System.out.println("Exception: " + ex.getMessage());
        }
        return types;
    }

    /**
     * Returns a list of URIs that correspond to the selected change template
     *
     * @param change_template the selected change template
     * @param limitResults the limit (top-k) for the results
     * @return a list of URIs that correspond to the selected change template
     */
    public List<String> fetchURIsByChangeType(String change_template, int limitResults) {
        List<String> uris = null;

        if (change_template != null) {
            //additions
            switch (change_template) {
                case "Add Generalization Of":
                    uris = fetchTopAddedSubclasses(limitResults);
                    break;
                case "Add Instance Of":
                    uris = fetchTopAddedProperties(limitResults);
                    break;
                case "Add Object Of Type":
                    uris = fetchTopAddedTypes(limitResults);
                    break;
                case "Add Specialization Of":
                    uris = fetchTopAddedSuperclasses(limitResults);
                    break;
                //deletions        
                case "Delete Generalization From":
                    uris = fetchTopDeletedSubclasses(limitResults);
                    break;
                case "Delete Instance Of":
                    uris = fetchTopDeletedProperties(limitResults);
                    break;
                case "Delete Object Of Type":
                    uris = fetchTopDeletedTypes(limitResults);
                    break;
                case "Delete Specialization From":
                    uris = fetchTopDeletedSuperclasses(limitResults);
                    break;
                //updates
                case "Update Label":
                    uris = fetchTopUpdatedLabels(limitResults);
                    break;
                case "Update Comment":
                    uris = fetchTopUpdatedComments(limitResults);
                    break;
                case "Update Domain":
                    uris = fetchTopUpdatedDomains(limitResults);
                    break;
                case "Update Range":
                    uris = fetchTopUpdatedRanges(limitResults);
                    break;
                case "Update Property":
                    uris = fetchTopUpdatedProperties(limitResults);
                    break;
            }
        }
        return uris;
    }

    /**
     * Returns a list of change names according to selected change type
     *
     * @param changeType the selected change type
     * @return a list of change names according to selected change type
     */
    public List<String> fetchChangesNames(String changeType) {
        List<String> names = new ArrayList<>();
        String query;
        if (changeType != null) {
            query = "select ?cc_name from <" + changesOntologySchema + "> where { "
                    + "?cc rdfs:subClassOf co:" + changeType + "; "
                    + "co:name ?cc_name. "
                    + "} order by ?cc_name ";
        } else {
            query = "select ?cc_name from <" + changesOntologySchema + "> where { "
                    + "?cc rdfs:subClassOf ?sc; "
                    + "co:name ?cc_name. "
                    + "} order by ?cc_name";
        }
        ResultSet results = rep.executeSparqlQuery(query, false);
        try {
            if (!results.next()) {
                return names;
            }
            do {
                names.add(results.getString(1));
            } while (results.next());
        } catch (SQLException ex) {
            System.out.println("Exception: " + ex.getMessage());
        }
        return names;
    }

    /**
     * Returns a list of parameter names of a selected change
     *
     * @param scName the name of the selected change
     * @return a list of parameter names of a selected change
     */
    public List<String> fetchSCParameterNames(String scName) {
        List<String> names = new ArrayList<>();
        String query = "select ?param_name from <" + changesOntologySchema + "> where { \n"
                + "?sc rdfs:subClassOf co:Simple_Change; \n"
                + "co:name '" + scName + "';\n"
                + "?p ?param. \n"
                + "?p co:name ?param_name.\n"
                + "}";
        ResultSet results = rep.executeSparqlQuery(query, false);
        try {
            if (!results.next()) {
                return names;
            }
            do {
                names.add(results.getString(1));
            } while (results.next());
        } catch (SQLException ex) {
            System.out.println("Exception: " + ex.getMessage());
        }
        return names;
    }

    /**
     * Returns a list of assigned simple changes parameters
     *
     * @param manager this object determines the assigned simple changes
     * @return a list of assigned simple changes parameters
     */
    public List<String> fetchAssignedSCParams(CCManager manager) {
        List<String> params = new ArrayList<>();
        for (SCDefinition scDef : manager.getSChanges()) {
            String sc = scDef.getsChangeType().toString();
            String uri = scDef.getsChangeUri();
            for (String param : fetchSCParameterNames(sc)) {
                params.add(uri + ":-" + param);
            }
        }
        return params;
    }

    /**
     * Terminates connection to the Virtuoso RDF store.
     */
    public void terminate() {
        this.rep.terminate();
    }

    /**
     * Returns a map of all assigned dataset versions (URIs) for a selected
     * changes ontology
     *
     * @param changesOntology the selected changes ontology
     * @return a map of all assigned dataset versions for a selected changes
     * ontology
     */
    public Map<String, String> fetchChangeOntologyVersions(String changesOntology) {
        Map<String, String> result = new HashMap<>();
        String query = "select ?v1 ?v2 from <" + datasetsGraph + "> where {\n"
                + "<" + changesOntology + "> co:old_version ?v1.\n"
                + "<" + changesOntology + "> co:new_version ?v2.\n"
                + "}";
        ResultSet results = rep.executeSparqlQuery(query, false);
        try {
            if (!results.next()) {
                return null;
            }
            do {
                result.put(results.getString(1), results.getString(2));
                break;
            } while (results.next());
        } catch (SQLException ex) {
            System.out.println("Exception: " + ex.getMessage());
        }
        return result;
    }

    /**
     * Returns a map of all assigned dataset labels for a selected changes
     * ontology
     *
     * @param changesOntology the selected changes ontology
     * @return a map of all assigned dataset labels for a selected changes
     * ontology
     */
    public Map<String, String> fetchVersionLabels(String changesOntology) {
        Map<String, String> result = new HashMap<>();
        String query = "select ?v1 ?v2 from <" + datasetsGraph + "> where {\n"
                + "<" + changesOntology + "> co:old_version [rdfs:label ?v1].\n"
                + "<" + changesOntology + "> co:new_version [rdfs:label ?v2].\n"
                + "}";
        ResultSet results = rep.executeSparqlQuery(query, false);
        try {
            if (!results.next()) {
                return null;
            }
            do {
                result.put(results.getString(1), results.getString(2));
                break;
            } while (results.next());
        } catch (SQLException ex) {
            System.out.println("Exception: " + ex.getMessage());
        }
        return result;
    }

    /**
     * Returns the changes ontology based on the old and new version
     *
     * @param oldVersion the selected old version
     * @param newVersion the selected new version
     * @return the changes ontology based on the old and new version
     */
    public String fetchChangesOntology(String oldVersion, String newVersion) {
        String changesUri;
        if (datasetUri.endsWith("/")) {
            changesUri = datasetUri + "changes";
        } else {
            changesUri = datasetUri + "/changes";
        }
        StringBuilder query = new StringBuilder();
        query.append("select ?ontology from <" + datasetsGraph + "> where {\n");
        if (oldVersion != null) {
            query.append("?ontology co:old_version <" + oldVersion + ">.");
        }
        query.append("?ontology co:new_version <" + newVersion + ">.\n"
                + "<" + changesUri + "> rdfs:member ?ontology.\n"
                + "filter (!regex (?ontology, '/temp')).\n"
                + "}");
        ResultSet results = rep.executeSparqlQuery(query.toString(), false);
        try {
            if (!results.next()) {
                return null;
            }
            do {
                return results.getString(1);
            } while (results.next());
        } catch (SQLException ex) {
            System.out.println("Exception: " + ex.getMessage());
        }
        return null;
    }

    /**
     * Returns the JDBCRepository connection object
     *
     * @return the JDBCRepository connection object
     */
    public JDBCVirtuosoRep getJDBCRepository() {
        return rep;
    }

    /**
     * Returns the JSON as a string which contains the selected complex change
     * definition
     *
     * @param ccName the name of the selected complex change
     * @return the JSON as a string which contains the selected complex change
     * definition
     */
    public String fetchCCJson(String ccName) {
        String query = "select ?json from <" + changesOntologySchema + "> where {\n"
                + "?cc co:name '" + ccName + "'.\n"
                + "?cc co:json ?json.\n"
                + "}";
        ResultSet results = rep.executeSparqlQuery(query, false);
        try {
            if (!results.next()) {
                return null;
            } else {
                return results.getString(1);
            }
        } catch (SQLException ex) {
            System.out.println("Exception: " + ex.getMessage());
        }
        return null;
    }

    private Map<String, Long> fetchVersionsForValue(String value) {
        LinkedHashMap<String, Long> result = new LinkedHashMap<>();
        String query;
        long cnt = 0;
        for (String ontology : changesOntologies) {
            query = "select count(*) from <" + ontology + "> where { \n"
                    + "?dc ?param ?vv. \n"
                    + "filter(str(?vv) = '" + value + "'). \n"
                    + "FILTER NOT EXISTS {?consumedBy co:consumes ?dc }.\n"
                    + "}";
            try {
                ResultSet results = rep.executeSparqlQuery(query, false);
                if (results.next()) {
                    do {
                        cnt = results.getLong(1);
                    } while (results.next());
                }
            } catch (Exception ex) {
                System.out.println("Exception: " + ex.getMessage() + " occured .");
            }
            result.put(ontology, cnt);
        }
        return result;
    }

    private Map<String, Long> fetchVersionsForChange(String changeName) {
        LinkedHashMap<String, Long> result = new LinkedHashMap<>();
        String query;
        long cnt = 0;
        for (String ontology : changesOntologies) {
            query = "select  count(*) where { \n"
                    + "graph <" + changesOntologySchema + "> { \n"
                    + "?ch co:name '" + changeName + "'. \n"
                    + "} \n"
                    + "graph <" + ontology + "> { \n"
                    + "?dc a ?ch.\n"
                    + "FILTER NOT EXISTS {?consumedBy co:consumes ?dc }\n"
                    + "} \n"
                    + "}";
            try {
                ResultSet results = rep.executeSparqlQuery(query, false);
                if (results.next()) {
                    do {
                        cnt = results.getLong(1);
                    } while (results.next());
                }
            } catch (Exception ex) {
                System.out.println("Exception: " + ex.getMessage() + " occured .");
            }
            result.put(ontology, cnt);
        }
        return result;
    }

    private Map<String, Long> fetchValuesForNewVersion(String newVersion) {
        String changesOntology = fetchChangesOntology(null, newVersion);
        LinkedHashMap<String, Long> result = new LinkedHashMap<>();
        String query = "select ?value count(*) where {\n"
                + "graph <" + changesOntologySchema + "> { \n"
                + "?ch rdfs:subClassOf ?change. \n"
                + "filter (?change = co:Simple_Change || ?change = co:Complex_Change).\n"
                + "}\n"
                + "graph <" + changesOntology + "> { \n"
                + "?dc a ?ch.\n"
                + "?dc ?param ?value. \n"
                + "filter (?param != rdf:type).\n"
                + "FILTER NOT EXISTS {?consumedBy co:consumes ?dc }.\n"
                + "}\n"
                + "}";
        try {
            ResultSet results = rep.executeSparqlQuery(query, false);
            if (results.next()) {
                do {
                    result.put(results.getString(1), results.getLong(2));
                } while (results.next());
            }
        } catch (Exception ex) {
            System.out.println("Exception: " + ex.getMessage() + " occured .");
        }
        return result;
    }

    private Map<String, Long> fetchValuesForChange(String changeName) {
        LinkedHashMap<String, Long> result = new LinkedHashMap<>();
        String value;
        long cnt = 0;
        for (String ontology : changesOntologies) {
            String query = "select ?value count(*) where {\n"
                    + "graph <" + changesOntologySchema + "> { \n"
                    + "?ch co:name '" + changeName + "'. \n"
                    + "}\n"
                    + "graph <" + ontology + "> { \n"
                    + "?dc a ?ch.\n"
                    + "?dc ?param ?value. \n"
                    + "filter (?param != rdf:type).\n"
                    + "FILTER NOT EXISTS {?consumedBy co:consumes ?dc }.\n"
                    + "}\n"
                    + "}";
            try {
                ResultSet results = rep.executeSparqlQuery(query, false);
                if (results.next()) {
                    do {
                        value = results.getString(1);
                        cnt = results.getLong(2);
                        if (result.get(value) != null) {
                            cnt += result.get(value);
                        }
                        result.put(value, cnt);
                    } while (results.next());
                }
            } catch (Exception ex) {
                System.out.println("Exception: " + ex.getMessage() + " occured .");
            }
        }
        return result;
    }

    /**
     * Used in VERSION-centric view to return a map which contains as key the
     * change ontology that contains the respective version pair and as value
     * the total number of selected changes.
     *
     * @param versions a set of selected versions (URIs as string or "ALL" for
     * all versions)
     * @param changes a set of selected changes (change names, "Simple_Change"
     * for only simple changes, "Complex_Change" for only complex changes, "ALL"
     * for all changes)
     * @return a map containing information about each version pair and the
     * total number of changes
     */
    public Map<String, Long> fetchVersionsForChanges(Set<String> versions, Set<String> changes) {
        LinkedHashMap<String, Long> result = new LinkedHashMap<>();
        List<String> ontologiesList = createChOntologiesSublist(versions);
        List<String> changesList = createChangesSublist(changes);
        long cnt = 0;
        for (String ontology : ontologiesList) {
            StringBuilder query = new StringBuilder();
            query.append("select  count(*) where { \n")
                    .append("graph <" + changesOntologySchema + "> { \n")
                    .append("?ch co:name ?change_name. \n");
            if (!changesList.isEmpty()) {
                StringBuilder changesString = new StringBuilder();
                int cnt2 = 0;
                for (String child : changesList) {
                    changesString.append("'").append(child).append("'");
                    if (cnt2 < changesList.size() - 1) {
                        changesString.append(", ");
                    }
                    cnt2++;
                }
                query.append("filter (?change_name in ( " + changesString.toString() + " )).\n");
            } else {
                return result;
            }
            query.append("} \n"
                    + "graph <" + ontology + "> { \n"
                    + "?dc a ?ch.\n"
                    + "FILTER NOT EXISTS {?consumedBy co:consumes ?dc }.\n"
                    + "} \n"
                    + "}");
            try {
                ResultSet results = rep.executeSparqlQuery(query.toString(), false);
                if (results.next()) {
                    do {
                        cnt = results.getLong(1);
                    } while (results.next());
                }
            } catch (Exception ex) {
                System.out.println("Exception: " + ex.getMessage() + " occured .");
            }
            result.put(ontology, cnt);
        }
        return result;
    }

    /**
     * Used in CHANGE-centric view to return a map which contains as key a
     * change name and as value the respective change occurrences appeared in
     * the selected versions.
     *
     * @param versions a set of selected versions (URIs as string or "ALL" for
     * all versions)
     * @param changes a set of changes (change names, "Simple_Change" for only
     * simple changes, "Complex_Change" for only complex changes, "ALL" for all
     * changes)
     * @param tempOntology denotes whether we give a set of version URIs,
     * (tempOntology=false) or the temp changes ontology URI (tempOntology=true)
     * @return a map containing information about each change name and the
     * number of respective occurrences
     */
    public Map<String, Long> fetchChangesForVersions(Set<String> versions, Set<String> changes, boolean tempOntology) {
        LinkedHashMap<String, Long> result = new LinkedHashMap<>();
        List<String> ontologiesList;
        if (!tempOntology) {
            ontologiesList = createChOntologiesSublist(versions);
        } else {
            ontologiesList = new ArrayList<>();
            ontologiesList.addAll(versions);
        }
        List<String> changesList = createChangesSublist(changes);
        //
        StringBuilder query = new StringBuilder();
        query.append("select ?change_name, count(*) where { \n")
                .append("graph <" + changesOntologySchema + "> { \n")
                .append("?ch co:name ?change_name. \n");
        if (!changesList.isEmpty()) {
            StringBuilder changesString = new StringBuilder();
            int cnt2 = 0;
            for (String child : changesList) {
                changesString.append("'").append(child).append("'");
                if (cnt2 < changesList.size() - 1) {
                    changesString.append(", ");
                }
                cnt2++;
            }
            query.append("filter (?change_name in ( " + changesString.toString() + " )).\n");
        } else {
            return result;
        }
        query.append("} \n"
                + "graph ?ontol { \n"
                + "?dc a ?ch.\n"
                + "FILTER NOT EXISTS {?consumedBy co:consumes ?dc }.\n"
                + "}\n");
        if (!ontologiesList.isEmpty()) {
            StringBuilder ontolString = new StringBuilder();
            int cnt2 = 0;
            for (String ontology : ontologiesList) {
                ontolString.append("<").append(ontology).append(">");
                if (cnt2 < ontologiesList.size() - 1) {
                    ontolString.append(", ");
                }
                cnt2++;
            }
            query.append("filter (?ontol in ( " + ontolString.toString() + " )).\n");
        }
        query.append("}");
        try {
            ResultSet results = rep.executeSparqlQuery(query.toString(), false);
            if (results.next()) {
                do {
                    result.put(results.getString(1), results.getLong(2));
                } while (results.next());
            }
        } catch (Exception ex) {
            System.out.println("Exception: " + ex.getMessage() + " occured .");
        }
        return result;
    }

    /**
     * Used in TERM-centric view to return a map which contains as key a change
     * name and as value the respective change occurrences appeared in the
     * selected versions.
     *
     * @param value the value that denotes the selected term
     * @param versions a set of selected versions (URIs as string or "ALL" for
     * all versions)
     * @param changes a set of changes (change names, "Simple_Change" for only
     * simple changes, "Complex_Change" for only complex changes, "ALL" for all
     * changes)
     * @param tempOntology denotes whether we give a set of version URIs,
     * (tempOntology=false) or the temp changes ontology URI (tempOntology=true)
     * @return a map containing information about each version pair and the
     * total number of changes where the selected term appeared.
     */
    public Set<DetChange> fetchDetChangesContainValue(String value, Set<String> versions, Set<String> changes, boolean tempOntology) {
        Set<DetChange> detChanges = new TreeSet<>();
        List<String> ontologiesList;
        if (!tempOntology) {
            ontologiesList = createChOntologiesSublist(versions);
        } else {
            ontologiesList = new ArrayList<>();
            ontologiesList.addAll(versions);
        }
        List<String> changesList = createChangesSublist(changes);
        for (String changesOntology : ontologiesList) {
            StringBuilder query = new StringBuilder();
            query.append("select ?dc ?change_name ?param_name ?param_value ?param ?description where { \n").
                    append("graph <" + changesOntologySchema + "> { \n").
                    append("?ch co:name ?change_name. \n").
                    append("optional {?ch co:description ?description.}. \n");
            if (!changesList.isEmpty()) {
                StringBuilder changesString = new StringBuilder();
                int cnt2 = 0;
                for (String child : changesList) {
                    changesString.append("'").append(child).append("'");
                    if (cnt2 < changesList.size() - 1) {
                        changesString.append(", ");
                    }
                    cnt2++;
                }
                query.append("filter (?change_name in ( " + changesString.toString() + " )).\n");
            }
            query.append("?param co:name ?param_name. \n").
                    append("} \n").
                    append("graph <" + changesOntology + "> { \n").
                    append("?dc a ?ch; \n").
                    append("?param ?param_value. \n").
                    append("FILTER NOT EXISTS {?consumedBy co:consumes ?dc }.\n").
                    append("{ ").
                    append("select ?dc from <" + changesOntology + "> where { ?dc ?param ?vv. filter(str(?vv) = '" + value + "'). }\n").
                    append("} \n").
                    append("} \n").
                    append("} order by ?dc \n");
            Map<String, String> ontolVersions = fetchChangeOntologyVersions(changesOntology);
            String oldVersion = ontolVersions.keySet().iterator().next();
            String newVersion = ontolVersions.get(oldVersion);
            ResultSet results = rep.executeSparqlQuery(query.toString(), false);
            try {
                if (!results.next()) {
                    continue;
                }
                String dch = "";
                DetChange change = null;
                do {
                    if (!dch.equals(results.getString(1))) { //we are in a new change
                        if (change != null) { //check the previous change if it has the given URI as parameter
                            detChanges.add(change);
                        }
                        dch = results.getString(1);
                        String chName = results.getString(2);
                        String chDescr = results.getString(6);
                        change = new DetChange(dch, chName, chDescr, oldVersion, newVersion);
                    }
                    String parName = results.getString(3);
                    String parValue = results.getString(4);
                    String paramUri = results.getString(5);
                    Parameter param = new Parameter(paramUri, parName, parValue);
                    change.addParameter(param);
                } while (results.next());
                detChanges.add(change);
            } catch (SQLException ex) {
                System.out.println("Exception: " + ex.getMessage());
            }
        }
        return detChanges;
    }

    /**
     * Returns a set of DetChange objects which denotes detected changes with
     * their respective information
     *
     * @param versions a set of selected versions (URIs as string or "ALL" for
     * all versions)
     * @param changes a set of changes (change names, "Simple_Change" for only
     * simple changes, "Complex_Change" for only complex changes, "ALL" for all
     * changes)
     * @param tempOntology denotes whether we give a set of version URIs,
     * (tempOntology=false) or the temp changes ontology URI (tempOntology=true)
     * @param limitPerChange an integer that limits results per version pair and
     * per change.
     * @return a set of DetChange objects which denote detected changes with
     * their respective information
     */
    public Set<DetChange> fetchDetChangesForVersions(Set<String> versions, Set<String> changes, boolean tempOntology, int limitPerChange) {
        Set<DetChange> detChanges = new TreeSet<>();
        List<String> ontologiesList;
        if (!tempOntology) {
            ontologiesList = createChOntologiesSublist(versions);
        } else {
            ontologiesList = new ArrayList<>();
            ontologiesList.addAll(versions);
        }
        List<String> changesList = createChangesSublist(changes);
        Map<String, Integer> changesOccur = new HashMap<>();
        for (String changesOntology : ontologiesList) {
            for (String change : changesList) {
                changesOccur.put(change, 0);
            }
            StringBuilder query = new StringBuilder();
            Map<String, String> ontolVersions = fetchChangeOntologyVersions(changesOntology);
            String oldVersion = ontolVersions.keySet().iterator().next();
            String newVersion = ontolVersions.get(oldVersion);
            query.append("select ?dc ?change_name ?param_name ?param_value ?param ?description where { \n").
                    append("graph <" + changesOntologySchema + "> { \n").
                    append("?param co:name ?param_name. \n").
                    append("?ch co:name ?change_name. \n").
                    append("optional {?ch co:description ?description.}. \n");
            if (!changesList.isEmpty()) {
                StringBuilder changesString = new StringBuilder();
                int cnt2 = 0;
                for (String change : changesList) {
                    changesString.append("'").append(change).append("'");
                    if (cnt2 < changesList.size() - 1) {
                        changesString.append(", ");
                    }
                    cnt2++;
                }
                query.append("filter (?change_name in ( " + changesString.toString() + " )).\n");
            }
            query.append("} \n");
            query.append("graph <" + changesOntology + "> { \n").
                    append("?dc a ?ch; ").
                    append("?param ?param_value. \n").
                    append("FILTER NOT EXISTS {?consumedBy co:consumes ?dc }.\n").
                    append("} \n").
                    append("} order by ?dc");
            ResultSet results = rep.executeSparqlQuery(query.toString(), true);
            int occur = 0;
            try {
                if (!results.next()) {
                    continue;
                }
                String dch = "";
                DetChange change = null;
                do {
                    String chName = results.getString(2);
                    //check if we have reached the limit for this change type
                    if (changesOccur.get(chName) >= limitPerChange) {
                        continue;
                    }
                    if (!dch.equals(results.getString(1))) { //we are in a new change
                        if (change != null) { //check the previous change if it has the given URI as parameter
                            occur = changesOccur.get(change.getChangeName());
                            if (occur < limitPerChange) {
                                detChanges.add(change);
                                changesOccur.put(change.getChangeName(), ++occur);
                            }
                        }
                        dch = results.getString(1);
                        String chDescr = results.getString(6);
                        change = new DetChange(dch, chName, chDescr, oldVersion, newVersion);
                    }
                    String parName = results.getString(3);
                    String parValue = results.getString(4);
                    String paramUri = results.getString(5);
                    Parameter param = new Parameter(paramUri, parName, parValue);
                    change.addParameter(param);
                } while (results.next());
                if (occur < limitPerChange) {
                    detChanges.add(change);
                }
            } catch (SQLException ex) {
                System.out.println("Exception: " + ex.getMessage());
            }
        }
        return detChanges;
    }

    private List<String> createChangesSublist(Set<String> changes) {
        //
        /// create the changes list
        List<String> changesList = new ArrayList<>();
        if (changes.contains("Simple_Change")) {
            changesList.addAll(fetchChangesNames("Simple_Change"));
            changes.remove("Simple_Change");
        }
        if (changes.contains("Complex_Change")) {
            changesList.addAll(fetchChangesNames("Complex_Change"));
            changes.remove("Complex_Change");
        }
        if (changes.contains("ALL")) {
            changesList.addAll(fetchChangesNames(null));
            changes.remove("ALL");
        }
        changesList.addAll(changes);
        return changesList;
    }

    private List<String> createChOntologiesSublist(Set<String> versions) {
        /// create the changes ontologies list from versions
        List<String> ontologiesList = new ArrayList<>();
        if (versions.contains("ALL")) {
            ontologiesList = changesOntologies;
        } else {
            for (String version : versions) {
                ontologiesList.add(fetchChangesOntology(null, version));
            }
        }
        return ontologiesList;
    }

}
