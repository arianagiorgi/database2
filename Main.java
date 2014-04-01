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

public class Main {

	public static void main(String[] args) {

		try {
			Entity entity = null;
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
			url.put("key", accountKey);
			HttpRequest request = requestFactory.buildGetRequest(url);
			HttpResponse httpResponse = request.execute();
			JSONObject response = (JSONObject)parser.parse(httpResponse.parseAsString());
			JSONArray results = (JSONArray)response.get("result");

			ArrayList<String> midList = new ArrayList<String>();
			ArrayList<String> nameList = new ArrayList<String>();

			//make a list of the mid results
			for (Object result : results) {
				midList.add(JsonPath.read(result,"$.mid").toString());
				nameList.add(JsonPath.read(result,"$.name").toString());
			}
			//list of names for mid results

			//System.out.println(midList);

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

					//System.out.println(topicList);

					//if the freebase type matches entity type, save in list
					for(int k=0; k<topicList.size(); k++){
						for(int l=0; l<freebaseTypeList.length; l++){
							if(topicList.get(k).equals(freebaseTypeList[l])){
								//change the type into what we need	
								entityTypeList.add(topicList.get(k));
							}
						}
						//if we found a match, break out of the loop for running the mids through the topic API
						if(k == topicList.size()-1 && entityTypeList.size() != 0){
							//get the mid we need for the infobox

							String mid = midList.get(i);
							String name = nameList.get(i);
							//we can use the mid here to call different entities;
							System.out.println ("Mid: "+mid);
							entity = new Entity (mid);
							if(entityTypeList.contains("/people/person")){
								entity = new Person (mid);
								setPersonType((Person)entity);
								Person person = new Person (mid);
								//name
								person.setName(name);
								System.out.println("Name: " + person.getName());
								//birthday
								person.setBirthday(JsonPath.read(topic,"$.property['/people/person/date_of_birth'].values[0].text").toString());
								System.out.println("Date of Birth: "+person.getBirthday());
								//place of birth
								person.setPlaceofBirth(JsonPath.read(topic,"$.property['/people/person/place_of_birth'].values[0].text").toString());
								System.out.println("Place of Birth: "+person.getPlaceofBirth());
								//death
								if(JsonPath.read(topic, "$").toString().contains("deceased_person")){
									String count = JsonPath.read(topic,"$.property['/people/deceased_person/cause_of_death'].count").toString();
									double numCount = Double.valueOf(count);
									if(numCount >= 1){
										person.setDeathCause(JsonPath.read(topic,"$.property['/people/deceased_person/cause_of_death'].values[0].text").toString());
									}else{
										person.setDeathCause("unknown");
									}							
									person.setDeathDate(JsonPath.read(topic,"$.property['/people/deceased_person/date_of_death'].values[0].text").toString());
									person.setDeathPlace(JsonPath.read(topic,"$.property['/people/deceased_person/place_of_death'].values[0].text").toString());
									System.out.println("Death: "+person.Death());
								}
								//siblings
								String siblings = "";
								String count = JsonPath.read(topic,"$.property['/people/person/sibling_s'].count").toString();
								double numSiblings = Double.valueOf(count);
								for(int l=0; l<numSiblings; l++){
									String sibling = (JsonPath.read(topic,"$.property['/people/person/sibling_s'].values["+l+"].property['/people/sibling_relationship/sibling'].values[0].text").toString());
									if(l==0){
										siblings = siblings+sibling;
									}else{
										siblings = siblings+", "+sibling;
									}
								}
								person.setSiblings(siblings);
								System.out.println("Sibling(s): " + person.getSiblings());
								//spouses
								String spouses = "";
								count = JsonPath.read(topic,"$.property['/people/person/spouse_s'].count").toString();
								double numSpouses = Double.valueOf(count);
								for(int l=0; l<numSpouses; l++){
									String spouse = (JsonPath.read(topic,"$.property['/people/person/spouse_s'].values["+l+"].property['/people/marriage/spouse'].values[0].text").toString());
									if(l==0){
										spouses = spouses+spouse;
									}else{
										spouses = spouses+", "+spouse;
									}
								}
								person.setSpouses(spouses);
								System.out.println("Spouse(s): " + person.getSpouses());
								//description
								person.setDescription(JsonPath.read(topic,"$.property['/common/topic/description'].values[0].value").toString());
								System.out.println("Description: " + person.getDescription());
							}

							if(entityTypeList.contains("/book/author")){
								entity = new Author (mid);
								setAuthorType((Author)entity);
								Author author = new Author (mid);
								//Books
								ArrayList<String> books = new ArrayList<String>();
								String count;
								count = JsonPath.read(topic,"$.property['/book/author/works_written'].count").toString();
								double numBooks = Double.valueOf(count);
								if(numBooks>10){
									numBooks = 10;
								}
								for(int l=0; l<numBooks; l++){
									String book = (JsonPath.read(topic,"$.property['/book/author/works_written'].values["+l+"].text").toString());
									books.add(book);
								}
								String[] bookArr = new String[books.size()];
								for(int l=0; l<bookArr.length; l++){
									bookArr[l] = books.get(l);
								}
								author.setBooks(bookArr);
								System.out.println("Books: "+ author.getBooks());
								//Book about the author
								ArrayList<String> booksAbout = new ArrayList<String>();
								count = JsonPath.read(topic,"$.property['/book/book_subject/works'].count").toString();
								double numBooksAbout = Double.valueOf(count);
								if(numBooksAbout>10){
									numBooksAbout=10;
								}
								for(int l=0; l<numBooksAbout; l++){
									String book = (JsonPath.read(topic,"$.property['/book/book_subject/works'].values["+l+"].text").toString());
									booksAbout.add(book);
								}
								String[] bookAboutArr = new String[booksAbout.size()];
								for(int l=0; l<bookAboutArr.length; l++){
									bookAboutArr[l] = booksAbout.get(l);
								}
								author.setBooksAbouttheAuthor(bookAboutArr);
								System.out.println("Books about: " + author.getBooksAbouttheAuthor());
								//Influenced
								ArrayList<String> influenced = new ArrayList<String>();
								count = JsonPath.read(topic,"$.property['/influence/influence_node/influenced'].count").toString();
								double numInfluenced = Double.valueOf(count);
								if(numInfluenced>10){
									numInfluenced=10;
								}
								for(int l=0; l<numInfluenced; l++){
									String influencedPerson = (JsonPath.read(topic,"$.property['/influence/influence_node/influenced'].values["+l+"].text").toString());
									influenced.add(influencedPerson);
								}
								String[] influencedArr = new String[influenced.size()];
								for(int l=0; l<influencedArr.length; l++){
									influencedArr[l] = influenced.get(l);
								}
								author.setInfluenced(influencedArr);
								System.out.println("Influenced: " + author.getInfluenced());
								//Influenced by
								if(JsonPath.read(topic, "$").toString().contains("influenced_by")){
									ArrayList<String> influencedby = new ArrayList<String>();
									count = JsonPath.read(topic,"$.property['/influence/influence_node/influenced_by'].count").toString();
									double numInfluencedBy = Double.valueOf(count);
									if(numInfluencedBy>10){
										numInfluencedBy=10;
									}
									for(int l=0; l<numInfluencedBy; l++){
										String influencedByPerson = (JsonPath.read(topic,"$.property['/influence/influence_node/influenced_by'].values["+l+"].text").toString());
										influencedby.add(influencedByPerson);
									}
									String[] influencedbyArr = new String[influencedby.size()];
									for(int l=0; l<influencedbyArr.length; l++){
										influencedbyArr[l] = influencedby.get(l);
									}
									author.setInfluencedby(influencedbyArr);
									System.out.println("Influenced By: "+author.getInfluencedby());
								}

							}

							if(JsonPath.read(topic, "$").toString().contains("film")){
									ArrayList<String> films = new ArrayList<String>();
									ArrayList<String> characters = new ArrayList<String>();
									String count = JsonPath.read(topic,"$.property['/film/actor/film'].count").toString();
									double numFilms = Double.valueOf(count);
									if(numFilms>10){
										numFilms=10;
									}
									for(int l=0; l<numFilms; l++){
										String film = (JsonPath.read(topic,"$.property['/film/actor/film'].values["+l+"].property['/film/performance/film'].values[0].text").toString());
										String character = (JsonPath.read(topic,"$.property['/film/actor/film'].values["+l+"].property['/film/performance/character'].values[0].text").toString());
										films.add(film);
										characters.add(character);
									}
									String[] filmArr = new String[films.size()];
									String[] charArr = new String[characters.size()];
									for(int l=0; l<filmArr.length; l++){
										filmArr[l] = films.get(l);
										charArr[l] = characters.get(l);
									}
									actor.setFilmsParticipated(charArr, filmArr);
									System.out.println("Films: "+actor.getFilmsParticipated());
								}								
							}  

							if(entityTypeList.contains("/organization/organization_founder") || entityTypeList.contains("/business/board_member")){
								entity = new BusinessPerson (mid);
								setBusinessPersonType((BusinessPerson)entity);
								BusinessPerson businessperson = new BusinessPerson (mid);
								//founder
								if(JsonPath.read(topic, "$").toString().contains("organizations_founded")){
									ArrayList<String> founded = new ArrayList<String>();
									String count = JsonPath.read(topic,"$.property['/organization/organization_founder/organizations_founded'].count").toString();
									double numOrgFounded = Double.valueOf(count);
									if(numOrgFounded>10){
										numOrgFounded=10;
									}
									for(int l=0; l<numOrgFounded; l++){
										String org = (JsonPath.read(topic,"$.property['/organization/organization_founder/organizations_founded'].values["+l+"].text").toString());
										founded.add(org);
									}
									String[] foundedArr = new String[founded.size()];
									for(int l=0; l<foundedArr.length; l++){
										foundedArr[l] = founded.get(l);
									}
									businessperson.setFoundedOrganizationName(foundedArr);
									System.out.println("Founded:");
									for(String str : foundedArr){
										System.out.println(str);
									}
								}
								//leadership
								if(JsonPath.read(topic, "$").toString().contains("leader_of")){
									ArrayList<String> leadershipFrom = new ArrayList<String>();
									ArrayList<String> leadershipTo = new ArrayList<String>();
									ArrayList<String> leadershipOrganization = new ArrayList<String>();
									ArrayList<String> leadershipRole = new ArrayList<String>();
									ArrayList<String> leadershipTitle = new ArrayList<String>();
									String count = JsonPath.read(topic,"$.property['/business/board_member/leader_of'].count").toString();
									double numLeadership = Double.valueOf(count);
									for(int l=0; l<numLeadership; l++){
										String leadFrom = (JsonPath.read(topic,"$.property['/business/board_member/leader_of'].values["+l+"].property['/organization/leadership/from'].values[0].text").toString());
										leadershipFrom.add(leadFrom);
										String leadTo = (JsonPath.read(topic,"$.property['/business/board_member/leader_of'].values["+l+"].property['/organization/leadership/to'].values[0].text").toString());
										leadershipTo.add(leadTo);
										String leadOrg = (JsonPath.read(topic,"$.property['/business/board_member/leader_of'].values["+l+"].property['/organization/leadership/organization'].values[0].text").toString());
										leadershipOrganization.add(leadOrg);
										String leadRole = (JsonPath.read(topic,"$.property['/business/board_member/leader_of'].values["+l+"].property['/organization/leadership/role'].values[0].text").toString());
										leadershipRole.add(leadRole);
										String leadTitle = (JsonPath.read(topic,"$.property['/business/board_member/leader_of'].values["+l+"].property['/organization/leadership/title'].values[0].text").toString());
										leadershipTitle.add(leadTitle);
									}
									String[] leadFromArr = new String[leadershipFrom.size()];
									String[] leadToArr = new String[leadershipTo.size()];
									String[] leadOrgArr = new String[leadershipOrganization.size()];
									String[] leadRoleArr = new String[leadershipRole.size()];
									String[] leadTitleArr = new String[leadershipTitle.size()];
									for(int l=0; l<leadFromArr.length; l++){
										leadFromArr[l] = leadershipFrom.get(l);
										leadToArr[l] = leadershipTo.get(l);
										leadOrgArr[l] = leadershipOrganization.get(l);
										leadRoleArr[l] = leadershipRole.get(l);
										leadTitleArr[l] = leadershipTitle.get(l);
									}
									businessperson.setLeadership(leadFromArr, leadToArr, leadOrgArr, leadRoleArr, leadTitleArr);
									System.out.println("Leadership:\nOrganization/Role/Title/From-To");
									for(int l=0; l<leadFromArr.length; l++){
										System.out.println(leadOrgArr[l]+"/"+leadRoleArr[l]+"/"+leadTitleArr[l]+"/"+leadFromArr[l]+" to "+leadToArr[l]);
									}
								}
								//board member
								if(JsonPath.read(topic, "$").toString().contains("board_memberships")){
									ArrayList<String> boardFrom = new ArrayList<String>();
									ArrayList<String> boardTo = new ArrayList<String>();
									ArrayList<String> boardOrganization = new ArrayList<String>();
									ArrayList<String> boardRole = new ArrayList<String>();
									ArrayList<String> boardTitle = new ArrayList<String>();
									String count = JsonPath.read(topic,"$.property['/business/board_member/organization_board_memberships'].count").toString();
									double numMemberships = Double.valueOf(count);
									for(int l=0; l<numMemberships; l++){
										if(JsonPath.read(topic,"$.property['/business/board_member/organization_board_memberships'].values["+l+"]").toString().contains("from")){
											String bmFrom = (JsonPath.read(topic,"$.property['/business/board_member/organization_board_memberships'].values["+l+"].property['/organization/organization_board_membership/from'].values[0].text").toString());
											boardFrom.add(bmFrom);
										}else{
											boardFrom.add(" ");
										}
										//System.out.println(JsonPath.read(topic,"$").toString());
										if(JsonPath.read(topic,"$.property['/business/board_member/organization_board_memberships'].values["+l+"]").toString().contains("board_membership\\/to")){
											String bmTo = (JsonPath.read(topic,"$.property['/business/board_member/organization_board_memberships'].values["+l+"].property['/organization/organization_board_membership/to'].values[0].text").toString());
											boardTo.add(bmTo);
										}else if(JsonPath.read(topic,"$.property['/business/board_member/organization_board_memberships'].values["+l+"]").toString().contains("from")){
											boardTo.add("now");
										}else{
											boardTo.add(" ");
										}
										String bmOrg = (JsonPath.read(topic,"$.property['/business/board_member/organization_board_memberships'].values["+l+"].property['/organization/organization_board_membership/organization'].values[0].text").toString());
										boardOrganization.add(bmOrg);
										if(JsonPath.read(topic,"$.property['/business/board_member/organization_board_memberships'].values["+l+"]").toString().contains("role")){
											String bmRole = (JsonPath.read(topic,"$.property['/business/board_member/organization_board_memberships'].values["+l+"].property['/organization/organization_board_membership/role'].values[0].text").toString());
											boardRole.add(bmRole);
										}else{
											boardRole.add(" ");
										}
										if(JsonPath.read(topic,"$.property['/business/board_member/organization_board_memberships'].values["+l+"]").toString().contains("board_membership\\/title")){
											String bmTitle = (JsonPath.read(topic,"$.property['/business/board_member/organization_board_memberships'].values["+l+"].property['/organization/organization_board_membership/title'].values[0].text").toString());
											boardTitle.add(bmTitle);
										}else{
											boardTitle.add(" ");
										}
									}
									String[] bmFromArr = new String[boardFrom.size()];
									String[] bmToArr = new String[boardTo.size()];
									String[] bmOrgArr = new String[boardOrganization.size()];
									String[] bmRoleArr = new String[boardRole.size()];
									String[] bmTitleArr = new String[boardTitle.size()];
									for(int l=0; l<bmFromArr.length; l++){
										bmFromArr[l] = boardFrom.get(l);
										bmToArr[l] = boardTo.get(l);
										bmOrgArr[l] = boardOrganization.get(l);
										bmRoleArr[l] = boardRole.get(l);
										bmTitleArr[l] = boardTitle.get(l);
									}
									businessperson.setLeadership(bmFromArr, bmToArr, bmOrgArr, bmRoleArr, bmTitleArr);
									System.out.println("Board Membership:\nOrganization/Role/Title/From-To");
									for(int l=0; l<bmOrgArr.length; l++){
										System.out.println(bmOrgArr[l]+"/"+bmRoleArr[l]+"/"+bmTitleArr[l]+"/"+bmFromArr[l]+" to "+bmToArr[l]);
									}
								}
							}

							if(entityTypeList.contains("/sports/sports_league")){
								entity = new League (mid);
								setLeagueType((League)entity);
								League league = new League (mid);
								//name
								league.setName(name);
								System.out.println("Name: " + league.getName());
								//sport
								league.setSport(JsonPath.read(topic,"$.property['/sports/sports_league/sport'].values[0].text").toString());
								System.out.println("Sport: "+league.getSport());
								//slogan
								if(JsonPath.read(topic,"$").toString().contains("slogan")){
									league.setSlogan(JsonPath.read(topic,"$.property['/organization/organization/slogan'].values[0].text").toString());
									System.out.println("Slogan: "+league.getSlogan());
								}
								//Official website
								league.setOfficialWebsite(JsonPath.read(topic,"$.property['/common/topic/official_website'].values[0].text").toString());
								System.out.println("Official website: "+league.getOfficialWebsite());
								//championship
								league.setChampionship(JsonPath.read(topic,"$.property['/sports/sports_league/championship'].values[0].text").toString());
								System.out.println("Championship: "+league.getChampionship());
								//teams
								ArrayList<String> teams = new ArrayList<String>();
								String count = JsonPath.read(topic,"$.property['/sports/sports_league/teams'].count").toString();
								double numTeams = Double.valueOf(count);
								if(numTeams>10){
									numTeams=10;
								}
								for(int l=0; l<numTeams; l++){
									String team = (JsonPath.read(topic,"$.property['/sports/sports_league/teams'].values["+l+"].property['/sports/sports_league_participation/team'].values[0].text").toString());
									teams.add(team);
								}
								String[] teamsArr = new String[teams.size()];
								for(int l=0; l<teamsArr.length; l++){
									teamsArr[l] = teams.get(l);
								}
								league.setTeams(teamsArr);
								System.out.println("Teams:");
								for(String str : teamsArr){
									System.out.println(str);
								}
								//description
								league.setDescription(JsonPath.read(topic,"$.property['/common/topic/description'].values[0].value").toString());
								System.out.println("Description: "+league.getDescription());
							}

							if(entityTypeList.contains("/sports/sports_team") || entityTypeList.contains("/sports/professional_sports_team")){
								entity = new SportsTeam(mid);
								setSportsTeamType((SportsTeam)entity);
								SportsTeam sportsteam = new SportsTeam (mid);
								//Name
								sportsteam.setName(name);
								System.out.println("Name: " + sportsteam.getName());
								//Sport
								sportsteam.setSport(JsonPath.read(topic,"$.property['/sports/sports_team/sport'].values[0].text").toString());
								System.out.println("Sport: "+sportsteam.getSport());
								//Arena
								sportsteam.setArena(JsonPath.read(topic,"$.property['/sports/sports_team/arena_stadium'].values[0].text").toString());
								System.out.println("Arena: "+sportsteam.getArena());
								//Championships
								ArrayList<String> champs = new ArrayList<String>();
								String count = JsonPath.read(topic,"$.property['/sports/sports_team/championships'].count").toString();
								double numChamps = Double.valueOf(count);
								if(numChamps>10){
									numChamps=10;
								}
								for(int l=0; l<numChamps; l++){
									String champ = (JsonPath.read(topic,"$.property['/sports/sports_team/championships'].values["+l+"].text").toString());
									champs.add(champ);
								}
								String[] champsArr = new String[champs.size()];
								for(int l=0; l<champsArr.length; l++){
									champsArr[l] = champs.get(l);
								}
								sportsteam.setChampionships(champsArr);
								System.out.println("Championships:");
								for(String str : champsArr){
									System.out.println(str);
								}
								//Founded
								sportsteam.setFounded(JsonPath.read(topic,"$.property['/sports/sports_team/founded'].values[0].text").toString());
								System.out.println("Founded: "+sportsteam.getFounded());
								//Leagues
								sportsteam.setLeagues(JsonPath.read(topic,"$.property['/sports/sports_team/league'].values[0].property['/sports/sports_league_participation/league'].values[0].text").toString());
								System.out.println("Leagues: "+sportsteam.getLeagues());
								//Location
								sportsteam.setLocations(JsonPath.read(topic,"$.property['/sports/sports_team/location'].values[0].text").toString());
								System.out.println("Locations: "+sportsteam.getLocations());
								//Coaches
								ArrayList<String> coachFrom = new ArrayList<String>();
								ArrayList<String> coachTo = new ArrayList<String>();
								ArrayList<String> coachName = new ArrayList<String>();
								ArrayList<String> coachPos = new ArrayList<String>();
								count = JsonPath.read(topic,"$.property['/sports/sports_team/coaches'].count").toString();
								double numCoaches = Double.valueOf(count);
								for(int l=0; l<numCoaches; l++){
									if(JsonPath.read(topic,"$.property['/sports/sports_team/coaches'].values["+l+"]").toString().contains("from")){
										String coFrom = (JsonPath.read(topic,"$.property['/sports/sports_team/coaches'].values["+l+"].property['/sports/sports_team_coach_tenure/from'].values[0].text").toString());
										coachFrom.add(coFrom);
									}else{
										coachFrom.add(" ");
									}
									if(JsonPath.read(topic,"$.property['/sports/sports_team/coaches'].values["+l+"]").toString().contains("coach_tenure\\/to") && 
											JsonPath.read(topic,"$.property['/sports/sports_team/coaches'].values["+l+"].property['/sports/sports_team_coach_tenure/to'].values").toString().contains("text")){
										String coTo = (JsonPath.read(topic,"$.property['/sports/sports_team/coaches'].values["+l+"].property['/sports/sports_team_coach_tenure/to'].values[0].text").toString());
										coachTo.add(coTo);
									}else if(JsonPath.read(topic,"$.property['/sports/sports_team/coaches'].values["+l+"]").toString().contains("from")){
										coachTo.add("now");
									}else{
										coachTo.add(" ");
									}
									if(JsonPath.read(topic,"$.property['/sports/sports_team/coaches'].values["+l+"]").toString().contains("position")){
										String posCount = JsonPath.read(topic,"$.property['/sports/sports_team/coaches'].values["+l+"].property['/sports/sports_team_coach_tenure/position'].count").toString();
										double posNum = Double.valueOf(posCount);
										String posStr = "";
										for(int m =0; m<posNum; m++){
											String coPos = (JsonPath.read(topic,"$.property['/sports/sports_team/coaches'].values["+l+"].property['/sports/sports_team_coach_tenure/position'].values["+m+"].text").toString());
											if (m==0)
												posStr = posStr+coPos;
											else
												posStr = posStr+", "+coPos;
										}
										coachPos.add(posStr);
									}else{
										coachPos.add(" ");
									}
									String coName = (JsonPath.read(topic,"$.property['/sports/sports_team/coaches'].values["+l+"].property['/sports/sports_team_coach_tenure/coach'].values[0].text").toString());
									coachName.add(coName);
								}
								String[] cfromArr = new String[coachFrom.size()];
								String[] ctoArr = new String[coachTo.size()];
								String[] cposArr = new String[coachPos.size()];
								String[] cnameArr = new String[coachName.size()];
								for(int l=0; l<cfromArr.length; l++){
									cfromArr[l] = coachFrom.get(l);
									ctoArr[l] = coachTo.get(l);
									cposArr[l] = coachPos.get(l);
									cnameArr[l] = coachName.get(l);
								}
								sportsteam.setCoaches(cnameArr, cposArr, cfromArr, ctoArr);
								System.out.println("Coaches:\nName/Position/From-To");
								for(int l=0; l<cnameArr.length; l++){
									System.out.println(cnameArr[l]+"/"+cposArr[l]+"/"+cfromArr[l]+"-"+ctoArr[l]);
								}
								//Player roster
								ArrayList<String> playerFrom = new ArrayList<String>();
								ArrayList<String> playerTo = new ArrayList<String>();
								ArrayList<String> playerName = new ArrayList<String>();
								ArrayList<String> playerPos = new ArrayList<String>();
								ArrayList<String> playerNum = new ArrayList<String>();
								count = JsonPath.read(topic,"$.property['/sports/sports_team/roster'].count").toString();
								double numPlayers = Double.valueOf(count);
								if(numPlayers>10){
									numPlayers=10;
								}
								for(int l=0; l<numPlayers; l++){
									if(JsonPath.read(topic,"$.property['/sports/sports_team/roster'].values["+l+"]").toString().contains("from")){
										String playFrom = (JsonPath.read(topic,"$.property['/sports/sports_team/roster'].values["+l+"].property['/sports/sports_team_roster/from'].values[0].text").toString());
										playerFrom.add(playFrom);
									}else{
										playerFrom.add(" ");
									}
									if(JsonPath.read(topic,"$.property['/sports/sports_team/roster'].values["+l+"]").toString().contains("team_roster\\/to")){
										String playTo = (JsonPath.read(topic,"$.property['/sports/sports_team/roster'].values["+l+"].property['/sports/sports_team_roster/to'].values[0].text").toString());
										playerTo.add(playTo);
									}else if(JsonPath.read(topic,"$.property['/sports/sports_team/roster'].values["+l+"]").toString().contains("from")){
										playerTo.add("now");
									}else{
										playerTo.add(" ");
									}
									if(JsonPath.read(topic,"$.property['/sports/sports_team/roster'].values["+l+"]").toString().contains("number")){
										String playNum = (JsonPath.read(topic,"$.property['/sports/sports_team/roster'].values["+l+"].property['/sports/sports_team_roster/number'].values[0].text").toString());
										playerNum.add(playNum);
									}else{
										playerNum.add(" ");
									}
									if(JsonPath.read(topic,"$.property['/sports/sports_team/roster'].values["+l+"]").toString().contains("position")){
										String posCount = JsonPath.read(topic,"$.property['/sports/sports_team/roster'].values["+l+"].property['/sports/sports_team_roster/position'].count").toString();
										double posNum = Double.valueOf(posCount);
										String posStr = "";
										for(int m =0; m<posNum; m++){
											String playPos = (JsonPath.read(topic,"$.property['/sports/sports_team/roster'].values["+l+"].property['/sports/sports_team_roster/position'].values["+m+"].text").toString());
											if (m==0)
												posStr = posStr+playPos;
											else
												posStr = posStr+", "+playPos;
										}
										playerPos.add(posStr);
									}else{
										playerPos.add(" ");
									}
									String playName = (JsonPath.read(topic,"$.property['/sports/sports_team/roster'].values["+l+"].property['/sports/sports_team_roster/player'].values[0].text").toString());
									playerName.add(playName);
								}
								String[] fromArr = new String[playerFrom.size()];
								String[] toArr = new String[playerTo.size()];
								String[] numArr = new String[playerNum.size()];
								String[] posArr = new String[playerPos.size()];
								String[] nameArr = new String[playerName.size()];
								for(int l=0; l<fromArr.length; l++){
									fromArr[l] = playerFrom.get(l);
									toArr[l] = playerTo.get(l);
									numArr[l] = playerNum.get(l);
									posArr[l] = playerPos.get(l);
									nameArr[l] = playerName.get(l);
								}
								sportsteam.setPlayersRoster(nameArr, posArr, numArr, fromArr, toArr);
								System.out.println("PlayersRoster:\nName/Position/Number/From-To");
								for(int l=0; l<nameArr.length; l++){
									System.out.println(nameArr[l]+"/"+posArr[l]+"/"+numArr[l]+"/"+fromArr[l]+" to "+toArr[l]);
								}							
								//Description
								sportsteam.setDescription(JsonPath.read(topic,"$.property['/common/topic/description'].values[0].value").toString());
								System.out.println("Description: "+sportsteam.getDescription());
							}
							break topicloop;
						}

					}
				}




			System.out.println(entityTypeList);

		} catch (Exception ex) {
			ex.printStackTrace();

		}

	}
	//call different types of entity
	private static void setPersonType (Person et){
		System.out.println("Person");
	}

	private static void setAuthorType (Author et){
		System.out.println("Author");
	}

	private static void setActorType (Actor et){
		System.out.println("Actor");
	}

	private static void setBusinessPersonType (BusinessPerson et){
		System.out.println("BusinessPerson");
	}

	private static void setLeagueType (League et){
		System.out.println("League");
	}

	private static void setSportsTeamType (SportsTeam et){
		System.out.println("SportsTeam");
	}

}
