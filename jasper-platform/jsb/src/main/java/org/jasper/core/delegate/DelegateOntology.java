package org.jasper.core.delegate;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import org.apache.activemq.util.ByteArrayInputStream;
import org.apache.log4j.Logger;
import org.jasper.core.constants.JasperOntologyConstants;
import org.jasper.core.persistence.PersistenceFacade;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import com.hazelcast.core.EntryEvent;
import com.hazelcast.core.EntryListener;
import com.hazelcast.core.IMap;
import com.hp.hpl.jena.ontology.OntModel;
import com.hp.hpl.jena.query.Query;
import com.hp.hpl.jena.query.QueryExecution;
import com.hp.hpl.jena.query.QueryExecutionFactory;
import com.hp.hpl.jena.query.QueryFactory;
import com.hp.hpl.jena.query.QuerySolution;
import com.hp.hpl.jena.query.ResultSet;
import com.hp.hpl.jena.query.ResultSetFormatter;
import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.ModelFactory;
import com.hp.hpl.jena.sparql.resultset.JSONOutput;

public class DelegateOntology implements EntryListener<String, String>{
	
	private OntModel model;
	private IMap<String,String> dtaTriples;
	private Map<String,Model> dtaSubModels;
	
	static Logger logger = Logger.getLogger(DelegateOntology.class.getName());

	public static final String JTA = "jta";
	public static final String QUEUE = "queue";
	public static final String PARAMS = "params";
	public static final String PROVIDES = "provides";

	// Cardinality (parameter restriction) values for Vocabulary version 2
    public static final String CARD_0_S_Opt = "noCardinality";  // Not defined in owl schema. Only used internally since there is no restriction in Ontology for case = '0_*' 
    public static final String CARD_0_1_Opt = "http://www.w3.org/2002/07/owl#maxCardinality";
    public static final String CARD_1_S_Req = "http://www.w3.org/2002/07/owl#minCardinality";
    public static final String CARD_1_1_Req = "http://www.w3.org/2002/07/owl#cardinality";

	public DelegateOntology(PersistenceFacade cachingSys, OntModel model){
		this.model = model;
		dtaTriples = (IMap<String, String>) cachingSys.getMap("dtaTriples");
		dtaTriples.addEntryListener(this, true);
		dtaSubModels = new HashMap<String,Model>();
		
		for(String dtaName:dtaTriples.keySet()){
			Model subModel = ModelFactory.createDefaultModel();
			String triples = dtaTriples.get(dtaName);
			InputStream in = new ByteArrayInputStream(triples.getBytes());
			subModel.read(in,"","TURTLE");
			model.addSubModel(subModel);
			dtaSubModels.put(dtaName, subModel);
		}
		
	}

	// TODO: remove old vocab methods
	public JsonArray getQandParams(String ruri,Map<String, String> params){
		if(!isSupportedRURI(ruri)) return null;
		
		ArrayList<String> jtaList = getJTAs(ruri);
		
		if(jtaList.size() == 0)return null;
		
		JsonArray jsonArray = new JsonArray();
		for(String jta:jtaList){
			JsonObject qAndParams = new JsonObject();
			qAndParams.addProperty(JTA, jta);
			qAndParams.addProperty(QUEUE,getQ(jta));
			qAndParams.add(PARAMS, getParams(jta,ruri,params));
			qAndParams.addProperty(PROVIDES, getProvides(jta,ruri));
			jsonArray.add(qAndParams);
		}
		return jsonArray;
	}

	// TODO: remove old vocab methods
	private String getProvides(String jta, String ruri) {
		String queryString = 
				 JasperOntologyConstants.PREFIXES +
	             "SELECT ?provides  WHERE" +
	             "   {<" + jta + "> :provides ?provides}" ;
		
		Query query = QueryFactory.create(queryString) ;
		QueryExecution qexec = QueryExecutionFactory.create(query, model);
		ArrayList<String> array = new ArrayList<String>();
		try {
			ResultSet results = qexec.execSelect() ;
			for ( ; results.hasNext() ; )
			{
				QuerySolution soln = results.nextSolution();
				array.add(soln.get("provides").toString());
				
			}
		}finally{
			qexec.close();
		}
		return (array.size()==1)?array.get(0):ruri;
	}

	// TODO: remove old vocab methods
	private JsonObject getParams(String jta, String ruri, Map<String, String> params) {
		/*
		 * get list of parameters need for jta to get ruri
		 * if param list is 0 than no parameters are needed and we return 
		 */
		JsonObject jsonParams = new JsonObject();
		ArrayList<String> jtaParams = getJtaParams(jta);
		if(jtaParams.size() == 0) return jsonParams;
		
		/*
		 * if parameters are needed we check which are available and which we
		 * need to lookup. At the end of this for loop
		 *   - availableParams list contains all the parameters needed and available
		 *   - jtaParamsToLookp list contains all parameters needed to be looked up
		 */
		ArrayList<String> availableParams = new ArrayList<String>();
		ArrayList<String> jtaParamsToLookup = getJtaParams(jta);
		for(String param:jtaParams){
			if(params.containsKey(param)){
				availableParams.add(param);
				jtaParamsToLookup.remove(param);
			}
		}
		
		for(String param:availableParams){
			jsonParams.addProperty(param, params.get(param));
		}
		
		for(String param:jtaParamsToLookup){
			jsonParams.add(param,getQandParams(param,params));
		}
				
		return jsonParams;
	}

	// TODO: remove old vocab methods
	private String getQ(String jta) {
		String queryString = 
				 JasperOntologyConstants.PREFIXES +
	             "SELECT ?q  WHERE " +
	             "   {<" + jta + "> :queue ?q .}" ;
		
		Query query = QueryFactory.create(queryString) ;
		QueryExecution qexec = QueryExecutionFactory.create(query, model);
		ArrayList<String> array = new ArrayList<String>();
		try {
			ResultSet results = qexec.execSelect() ;
			for ( ; results.hasNext() ; )
			{
				QuerySolution soln = results.nextSolution();
				array.add(soln.get("q").toString());
				
			}
		}finally{
			qexec.close();
		}
		return (array.size()==1)?array.get(0):null;
	}
	
	// TODO: remove old vocab methods
	public ArrayList<String> getJtaParams(String jta) {
		String queryString = 
				 JasperOntologyConstants.PREFIXES +
	             "SELECT ?params  WHERE " +
	             "   {<" + jta + "> :param ?params .}" ;
		
		Query query = QueryFactory.create(queryString) ;
		QueryExecution qexec = QueryExecutionFactory.create(query, model);
		ArrayList<String> array = new ArrayList<String>();
		try {
			ResultSet results = qexec.execSelect() ;
			for ( ; results.hasNext() ; )
			{
				QuerySolution soln = results.nextSolution();
				array.add(soln.get("params").toString());
				
			}
		}finally{
			qexec.close();
		}
		return array;
	}

	// TODO: remove old vocab methods
	private ArrayList<String> getJTAs(String ruri) {
		if(ruri.startsWith("http://")){
			ruri = "<" + ruri +">";
		}else{
			ruri = "\"" + ruri +"\"";
		}		String queryString = 
				 JasperOntologyConstants.PREFIXES +
	             "SELECT ?jta  WHERE " +
	             "   {" +
	             "       {" +
	             "         ?jta             :is           :jta ." +
	             "         ?jtaProvidedData :subClassOf    " + ruri + "." +
	             "         ?jta             :provides      ?jtaProvidedData ." +
	             "       }" +
	             "       UNION" +
	             "       {" +
	             "         ?jta             :is           :jta ." +
	             "         ?jta             :provides    " + ruri + "." +
	             "       }" +
	             "   }" ;
		
		Query query = QueryFactory.create(queryString) ;
		QueryExecution qexec = QueryExecutionFactory.create(query, model);
		ArrayList<String> array = new ArrayList<String>();
		try {
			ResultSet results = qexec.execSelect() ;
			for ( ; results.hasNext() ; )
			{
				QuerySolution soln = results.nextSolution();
				array.add(soln.get("jta").toString());
				
			}
		}finally{
			qexec.close();
		}
		return array;	
	}

	// TODO: remove old vocab methods
	private boolean isSupportedRURI(String ruri) {
		if(ruri.startsWith("http://")){
			ruri = "<" + ruri +">";
		}else{
			ruri = "\"" + ruri +"\"";
		}
		String queryString = 
				 JasperOntologyConstants.PREFIXES +
	             "ASK  WHERE " +
	             "   {" +
	             "       {" +
	             "         ?jta             :is           :jta ." +
	             "         ?jtaProvidedData :subClassOf    " + ruri + "." +
	             "         ?jta             :provides      ?jtaProvidedData ." +
	             "       }" +
	             "       UNION" +
	             "       {" +
	             "         ?jta             :is           :jta ." +
	             "         ?jta             :provides    " + ruri + "." +
	             "       }" +
	             "   }" ;
		
		try{
			Query query = QueryFactory.create(queryString) ;
			QueryExecution qexec = QueryExecutionFactory.create(query, model);
			return qexec.execAsk();
		}catch(Exception ex){
			logger.error("Exception caught while parsing request " + ex);
			return false;
		}
	}

	//========================================================================================== 
	// METHOD:  isRuriKnownForInputPost
	//
	// INPUT:   String (RURI)
	// OUTPUT:  boolean
	//
	// PURPOSE: Determine if this RURI is known in the ontology and is an input to a POST
	//          operation.
	//========================================================================================== 

	public boolean isRuriKnownForInputPost(String ruri) 
	{
		if (ruri == null)
			return false;

		String queryString = 
				JasperOntologyConstants.PREFIXES +
	             "ASK  WHERE " +
	             "   {" +
	             "         ?dta    a                               dta:DTA       .\n" +
	             "         ?dta    dta:operation                   ?oper         .\n" +
	             "         ?oper   dta:kind                        dta:Post      .\n" +
	             "         ?oper   dta:input                       <" + ruri + "> \n" +
	             "   }" ;
		
		try{
			Query query = QueryFactory.create(queryString) ;
			QueryExecution qexec = QueryExecutionFactory.create(query, model);
			return qexec.execAsk();
		}catch(Exception ex){
			logger.error("Exception caught while parsing request " + ex);
			return false;
		}
	}

	//========================================================================================== 
	// METHOD:  isRuriKnownForInputGet
	//
	// INPUT:   String (RURI)
	// OUTPUT:  boolean
	//
	// PURPOSE: Determine if this RURI is known in the ontology and is an input to a GET
	//          operation.
	//========================================================================================== 

	public boolean isRuriKnownForInputGet(String ruri) 
	{
		if (ruri == null)
			return false;

		String queryString = 
				JasperOntologyConstants.PREFIXES +
	             "ASK  WHERE " +
	             "   {" +
	             "         ?dta    a                               dta:DTA       .\n" +
	             "         ?dta    dta:operation                   ?oper         .\n" +
	             "         ?oper   dta:kind                        dta:Get       .\n" +
	             "         ?oper   dta:input                       <" + ruri + "> \n" +
	             "   }" ;
		
		try{
			Query query = QueryFactory.create(queryString) ;
			QueryExecution qexec = QueryExecutionFactory.create(query, model);
			return qexec.execAsk();
		}catch(Exception ex){
			logger.error("Exception caught while parsing request " + ex);
			return false;
		}
	}

	//========================================================================================== 
	// METHOD:  isRuriKnownForOutputGet
	//
	// INPUT:   String (RURI)
	// OUTPUT:  boolean
	//
	// PURPOSE: Determine if this RURI is known in the ontology and is an output of a GET
	//          operation.
	//========================================================================================== 

	public boolean isRuriKnownForOutputGet(String ruri) 
	{
		if (ruri == null)
			return false;

		String queryString = 
				JasperOntologyConstants.PREFIXES +
	             "ASK  WHERE " +
	             "   {" +
	             "         ?dta    a                               dta:DTA       .\n" +
	             "         ?dta    dta:operation                   ?oper         .\n" +
	             "         ?oper   dta:kind                        dta:Get       .\n" +
	             "         ?oper   dta:output                      <" + ruri + "> \n" +
	             "   }" ;
		
		try{
			Query query = QueryFactory.create(queryString) ;
			QueryExecution qexec = QueryExecutionFactory.create(query, model);
			return qexec.execAsk();
		}catch(Exception ex){
			logger.error("Exception caught while parsing request " + ex);
			return false;
		}
	}
	
	//========================================================================================== 
	// METHOD:  isEncapsulatedRuriKnownForOutput
	//
	// INPUT:   String (RURI)
	// OUTPUT:  boolean
	//
	// PURPOSE: Determine if this RURI is known in the ontology and is an encapsulated output
	//          of a GET operation.  (Note: current implementation extends only one
	//          level deep, ie, one subclass.)
	//========================================================================================== 

	public boolean isEncapsulatedRuriKnownForOutput(String ruri) 
	{
		if (ruri == null)
			return false;

		String queryString = 
				JasperOntologyConstants.PREFIXES +
	             "ASK  WHERE " +
	             "   {" +
	             "      ?dta              a                dta:DTA        .\n" +
	             "      ?dta              dta:operation    ?operation     .\n" +
	             "      ?operation        dta:kind         dta:Get        .\n" +
	             "      ?operation        dta:output       ?superRuri     .\n" +
	             "      ?superRuri        rdfs:range       ?superType     .\n" +
	             "      <" + ruri + ">    rdfs:domain      ?superType     \n" +
	             "   }" ;
		
		try{
			Query query = QueryFactory.create(queryString) ;
			QueryExecution qexec = QueryExecutionFactory.create(query, model);
			return qexec.execAsk();
		}catch(Exception ex){
			logger.error("Exception caught while parsing request " + ex);
			return false;
		}
	}
	
	//========================================================================================== 
	// METHOD:  getProvideOperations
	//
	// INPUT:   String (RURI)
	// OUTPUT:  ArrayList<String> (array of strings, each string identifies an operation)
	//
	// PURPOSE: Return a list of provide type operations that return the RURI object.
	//========================================================================================== 

	public ArrayList<String> getProvideOperations(String ruri) 
	{
		if (ruri == null)
			return null;
		
		String queryString = 
				JasperOntologyConstants.PREFIXES +
	             "SELECT ?operation  WHERE \n" +
	             "   {\n" +
	             "      ?dta              a                dta:DTA        .\n" +
	             "      ?dta              dta:operation    ?operation     .\n" +
	             "      ?operation        dta:kind         dta:Get        .\n" +
	             "      ?operation        dta:output       <" + ruri + ">  \n" +
	             "   }" ;
		
		Query query = QueryFactory.create(queryString) ;
		QueryExecution qexec = QueryExecutionFactory.create(query, model);
		ArrayList<String> array = new ArrayList<String>();
		try {
			ResultSet results = qexec.execSelect() ;
			for ( ; results.hasNext() ; )
			{
				QuerySolution soln = results.nextSolution();
				array.add(soln.get("operation").toString());
				
			}
		}finally{
			qexec.close();
		}
		return array;	
	}	

	//========================================================================================== 
	// METHOD:  getProvideOperationsEncapsulated
	//
	// INPUT:   String (RURI)
	// OUTPUT:  ArrayList<String> (array of strings, each string identifies an operation)
	//
	// PURPOSE: Return a list of provide type operations that return an object that
	//          encapsulates the RURI.  (Note: current implementation extends only one
	//          level deep, ie, one subclass.)
	//========================================================================================== 

	public ArrayList<String> getProvideOperationsEncapsulated(String ruri) 
	{
		if (ruri == null)
			return null;
		
		String queryString = 
				JasperOntologyConstants.PREFIXES +
	             "SELECT ?operation  WHERE \n" +
	             "   {\n" +
	             "      ?dta              a                dta:DTA        .\n" +
	             "      ?dta              dta:operation    ?operation     .\n" +
	             "      ?operation        dta:kind         dta:Get        .\n" +
	             "      ?operation        dta:output       ?superRuri     .\n" +
	             "      ?superRuri        rdfs:range       ?superType     .\n" +
	             "      <" + ruri + ">    rdfs:domain      ?superType     \n" +
	             "   }" ;
				
		Query query = QueryFactory.create(queryString) ;
		QueryExecution qexec = QueryExecutionFactory.create(query, model);
		ArrayList<String> array = new ArrayList<String>();
		try {
			ResultSet results = qexec.execSelect() ;
			for ( ; results.hasNext() ; )
			{
				QuerySolution soln = results.nextSolution();
				array.add(soln.get("operation").toString());
				
			}
		}finally{
			qexec.close();
		}
		return array;	
	}	

	//========================================================================================== 
	// METHOD:  getProvideOperationInputObject
	//
	// INPUT:   String (RURI of the operation)
	// OUTPUT:  String (RURI of the input object)
	//
	// PURPOSE: Retrieves the input object(s) for the the provide operation.
	//========================================================================================== 

	public String getProvideOperationInputObject(String oper) 
	{
		if (oper == null)
			return null;

		String queryString = 
				JasperOntologyConstants.PREFIXES +
	             "SELECT ?input  WHERE \n" +
	             "   {\n" +
	             "      ?dta            a              dta:DTA        .\n" +
	             "      ?dta            dta:operation  <" + oper + "> .\n" +
	             "      <" + oper + ">  dta:kind       dta:Get        .\n" +
	             "      <" + oper + ">  dta:input      ?input          \n" +
	             "   }" ;

		Query query = QueryFactory.create(queryString) ;
		QueryExecution qexec = QueryExecutionFactory.create(query, model);
		ArrayList<String> array = new ArrayList<String>();
		try {
			ResultSet results = qexec.execSelect() ;
			for ( ; results.hasNext() ; )
			{
				QuerySolution soln = results.nextSolution();
				array.add(soln.get("input").toString());
				
			}
		}finally{
			qexec.close();
		}
		return (array.size()==1)?array.get(0):null;
	}		
	
	//========================================================================================== 
	// METHOD:  getProvideDestinationQueue
	//
	// INPUT:   String (RURI of the operation)
	// OUTPUT:  ArrayList<String> (array of strings, each string identifies a queue)
	//
	// PURPOSE: Retrieves the destination queue(s) of the provide operation(s) that supports
	//          this RURI.
	//========================================================================================== 

	public String getProvideDestinationQueue(String oper) 
	{
		if (oper == null)
			return null;

		String queryString = 
				JasperOntologyConstants.PREFIXES +
	             "SELECT ?dest  WHERE \n" +
	             "   {\n" +
	             "      ?dta            a                dta:DTA        .\n" +
	             "      ?dta            dta:operation    <" + oper + "> .\n" +
	             "      <" + oper + ">  dta:kind         dta:Get        .\n" +
	             "      <" + oper + ">  dta:destination  ?dest           \n" +
	             "   }" ;
		
		Query query = QueryFactory.create(queryString) ;
		QueryExecution qexec = QueryExecutionFactory.create(query, model);
		ArrayList<String> array = new ArrayList<String>();
		
		try 
		{
			ResultSet results = qexec.execSelect() ;
			for ( ; results.hasNext() ; )
			{
				QuerySolution soln = results.nextSolution();
				array.add(soln.get("dest").asLiteral().getString());
				
			}
		}
		finally
		{
			qexec.close();
		}
		return (array.size()==1)?array.get(0):null;	
	}	
	
	//========================================================================================== 
	// METHOD:  createJsonSchema
	//
	// INPUT:   String (RURI of the input object)
	// INPUT:   boolean (must be true.  Only set to false on internal recursive invocation)
	// OUTPUT:  JsonObject
	//
	// PURPOSE: Create the JSON-Schema for the input object.
	//========================================================================================== 

	public JsonObject createJsonSchema(String inputObject, boolean firstPass) 
	{
		JsonObject objDesc = new JsonObject();
		JsonArray reqArray = new JsonArray();

		if (!isRuriKnownForInputGet(inputObject))
		{
			return null;
		}
		
		// The object description is a JSON-Schema with the following fields:
		//
		//     "id"
		//     "type"
		//     "properties"
		//     "required"
		
		// Add the "id" field only on the first invocation, not on subsequent recursive invocations
		if (firstPass) 
			objDesc.addProperty("id", inputObject);

		// Add the "type" field
		objDesc.addProperty("type", "object");
		
		// Build the "properties" field values

		// GET THE DATA-TYPE (SIMPLE) PROPERTIES
		Map<String, String> propMap = new HashMap<String, String>();		
		propMap = getDatatypeProperties(inputObject);

		JsonObject propsDesc = new JsonObject();
		for (Map.Entry<String,String> entry : propMap.entrySet())
		{
			JsonObject typeDesc = new JsonObject();
			typeDesc.addProperty("type", entry.getValue());
			propsDesc.add(entry.getKey(), typeDesc);

			String cardString = getParameterRestriction(inputObject, entry.getKey());

			if (cardString == null)
			{
				logger.error("cardinality error: " + inputObject + " ; " + entry.getKey());
				return null;
			}
			
			if ((cardString.compareTo(CARD_1_1_Req) == 0) || 
				(cardString.compareTo(CARD_1_S_Req) == 0))
			{
				JsonPrimitive element = new JsonPrimitive(entry.getKey());
				reqArray.add(element);
			}
		}
		
		// GET THE OBJECT-TYPE (COMPLEX) PROPERTIES
		Map<String, String> objMap = new HashMap<String, String>();				
		objMap = getObjectTypeProperties(inputObject);
		
		for (Map.Entry<String,String> entry : objMap.entrySet())
		{
			// RECURSIVELY GET DESCRIPTION OF COMPLEX OBJECT
			propsDesc.add(entry.getKey(), createJsonSchema(entry.getValue(), false));
			
			String cardString = getParameterRestriction(inputObject, entry.getKey());

			if (cardString == null)
			{
				logger.error("cardinality error, " + inputObject + " ; " + entry.getKey());
				return null;
			}
			
			if ((cardString.compareTo(CARD_1_1_Req) == 0) ||
				(cardString.compareTo(CARD_1_S_Req) == 0))
			{
				JsonPrimitive element = new JsonPrimitive(entry.getKey());
				reqArray.add(element);
			}		
		}

		// Add the "properties" field and value(s)
		objDesc.add("properties", propsDesc);

		// Add the "required" field	 and value(s)	
		objDesc.add("required", reqArray);

		return objDesc;
	}
	
	//========================================================================================== 
	// METHOD:  getDatatypeProperties
	//
	// INPUT:   String (RURI of the input object)
	// OUTPUT:  Map<String, String> (Map of strings, each string pair identifies a simple or
	//                               data-type property and it's type)
	//
	// PURPOSE: Retrieves the data-type property information for the input object.  The output
	//          is a <String, String> map where the value pairs represent:
	//                   key   = data-type property name
	//                   value = type of the property (ie, integer, string, etc)
	//========================================================================================== 

	private Map<String, String> getDatatypeProperties(String inObject)
	{
		if (inObject == null)
			return null;
		
		String queryString = 
				JasperOntologyConstants.PREFIXES +
	             "SELECT ?props ?proptypes  WHERE \n" +
	             "   {\n" +
	             "      ?props          rdfs:domain      <" + inObject + ">  .\n" +
	             "      ?props          rdfs:range       ?proptypes          .\n" +
	             "      ?props          a                owl:DatatypeProperty \n" +
	             "   }" ;

		Query query = QueryFactory.create(queryString) ;
		QueryExecution qexec = QueryExecutionFactory.create(query, model);
		Map<String, String> map = new HashMap<String, String>();
		
		try 
		{
			ResultSet results = qexec.execSelect() ;
			for ( ; results.hasNext() ; )
			{
				QuerySolution soln = results.nextSolution();
				map.put(soln.get("props").toString(), soln.get("proptypes").toString());				
			}
		}
		finally{
			qexec.close();
		}
		return map;			
	}
	
	//========================================================================================== 
	// METHOD:  getObjectTypeProperties
	//
	// INPUT:   String (RURI of the input object)
	// OUTPUT:  Map<String, String> (Map of strings, each string pair identifies an object-type
	//                               property.)
	//
	// PURPOSE: Retrieves the object-type property information for the input object.  The output
	//          is a <String, String> map where the value pairs represent:
	//                   key   = object-type property name
	//                   value = type of the property (ie, "object")
	//========================================================================================== 

	private Map<String, String> getObjectTypeProperties(String inObject)
	{
		if (inObject == null)
			return null;
		
		String queryString = 
				JasperOntologyConstants.PREFIXES +
	             "SELECT ?props ?proptypes  WHERE \n" +
	             "   {\n" +
	             "      ?props     rdfs:domain      <" + inObject + "> .\n" +
	             "      ?props     rdfs:range       ?proptypes         .\n" +
	             "      ?props     a                owl:ObjectProperty  \n" +
	             "   }" ;

		Query query = QueryFactory.create(queryString) ;
		QueryExecution qexec = QueryExecutionFactory.create(query, model);
		Map<String, String> map = new HashMap<String, String>();
		
		try 
		{
			ResultSet results = qexec.execSelect() ;
			for ( ; results.hasNext() ; )
			{
				QuerySolution soln = results.nextSolution();
				map.put(soln.get("props").toString(),soln.get("proptypes").toString());				
			}
		}
		finally
		{
			qexec.close();
		}
		return map;			
	}

	//========================================================================================== 
	// METHOD:  getParameterRestriction
	//
	// INPUT:   String (RURI of the input object)
	// INPUT:   String (RURI of the parameter within the input object)
	// OUTPUT:  String (restriction, one of the defined string constants below:
	//
	//    			CARD_0_S_Opt  means optional  parameter, 0 or more  (collection) 
    //    			CARD_0_1_Opt  means optional  parameter, 0 or 1     (item)
    //    			CARD_1_S_Req  means mandatory parameter, 1 or more  (collection)
    //    			CARD_1_1_Req  means mandatory parameter, 1 only     (item)
	//          )
    //
	// PURPOSE: Retrieves the parameter restriction for the specific input object parameter.
	//========================================================================================== 

	private String getParameterRestriction(String ruri, String parm)
	{
		if (parm == null)
			return null;

		String queryString = 
				JasperOntologyConstants.PREFIXES +
	             "SELECT ?card  WHERE \n" +
	             "   {\n" +
	             "      <" + ruri + ">  a                owl:Class      .\n" +
	             "      <" + ruri + ">  dta:restriction  ?restrict      .\n" +
	             "      ?restrict       owl:onProperty   <" + parm + "> .\n" +
	             "      ?restrict       ?card            \"1\"^^xsd:int  \n" +
	             "   }" ;

		Query query = QueryFactory.create(queryString) ;
		QueryExecution qexec = QueryExecutionFactory.create(query, model);
		ArrayList<String> array = new ArrayList<String>();
		try {
			ResultSet results = qexec.execSelect() ;
			for ( ; results.hasNext() ; )
			{
				QuerySolution soln = results.nextSolution();
				if (soln != null) 
					array.add(soln.get("card").toString());
				
			}
		}finally{
			qexec.close();
		}

		if (array.size() == 1)
		{
			return array.get(0);
		}
		
		if (array.size() == 0)
		{
			return CARD_0_S_Opt;
		}
		else
		{
			logger.error("invalid parameter restriction.  size=" + array.size() + " " + parm);
			return null;		
		}
	}			
	
	public String queryModel(String queryString,String output){
		QueryExecution queryExecution = QueryExecutionFactory.create(queryString, model);
		
		ResultSet resultSet = queryExecution.execSelect();
		String result;
		
		if(output.equalsIgnoreCase("json")){
			JSONOutput jsonOutput = new JSONOutput();
			result = jsonOutput.asString(resultSet);
		}
		else{
			result = ResultSetFormatter.asXMLString(resultSet);
		}
		
		return result;
	}

	public OntModel getModel() {
		return model;
	}
	
    public void add(String dtaName, String triples) {
        if(dtaName != null && dtaName.length() > 0 && triples != null && triples.length() > 0){
                dtaTriples.put(dtaName, triples);
        }else{
                logger.warn("trying to add an invalid entry to our dtaTriples Map. dtaName : " + dtaName + " triples : " + triples);
        }
    }
		
    public void remove(String dtaName) {
        if(dtaName != null && dtaName.length() > 0){
                dtaTriples.remove(dtaName);
        }else{
                logger.warn("trying to remove an invalid entry from our dtaTriples Map. dtaName : " + dtaName);
        }
    }
        
    public void entryAdded(EntryEvent<String, String> entryEvent) {
        String dtaName = entryEvent.getKey();
        String triples = entryEvent.getValue();
        if(dtaSubModels.containsKey(dtaName)){
                logger.warn("adding a new subModel using same dtaName, removing old subModel for : " + dtaName);
                Model subModel = dtaSubModels.remove(dtaName);
                model.removeSubModel(subModel);
        }

        Model subModel = ModelFactory.createDefaultModel();
        InputStream in = new ByteArrayInputStream(triples.getBytes());
        subModel.read(in,"","TURTLE");
        model.addSubModel(subModel);
        dtaSubModels.put(dtaName, subModel);
    }
    
    public void entryEvicted(EntryEvent<String, String> event) {
        logger.warn("entry Evicted" + event);;
    }
    
    public void entryRemoved(EntryEvent<String, String> entryEvent){
        String dtaName = entryEvent.getKey();
        if(dtaSubModels.containsKey(dtaName)){
                Model subModel = dtaSubModels.remove(dtaName);
                model.removeSubModel(subModel);
                if(logger.isInfoEnabled()){
                        logger.info("removing subModel for dta : " + dtaName);
                }
        }else{
                logger.warn("trying to remove dtaName that does not exist in map : " + dtaName);
        }
    }

    public void entryUpdated(EntryEvent<String, String> event) {
        if(logger.isInfoEnabled())logger.info("entry uptdated - event = " + event);
    }
}
