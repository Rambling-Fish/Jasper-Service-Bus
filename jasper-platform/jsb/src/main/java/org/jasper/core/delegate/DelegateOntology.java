package org.jasper.core.delegate;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.Map;

import org.apache.jena.atlas.json.JsonArray;
import org.apache.jena.atlas.json.JsonObject;
import org.apache.log4j.Logger;
import org.jasper.core.constants.JasperOntologyConstants;

import com.hazelcast.core.EntryEvent;
import com.hazelcast.core.EntryListener;
import com.hazelcast.core.MultiMap;
import com.hazelcast.impl.base.Values;
import com.hp.hpl.jena.query.Query;
import com.hp.hpl.jena.query.QueryExecution;
import com.hp.hpl.jena.query.QueryExecutionFactory;
import com.hp.hpl.jena.query.QueryFactory;
import com.hp.hpl.jena.query.QuerySolution;
import com.hp.hpl.jena.query.ResultSet;
import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.Property;
import com.hp.hpl.jena.rdf.model.RDFNode;
import com.hp.hpl.jena.rdf.model.Resource;
import com.hp.hpl.jena.rdf.model.Statement;
import com.hp.hpl.jena.sparql.resultset.JSONOutput;

public class DelegateOntology implements EntryListener{
	
	private Model model;
	private MultiMap<String, String[]> jtaStatements;
	
	static Logger logger = Logger.getLogger(DelegateOntology.class.getName());

	
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
			qAndParams.put("jta", jta);
			qAndParams.put("queue",getQ(jta));
			qAndParams.put("params", getParams(jta,ruri,params));
			qAndParams.put("provides", getProvides(jta,ruri));
			jsonArray.add(qAndParams);
		}
		return jsonArray;
	}
	
	private String getProvides(String jta, String ruri) {
		String queryString = 
				 JasperOntologyConstants.PREFIXS +
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
				 JasperOntologyConstants.PREFIXS +
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
	
	private ArrayList<String> getJtaParams(String jta) {
		String queryString = 
				 JasperOntologyConstants.PREFIXS +
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
		if(ruri.startsWith("http://")) ruri = "<" + ruri +">";
		String queryString = 
				 JasperOntologyConstants.PREFIXS +
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
		if(ruri.startsWith("http://")) ruri = "<" + ruri +">";
		String queryString = 
				 JasperOntologyConstants.PREFIXS +
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

//		String newQueryString = 
//				 JasperOntologyConstants.PREFIXS +
//	             "SELECT ?jta ?jtaProvidedData ?params\n" + 
//	             "WHERE\n" + 
//	             "{\n" + 
//	             "	{\n" + 
//	             "		?jta :is       :jta .\n" + 
//	             "		?jta :provides ?jtaProvidedData .\n" + 
//	             "	}\n" + 
//	             "	UNION\n" + 
//	             "	{\n" + 
//	             "		?jta :is       :jta .\n" + 
//	             "		?jta :param    ?params .\n" + 
//	             "	}	\n" + 
//	             "}" ;
//		System.out.println("######## queryString = " + queryString);

		QueryExecution queryExecution = QueryExecutionFactory.create(queryString, model);
//		QueryExecution queryExecution = QueryExecutionFactory.create(newQueryString, model);
		
		ResultSet resultSet = queryExecution.execSelect();
		
		JSONOutput jsonOutput = new JSONOutput();
		String result = jsonOutput.asString(resultSet);
		
//		System.out.println("######## queryModel Start");
//		System.out.println(result);
//		System.out.println("######## queryModel End");
		
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

	public void entryAdded(EntryEvent entryEvent) {
		String[] sArray = (String[]) entryEvent.getValue();      
        Resource s = model.getResource(sArray[0]);
    	Property p = model.getProperty(sArray[1]);
		RDFNode o = model.getResource(sArray[2]);
		Statement statements = model.createStatement(s, p, o);
		model.add(statements);
		model.write(System.out, "TTL");
		
	}

	public void entryEvicted(EntryEvent arg0) {
		// TODO Auto-generated method stub
		
	}

	public void entryRemoved(EntryEvent entryEvent){
		if(!(entryEvent.getValue() instanceof Values)){
			logger.warn("entryRemoved value is not of the type Values, ignoring event and not updating model");
		}
		
		if(logger.isInfoEnabled()){
			logger.info("removing all entries for key : " + entryEvent.getKey());
		}
		
		Values val = (Values) entryEvent.getValue();
		Iterator<String[]> it = val.iterator();
		for(String[] sArray = it.next();it.hasNext();){
			Resource s = model.getResource(sArray[0]);
			Property p = model.getProperty(sArray[1]);
			RDFNode o = model.getResource(sArray[2]);
			Statement statements = model.createStatement(s, p, o);
			if(!jtaStatements.containsValue(sArray)){
				model.remove(statements);
				if(logger.isDebugEnabled()){
					logger.debug("removing entry from model: " + statements);
				}
			}else{
				if(logger.isDebugEnabled()){
					logger.debug("duplicate entry in model belonging to another JTA, ignoring the removal of entry : " + statements);
				}
			}
			sArray = it.next();
		}		
	}

	public void entryUpdated(EntryEvent arg0) {
		// TODO Auto-generated method stub
		
	}
}
