package wiki;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.ArrayList;

/**
 * @author brooke mccurdy
 */
public class WikipediaOperations {
	
	private static String GET_ARTICLE_URL = "https://en.wikipedia.org/w/api.php?action=query&prop=pageprops|extracts&format=xml&explaintext=&exsectionformat=plain&ppprop=disambiguation&rawcontinue=&titles=";
	private static String REDIRECTS_ALIAS_URL = "https://en.wikipedia.org/w/api.php?action=query&prop=pageprops|pageterms|redirects&format=xml&ppprop=disambiguation&wbptterms=alias&rdprop=title&rdnamespace=0&rdlimit=max&rawcontinue=&titles=";
	private static String OPENSEARCH_URL = "https://en.wikipedia.org/w/api.php?action=query&list=search&format=xml&srsearch=";
	private static String ARTICLE_SNIPPET_URL = "https://en.wikipedia.org/w/api.php?action=query&prop=pageprops|extracts&format=xml&exsentences=2&exintro=&explaintext=&exsectionformat=plain&ppprop=disambiguation&rawcontinue=&titles=";
	
	public static ArrayList<String> getArticle(String term) {
		try {
			term = URLEncoder.encode(term, "UTF-8");
		} catch (UnsupportedEncodingException e) {
			e.printStackTrace();
		}
		String url = GET_ARTICLE_URL + term + "&redirects=&maxlag=5";
		return invokeService(url);
	}
	
	public static ArrayList<String> getRedirectsAlias(String term) {
		try {
			term = URLEncoder.encode(term, "UTF-8");
		} catch (UnsupportedEncodingException e) {
			e.printStackTrace();
		}
		String url = REDIRECTS_ALIAS_URL + term + "&redirects=&maxlag=5";
		return invokeService(url);
	}
			
	public static ArrayList<String> getSearchOptions(String term) {
		try {
			term = URLEncoder.encode(term, "UTF-8");
		} catch (UnsupportedEncodingException e) {
			e.printStackTrace();
		}
		String url = OPENSEARCH_URL + term + "&srnamespace=0&srinfo=suggestion&srprop=snippet%7Cisfilematch&srlimit=10&rawcontinue=&redirects=&maxlag=5";
		ArrayList<String> temp = invokeService(url);
		if (!temp.isEmpty() && temp.get(0).contains("PAGE-STATUS::found")){
			temp.remove(0);
			temp.add(0, "PAGE-STATUS::search");
		}
		return temp;
	}
	
	public static ArrayList<String> getArticleSnippet(String term) {
		try {
			term = URLEncoder.encode(term, "UTF-8");
		} catch (UnsupportedEncodingException e) {
			e.printStackTrace();
		}
		String url = ARTICLE_SNIPPET_URL + term + "&redirects=&maxlag=5";
		return invokeService(url);
	}
	
	private static ArrayList<String> invokeService(String serviceURL) {
	
		InvokeWikipediaWebService invokeWS = new InvokeWikipediaWebService(serviceURL);
		String content = invokeWS.invokeWebService();
		
		SAXWikiWSParser saxParser = new SAXWikiWSParser(content);
		ArrayList<String> data = saxParser.parse();
//		if (!data.isEmpty() && data.get(0).contains("PAGE-STATUS::redirected")){
//			data.remove(0);
//			data.add(0, "PAGE-STATUS::found");
//		}
		if (data.size() > 1 && data.get(1).contains("PAGE-STATUS::disambiguation")){
			data.remove(0);
		}
		if (!data.isEmpty() && !data.get(0).contains("PAGE-STATUS::")){
			data.add(0, "PAGE-STATUS::found");
		}
		if(data.isEmpty()){
			data.add(0, "INTERNAL-ERROR");
		}
		return data;
	}
	
	public static void main(String[] args){
		//TESTING
//		ArrayList<String> temp = getRedirectsAlias("Team sport");
//		ArrayList<String> temp = getSearchOptions("radionucleiretardation");
//		ArrayList<String> temp = getArticleSnippet("Paper");
		ArrayList<String> temp = getArticle("Ultisol");
		for (String s : temp){
			System.out.println("Have: " + s);
		}
//		System.out.println("\n\n");
//		ArrayList<String> temp2 = getRedirectsAlias("Team");
//		for (String s : temp2){
//			System.out.println(s);
//		}
	}

}

