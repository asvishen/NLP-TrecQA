package edu.asu.cse.nlp;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.net.URL;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;

import org.apache.jena.query.Query;
import org.apache.jena.query.QueryExecution;
import org.apache.jena.query.QueryExecutionFactory;
import org.apache.jena.query.QueryFactory;
import org.apache.jena.query.QuerySolution;
import org.apache.jena.query.ResultSet;
import org.apache.jena.rdf.model.Literal;
import org.apache.jena.rdf.model.RDFNode;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import com.github.jsonldjava.utils.Obj;
import com.mashape.unirest.http.HttpResponse;
import com.mashape.unirest.http.JsonNode;
import com.mashape.unirest.http.Unirest;
import com.mashape.unirest.http.exceptions.UnirestException;

import edu.stanford.nlp.trees.Tree;
import edu.stanford.nlp.util.StringUtils;

public class POS_tagger {

	private String subject;
	private String question;
	private String url;


	SentenceParser parser;


	private static HashMap<String, String> URI_map = new HashMap<String, String>();

	static
	{
		URI_map.put("Hale Bopp comet", "Comet_Hale-Bopp");
		URI_map.put("Rhodes scholars", "Rhodes_Scholarship");
		URI_map.put("agouti", "Common_agouti");
		URI_map.put("Black Panthers", "Black_Panther_Party");
		URI_map.put("prions", "Prion");
		URI_map.put("the band Nirvana", "Nirvana_(band)");
		URI_map.put("cataract", "Cataract");
		URI_map.put("boxer Floyd Patterson", "Floyd_Patterson");
		URI_map.put("architect Frank Gehry", "Frank_Gehry");
		URI_map.put("Harlem Globe Trotters", "Harlem_Globetrotters");
		URI_map.put("Abercrombie and Fitch", "Abercrombie_&_Fitch");
		URI_map.put("Tale of Genji", "The_Tale_of_Genji");
		URI_map.put("minstrel Al Jolson", "Al_Jolson");
		URI_map.put("Wiggles", "The_Wiggles");
		URI_map.put("Chester Nimitz", "Chester_W._Nimitz");
		URI_map.put("Nobel prize", "Nobel_Prize");
		URI_map.put("Bashar Assad", "Bashar_al-Assad");
		URI_map.put("Cassini space probe", "Cassini_Huygens");
		URI_map.put("Conde Nast", "Conde_Nast");
		URI_map.put("Eileen Marie Collins", "Eileen_Collins");
		URI_map.put("Liberty Bell 7", "Mercury-Redstone_4");
		URI_map.put("International Finance Corporation (IFC)", "International_Finance_Corporation");
		URI_map.put("philanthropist Alberto Vilar", "Alberto_Vilar");
		URI_map.put("senator Jim Inhofe", "Jim_Inhofe");
		URI_map.put("Berkman Center for Internet and Society", "Berkman_Center_for_Internet_&_Society");
		URI_map.put("boll weevil", "Boll_weevil");
		URI_map.put("space shuttles", "Space_Shuttle");
		URI_map.put("quarks", "Quarks");
	}


	public POS_tagger(String subject, String question) {
		this.subject = subject;
		this.question = question;
	}



	public SentenceParser getParser() {
		return parser;
	}

	public String getResult() throws JSONException, IOException, UnirestException, InterruptedException
	{
		String URI = setSubjectPageURI();
		String url = "<http://dbpedia.org/resource/" + URI + ">";

		//System.out.println("Question is ---" + this.question);
		RuntimeExec rt = new RuntimeExec();
		String questionType = rt.getType(this.question);

		String[] q = questionType.split("\n");
		String finalQuestionType = q[q.length-1];

		//System.out.println("question type ---- " + finalQuestionType);
		String[] qt = finalQuestionType.split(" ");


		HashSet<String> tags = getTags(URI);

		HashMap<String, Integer> dbpedia_tags = getDbpediaTags(tags);
		//System.out.println(dbpedia_tags.size());

		//classifying the tags
		HashMap<String, Integer> finalClassifiedTags = classifyTags(dbpedia_tags, qt[0]);
		//System.out.println(finalClassifiedTags.size());

		String queryResult = "The system cannot answer this now ! We are working on it :)";

		if(! finalClassifiedTags.isEmpty()){

			String final_tag = rankTags(finalClassifiedTags);
			//System.out.println("final tag" + final_tag);
			int rank = finalClassifiedTags.get(final_tag);
			System.out.println("in main rank is " + rank);

			if(rank > 0){

				try{

					queryResult = getSparqlQuery(final_tag, url);
					if(queryResult.contains("dbpedia.org")){

						String[] ch = queryResult.split("/");
						queryResult = ch[ch.length-1];
					}
					else{

						//System.out.println("here");
						String[] ch = queryResult.split("\\^");
						queryResult = ch[0];
					}
				}
				catch(Exception ex)
				{
					queryResult="The system cannot answer this now ! We are working on it :)";

				}
			}
			else{

				//System.out.println("here");
				queryResult=parseAbstract(url, questionType);

			}
		}
		else{



			queryResult=parseAbstract(url, questionType);

		}


		return queryResult;
	}

	public String setSubjectPageURI()
	{
		//System.out.println("this is here");
		String URI;

		if(URI_map.containsKey(subject)){

			URI = URI_map.get(subject);
		}
		else{

			URI = subject.replace(" ", "_");
		}


		return URI;
	}

	public HashSet<String> getTags(String subjectJson) throws IOException, JSONException{

		JSONObject json = readJsonFromUrl("http://dbpedia.org/data/"+subjectJson+".json");
		json = (JSONObject) json.get("http://dbpedia.org/resource/" + subjectJson);

		String [] jsonArray = (JSONObject.getNames(json));

		HashSet<String> tags = new HashSet<String>();

		for(String s : jsonArray){
			//System.out.println(s);
			tags.add(s);
		}

		// System.out.println("reached here");

		return tags;

	}

	public JSONObject readJsonFromUrl(String url) throws IOException, JSONException {
		InputStream is = new URL(url).openStream();
		try {
			BufferedReader rd = new BufferedReader(new InputStreamReader(is, Charset.forName("UTF-8")));
			String jsonText = readAll(rd);
			//System.out.println(jsonText);
			JSONObject json = new JSONObject(jsonText);
			return json;
		} finally {
			is.close();
		}
	}

	private static String readAll(Reader rd) throws IOException {
		StringBuilder sb = new StringBuilder();
		int cp;
		while ((cp = rd.read()) != -1) {
			sb.append((char) cp);
		}
		return sb.toString();
	}

	public HashMap<String, Integer> getDbpediaTags(HashSet<String> tags){

		HashMap<String, Integer> dbpedia_tags = new HashMap<>();

		for(String tag:tags){

			if(tag.contains("ontology")){

				String dbo_tag = tag.substring(28);
				//System.out.println(dbo_tag);

				dbo_tag = "dbo:" + dbo_tag;
				dbpedia_tags.put(dbo_tag, 0);
			}

			else if(tag.contains("property")){

				String dbp_tag = tag.substring(28);
				//System.out.println(dbp_tag);

				dbp_tag = "dbp:" + dbp_tag;
				dbpedia_tags.put(dbp_tag, 0);
			}
		}


		return dbpedia_tags;
	}

	public HashMap<String, Integer> classifyTags(HashMap<String, Integer> dbpedia_tags, String questionType) throws InterruptedException, IOException{

		System.out.println("Inside classify tags");

		HashMap<String, Integer> finalClassifiedTags = new HashMap<String, Integer>();
		RuntimeExec runTime = new RuntimeExec();

		for(String key:dbpedia_tags.keySet()){

			String[] jTag = key.split(":");
			String justTag = jTag[1];

			String[] splitTagPhrase = justTag.split("(?<=[a-z])(?=[A-Z])");
			String tagPhrase = "";

			for(String tp:splitTagPhrase){

				tagPhrase += tp + " ";
			}

			tagPhrase = tagPhrase.trim();
			String tagType = runTime.getType(tagPhrase);	

			String[] t = tagType.split("\n");
			String tType = t[t.length-1];
			String tt[] = tType.split(" ");
			String finalTagType = tt[0];

			if(finalTagType.toLowerCase().equals(questionType.toLowerCase())){

				finalClassifiedTags.put(key, 0);
			}

		}

		System.out.println("final classified tags : \n" + finalClassifiedTags.size());
		return finalClassifiedTags;
	}



	public String rankTags(HashMap<String, Integer> dbpedia_tags) throws UnirestException{

		String final_tag = null;

		parser = new SentenceParser();
		parser.setProcessedTree(question);

		List<Tree> vbnList = new ArrayList<Tree>();
		List<Tree> verbList = new ArrayList<Tree>();
		List<Tree> nnList = new ArrayList<Tree>();
		List<Tree> nnsList = new ArrayList<Tree>();
		List<Tree> vbdList = new ArrayList<Tree>();
		List<Tree> whList = new ArrayList<Tree>();


		vbnList = parser.getVbnList();
		verbList = parser.getVerbList();
		nnList = parser.getNnList();
		nnsList = parser.getNnsList();
		vbdList = parser.getVbdList();
		whList = parser.getWhList();



		List<String> lemma = new ArrayList<String>();

		//dictionary for wh words

		List<String> whenDict = new ArrayList<String>();
		whenDict.add("date");
		whenDict.add("year");

		List<String> whereDict = new ArrayList<String>();
		whereDict.add("place");
		whereDict.add("location");
		whereDict.add("residence");
		whereDict.add("hometown");

		//System.out.println("verb list" + verbList);


		for(Tree vbn : vbnList){

			Lemmatizer lemmaObject = new Lemmatizer();
			List<String> lst = lemmaObject.lemmatize(vbn.toString());
			lemma.add(lst.get(0));
		}


		//getting lemma of ver

		for(Tree vbn : verbList){

			Lemmatizer lemmaObject = new Lemmatizer();
			List<String> lst = lemmaObject.lemmatize(vbn.toString());
			lemma.add(lst.get(0));
		}

		for(Tree vbn : nnList){

			Lemmatizer lemmaObject = new Lemmatizer();
			List<String> lst = lemmaObject.lemmatize(vbn.toString());
			lemma.add(lst.get(0));
		}

		for(Tree vbn : nnsList){

			Lemmatizer lemmaObject = new Lemmatizer();
			List<String> lst = lemmaObject.lemmatize(vbn.toString());
			lemma.add(lst.get(0));
		}

		for(Tree vbn : vbdList){

			Lemmatizer lemmaObject = new Lemmatizer();
			List<String> lst = lemmaObject.lemmatize(vbn.toString());
			lemma.add(lst.get(0));
		}

		//System.out.println(lemma + "lemma ------------------------------");

		List<String> related_terms = new ArrayList<String>();

		//handling die

		int flag = 0;

		if(!verbList.isEmpty() && verbList.get(0).toString().toLowerCase().equals("die")){

			lemma.add("death");
			flag = 1;
		}


		for(String lem : lemma){

			try{
				HttpResponse<JsonNode> response = Unirest.get("https://wordsapiv1.p.mashape.com/words/"+lem+"/synonyms").header("X-Mashape-Key", "jbww4coyOHmshYmdYYBixq9DtwsYp1PgetcjsnmKRdjNTLbMQ8")
						.header("Content-Type", "application/x-www-form-urlencoded")
						.header("Accept", "application/json").asJson();
				//System.out.println(response.getBody());

				JSONObject obj = new JSONObject(response.getBody().toString());
				JSONArray arr = (JSONArray)obj.get("synonyms");

				for(int i=0; i<arr.length(); i++)
					related_terms.add(arr.get(i).toString());

				HttpResponse<JsonNode> response_der = Unirest.get("https://wordsapiv1.p.mashape.com/words/"+lem+"/derivation").header("X-Mashape-Key", "jbww4coyOHmshYmdYYBixq9DtwsYp1PgetcjsnmKRdjNTLbMQ8")
						.header("Content-Type", "application/x-www-form-urlencoded")
						.header("Accept", "application/json").asJson();

				JSONObject obj_der = new JSONObject(response_der.getBody().toString());
				//System.out.println(response_der.getBody());
				JSONArray arr_der = (JSONArray)obj_der.get("derivation");

				for(int i=0; i<arr_der.length(); i++)
					related_terms.add(arr_der.get(i).toString());


			}

			catch(Exception e){

				e.printStackTrace();
			}


		}


		//System.out.println(related_terms);


		for(String tag :dbpedia_tags.keySet()){

			//checking wh word

			if(! whList.isEmpty()){

				//when 

				if(whList.get(0).toString().toLowerCase().equals("when")){

					if(flag == 1)
						related_terms.remove("cause");


					for(String word : whenDict){

						if(tag.toLowerCase().contains(word.toLowerCase())){

							dbpedia_tags.put(tag, dbpedia_tags.get(tag)+1*2);
							//System.out.println(dbpedia_tags.get(tag)+ "------");
						}
					}
				}

				//where

				if(whList.get(0).toString().toLowerCase().equals("where")){




					for(String word : whereDict){

						if(tag.toLowerCase().contains(word.toLowerCase())){

							dbpedia_tags.put(tag, dbpedia_tags.get(tag)+1*2);
							//System.out.println(dbpedia_tags.get(tag)+ "------");
						}
					}
				}



			}

			//rank acc to vbn list

			if(! vbnList.isEmpty()){

				for(Tree verb: vbnList){

					String v = verb.toString();

					if(tag.toLowerCase().contains(v.toLowerCase())){


						dbpedia_tags.put(tag, dbpedia_tags.get(tag)+1*4);
						//System.out.println(dbpedia_tags.get(tag)+ "------");
					}
				}
			}

			//rank acc to verb list

			if(! verbList.isEmpty()){

				for(Tree verb: verbList){

					String v = verb.toString();

					if(tag.toLowerCase().contains(v.toLowerCase())){

						dbpedia_tags.put(tag, dbpedia_tags.get(tag)+1*4);
						//System.out.println(dbpedia_tags.get(tag)+ "------");
					}
				}
			}

			//rank acc to NN

			if(! nnList.isEmpty()){

				for(Tree noun: nnList){

					String n = noun.toString();

					if(tag.toLowerCase().contains(n.toLowerCase())){

						dbpedia_tags.put(tag, dbpedia_tags.get(tag)+1*4);
						//System.out.println(dbpedia_tags.get(tag)+ "------");
					}
				}
			}

			// rank according to NNS

			if(! nnsList.isEmpty()){

				for(Tree noun: nnsList){

					String n = noun.toString();

					if(tag.toLowerCase().contains(n.toLowerCase())){

						dbpedia_tags.put(tag, dbpedia_tags.get(tag)+1*4);
						//System.out.println(dbpedia_tags.get(tag)+ "------");
					}
				}
			}

			if(! lemma.isEmpty()){

				for(String lem:lemma){

					if(tag.toLowerCase().contains(lem.toLowerCase())){

						dbpedia_tags.put(tag, dbpedia_tags.get(tag)+1*3);
						//System.out.println(dbpedia_tags.get(tag)+ "------");
					}
				}

			}

			if(! related_terms.isEmpty()){

				for(String s : related_terms){

					if(tag.toLowerCase().contains(s.toLowerCase())){

						dbpedia_tags.put(tag, dbpedia_tags.get(tag)+1);
						//System.out.println(dbpedia_tags.get(tag)+ "------");

					}
				}
			}


		}

		//selecting the tag with highest value. 
		int max = -99;
		final_tag = null;

		for(String tag:dbpedia_tags.keySet()){

			if(dbpedia_tags.get(tag) > max){
				max = dbpedia_tags.get(tag);
				final_tag = tag;
			}
		}


		//System.out.println("RANK " + dbpedia_tags.get(final_tag));
		return final_tag;
	}

	public String getSparqlQuery(String final_tag, String url){


		String sparql_query = "SELECT ?variable WHERE {" + url + " " + final_tag + " ?variable }";
		String stringQuery = "PREFIX dbo: <http://dbpedia.org/ontology/> " +
				"PREFIX dbp: <http://dbpedia.org/property/>" +
				"PREFIX geo: <http://www.w3.org/2003/01/geo/wgs84_pos#> " 
				+"PREFIX rdfs: <http://www.w3.org/2000/01/rdf-schema#>"+ sparql_query;

		System.out.println(stringQuery);

		Query query = QueryFactory.create(stringQuery);
		QueryExecution qexec = QueryExecutionFactory.sparqlService("http://dbpedia.org/sparql", query);

		ResultSet results = qexec.execSelect();
		QuerySolution soln = results.nextSolution();

		RDFNode l = soln.get("variable");
		//		Literal l = (Literal) soln.get("variable");
		//		System.out.println(l);
		qexec.close() ;
		return l.toString();


	}

	public String parseAbstract(String url, String questionType) throws UnirestException{

		String sparql_query = "SELECT ?variable WHERE {" + url + " dbo:abstract ?variable FILTER langMatches(lang(?variable) ,\"EN\") . }";
		String stringQuery = "PREFIX dbo: <http://dbpedia.org/ontology/> " +
				"PREFIX dbp: <http://dbpedia.org/property/>" +
				"PREFIX geo: <http://www.w3.org/2003/01/geo/wgs84_pos#> " 
				+"PREFIX rdfs: <http://www.w3.org/2000/01/rdf-schema#>"+ sparql_query;

		//System.out.println(stringQuery);

		Query query = QueryFactory.create(stringQuery);
		QueryExecution qexec = QueryExecutionFactory.sparqlService("http://dbpedia.org/sparql", query);

		ResultSet results = qexec.execSelect();
		QuerySolution soln = results.nextSolution();

		//RDFNode l = soln.get("variable");
		Literal l = (Literal) soln.get("variable");
		//		System.out.println(l);
		qexec.close() ;
		String abs = l.toString();
		//System.out.println("ABSTRACT == " + abs);

		String[] absLines = abs.split("\\.");

		//parsing the question
		parser = new SentenceParser();
		parser.setProcessedTree(question);

		List<Tree> vbnList = new ArrayList<Tree>();
		List<Tree> verbList = new ArrayList<Tree>();
		List<Tree> nnList = new ArrayList<Tree>();
		List<Tree> nnsList = new ArrayList<Tree>();
		List<Tree> vbdList = new ArrayList<Tree>();
		List<Tree> whList = new ArrayList<Tree>();
		List<Tree> adjList = new ArrayList<Tree>();


		vbnList = parser.getVbnList();
		verbList = parser.getVerbList();
		nnList = parser.getNnList();
		nnsList = parser.getNnsList();
		vbdList = parser.getVbdList();
		whList = parser.getWhList();
		adjList = parser.getAdjList();

		String ans = null;

		if(! whList.isEmpty()){

			String whQuestion = whList.get(0).toString();

			if(whQuestion.toLowerCase().equals("when")){



				for(String line : absLines){

					if(! vbnList.isEmpty()){

						if(line.contains(vbnList.get(0).toString())){

							ans = findDate(line);
							break;
						}
					}

					if(! adjList.isEmpty()){

						if(! adjList.isEmpty() && ! nnList.isEmpty()){

							if(line.contains(adjList.get(0).toString()) && line.contains(nnList.get(0).toString())){

								ans = findDate(line);
								break;
							}

						}


					}
					else{

						if(! nnList.isEmpty()){

							if(line.contains(nnList.get(0).toString())){

								ans = findDate(line);
								break;
							}
						}
					}

					//whenAns = findFirstDate(absLines);


				}
			}

			if(whQuestion.toLowerCase().equals("where")){
				
				int vFlag = 0;
				int nFlag = 0;
				
				for(String line:absLines){
					
					if(! verbList.isEmpty()){
						
						if(line.contains(verbList.get(0).toString())){
							
							vFlag = 1;
							ans = getLocation(line);
							break;
						}
					}
					
					if(! nnList.isEmpty()){
						
						if(line.contains(nnList.get(0).toString())){
							
							nFlag = 1;
							ans = getLocation(line);
							break;
						}
					}
				
				}
				
				if(vFlag == 0 && nFlag == 0){
					
					for(String li : absLines){
						
						li = li.replace(" ", "+");
						li = li.replace(",", "%2c");
						HttpResponse<JsonNode> response = Unirest.get("https://webknox-text-processing.p.mashape.com/text/locations?text=" + li)
								.header("X-Mashape-Key", "19RiyMYdg0mshnjhf293boQnBnvqp1HKSiojsn3fF2JXZ5vcHK")
								.header("Accept", "application/json")
								.asJson();
						
						if(response.getBody().toString().length() > 2){
							
							String result = "{ \"result\" : " + response.getBody().toString() + "}";
							JSONObject resp = new JSONObject(result);
							JSONArray arr = (JSONArray) resp.get("result");
							resp = (JSONObject) arr.get(0);
							ans = (String) resp.get("name");
							return ans;
						}
					}
				}
				
			}
			
			if(whQuestion.toLowerCase().equals("who")){
				
				int vbdFlag =0;
				int nnFlag = 0;
				int nnsFlag = 0;
				
				for(String line: absLines){
					
					if(! vbdList.isEmpty()){
						
						if(line.contains(vbdList.get(0).toString())){
							
							//vbdFlag = 1;
							ans = getPerson(line);
							break;
						}
					}
					
					if(! nnList.isEmpty()){
						
						if(line.contains(nnList.get(0).toString())){
							
							ans = getPerson(line);
							break;
						}
					}
					
				
				}
			}
		}

		return(ans);

	}

	

	public String findDate(String line){

		String date = null;
		line= line.trim();
		System.out.println(line);
		line = line.replace(" ", "+");
		line = line.replace(",", "%2C");

		return date;
	}

	public String getDate(String line){
		String[] words = line.split(" ");
		System.out.println("line"+  line);
		System.out.println("words is "+ words.length);

		for(String str : words){
			System.out.println("string is :"+ str);
			if(str.length() > 2 && StringUtils.isNumeric(str.substring(0, 1))){
				return str;
			}
		}return null;
	}
	
	public String getLocation(String line) throws UnirestException{
		
		line = line.replace(" ", "+");
		line.replace(",", "%2c");
		String ans = null;
		
		HttpResponse<JsonNode> response = Unirest.get("https://webknox-text-processing.p.mashape.com/text/locations?text=" + line)
				.header("X-Mashape-Key", "19RiyMYdg0mshnjhf293boQnBnvqp1HKSiojsn3fF2JXZ5vcHK")
				.header("Accept", "application/json")
				.asJson();
		String result = "{ \"result\" : " + response.getBody().toString() + "}";
		JSONObject resp = new JSONObject(result);
		JSONArray arr = (JSONArray) resp.get("result");
		resp = (JSONObject) arr.get(0);
		ans = (String) resp.get("name");
		
		//ans = arr.get(0).toString();
		System.out.println("location ans---" + ans);
		
		return ans;
	}
	
	public String getPerson(String line) throws UnirestException{
		
		line = line.replace(" ", "+");
		line = line.replace(",", "%2c");
		String ans = null;
		
		HttpResponse<JsonNode> response = Unirest.get("https://webknox-text-processing.p.mashape.com/text/entities?text=This+text+contains+entities+such+as+the+singer+John+Hiatt+and+the+actor+Jim+Carry.")
				.header("X-Mashape-Key", "r2iAzEK2ilmshMy6isqkHL9j8UiJp1XMo3ojsn1IMggY2xD7DK")
				.header("Accept", "application/json")
				.asJson();
		
		String result = "{ \"result\" : " + response.getBody().toString() + "}";
		JSONObject resp = new JSONObject(result);
		JSONArray arr = (JSONArray)resp.get("result");
		
		if(resp.get("type").equals("PER")){
			ans = (String) resp.get("entity");
		}
		
		return ans;
	}

	//	public String findFirstDate(String[] absLines){
	//		
	//		String date = null;
	//		
	//		for(String line:)
	//		
	//		return date;
	//	}



	//	public String getResultForQuestion(){
	//
	//		String whQuestion = parser.getWhList().get(0).toString();
	//
	//		String result= "";
	//
	//		if(whQuestion.equals("When")){
	//
	//			//System.out.println("Inside When");
	//
	//			result = getWhenResults();
	//
	//		}
	//		return result;
	//	}


	//	public String getWhenResults(){
	//
	//		//String subject = "James Dean";
	//
	//		String sparql_query="";
	//		String result;
	//
	//		if()
	//		else
	//		{
	//			String ans = getResultFromAbstract();
	//			if(ans.endsWith(",")){
	//				ans = ans.substring(0, ans.length()-1);
	//			}
	//			return ans;
	//		}
	//
	//		return getSpaqrlQueryResponse(sparql_query);
	//	}

	//	public String getSpaqrlQueryResponse(String sparql_query)
	//	{
	//		String stringQuery = "PREFIX dbo: <http://dbpedia.org/ontology/> " +
	//				"PREFIX dbp: <http://dbpedia.org/property/>" +
	//				"PREFIX geo: <http://www.w3.org/2003/01/geo/wgs84_pos#> " 
	//				+"PREFIX rdfs: <http://www.w3.org/2000/01/rdf-schema#>"+ sparql_query;
	//
	//		// System.out.println(stringQuery);
	//
	//		Query query = QueryFactory.create(stringQuery);
	//		QueryExecution qexec = QueryExecutionFactory.sparqlService("http://dbpedia.org/sparql", query);
	//
	//		ResultSet results = qexec.execSelect();
	//		QuerySolution soln = results.nextSolution();
	//		Literal l = (Literal) soln.get("variable");
	//		qexec.close() ;
	//		return l.getString();
	//	}

	//	public String getResultFromAbstract(){
	//		StringBuilder finalQuery = new StringBuilder();
	//		String query1 = "SELECT ?variable WHERE {";
	//		String query2 = " dbo:abstract ?variable ";
	//		String filter = "FILTER(langMatches(lang(?variable),\"EN\"))}";
	//
	//		finalQuery.append(query1).append(url).append(query2).append(filter);
	//		String query = finalQuery.toString();
	//
	//		String abs = getSpaqrlQueryResponse(query);
	//		String[] abstractLines = abs.split("\\.\\s");
	//
	//		SentenceParser parser = new SentenceParser();
	//		String date = null;
	//
	//		boolean found = false;
	//		for(String s : abstractLines){
	//			parser.setProcessedTree(s);
	//			if(!parser.getVbnList().isEmpty()){
	//				found = checkVerb(parser);
	//				if(found){
	//					//verbFound = true;
	//					date = getDate(s);
	//					if(date != null)
	//						return date;
	//					//System.out.println(date);
	//				}	
	//			}
	//		}
	//
	//		for(String s : abstractLines){
	//			parser = new SentenceParser();
	//			parser.setProcessedTree(s);
	//			if(!parser.getAdjList().isEmpty()){
	//				found = checkAdjective(parser);
	//				if(found){
	//					date = getDate(s);
	//					if(date != null)
	//						return date;
	//				}
	//			}
	//		}
	//
	//		for(String s : abstractLines){
	//			parser = new SentenceParser();
	//
	//			parser.setProcessedTree(s);
	//			if(!parser.getNnList().isEmpty()){
	//				found = checkNoun(parser);
	//				if(found){
	//					date = getDate(s);
	//					if(date != null)
	//						return date;
	//				}
	//			}
	//		}
	//
	//		if(date == null) {
	//			for(String s : abstractLines){
	//				date = getDate(s);
	//				if(date != null)
	//					return date;
	//			}
	//		}
	//		return "";
	//	}
	//
	//	public boolean checkVerb(SentenceParser parser){
	//		List<Tree> vbnQues = getParser().getVbnList();
	//		List<Tree> vbnAbstract = parser.getVbnList();
	//		//List<Tree> nnsAbstract = parser.getNnsList();
	//
	//		boolean wordFound = false;
	//		Tree t = null;
	//
	//		if (!vbnQues.isEmpty()){
	//			t=vbnQues.get(0);
	//
	//			for(int i=0; i< vbnAbstract.size();i++)
	//			{
	//				if(vbnAbstract.get(i)!=null && t.toString().equals(vbnAbstract.get(i).toString()))
	//				{	
	//					wordFound = true;
	//					break;
	//					//List<Tree> npList = parser.getNpList();
	//					//System.out.println(npList);
	//				}
	//			}	    	
	//			//System.out.println("wordFound is : " + wordFound);
	//			return wordFound;
	//		}return false;
	//	}
	//
	//
	//	public boolean checkAdjective(SentenceParser parser){
	//
	//		boolean wordFound = false;
	//		List<Tree> adjAbstract = parser.getAdjList();
	//		Tree t = null;
	//
	//		if (!getParser().getAdjList().isEmpty()){
	//			t=getParser().getAdjList().get(0);
	//
	//			for(int i=0; i<adjAbstract.size(); i++){
	//				if(adjAbstract.get(i)!=null && t.toString().equals(adjAbstract.get(i).toString()))
	//				{
	//					wordFound = true;
	//					break;
	//					//List<Tree> npList = parser.getNpList();
	//					//System.out.println(npList);
	//				}
	//			}
	//
	//			return wordFound;
	//		}return false;
	//	}
	//
	//
	//	public boolean checkNoun(SentenceParser parser){
	//
	//		boolean wordFound = false;
	//		Tree t = null;
	//		List<Tree> nnAbstract = parser.getNnList();
	//
	//		if(!getParser().getNnList().isEmpty()){
	//			t = getParser().getNnList().get(0);
	//
	//			for(int i=0; i<nnAbstract.size(); i++){
	//				if(nnAbstract.get(i)!=null && t.toString().equals(nnAbstract.get(i).toString()))
	//				{	
	//					wordFound = true;
	//					break;
	//
	//					//List<Tree> npList = parser.getNpList();
	//					//System.out.println(npList);
	//				}
	//			}
	//			return wordFound;
	//		}return false;
	//	}

}
