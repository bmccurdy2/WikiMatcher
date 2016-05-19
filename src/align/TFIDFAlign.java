package align;

import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.URI;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;

import org.semanticweb.owlapi.model.OWLEntity;

import com.wcohen.ss.Levenstein;

import fr.inrialpes.exmo.align.impl.URIAlignment;
import tfidf.DocumentParser;
import util.AlternateCache;
import util.EntityData;
import util.OWLData;
import util.Preprocessing;
import util.Utilities;
import wiki.WikipediaOperations;

public class TFIDFAlign {

	private OWLData owldata;
	private AlternateCache util;
	
	TFIDFAlign(String ontology){
		this.owldata = new OWLData(ontology);
		this.util = new AlternateCache(ontology, "tfidf");
	}
	
	
	public void generateData(Set<OWLEntity> entities, String dataSet){
		int i = 0;
		System.out.println("Size: " + entities.size());
		for (OWLEntity e : entities){
			if(i % 100 == 0){
				System.out.println("i = " + i);
//				this.util.saveCache();
			}
			i++;
			String label = owldata.preprocessLabel(e, dataSet);
			if (this.util.hasValue(label)){
				continue;
			}
			
			EntityData tmp = new EntityData(label);
			
			ArrayList<String> redirects = new ArrayList<String>();
			int tries = 0;
			while(tries < 3 && (redirects.isEmpty() || (redirects.size() == 1 && (redirects.get(0).equals("INTERNAL-ERROR") || redirects.get(0).equals(""))))){
				redirects = processTerms(label, WikipediaOperations.getRedirectsAlias(label));
				tries++;
				try {
					Thread.sleep(100);
				} catch (InterruptedException ex) {
					ex.printStackTrace();
				}
			}
			if(redirects.isEmpty()){
				redirects.add(label);
			}
			if(redirects.get(0).contains("PAGE-STATUS::")){
				tmp.setPageStatus(redirects.get(0).replace("PAGE-STATUS::", ""));
				redirects.remove(0);
			}
			tmp.setTerms(redirects);
			
			if(tmp.getPageStatus().equals("found") || tmp.getPageStatus().equals("redirected")){
				ArrayList<String> article = new ArrayList<String>();
				tries = 0;
				while(tries < 3 && (article.isEmpty() || (article.size() == 1 && (article.get(0).equals("INTERNAL-ERROR") || article.get(0).equals(""))))){
					article = processTerms(label, WikipediaOperations.getArticle(label));
					tries++;
					try {
						Thread.sleep(100);
					} catch (InterruptedException ex) {
						ex.printStackTrace();
					}
				}
				if (article.size() >= 2 && (article.get(0).equals("PAGE-STATUS::found") || article.get(0).equals("PAGE-STATUS::redirected"))){
					tmp.setArticle(article.get(1));
				}
				
				ArrayList<String> snippet = new ArrayList<String>();
				tries = 0;
				while(tries < 3 && (snippet.isEmpty() || (snippet.size() == 1 && (snippet.get(0).equals("INTERNAL-ERROR") || snippet.get(0).equals(""))))){
					snippet = processTerms(label, WikipediaOperations.getArticleSnippet(label));
					tries++;
					try {
						Thread.sleep(100);
					} catch (InterruptedException ex) {
						ex.printStackTrace();
					}
				}
				if (snippet.size() >= 2 && (snippet.get(0).equals("PAGE-STATUS::found") || snippet.get(0).equals("PAGE-STATUS::redirected"))){
					tmp.setSnippet(snippet.get(1));
				}
			}
			
			if(tmp.getPageStatus().equals("disambiguation") || tmp.getPageStatus().equals("missing")){
				ArrayList<String> search = new ArrayList<String>();
				tries = 0;
				while(tries < 3 && (search.isEmpty() || (search.size() == 1 && search.get(0).equals("")))){
					search = WikipediaOperations.getSearchOptions(label);
					tries++;
					try {
						Thread.sleep(100);
					} catch (InterruptedException ex) {
						ex.printStackTrace();
					}
				}
				if (search.size() > 1 && search.get(0).equals("PAGE-STATUS::search")){
					tmp.setPageStatus("search");
					ArrayList<String> t = processSearchTerms(label, search, "T:");
					if(t.size() > 1){
						tmp.setTerms(t);
						tmp.setSnippet(processSearchTerms(label, search, "S:"));
					} else {
						tmp.setPageStatus("missing");
					}
					
				}
			}
			
			this.util.storeValue(label, tmp);
		}
	}
	
	
	private ArrayList<String> processTerms(String label, ArrayList<String> data) {
		ArrayList<String> newdata = new ArrayList<String>();
		if (data.get(0).equals("PAGE-STATUS::API-ERROR")){
			System.out.println("There is a timeout or some other error with API. Saving cache and exiting.");
			this.util.saveCache();
			System.out.println("Logging: api error occured: " + label);
			try {
				Thread.sleep(15000);
			} catch (InterruptedException ex) {
				ex.printStackTrace();
			}
			return newdata;
		}
		if (data.get(0).equals("INTERNAL-ERROR")){
			System.out.println("Internal Error for: " + label + ". Added the label to the label; list and moving on.");
			return newdata;
		} 
		if (data.get(0).equals("PAGE-STATUS::missing")){
			newdata.add("PAGE-STATUS::missing");
			return newdata;
		}
		if (data.get(0).equals("PAGE-STATUS::disambiguation")){
			data.remove(0);
			newdata.add("PAGE-STATUS::disambiguation");
			for (String s : data){
				s = s.replaceAll("\\(.*?\\) ?", "");
				newdata.add(Preprocessing.preprocess(s));
			}
			return newdata;
		}
		if (data.get(0).equals("PAGE-STATUS::found")){
			data.remove(0);
			newdata.add("PAGE-STATUS::found");
			for (String s : data){
				s = s.replaceAll("\\(.*?\\) ?", "");
				newdata.add(Preprocessing.preprocess(s));
			}
			return newdata;
		}
		if (data.get(0).equals("PAGE-STATUS::redirected")){
			data.remove(0);
			newdata.add("PAGE-STATUS::redirected");
			for (String s : data){
				s = s.replaceAll("\\(.*?\\) ?", "");
				newdata.add(Preprocessing.preprocess(s));
			}
			return newdata;
		}
		System.out.println("ISSUE WITH: " + label + ". Added the label to the labellist and moving on.");
		return newdata;
	}
	
	private ArrayList<String> processSearchTerms(String label, ArrayList<String> data, String SorT) {
		ArrayList<String> newdata = new ArrayList<String>();
		if (data.get(0).equals("PAGE-STATUS::search")){
			data.remove(0);
			for (String s : data){
				if (s.startsWith(SorT)){
					s = s.replaceAll(SorT, "");
					if (SorT.equals("S:")){
						s = s.replace("<span class=\"searchmatch\">", "");
						s = s.replace("</span>", "");
					}
					s = s.replaceAll("\\(.*?\\) ?", "");
					newdata.add(Preprocessing.preprocess(s));
				}
			}
			data.add(0, "PAGE-STATUS::search");
			if (SorT.equals("T:")) newdata.add(label);
			return newdata;
		}
		return newdata;
	}
	
	public void TFIDFAlignment(URI uri1, URI uri2, String name1, String name2, PrintWriter output) throws Exception{
		
		Set<OWLEntity> aEntities = new HashSet<>();
		Set<OWLEntity> bEntities = new HashSet<>();
		try {
			aEntities = owldata.getData(uri1, "A");
			bEntities = owldata.getData(uri2, "B");
		} catch (Exception e) {
			System.out.println("Error retrieving initial data from OWL files.");
			System.exit(0);
		}
		
		if (this.util.allKeys().size() != 5093){
			generateData(aEntities, "A");
			this.util.saveCache();
			generateData(bEntities, "B");
			this.util.saveCache();
		}
		
		
		HashMap<String, String> allArticles = new HashMap<>();
		HashMap<String, String> allSnippets = new HashMap<>();
		
		for (String label : this.util.allKeys()){
			EntityData tmp = this.util.getValue(label);
			if (!tmp.getArticle().equals("")){
				allArticles.put(label, tmp.getArticle());
			}
			if (!tmp.getSnippet().equals("")){
				allSnippets.put(label, tmp.getArticle());
			}
		}
		
		DocumentParser articleTFIDF = new DocumentParser(allArticles);
		HashMap<String, double[]> x = articleTFIDF.tfIdfCalculator();
		for(String thing : x.keySet()){
			double[] t = x.get(thing);
			EntityData tmp = this.util.getValue(thing);
			tmp.setTfidfScoreArticle(t);
			this.util.storeValue(thing, tmp);
		}
		this.util.saveCache();
		DocumentParser snippetTFIDF = new DocumentParser(allSnippets);
		HashMap<String, double[]> h = snippetTFIDF.tfIdfCalculator();
		for(String thing : h.keySet()){
			double[] t = h.get(thing);
			EntityData tmp = this.util.getValue(thing);
			tmp.setTfidfScoreSnippet(t);
			this.util.storeValue(thing, tmp);
		}
		this.util.saveCache();
		int i = 0;
		for (OWLEntity a : aEntities){
			if(i % 10 == 0){
				System.out.println("i = " + i);
			}
			i++;
			String labelA = owldata.preprocessLabel(a, "A");
			EntityData dataA = null;
			if (this.util.hasValue(labelA)){
				dataA = this.util.getValue(labelA);
			} else {
				continue;
			}
			
			for (OWLEntity b : bEntities){
				String labelB = owldata.preprocessLabel(b, "B");
				EntityData dataB = null;
				if (this.util.hasValue(labelB)){
					dataB = this.util.getValue(labelB);
				} else {
					continue;
				}
				
				if (labelA.equalsIgnoreCase(labelB)){
					owldata.storeCell(a, b, 1.0);
					output.println("EQUAL MATCH FOUND: " + labelA + "  -  " + labelB);
				} else {
					double confidence = 0.0;
					if((dataA.getPageStatus().equals("found") || dataA.getPageStatus().equals("redirected")) && (dataB.getPageStatus().equals("found") || dataB.getPageStatus().equals("redirected"))){
						//article alignment
						confidence = articleTFIDF.getCosineSimilarity(labelA, labelB);
//						confidence = articleTFIDF.getCosineSimilarity(dataA.getTfidfScoreArticle(), dataB.getTfidfScoreArticle());
						if (confidence == 1.0){
							confidence = 0.99;
						}
					} else if (dataA.getPageStatus().equals("search") && dataB.getPageStatus().equals("search")){
						//terms alignment
						confidence = computeLabelConfidence(dataA, dataB);
					} else if ((dataA.getPageStatus().equals("search") && (dataB.getPageStatus().equals("found") || dataB.getPageStatus().equals("redirected"))) || (dataB.getPageStatus().equals("search") && (dataA.getPageStatus().equals("found") || dataA.getPageStatus().equals("redirected")))){
						//snippet alignment
						confidence = snippetTFIDF.getCosineSimilarity(labelA, labelB);
						if (confidence == 1.0){
							confidence = 0.99;
						}
					} 
					if (confidence > 0.1){
						owldata.storeCell(a, b, confidence);
						output.println("WIKI MATCH FOUND: " + labelA + "  -  " + labelB);
					}
				}
			}
		}
		owldata.completeAlignment(name1, name2);
		owldata.cleanUp();
		util.saveCache();
		util.printCache();
		
	}


	@SuppressWarnings("unchecked")
	private double computeLabelConfidence(EntityData a, EntityData b){
		double confidence = 0.0;
		HashSet<String> x = (HashSet<String>) Arrays.asList(a.getTerms());
		HashSet<String> y = (HashSet<String>) Arrays.asList(b.getTerms());
		if( x.size() == 0 || y.size() == 0 ) {
            return 0.0;
        }
        
        Set<String> unionXY = new HashSet<String>(x);
        unionXY.addAll(y);
        
        Set<String> intersectionXY = new HashSet<String>(x);
        intersectionXY.retainAll(y);
        
        confidence = (double) intersectionXY.size() / (double) unionXY.size();
		if (confidence == 1.0){
			confidence = 0.99;
		}
        return confidence;
	}
	
	

	
	
	public static void doConference() throws Exception{
		FileOutputStream os1 = new FileOutputStream("./logging/conference_logging.txt");
		PrintWriter output = new PrintWriter(new OutputStreamWriter(os1,"UTF-8"), true);
		
		String[] files = {"./data/conference_v1/cmt.owl", "./data/conference_v1/conference.owl", "./data/conference_v1/confOf.owl",  "./data/conference_v1/edas.owl", "./data/conference_v1/ekaw.owl", "./data/conference_v1/iasted.owl", "./data/conference_v1/sigkdd.owl"};
		TFIDFAlign aligner = new TFIDFAlign("conference_v1");
		
		for (int i = 0; i < files.length; i++) {
			File f1 = new File(files[i]);
			if (!f1.toString().endsWith(".owl"))
				continue;
			for (int j = i; j < files.length; j++) {
				
				File f2 = new File(files[j]);
				if (!f2.toString().endsWith(".owl") || f1.toString().equals(f2.toString()))
					continue;

				String s1 = f1.toString();
				String s2 = f2.toString();
				String name1 = s1.substring(s1.lastIndexOf("/") + 1,s1.lastIndexOf("."));
				String name2 = s2.substring(s2.lastIndexOf("/") + 1,s2.lastIndexOf("."));

				System.out.println("\tAligning " + name1 + " and " + name2);

				aligner.TFIDFAlignment(f1.toURI(), f2.toURI(), name1, name2, output);
				
			}
		}
		output.close();
	}
	
	public static void doAnatomy() throws Exception{
		DateFormat dateFormat = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss");
		Date date = new Date();
		System.out.println("start: " + dateFormat.format(date));
		FileOutputStream os1 = new FileOutputStream("./logging/anatomy_logging.txt");
		PrintWriter output = new PrintWriter(new OutputStreamWriter(os1,"UTF-8"), true);
		
		TFIDFAlign aligner = new TFIDFAlign("anatomy");
		
		File f1 = new File("./data/anatomy/mouse.owl");
		File f2 = new File("./data/anatomy/human.owl");
				
		String s1 = f1.toString();
		String s2 = f2.toString();
		String name1 = s1.substring(s1.lastIndexOf("/") + 1,s1.lastIndexOf("."));
		String name2 = s2.substring(s2.lastIndexOf("/") + 1,s2.lastIndexOf("."));

		System.out.println("\tAligning " + name1 + " and " + name2);

		aligner.TFIDFAlignment(f1.toURI(), f2.toURI(), name1, name2, output);
				
		output.close();
		Date date2 = new Date();
		System.out.println("end: " +dateFormat.format(date2));
	}
	
	
	public static void main(String[] args) throws Exception {
//		doConference();
		doAnatomy();
//		TFIDFAlign aligner = new TFIDFAlign("test");
//		
//		String[] entities = new String[]{"Barack Obama", "Michele Obama", "asdgfaerav", "Gut epithelium"};
//		
//		aligner.generateData(entities, "A");
	}
}
