package org.jasper.core.delegate;

import java.util.ArrayList;
import java.util.Map;

import org.apache.jena.atlas.json.JsonArray;
import org.apache.jena.atlas.json.JsonObject;
import org.apache.log4j.Logger;
import org.jasper.core.constants.JasperOntologyConstants;

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
	
	public DelegateOntology(DelegateFactory factory, Model model){
		this.model = model;
		jtaStatements = factory.getHazelcastInstance().getMultiMap("jtaStatements");
		jtaStatements.addEntryListener(this, true);
	}

	public JsonArray getQandParams(String ruri,Map<String, String> params){
		if(!isSupportedRURI(ruri)) return null;
		
		ArrayList<String> jtaList = getJTAs(ruri);
		
		if(jtaList.size() == 0)return null;
		
		JsonArray jsonArray = new JsonArray();
		for(String jta:jtaList){
			JsonObject qAndParams = new JsonObject();
			qAndParams.put(JTA, jta);
			qAndParams.put(QUEUE,getQ(jta));
			qAndParams.put(PARAMS, getParams(jta,ruri,params));
			qAndParams.put(PROVIDES, getProvides(jta,ruri));
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
			jsonParams.put(param, params.get(param));
		}
		
		for(String param:jtaParamsToLookup){
			jsonParams.put(param,getQandParams(param,params));
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
		
		Query query = QueryFactory.create(queryString) ;
		QueryExecution qexec = QueryExecutionFactory.create(query, model);
		return qexec.execAsk();
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
