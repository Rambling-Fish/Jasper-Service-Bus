package org.jasper.core.delegate;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import org.apache.log4j.Logger;
import org.jasper.core.constants.JasperOntologyConstants;
import org.jasper.core.persistence.PersistenceFacade;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import com.hazelcast.core.EntryEvent;
import com.hazelcast.core.EntryListener;
import com.hazelcast.core.MultiMap;
import com.hp.hpl.jena.query.Query;
import com.hp.hpl.jena.query.QueryExecution;
import com.hp.hpl.jena.query.QueryExecutionFactory;
import com.hp.hpl.jena.query.QueryFactory;
import com.hp.hpl.jena.query.QuerySolution;
import com.hp.hpl.jena.query.ResultSet;
import com.hp.hpl.jena.query.ResultSetFormatter;
import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.Property;
import com.hp.hpl.jena.rdf.model.RDFNode;
import com.hp.hpl.jena.rdf.model.Resource;
import com.hp.hpl.jena.rdf.model.Statement;
import com.hp.hpl.jena.sparql.resultset.JSONOutput;

public class DelegateOntology implements EntryListener<String, String[]>{
	
	private Model model;
	private MultiMap<String, String[]> jtaStatements;
	
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

	public DelegateOntology(PersistenceFacade cachingSys, Model model){
		this.model = model;
		jtaStatements = (MultiMap<String, String[]>) cachingSys.getMultiMap("jtaStatements");
		jtaStatements.addEntryListener(this, true);
		
		for(String key:jtaStatements.keySet()){
			for(String[]triple:jtaStatements.get(key)){
				Resource s = model.getResource(triple[0]);
		    	Property p = model.getProperty(triple[1]);
				RDFNode o = model.getResource(triple[2]);
				Statement statements = model.createStatement(s, p, o);
				model.add(statements);
			}
		}
		
	}

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
	// METHOD:  isRuriKnown
	//
	// INPUT:   String (RURI)
	// OUTPUT:  boolean
	//
	// PURPOSE: Determine if this RURI is known in the ontology.
	//========================================================================================== 

	private boolean isRuriKnown(String ruri) 
	{
		if (ruri == null)
			return false;

		String queryString = 
				JasperOntologyConstants.PREFIXES +
	             "ASK  WHERE " +
	             "   {" +
	             "         <" + ruri + ">    a    owl:Class   " +
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
	// METHOD:  isProvideSupportedRURI
	//
	// INPUT:   String (RURI)
	// OUTPUT:  boolean
	//
	// PURPOSE: Determine if there is a provide operation that supports this RURI.
	//========================================================================================== 

	private boolean isProvideSupportedRURI(String ruri) 
	{
		if (ruri == null)
			return false;

		String queryString = 
				JasperOntologyConstants.PREFIXES +
	             "ASK  WHERE " +
	             "   {" +
	             "         ?dta    a                               dta:DTA       .\n" +
	             "         ?dta    dta:operation                   ?oper         .\n" +
	             "         ?oper   a                               dta:Provide   .\n" +
	             "         ?oper   dta:output/rdfs:subClassOf*     <" + ruri + "> \n" +
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

	private ArrayList<String> getProvideOperations(String ruri) 
	{
		if (ruri == null)
			return null;
		
		String queryString = 
				JasperOntologyConstants.PREFIXES +
	             "SELECT ?operation  WHERE \n" +
	             "   {\n" +
	             "      ?dta              a                dta:DTA        .\n" +
	             "      ?dta              dta:operation    ?operation     .\n" +
	             "      ?operation        a                dta:Provide    .\n" +
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
	//          level deep, ie, one one subclass.)
	//========================================================================================== 

	private ArrayList<String> getProvideOperationsEncapsulated(String ruri) 
	{
		if (ruri == null)
			return null;
		
		String queryString = 
				JasperOntologyConstants.PREFIXES +
	             "SELECT ?operation  WHERE \n" +
	             "   {\n" +
	             "      ?dta              a                dta:DTA        .\n" +
	             "      ?dta              dta:operation    ?operation     .\n" +
	             "      ?operation        a                dta:Provide    .\n" +
	             "      ?operation        dta:output       ?superRuri     .\n" +
	             "      ?ruriProp         rdfs:domain      ?superRuri     .\n" +
	             "      ?ruriProp         rdfs:range       <" + ruri + ">  \n" +
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

	private String getProvideOperationInputObject(String oper) 
	{
		if (oper == null)
			return null;

		String queryString = 
				JasperOntologyConstants.PREFIXES +
	             "SELECT ?input  WHERE \n" +
	             "   {\n" +
	             "      ?dta            a              dta:DTA        .\n" +
	             "      ?dta            dta:operation  <" + oper + "> .\n" +
	             "      <" + oper + ">  a              dta:Provide    .\n" +
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

	private String getProvideDestinationQueue(String oper) 
	{
		if (oper == null)
			return null;

		String queryString = 
				JasperOntologyConstants.PREFIXES +
	             "SELECT ?dest  WHERE \n" +
	             "   {\n" +
	             "      ?dta            a                dta:DTA        .\n" +
	             "      ?dta            dta:operation    <" + oper + "> .\n" +
	             "      <" + oper + ">  a                dta:Provide    .\n" +
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
				array.add(soln.get("dest").toString());
				
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

		if (!isRuriKnown(inputObject))
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

	public Model getModel() {
		return model;
	}
	
	public void add(String jtaName, String[] statement) {
		if(jtaName != null && jtaName.length() > 0 && statement != null && statement.length == 3){
			jtaStatements.put(jtaName, statement);
		}
	}
	
	public void remove(String jtaName) {
		if(jtaName != null && jtaName.length() > 0){
			jtaStatements.remove(jtaName);
		}
	}

	public void entryAdded(EntryEvent<String, String[]> entryEvent) {
		String[] sArray = entryEvent.getValue();      
        Resource s = model.getResource(sArray[0]);
    	Property p = model.getProperty(sArray[1]);
		RDFNode o = model.getResource(sArray[2]);
		Statement statements = model.createStatement(s, p, o);
		model.add(statements);
		
	}

	public void entryEvicted(EntryEvent<String, String[]> event) {
		logger.warn("entry Evicted" + event);;
	}

	public void entryRemoved(EntryEvent<String, String[]> entryEvent){
		if(!(entryEvent.getValue() instanceof String[])){
			logger.warn("entryRemoved value is not of the type String[], ignoring event and not updating model");
			return;
		}
		
		if(entryEvent.getValue().length != 3){
			logger.warn("entryRemoved value String[] is not a triple, ignoring event and not updating model");
			return;
		}
		
		String[]val = entryEvent.getValue();
		
		if(logger.isInfoEnabled()){
			logger.info("removing entry " + val[0] + " " + val[1] + " " + val[2] + " for key : " + entryEvent.getKey());
		}
		
		if(!jtaStatements.containsValue(val)){
			Resource s = model.getResource(val[0]);
			Property p = model.getProperty(val[1]);
			RDFNode o = model.getResource(val[2]);
			Statement statements = model.createStatement(s, p, o);
			model.remove(statements);
			if(logger.isDebugEnabled()){
				logger.debug("removing entry from model: " + statements);
			}
		}else{
			if(logger.isDebugEnabled()){
				logger.debug("duplicate entry in model belonging to another JTA, ignoring the removal from the model for triple : " +  val[0] + " " + val[1] + " " + val[2]);
			}
		}
					
	}

	public void entryUpdated(EntryEvent<String, String[]> event) {
		if(logger.isInfoEnabled())logger.info("entry uptdated - event = " + event);
	}
}
