package org.cy3sbml.ols;

import com.mashape.unirest.http.HttpResponse;
import com.mashape.unirest.http.JsonNode;
import com.mashape.unirest.http.Unirest;
import com.mashape.unirest.http.exceptions.UnirestException;

import org.cy3sbml.miriam.RegistryUtil;
import org.identifiers.registry.RegistryDatabase;
import org.identifiers.registry.RegistryLocalProvider;
import org.identifiers.registry.RegistryUtilities;
import org.identifiers.registry.data.DataType;
import org.identifiers.registry.data.PhysicalLocation;
import org.json.JSONArray;
import org.json.JSONObject;


import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * Class for looking up terms via the Ontology Lookup Service.
 *      http://www.ebi.ac.uk/ols/index
 * with REST API documented here
 *      http://www.ebi.ac.uk/ols/docs/api
 *      http://stateless.co/hal_specification.html
 *
 * Use UniRest for REST queries.
 */
public class OLSRestQuery {
    public static final String OLS_PURL_PREFIX = "http%253A%252F%252Fpurl.obolibrary.org%252Fobo%252F";

    /**
     * Create client and perform query.
     * @throws UnirestException
     */
    public static JSONObject olsQuery(String uri) {
        JSONObject jsonObject = null;
        try {
            HttpResponse<JsonNode> response = Unirest.get(uri).asJson();
            Integer status = response.getStatus();
            if (status != 200){
                System.out.println("Query unsuccessful, status <" + status +">");
                return null;
            }

            JsonNode node = response.getBody();
            if (false) {
                System.out.println("--------------------------------------------");
                System.out.println(node.toString());
                System.out.println("--------------------------------------------");
            }
            jsonObject = node.getObject();
        } catch (UnirestException e){
            e.printStackTrace();
        }
        return jsonObject;
    }

    /**
     * Get available ontologies from OLS.
     * Queries all the ontologies and parses them in ontology objects.
     */
    public static JSONArray getOntologies(){

        // set a high size parameter, so all ontologies are returned within one query.
        JSONObject jsonObject = olsQuery("http://www.ebi.ac.uk/ols/api/ontologies?size=500");
        JSONArray ontologies = jsonObject.getJSONObject("_embedded").getJSONArray("ontologies");

        for (int i=0; i<ontologies.length(); i++){
            JSONObject jsonOntology = ontologies.getJSONObject(i);
        }
        return ontologies;
    }

    /**
     * Searches the OLS physical location and
     * returns the root URL which can be used for term
     * queries with OLS.
     */
    public static String getURLRootForOLS(String uri){
        String OLS_PREFIX = "http://www.ebi.ac.uk/ols/ontologies/";
        // find the ontology from the resources
        String dataPart = RegistryUtilities.getDataPart(uri);

        // MIRIAM DataType from the given dataPart
        DataType dataType = RegistryDatabase.getInstance().getDataTypeByURI(dataPart);
        String rootURL = null;
        if (dataType != null){
            for (PhysicalLocation location : dataType.getPhysicalLocations()){
                String url = location.getUrlRoot();
                if (url.startsWith(OLS_PREFIX)) {
                    rootURL = url;
                    break;
                }
            }
        }
        return rootURL;
    }

    /**
     * Get a term.
     */
    public static JSONObject getTermFromResource(String uri){
        JSONObject term = null;
        // It is an ontology supported by OLS
        String urlRoot = getURLRootForOLS(uri);

        // TODO: go via the data entry and replace the $id
        // than parse all the terms in the returned JSON.

        if (urlRoot!= null){
            urlRoot = urlRoot.replace("http://www.ebi.ac.uk/org.cy3sbml.ols/ontologies/", "http://www.ebi.ac.uk/org.cy3sbml.ols/api/ontologies/");
            System.out.println("rootURL: " + urlRoot);
            // term to search
            String termId = RegistryUtilities.getElementPart(uri);
            termId = termId.replace(":", "_");

            // finally request the term
            String query = urlRoot + "/terms/" + OLS_PURL_PREFIX + termId;
            System.out.println("query: " +  query);
            System.out.println(query);
            term = olsQuery(query);
        } else {
            System.out.println("Resource is not an ontology: " + uri);
        }
        return term;
    }


	/**
	 * Test the Restful API by direct querying.
	 *
	 * For Json parsing see
	 *  http://stackoverflow.com/questions/2591098/how-to-parse-json-in-java
	 *
	 *      <rdf:li rdf:resource="http://identifiers.org/biomodels.sbo/SBO:0000247" />
     *      <rdf:li rdf:resource="http://identifiers.org/chebi/CHEBI:25858" />
     *      <rdf:li rdf:resource="http://identifiers.org/kegg.compound/C13747" />
     */
    public static void main(String[] args){
        RegistryUtil.loadRegistry();

        // Get ontologies
        getOntologies();

        // Get the resources
        String[] uris = {
                // http://www.ebi.ac.uk/ols/api/ontologies/sbo/terms/http%253A%252F%252Fpurl.obolibrary.org%252Fobo%252FSBO_0000247
                "http://identifiers.org/biomodels.sbo/SBO:0000247",
                // http://www.ebi.ac.uk/ols/api/ontologies/chebi/terms/http%253A%252F%252Fpurl.obolibrary.org%252Fobo%252FCHEBI_25858
                "http://identifiers.org/chebi/CHEBI:25858",
                "http://identifiers.org/kegg.compound/C13747",
                "http://identifiers.org/efo/0000589"
        };


        // TODO: get the physical location of OLS via MIRIAM, i.e. there should be a a resource through OLS
        // Parse the resourses, which are primary resources and use them for the lookup of the term
        // http://www.ebi.ac.uk/ols/api/ontologies/efo/terms?obo_id=EFO:0004859

        for (String uri : uris){
            String[] locations = new RegistryLocalProvider().getLocations(uri);
            JSONObject term = getTermFromResource(uri);
            System.out.println(term);
        }

    }
}
