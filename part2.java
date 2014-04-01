
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

public class part2 {
  public static void main(String[] args) {
    try {
      HttpTransport httpTransport = new NetHttpTransport();
      HttpRequestFactory requestFactory = httpTransport.createRequestFactory();
      JSONParser parser = new JSONParser();
      String query = "[{\"/visual_art/visual_artist/artworks\":[{\"a:name\":null,\"name~=\":\"Mona Lisa\"}],\"id\":null,\"name\":null,\"type\":\"/visual_art/visual_artist\"}]";
      GenericUrl url = new GenericUrl("https://www.googleapis.com/freebase/v1/mqlread");
      url.put("query", query);
      url.put("key", "AIzaSyCIQ8gDGEMgxJpSsGK6BwkfLZXtN4MTf4E");
      HttpRequest request = requestFactory.buildGetRequest(url);
      HttpResponse httpResponse = request.execute();
      JSONObject response = (JSONObject)parser.parse(httpResponse.parseAsString());
      JSONArray results = (JSONArray)response.get("result");
      for (Object result : results) {
        System.out.println(JsonPath.read(result,"$.name").toString());
      }
    } catch (Exception ex) {
      ex.printStackTrace();
    }
  }
}
