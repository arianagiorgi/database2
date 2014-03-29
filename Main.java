//package dbproj2;

import com.google.api.client.http.GenericUrl;
import com.google.api.client.http.HttpRequest;
import com.google.api.client.http.HttpRequestFactory;
import com.google.api.client.http.HttpResponse;
import com.google.api.client.http.HttpTransport;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.jayway.jsonpath.JsonPath;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;

import java.util.ArrayList;

public class main {
	public static void main(String[] args) {
		try {
            
			//query (will eventually be from parameters)
			String query = null;
			String accountKey = null;
			query = "Bill Gates";
			accountKey = "AIzaSyCIQ8gDGEMgxJpSsGK6BwkfLZXtN4MTf4E";
            
			//load and read content in search API results
			HttpTransport httpTransport = new NetHttpTransport();
			HttpRequestFactory requestFactory = httpTransport.createRequestFactory();
			JSONParser parser = new JSONParser();
			GenericUrl url = new GenericUrl("https://www.googleapis.com/freebase/v1/search");
			url.put("query", query);
			//url.put("filter", "(all type:/music/artist created:\"The Lady Killer\")");
			url.put("limit", "5");
			url.put("indent", "true");
			url.put("key", accountKey);
			HttpRequest request = requestFactory.buildGetRequest(url);
			HttpResponse httpResponse = request.execute();
			JSONObject response = (JSONObject)parser.parse(httpResponse.parseAsString());
			JSONArray results = (JSONArray)response.get("result");
            
			ArrayList<String> midList = new ArrayList<String>();
            
			//make a list of the mid results
			for (Object result : results) {
				midList.add(JsonPath.read(result,"$.mid").toString());
			}
            
			System.out.println(midList);
            
			//define freebase Types
			String[] freebaseTypeList = {"/people/person", "/book/author", "/film/actor","/tv/tv_actor","/organization/organization_founder",
                "/business/board_member", "/sports/sports_league", "/sports/sports_team", "/sports/professional_sports_team"};
			
			ArrayList<String> entityTypeList = new ArrayList<String>();
            
			//running the mids through the Topic API
        topicloop:
			for(int i=0; i<midList.size(); i++){
                
				//loading Topic API content
				String topicId = midList.get(i);
				GenericUrl topicUrl = new GenericUrl("https://www.googleapis.com/freebase/v1/topic" + topicId);
				topicUrl.put("key", accountKey);
				HttpRequest request2 = requestFactory.buildGetRequest(topicUrl);
				HttpResponse httpResponse2 = request2.execute();
				JSONObject topic = (JSONObject)parser.parse(httpResponse2.parseAsString());
                
				ArrayList<String> topicList = new ArrayList<String>();
                
				//return first 10 freebase type results
				for(int j=0; j<10; j++){
					topicList.add(JsonPath.read(topic,"$.property['/type/object/type'].values["+j+"].id").toString());
				}
                
				System.out.println(topicList);
				
				//if the freebase type matches entity type, save in list
				for(int k=0; k<topicList.size(); k++){
					for(int l=0; l<freebaseTypeList.length; l++){
						if(topicList.get(k).equals(freebaseTypeList[l])){
							entityTypeList.add(topicList.get(k));
						}
					}
					//if we found a match, break out of the loop for running the mids through the topic API
					if(k == topicList.size()-1 && entityTypeList.size() != 0){
						break topicloop;
					}
				}
			}
			
			System.out.println(entityTypeList);
			
		} catch (Exception ex) {
			ex.printStackTrace();
		}
	}
}