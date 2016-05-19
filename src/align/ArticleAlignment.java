package align;

import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.URI;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;

import org.semanticweb.owlapi.model.OWLEntity;



import fr.inrialpes.exmo.align.impl.URIAlignment;
import util.OWLData;
import util.Preprocessing;
import util.Utilities;
import wiki.WikipediaOperations;

public class ArticleAlignment extends URIAlignment{
	
	private OWLData owldata;
	private Utilities util;
	
	ArticleAlignment(String ontology){
		this.owldata = new OWLData(ontology);
		this.util = new Utilities(ontology, "article");
	}
	
	public void articleAlign(URI uri1, URI uri2, String name1, String name2, PrintWriter output) throws Exception{
		super.init(uri1, uri2);
		Set<OWLEntity> aEntities = new HashSet<>();
		Set<OWLEntity> bEntities = new HashSet<>();
		try {
			aEntities = owldata.getData(uri1, "A");
			bEntities = owldata.getData(uri2, "B");
		} catch (Exception e) {
			System.out.println("Error retrieving initial data from OWL files.");
			System.exit(0);
		}
		int i = 0;
		for (OWLEntity a : aEntities){
			if(i % 10 == 0){
				System.out.println("i = " + i);
			}
			if(i % 100 == 0){
				this.util.saveCache();
			}
			i++;
			String labelA = owldata.preprocessLabel(a, "A");
			if (labelA.contains("Hy ")){
				labelA = labelA.replace("Hy ", "");
			}
			ArrayList<String> dataA = null;
			if (this.util.hasValue(labelA)){
				dataA = this.util.getValue(labelA);
			} else {
				dataA = getData(labelA, output);
			}
			
			for (OWLEntity b : bEntities){
				String labelB = owldata.preprocessLabel(b, "B");
				if (labelB.contains("Hy ")){
					labelB = labelB.replace("Hy ", "");
				}
				ArrayList<String> dataB = null;
				if (this.util.hasValue(labelB)){
					dataB = this.util.getValue(labelB);
				} else {
					dataB = getData(labelB, output);
				}
				
				double confidence = computeConfidence(dataA, dataB, labelA, labelB);
				if (labelA.equalsIgnoreCase(labelB)){
					owldata.storeCell(a, b, 1.0);
					output.println("EQUAL MATCH FOUND: " + labelA + "  -  " + labelB);
				}
				else if (confidence >= 0.7){
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
		if( a.size() == 0 || b.size() == 0 ) {
            return 0.0;
        }
		if( a.size() == 1 || b.size() == 1 ) {
			if (a.equals(b) && !a.get(0).equals("") && !b.get(0).equals("")){
				return 0.9;
			}
		}
//			if (a.contains(labelB) && b.contains(labelA)){
//	    		return 0.8;
//	    	}
//			if (a.contains(labelB) || b.contains(labelA)){
//	    		return 0.7;
//	    	}
//        }
//		if (a.size() == 1 && b.size() == 0){
//	    	if (a.contains(labelB)){
//	    		return 0.7;
//	    	}
//		} if (b.size() == 1 && a.size() == 0){
//	    	if (b.contains(labelA)){
//	    		return 0.7;
//	    	}
//	    }
		return 0.0;
	}
	
	private ArrayList<String> getData(String label, PrintWriter output) {
		ArrayList<String> data = processTerms(label, WikipediaOperations.getArticleSnippet(label));
		if (data.size() == 0){
			return data;
		}
		for( int i = 0; ((data.size() == 1 && data.get(0).equals("")) && i < 2); i++){
			try {
				Thread.sleep(250);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
			data = processTerms(label, WikipediaOperations.getArticle(label));
		}
		
		
		if (data.size() == 1 && data.get(0).equals("") || data.size() == 0){
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
//		output.print("Label: " + label + "    DATA: ");
//		output.println(data);
		this.util.storeValue(label, data);
		return data;
	}

	private ArrayList<String> processTerms(String label, ArrayList<String> data) {
		ArrayList<String> newdata = new ArrayList<String>();
		if (data.get(0).equals("PAGE-STATUS::API-ERROR")){
			System.out.println("There is a timeout or some other error with API. Saving cache and exiting.");
			this.util.saveCache();
			this.util.printCache();
			System.out.println("Logging: label error occured: " + label);
			try {
				Thread.sleep(5000);
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		if (data.get(0).equals("INTERNAL-ERROR")){
			System.out.println("Internal Error for: " + label + ". Added the label to the label; list and moving on.");
			newdata.add("");
			return newdata;
		} 
		if (data.get(0).equals("PAGE-STATUS::missing")){
			newdata.add("");
			return newdata;
		}
		if (data.get(0).equals("PAGE-STATUS::disambiguation")){
			newdata.add("");
			return newdata;
		}
		if (data.get(0).equals("PAGE-STATUS::found") || data.get(0).equals("PAGE-STATUS::redirected")){
			newdata.add(data.get(1).replaceAll("[^A-Za-z0-9 ]", ""));
			return newdata;
		}
		if (data.get(0).equals("PAGE-STATUS::search")){
			data.remove(0);	
			ArrayList<String> tmp = new ArrayList<>();
			for (int i = 0; i < 3 && i < data.size(); i ++){
				if (data.get(i).startsWith("T:")){
					String s = data.get(i).replaceAll("T:", "");
					newdata.add(s);
				}
			}
			String finalString = "";
			for (String s : newdata){
				ArrayList<String> nextdata = WikipediaOperations.getArticle(s);
				if (nextdata.size() > 1 && nextdata.get(0).equals("PAGE-STATUS::found")){
					finalString += nextdata.get(1).replaceAll("[^A-Za-z0-9 ]", "") + " ";
				}
			}
			newdata.add(finalString);
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
		ArticleAlignment aligner = new ArticleAlignment("conference_v1");
		
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

				aligner.articleAlign(f1.toURI(), f2.toURI(), name1, name2, output);
				
			}
		}
		output.close();
	}
	
	public static void doAnatomy() throws Exception{
		FileOutputStream os1 = new FileOutputStream("./logging/anatomy_logging.txt");
		PrintWriter output = new PrintWriter(new OutputStreamWriter(os1,"UTF-8"), true);
		
		ArticleAlignment aligner = new ArticleAlignment("anatomy");
		
		File f1 = new File("./data/anatomy/mouse.owl");
		File f2 = new File("./data/anatomy/human.owl");
				
		String s1 = f1.toString();
		String s2 = f2.toString();
		String name1 = s1.substring(s1.lastIndexOf("/") + 1,s1.lastIndexOf("."));
		String name2 = s2.substring(s2.lastIndexOf("/") + 1,s2.lastIndexOf("."));

		System.out.println("\tAligning " + name1 + " and " + name2);

		aligner.articleAlign(f1.toURI(), f2.toURI(), name1, name2, output);
				
		output.close();
	}
	
	public static void doGeo() throws Exception{
		FileOutputStream os1 = new FileOutputStream("./logging/geo_logging.txt");
		PrintWriter output = new PrintWriter(new OutputStreamWriter(os1,"UTF-8"), true);
		
		ArticleAlignment aligner = new ArticleAlignment("geo");
		
		File f1 = new File("./data/geo/envo.owl");
		File f2 = new File("./data/geo/whole_realm.owl");
				
		String s1 = f1.toString();
		String s2 = f2.toString();
		String name1 = s1.substring(s1.lastIndexOf("/") + 1,s1.lastIndexOf("."));
		String name2 = s2.substring(s2.lastIndexOf("/") + 1,s2.lastIndexOf("."));

		System.out.println("\tAligning " + name1 + " and " + name2);

		aligner.articleAlign(f1.toURI(), f2.toURI(), name1, name2, output);
				
		output.close();
	}
	
	public static void doHydro() throws Exception{
		FileOutputStream os1 = new FileOutputStream("./logging/hydro_logging.txt");
		PrintWriter output = new PrintWriter(new OutputStreamWriter(os1,"UTF-8"), true);
		
		ArticleAlignment aligner = new ArticleAlignment("hydro");
		
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

				aligner.articleAlign(f1.toURI(), f2.toURI(), name1, name2, output);
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
