package edu.asu.cse.nlp;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.net.URL;
import java.nio.charset.Charset;
import java.util.HashSet;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import com.mashape.unirest.http.HttpResponse;
import com.mashape.unirest.http.JsonNode;
import com.mashape.unirest.http.Unirest;
import com.mashape.unirest.http.exceptions.UnirestException;
import com.mashape.unirest.request.GetRequest;

import com.wordsapi.www.client.*;
import com.wordsapi.www.wordsapi.api.*;
import com.wordsapi.www.wordsapi.model.*;


public class ReadDBPedia {


	private static String readAll(Reader rd) throws IOException {
		StringBuilder sb = new StringBuilder();
		int cp;
		while ((cp = rd.read()) != -1) {
			sb.append((char) cp);
		}
		return sb.toString();
	}

	public static JSONObject readJsonFromUrl(String url) throws IOException, JSONException {
		InputStream is = new URL(url).openStream();
		try {
			BufferedReader rd = new BufferedReader(new InputStreamReader(is, Charset.forName("UTF-8")));
			String jsonText = readAll(rd);
			JSONObject json = new JSONObject(jsonText);
			return json;
		} finally {
			is.close();
		}
	}

	public void getTags(String uri, String subjectJson) throws IOException, JSONException{

		JSONObject json = readJsonFromUrl("http://dbpedia.org/data/"+subjectJson+".json");
		json = (JSONObject) json.get(uri);

		String [] jsonArray = (JSONObject.getNames(json));

		HashSet<String> tags = new HashSet<String>();

		for(String s : jsonArray){
			System.out.println(s);
			tags.add(s);
		}


	}

}
