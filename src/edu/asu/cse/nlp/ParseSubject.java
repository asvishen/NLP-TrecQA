package edu.asu.cse.nlp;

import org.apache.jena.query.Query;
import org.apache.jena.query.QueryExecution;
import org.apache.jena.query.QueryExecutionFactory;
import org.apache.jena.query.QueryFactory;
import org.apache.jena.query.QuerySolution;
import org.apache.jena.query.ResultSet;
import org.apache.jena.rdf.model.Literal;

public class ParseSubject {
	
	 public static void main(String[] args)
	 {
		 String arg1,arg2;
		 
		 String  sparql_query = "PREFIX rdfs: <http://www.w3.org/2000/01/rdf-schema#> select ?o  where { ?o rdfs:label ?l. "
			 							+ "?l bif:contains \"band\". " 
			 									+ "FILTER(regex(?o,\"Nirvana\")). }";
	    	Query query = QueryFactory.create(sparql_query);
	    	QueryExecution qexec = QueryExecutionFactory.sparqlService("http://dbpedia.org/sparql", query);

	    	ResultSet results = qexec.execSelect();
	    	QuerySolution soln = results.nextSolution();
	    	Literal l = (Literal) soln.get("o");
	    	qexec.close() ;
	    	System.out.println(l.getString());
	 }

}
