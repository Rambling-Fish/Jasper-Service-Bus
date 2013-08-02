package org.jasper.core.delegate;

import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
import java.util.Map;

import org.apache.jena.atlas.json.JsonArray;
import org.apache.jena.atlas.json.JsonObject;

import com.hp.hpl.jena.query.Query;
import com.hp.hpl.jena.query.QueryExecution;
import com.hp.hpl.jena.query.QueryExecutionFactory;
import com.hp.hpl.jena.query.QueryFactory;
import com.hp.hpl.jena.query.QuerySolution;
import com.hp.hpl.jena.query.ResultSet;
import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.ModelFactory;
import com.hp.hpl.jena.sparql.resultset.JSONOutput;
import com.hp.hpl.jena.sparql.resultset.JSONOutputResultSet;
import com.hp.hpl.jena.sparql.resultset.XMLOutput;

public class DelegateOntology {

	private static final String PREFIXS = 
			         "PREFIX hr:      <http://coralcea.ca/jasper/medicalSensor/heartRate/>" + "\n" +
		             "PREFIX :        <http://coralcea.ca/jasper/vocabulary/>" + "\n" +
					 "PREFIX bp:      <http://coralcea.ca/jasper/medicalSensor/bloodPressure/>" + "\n" +
					 "PREFIX jta:     <http://coralcea.ca/jasper/jta/>" + "\n" +
					 "PREFIX patient: <http://coralcea.ca/jasper/patient/>" + "\n" +
					 "PREFIX ms:      <http://coralcea.ca/jasper/medicalSensor/>" + "\n" +
					 "PREFIX bpData:  <http://coralcea.ca/jasper/medicalSensor/bloodPressure/data/>" + "\n" +
					 "PREFIX hrData:  <http://coralcea.ca/jasper/medicalSensor/heartRate/data/>" + "\n" +
					 "PREFIX jasper:  <http://coralcea.ca/jasper/>" + "\n";
	
	private Model model;
	
	public DelegateOntology(Model model){
		this.model = model;
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
				 PREFIXS +
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
				 PREFIXS +
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
				 PREFIXS +
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
				 PREFIXS +
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
				 PREFIXS +
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
	
	public String queryModel_backup(String queryString,String output){

		QueryExecution queryExecution = QueryExecutionFactory.create(queryString, model);
		Model result = queryExecution.execDescribe();
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		result.write(baos, output);
		
		System.out.println("######## queryModel Start");
		System.out.println(baos.toString());
		System.out.println("######## queryModel End");

		return baos.toString();

	}
	
	public String queryModel(String queryString,String output){

//		String newQueryString = 
//				 PREFIXS +
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
		System.out.println("######## queryString = " + queryString);

		QueryExecution queryExecution = QueryExecutionFactory.create(queryString, model);
		
		ResultSet resultSet = queryExecution.execSelect();
		
		JSONOutput jsonOutput = new JSONOutput();
		String result = jsonOutput.asString(resultSet);
		
		System.out.println("######## queryModel Start");
		System.out.println(result);
		System.out.println("######## queryModel End");
		
		return result;
	}

	public Model getModel() {
		return model;
	}
}
