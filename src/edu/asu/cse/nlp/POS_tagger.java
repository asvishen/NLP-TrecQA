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
	private String result;


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
		StringBuffer buff = new StringBuffer();
		System.out.println("Building URI from the subject ....");
		String URI = setSubjectPageURI();
		boolean abstractFlag = false;
		String url = "<http://dbpedia.org/resource/" + URI + ">";

		System.out.println("Getting question type from the classifier");
		RuntimeExec rt = new RuntimeExec();
		System.out.println("Question that is bein classified" + this.question);
		String questionType = rt.getType(this.question);

		String[] q = questionType.split("\n");
		String finalQuestionType = q[q.length-1];

		//System.out.println("question type ---- " + finalQuestionType);
		String[] qt = finalQuestionType.split(" ");

		buff.append("{ \"CoarseType\" : \""+ qt[0] + " \", \"FineType\" : \"" + qt[1] + "\",");

		result = buff.toString();
		
		System.out.println("Getting corresponding DBpedia tags");
		HashSet<String> tags = getTags(URI);

		HashMap<String, Integer> dbpedia_tags = getDbpediaTags(tags);
		//System.out.println(dbpedia_tags.size());
		String queryResult = "The system cannot answer this now ! We are working on it :)";

		if(this.question.toLowerCase().contains("what")){

			System.out.println("question is a what question .. ");

			//String queryResult = "The system cannot answer this now ! We are working on it :)";

			String final_tag = rankTags(dbpedia_tags, this.question);
			System.out.println("FINAL TAG " + final_tag);
			int rank = dbpedia_tags.get(final_tag);
			System.out.println("RANK FORMED -- " + rank);

			if(rank > 0){

				System.out.println("Rank not zero ... generating sparql query .... ");

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
					ex.printStackTrace();

				}
			}
			//			else{
			//
			//				System.out.println("Rank is 0 ... parsing abstract");
			//				queryResult=parseWhatAbstract(url, questionType);
			//
			//			}
			
		}
		else
		{
			//classifying the tags
			System.out.println("classifying the tags");
			HashMap<String, Integer> finalClassifiedTags = classifyTags(dbpedia_tags, qt[0]);
			//System.out.println(finalClassifiedTags.size());

			

			if(! finalClassifiedTags.isEmpty()){

				System.out.println("Ranking the tags ....");
				String final_tag = rankTags(finalClassifiedTags, this.question);
				if(final_tag == "dbo:birthPlace"){
					finalClassifiedTags.put("dbo:birthPlace", 10);
				}
				int rank = finalClassifiedTags.get(final_tag);

				if(rank > 0){

					System.out.println("Rank not zero ... generating sparql query .... ");

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
						ex.printStackTrace();

					}
				}
				else{

					System.out.println("Rank is 0 ... parsing abstract");
					abstractFlag = true;

					queryResult=parseAbstract(url, questionType);

				}
			}
			else{


				System.out.println("No tags of the same category ... parsing abstract ... ");
				result += "\"tags\": [{}]";
				abstractFlag = true;

				queryResult=parseAbstract(url, questionType);

			}


			
		}
		if(queryResult.contains("_"))
		{
				queryResult =  queryResult.replace("_", " ");
		}
		if(queryResult.contains("@"))
		{
			String[] splitted =  queryResult.split("@");
			queryResult = splitted[0];

		}
		
		String abs = (abstractFlag == true) ? "Yes" : "No" ; 
		result = result + ",\"abstract\" : " + "\"" + abs + "\"";
		result = result + ",\"answer\" : \"" + queryResult + "\" }";
		System.out.println("JSON result is : " +result);
		JSONObject obj = new JSONObject(result);

		return obj.toString();
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

		System.out.println("Final question type ---- " + questionType);
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
			System.out.println("The tag that is being classified --- " + tagPhrase);

			String[] t = tagType.split("\n");
			String tType = t[t.length-1];
			String tt[] = tType.split(" ");
			String finalTagType = tt[0];

			System.out.println("Final tag type ---- " + finalTagType);


			if(finalTagType.toLowerCase().equals(questionType.toLowerCase())){

				finalClassifiedTags.put(key, 0);
			}

		}

		System.out.println("final classified tags : \n" + finalClassifiedTags.size());
		return finalClassifiedTags;
	}

	//	public String rankWhatTags(HashMap<String, Integer> tagMap, String question){
	//		
	//		String final_tag = null;
	//		System.out.println("Inside rank what tags");
	//		System.out.println("Question is -- " + question);
	//		
	//		HashMap<String, String> mapDict = new HashMap<String, String>();
	//		mapDict.put("ethnic", "enthnicmakeup");
	//		mapDict.put("group", "band");
	//		mapDict.put("stand", "formername");
	//		
	//		return final_tag;
	//	}



	public String rankTags(HashMap<String, Integer> dbpedia_tags, String question) throws UnirestException{

		System.out.println("Inside rank tags");

		String final_tag = null;

		parser = new SentenceParser();
		parser.setProcessedTree(question);

		System.out.println("Parsing question ... ");

		List<Tree> vbnList = new ArrayList<Tree>();
		List<Tree> verbList = new ArrayList<Tree>();
		List<Tree> nnList = new ArrayList<Tree>();
		List<Tree> nnsList = new ArrayList<Tree>();
		List<Tree> vbdList = new ArrayList<Tree>();
		List<Tree> whList = new ArrayList<Tree>();
		List<Tree> wpList = new ArrayList<Tree>();
		List<Tree> wdtList = new ArrayList<Tree>();


		vbnList = parser.getVbnList();
		verbList = parser.getVerbList();
		nnList = parser.getNnList();
		nnsList = parser.getNnsList();
		vbdList = parser.getVbdList();
		whList = parser.getWhList();
		wpList = parser.getWpList();
		wdtList = parser.getWdtList();

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

		List<String> whoDict = new ArrayList<String>();
		whoDict.add("parents");
		whoDict.add("spouse");
		whoDict.add("keypeople");

		HashMap<String, String> whatDict = new HashMap<String, String>();
		whatDict.put("stand", "dbp:formerName");
		whatDict.put("durst", "dbp:associatedActs");
		whatDict.put("music", "dbo:genre");
		whatDict.put("hit", "dbp:title");
		whatDict.put("alien", "dbp:affiliation");
		whatDict.put("group", "dbp:associatedActs");
		whatDict.put("real", "dbp:birthName");
		whatDict.put("original", "dbp:birthName");
		whatDict.put("profession", "dbo:occupation");
		whatDict.put("famous", "dbo:knownFor");
		whatDict.put("original", "dbp:birthName");
		whatDict.put("war", "dbp:battles");
		whatDict.put("insect", "dbo:order");


		//System.out.println("verb list" + verbList);

		System.out.println("Getting lemmas ... ");
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

		System.out.println("Getting related terms");

		List<String> related_terms = new ArrayList<String>();

		//handling die

		int flag = 0;

		if(!verbList.isEmpty() && verbList.get(0).toString().toLowerCase().equals("die")){

			lemma.add("death");
			flag = 1;
		}



		for(String lem : lemma){

			try{
				if(lem.contains("\\/"))
				{
					String[] spl = lem.split("\\/");

					lem = spl[0];
				}
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
				System.out.println(lem);
				e.printStackTrace();
			}


		}

		String whQuestion = "";
		if(! whList.isEmpty())
			whQuestion = whList.get(0).toString().toLowerCase();
		if(! wpList.isEmpty())
			whQuestion = wpList.get(0).toString().toLowerCase();
		if(! wdtList.isEmpty())
			whQuestion = wdtList.get(0).toString().toLowerCase();

		//System.out.println(related_terms);

		if(whQuestion.equals("what")){

			System.out.println("what question dictionary");
			for(String word : whatDict.keySet()){

				if(question.toLowerCase().contains(word.toLowerCase())){

					String tag = whatDict.get(word);
					dbpedia_tags.put(tag, dbpedia_tags.get(tag)+1*10);
				}
			}

		}


		for(String tag :dbpedia_tags.keySet()){

			//checking wh word
			System.out.println("Ranking tags ... ");

			if(! whList.isEmpty() || !wpList.isEmpty() || ! wdtList.isEmpty()){



				//when 

				if(whQuestion.equals("when")){


					System.out.println("when question dictionary");

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

				if(whQuestion.equals("where")){

					System.out.println("where question dictionary");


					for(String word : whereDict){

						if(tag.toLowerCase().contains(word.toLowerCase())){

							dbpedia_tags.put(tag, dbpedia_tags.get(tag)+1*2);
							//System.out.println(dbpedia_tags.get(tag)+ "------");
						}
					}
				}

				if(whQuestion.equals("who")){

					System.out.println("who question dictionary");


					for(String word : whoDict){

						if(tag.toLowerCase().contains(word.toLowerCase())){

							dbpedia_tags.put(tag, dbpedia_tags.get(tag)+1*2);
							//System.out.println(dbpedia_tags.get(tag)+ "------");
						}
					}
				}



			}

			//rank acc to vbn list
			System.out.println("rank acc to vbn list");
			if(! vbnList.isEmpty()){

				for(Tree verb: vbnList){

					String v = verb.toString();

					if(tag.toLowerCase().contains(v.toLowerCase())){


						dbpedia_tags.put(tag, dbpedia_tags.get(tag)+1*5);
						//System.out.println(dbpedia_tags.get(tag)+ "------");
					}
				}
			}

			//rank acc to verb list
			System.out.println("rank acc to verb list");


			if(! verbList.isEmpty()){

				for(Tree verb: verbList){

					String v = verb.toString();

					if(tag.toLowerCase().contains(v.toLowerCase())){

						dbpedia_tags.put(tag, dbpedia_tags.get(tag)+1*5);
						//System.out.println(dbpedia_tags.get(tag)+ "------");
					}
				}
			}

			//rank acc to NN
			System.out.println("rank acc to nn list");


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
			System.out.println("rank acc to nns list");


			if(! nnsList.isEmpty()){

				for(Tree noun: nnsList){

					String n = noun.toString();

					if(tag.toLowerCase().contains(n.toLowerCase())){

						dbpedia_tags.put(tag, dbpedia_tags.get(tag)+1*4);
						//System.out.println(dbpedia_tags.get(tag)+ "------");
					}
				}
			}

			System.out.println("rank acc to lemma list");


			if(! lemma.isEmpty()){

				for(String lem:lemma){

					if(tag.toLowerCase().contains(lem.toLowerCase())){

						dbpedia_tags.put(tag, dbpedia_tags.get(tag)+1*3);
						//System.out.println(dbpedia_tags.get(tag)+ "------");
					}
				}

			}
			System.out.println("rank acc to related terms list");


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
		System.out.println("selecting tag with the highest value");

		if(!whList.isEmpty() && !vbnList.isEmpty() && whList.get(0).toString().toLowerCase().equals("where") && vbnList.get(0).toString().toLowerCase().equals("born")){
			final_tag = "dbo:birthPlace";
			result = result + "\"tags\": [{\"dbo:birthPlace\" : \"7\" }]";
			//dbpedia_tags.put("dbo:birthPlace", value)
			//System.out.println("here");
		}

		else{

			StringBuffer buff = new StringBuffer();
			buff.append("\"tags\": [" );

			//selecting the tag with highest value. 
			int max = -99;
			final_tag = null;

			for(String tag:dbpedia_tags.keySet()){
				buff.append("{\"").append(tag).append("\" : ");
				if(dbpedia_tags.get(tag) > max){
					max = dbpedia_tags.get(tag);
					final_tag = tag;
				}
				buff.append("\"").append(dbpedia_tags.get(tag)).append("\"}");
				buff.append(",");
			}
			String tags = buff.substring(0, buff.length()-1);
			tags = tags + "]";

			result = result+tags;
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
		System.out.println("retrieving abstract");


		String[] absLines = abs.split("\\.");

		//parsing the question
		System.out.println("parsing question");

		parser = new SentenceParser();
		parser.setProcessedTree(question);

		List<Tree> vbnList = new ArrayList<Tree>();
		List<Tree> verbList = new ArrayList<Tree>();
		List<Tree> nnList = new ArrayList<Tree>();
		List<Tree> nnsList = new ArrayList<Tree>();
		List<Tree> vbdList = new ArrayList<Tree>();
		List<Tree> whList = new ArrayList<Tree>();
		List<Tree> adjList = new ArrayList<Tree>();
		List<Tree> wpList = new ArrayList<Tree>();
		List<Tree> exList = new ArrayList<Tree>();


		vbnList = parser.getVbnList();
		verbList = parser.getVerbList();
		nnList = parser.getNnList();
		nnsList = parser.getNnsList();
		vbdList = parser.getVbdList();
		whList = parser.getWhList();
		adjList = parser.getAdjList();
		wpList = parser.getWpList();
		exList = parser.getexList();

		String ans ="The system cannot answer this now ! We are working on it :)";
;

		System.out.println("wh Questions .. ");
		System.out.println(whList);
		System.out.println("wp Questions .. ");
		System.out.println(wpList);


		if(! whList.isEmpty() || !wpList.isEmpty()){

			System.out.println("Into parsing abstract");
			String whQuestion = "";

			if(! whList.isEmpty())
				whQuestion = whList.get(0).toString();
			if(! wpList.isEmpty())
				whQuestion = wpList.get(0).toString();

			System.out.println("Question is a -- " + whQuestion);

			if(whQuestion.toLowerCase().equals("when")){

				System.out.println("When Question encountered --------------");

				for(String line : absLines){

					// System.out.println("Line is :" + line);


					if(! vbnList.isEmpty()){

						// System.out.println(vbnList.get(0));

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

					if(! nnList.isEmpty()){

						if(line.contains(nnList.get(0).toString())){

							ans = findDate(line);
							break;
						}
					}
					else{
						// System.out.println("Finding first date *************");

						ans = findFirstDate(line);
						if(! StringUtils.matches(ans, "[1-9][0-9][0-9][0-9][,.:;\"\']*") && ans.length() < 3){

						}
						else{
							break;
						}

					}
				}
			}

			if(whQuestion.toLowerCase().equals("where")){

				System.out.println("parsing abstract for where question");


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
						HttpResponse<JsonNode> response = null;
						try{
							response = Unirest.get("https://webknox-text-processing.p.mashape.com/text/locations?text=" + li)
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
						catch(Exception ex)
						{
							ex.printStackTrace();
						}

					
					}
				}

			}

			if(whQuestion.toLowerCase().equals("who")){

				System.out.println("parsing abstract for who question");

				int vbdFlag =0;
				int nnFlag = 0;
				int nnsFlag = 0;

				for(String line: absLines){

					if(! vbdList.isEmpty()){

						if(line.contains(vbdList.get(0).toString())){

							vbdFlag = 1;
							List<String> persons = getPersonList(line);
							for(String per: persons){

								ans += per + " ";
							}
							break;
						}
					}

					if(! nnList.isEmpty()){

						if(line.contains(nnList.get(0).toString())){

							nnFlag = 1;
							List<String> persons = getPersonList(line);
							for(String per: persons){

								ans += per + " ";
							}
							break;
						}
					}

					if(! nnsList.isEmpty()){

						if(line.contains(nnsList.get(0).toString())){

							nnsFlag = 1;
							List<String> persons = getPersonList(line);
							for(String per: persons){

								ans += per + " ";
							}
							break;
						}
					}

				}

				if(vbdFlag ==0 && nnFlag ==0 && nnsFlag ==0){

					for(String li:absLines){

						int foundFlag = 0;
						li = li.trim();
						li = li.replace(" ", "+");
						li.replace(",", "%2c");
						li = li.replace("\"", "%22");

						HttpResponse<JsonNode> response = Unirest.get("https://webknox-text-processing.p.mashape.com/text/entities?text="+li)
								.header("X-Mashape-Key", "r2iAzEK2ilmshMy6isqkHL9j8UiJp1XMo3ojsn1IMggY2xD7DK")
								.header("Accept", "application/json")
								.asJson();
						String result = "{ \"result\" : " + response.getBody().toString() + "}";
						JSONObject resp = new JSONObject(result);
						JSONArray arr = (JSONArray)resp.get("result");

						int len = arr.length();
						if(arr.length() == 0)
							break;
						for(int i=0; i<len; i++){

							JSONObject tempObj = arr.getJSONObject(i);
							//System.out.println(tempObj.toString());
							if(tempObj.get("type").equals("PER")){

								ans = (String) tempObj.get("entity");
								foundFlag = 1;
								break;

							}
						}

						if(foundFlag == 1)
							break;
					}
				}
			}

			if(whQuestion.toLowerCase().equals("how")){

				System.out.println("Parsing abstract for how question");

				for(String line:absLines){

					if(! nnsList.isEmpty()){

						for(Tree nns : nnsList){

							String nounPlural = nns.toString().toLowerCase();
							if(line.toLowerCase().contains(nounPlural)){

								ans = getCount(line);
								break;
							}
							else{

								//gettimg lemma of nns, getting derivation of lemma and get number after it. 
								Lemmatizer lemmaObject = new Lemmatizer();
								List<String> lst = lemmaObject.lemmatize(nounPlural);
								String lemma = lst.get(0);

								HttpResponse<JsonNode> response_der = Unirest.get("https://wordsapiv1.p.mashape.com/words/"+lemma+"/derivation").header("X-Mashape-Key", "jbww4coyOHmshYmdYYBixq9DtwsYp1PgetcjsnmKRdjNTLbMQ8")
										.header("Content-Type", "application/x-www-form-urlencoded")
										.header("Accept", "application/json").asJson();

								JSONObject obj_der = new JSONObject(response_der.getBody().toString());
								JSONArray arr_der = (JSONArray)obj_der.get("derivation");


								if(line.toLowerCase().contains(arr_der.get(0).toString())){

									ans = getNextCount(line, arr_der.get(0).toString());
									break;
								}

							}
						}
					}

					if(! exList.isEmpty()){

						if(line.toLowerCase().contains(exList.get(0).toString())){

							ans = getNextCount(line, exList.get(0).toString());
							break;
						}
					}

				}
			}
		}

		return(ans);

	}


	//when functions 

	public String findFirstDate(String line){

		System.out.println("Inside findFirstDate -----------");

		String date = "";

		String [] words = line.split(" ");

		for(String  word : words){
			System.out.println("word is: " + word);
			if(StringUtils.matches(word, "[1-9][0-9][0-9][0-9][,.:;\"\']*") && word.length() > 3){
				System.out.println("Date from Line:" + line);
				date = word;
				break;
			}
		}	
		return date;
	}


	public String findDate(String line){

		System.out.println("Inside findDate ------------");

		String date = "";

		String [] words = line.split(" ");


		for(String word : words){
			if(StringUtils.isNumeric(word) && word.length() > 2){
				date = word;
			}
		}

		return date;
	}

	public String getLocation(String line) throws UnirestException{

		System.out.println("Getting location");

		line = line.trim();
		line = line.replace(" ", "+");
		line.replace(",", "%2c");
		line = line.replace("\\\"", "%22");
		line = line.replace(";", "%3B");

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



	public List<String> getPersonList(String line) throws UnirestException{

		System.out.println("Getting person list");

		line = line.trim();
		line = line.replace(" ", "+");
		line = line.replace(",", "%2c");
		line = line.replace("\\\"", "%22");
		line = line.replace(";", "%3B");




		HttpResponse<JsonNode> response = Unirest.get("https://webknox-text-processing.p.mashape.com/text/entities?text="+line)
				.header("X-Mashape-Key", "r2iAzEK2ilmshMy6isqkHL9j8UiJp1XMo3ojsn1IMggY2xD7DK")
				.header("Accept", "application/json")
				.asJson();

		String result = "{ \"result\" : " + response.getBody().toString() + "}";
		JSONObject resp = new JSONObject(result);
		JSONArray arr = (JSONArray)resp.get("result");
		//JSONObject firstObj = arr.getJSONObject(0);


		int iterator = 0;
		int len = arr.length();
		List<String> persons = new ArrayList<String>();
		for(int i=0; i<len; i++){

			JSONObject tempObj = arr.getJSONObject(i);
			//System.out.println(tempObj.toString());
			if(tempObj.get("type").equals("PER") || tempObj.get("type").equals("City")){

				persons.add((String) tempObj.get("entity"));

			}

		}

		return persons;
	}

	public String getCount(String line){

		System.out.println("Inside get count");

		StringBuilder sb = new StringBuilder();
		boolean foundFlag = false;

		for(char c :line.toCharArray()){

			if(Character.isDigit(c)){

				sb.append(c);
				foundFlag = true;
			}
			else if(foundFlag)
				break;
		}

		System.out.println(sb.toString());
		return sb.toString();
	}

	public String getNextCount(String line, String thatWord){

		//System.out.println("Inside get next count");
		String ans = "";
		boolean foundWord = false;
		//System.out.println(line);
		String[] ansArray = line.split(" ");
		//System.out.println("Derivation" + thatWord);

		for(String word:ansArray){

			if(word.toLowerCase().contains(thatWord.toLowerCase())){
				//System.out.println("found the word -- " + word);
				foundWord = true;
			}

			if(foundWord == true){
				//System.out.println(word);
				if(StringUtils.matches(word, "[0-9]*[,.:;\"\']*[0-9]*")){

					ans = word;
					break;
				}
			}

		}

		return ans;

	}

	//	public String getNextCountWord(String line, String thatWord){
	//		
	//		String ans = "";
	//		boolean foundWord = false;
	//		String[] ansArray = line.split(" ");
	//		
	//		for(int i=0; i<ansArray.length; i++){
	//			
	//			if(ansArray[i].toLowerCase().contains(thatWord.toLowerCase())){
	//				
	//				foundWord = true;
	//			}
	//			if(foundWord == true){
	//				
	//				if(StringUtils.matches(ansArray[i], "[0-9]*[,.:;\"\']*[0-9]*")){
	//					
	//					ans += ansArray[i];
	//					ans+= " " + ansArray[i+1];
	//					break;
	//				}
	//			}
	//		}
	//		
	//		return ans;
	//	}

}

