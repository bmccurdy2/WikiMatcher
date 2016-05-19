package align;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.URI;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;

import org.semanticweb.owlapi.model.OWLEntity;

import com.wcohen.ss.Levenstein;

import fr.inrialpes.exmo.align.impl.URIAlignment;
import util.OWLData;
import util.Preprocessing;
import util.Utilities;
import wiki.WikipediaOperations;

public class SnippetAlignment extends URIAlignment{
	
	private OWLData owldata;
	private Utilities util;
	private Set<String> stopwords = new HashSet<>();
	SnippetAlignment(String ontology){
		this.owldata = new OWLData(ontology);
		this.util = new Utilities(ontology, "snippet");
		try (BufferedReader br = new BufferedReader(new FileReader("./data/stopwords/stopwords.txt"))) {
		    String line;
		    ArrayList<String> tmp = new ArrayList<>();
		    while ((line = br.readLine()) != null) {
		    	tmp.add(line);
		    }
		    stopwords.addAll(tmp);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	public void snippetAlign(URI uri1, URI uri2, String name1, String name2, PrintWriter output) throws Exception{
		super.init(uri1, uri2);
		Set<OWLEntity> aEntities = new HashSet<>();
		Set<OWLEntity> bEntities = new HashSet<>();
		try {
			System.out.println("DOING A");
			aEntities = owldata.getData(uri1, "A");
			System.out.println("DOING B");
			bEntities = owldata.getData(uri2, "B");
		} catch (Exception e) {
			System.out.println("Error retrieving initial data from OWL files.");
			System.exit(0);
		}
		int i = 0;
		for (OWLEntity a : aEntities){
			if(i % 10 == 0){
				System.out.println("i = " + i);
//				util.saveCache();
			}
			if(i % 100 == 0){
				this.util.saveCache();
			}
			i++;
			String labelA = owldata.preprocessLabel(a, "A");
//			if (labelA.contains("Hy ")){
//				labelA = labelA.replace("Hy ", "");
//			}
			ArrayList<String> dataA = null;
			if (this.util.hasValue(labelA)){
				dataA = this.util.getValue(labelA);
			} else {
				dataA = getData(labelA, output);
//				System.out.println(labelA + "  - " + dataA);
			}
			
			for (OWLEntity b : bEntities){
				String labelB = owldata.preprocessLabel(b, "B");
//				if (labelB.contains("Hy ")){
//					labelB = labelB.replace("Hy ", "");
//				}
				ArrayList<String> dataB = null;
				if (this.util.hasValue(labelB)){
					dataB = this.util.getValue(labelB);
				} else {
					dataB = getData(labelB, output);
//					System.out.println(labelB + "  - " + dataB);
				}
				double confidence = computeConfidence(dataA, dataB, labelA, labelB);
				if (labelA.equalsIgnoreCase(labelB)){
					owldata.storeCell(a, b, 1.0);
					output.println("EQUAL MATCH FOUND: " + labelA + "  -  " + labelB);
				}
				else if (confidence > 0.5){
					owldata.storeCell(a, b, confidence);
					output.println("WIKI MATCH FOUND: " + labelA + "  -  " + labelB);
					output.println("dataA: " + dataA);
					output.println("dataB: " + dataB);
				}
				
				
			}
		}
		owldata.completeAlignment(name1, name2);
		owldata.cleanUp();
		util.saveCache();
		util.printCache();
		
	}
	
	private double computeConfidence(ArrayList<String> a, ArrayList<String> b, String labelA, String labelB){
		double confidence = 0.0;
		if( a.size() == 0 || b.size() == 0 ) {
            return 0.0;
        } else if (a.size() == 1 && b.size() == 1){
        	if (a.get(0).equals(b.get(0)) && !a.get(0).equals("") && !b.get(0).equals("")){
        		return 0.99;
        	}
        	ArrayList<String> wordsA = new ArrayList<>();
        	for( String word : a.get(0).split("\\s+")){
        		if (!stopwords.contains(word)){
        			wordsA.add(word);
        		}
        	}
        	ArrayList<String> wordsB = new ArrayList<>();
        	for( String word : b.get(0).split("\\s+")){
        		if (!stopwords.contains(word)){
        			wordsB.add(word);
        		}
        	}
        	
            if (wordsB.size() > 1 && wordsA.size() > 1){
            	Set<String> unionXY = new HashSet<String>(wordsA);
                unionXY.addAll(wordsB);
                
                Set<String> intersectionXY = new HashSet<String>(wordsA);
                intersectionXY.retainAll(wordsB);
                
                confidence = (double) intersectionXY.size() / (double) unionXY.size();
        		
            }
        } else if (a.size() == 1 && b.size() == 0){
        	if (a.contains(labelB)){
        		confidence = 0.8;
        	}
        } else if (b.size() == 1 && a.size() == 0){
        	if (b.contains(labelA)){
        		confidence = 0.8;
        	}
        }
        
        if (confidence == 1.0){
			confidence = 0.99;
		} 
        
        return confidence;
	}
	
	private ArrayList<String> getData(String label, PrintWriter output) {
		ArrayList<String> data = processTerms(label, WikipediaOperations.getArticleSnippet(label));
		for( int i = 0; ((data.size() == 1 && data.get(0).equals("")) && i < 2); i++){
			try {
				Thread.sleep(250);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
			data = processTerms(label, WikipediaOperations.getArticleSnippet(label));
		}
		
		
		if ((data.size() == 1 && data.get(0).equals("")) || data.contains("MISSING") || data.size() == 0){
			data.clear();
			data.add(0, "");
			for( int i = 0; ((data.size() == 1 && data.get(0).equals("")) && i < 2); i++){
				try {
					Thread.sleep(250);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
				data = processTerms(label, WikipediaOperations.getSearchOptions(label));
			}
		}
		if (data.size() == 0){
			data.add(label);
		}
		output.print("Label: " + label + "    DATA: ");
		output.println(data);
		this.util.storeValue(label, data);
		return data;
	}

	private ArrayList<String> processTerms(String label, ArrayList<String> data) {
		ArrayList<String> newdata = new ArrayList<String>();
		if (data.get(0).equals("PAGE-STATUS::API-ERROR")){
			System.out.println("There is a timeout or some other error with API. Saving cache and exiting.");
			this.util.saveCache();
			System.out.println("Logging: label error occured: " + label);
//			System.exit(0);
			try {
				Thread.sleep(5000);
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		if (data.get(0).equals("INTERNAL-ERROR")){
			System.out.println("Internal Error for: " + label + ". Returning blank list.");
			newdata.add("");
			return newdata;
		} 
		if (data.get(0).equals("PAGE-STATUS::missing")){
			// for now return null 
			newdata.clear();
			newdata.add("MISSING");
			return newdata;
		}
		if (data.get(0).equals("PAGE-STATUS::disambiguation")){
			
			//TODO - for now just do the opensearch thing - might be better because we get a sample of possibilities
			newdata.clear();
			newdata.add("MISSING");
			return newdata;
		}
		if (data.get(0).equals("PAGE-STATUS::found") || data.get(0).equals("PAGE-STATUS::redirected")){
			String s = data.get(1);
			s = s.replaceAll("\\(.*?\\) ?", "");
			newdata.add(Preprocessing.preprocess(s));
			return newdata;
		}
		if (data.get(0).equals("PAGE-STATUS::search")){
			data.remove(0);
			String tmp = "";
			for (String s : data){
				if (s.startsWith("S:")){
					s = s.replace("S:", "");
					s = s.replaceAll("\\(.*?\\) ?", "");
					s = s.replace("<span class=\"searchmatch\">", "");
					s = s.replace("</span>", "");
					tmp = tmp + " " + s;
				}
			}
			newdata.add(Preprocessing.preprocess(tmp));
			return newdata;
		}
		System.out.println(data);
		System.out.println("ISSUE WITH: " + label + ". Added the label to the labellist and moving on.");
		newdata.add(label);
		return newdata;
	}
	
	public static void doConference() throws Exception{
		FileOutputStream os1 = new FileOutputStream("./logging/conference_logging.txt");
		PrintWriter output = new PrintWriter(new OutputStreamWriter(os1,"UTF-8"), true);
		
		String[] files = {"./data/conference_v1/cmt.owl", "./data/conference_v1/conference.owl", "./data/conference_v1/confOf.owl",  "./data/conference_v1/edas.owl", "./data/conference_v1/ekaw.owl", "./data/conference_v1/iasted.owl", "./data/conference_v1/sigkdd.owl"};
		SnippetAlignment aligner = new SnippetAlignment("conference_v1");
		
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

				aligner.snippetAlign(f1.toURI(), f2.toURI(), name1, name2, output);
				
			}
		}
		output.close();
	}
	
	public static void doAnatomy() throws Exception{
		FileOutputStream os1 = new FileOutputStream("./logging/anatomy_logging.txt");
		PrintWriter output = new PrintWriter(new OutputStreamWriter(os1,"UTF-8"), true);
		
		SnippetAlignment aligner = new SnippetAlignment("anatomy");
		
		File f1 = new File("./data/anatomy/mouse.owl");
		File f2 = new File("./data/anatomy/human.owl");
				
		String s1 = f1.toString();
		String s2 = f2.toString();
		String name1 = s1.substring(s1.lastIndexOf("/") + 1,s1.lastIndexOf("."));
		String name2 = s2.substring(s2.lastIndexOf("/") + 1,s2.lastIndexOf("."));

		System.out.println("\tAligning " + name1 + " and " + name2);

		aligner.snippetAlign(f1.toURI(), f2.toURI(), name1, name2, output);
				
		output.close();
	}
	
	public static void doGeo() throws Exception{
		FileOutputStream os1 = new FileOutputStream("./logging/geo_logging.txt");
		PrintWriter output = new PrintWriter(new OutputStreamWriter(os1,"UTF-8"), true);
		
		SnippetAlignment aligner = new SnippetAlignment("geo");
		
		File f1 = new File("./data/geo/envo.owl");
		File f2 = new File("./data/geo/whole_realm.owl");
				
		String s1 = f1.toString();
		String s2 = f2.toString();
		String name1 = s1.substring(s1.lastIndexOf("/") + 1,s1.lastIndexOf("."));
		String name2 = s2.substring(s2.lastIndexOf("/") + 1,s2.lastIndexOf("."));

		System.out.println("\tAligning " + name1 + " and " + name2);

		aligner.snippetAlign(f1.toURI(), f2.toURI(), name1, name2, output);
				
		output.close();
	}
	
	public static void doHydro() throws Exception{
		FileOutputStream os1 = new FileOutputStream("./logging/hydro_logging.txt");
		PrintWriter output = new PrintWriter(new OutputStreamWriter(os1,"UTF-8"), true);
		
		SnippetAlignment aligner = new SnippetAlignment("hydro");
		
		File dir = new File("./data/hydro/");
		File[] files = dir.listFiles();
				
		for (int i = 0; i < files.length; i++) {
			File f1 = files[i];
			if (!f1.toString().endsWith(".owl"))
				continue;
			for (int j = i; j < files.length; j++) {
				File f2 = files[j];
				if (!f2.toString().endsWith(".owl") || f1.toString().equals(f2.toString()))
					continue;
				String s1 = f1.toString();
				String s2 = f2.toString();
				String name1 = s1.substring(s1.lastIndexOf("/") + 1,s1.lastIndexOf("."));
				String name2 = s2.substring(s2.lastIndexOf("/") + 1,s2.lastIndexOf("."));

				System.out.println("\tAligning " + name1 + " and " + name2);

				aligner.snippetAlign(f1.toURI(), f2.toURI(), name1, name2, output);
			}
		}
	}
	
	public static void main(String[] args) throws Exception {
//		doConference();
//		doAnatomy();
		doGeo();
//		doHydro();
	}
}
