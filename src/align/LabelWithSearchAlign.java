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

import com.wcohen.ss.Levenstein;

import fr.inrialpes.exmo.align.impl.URIAlignment;
import util.OWLData;
import util.Preprocessing;
import util.Utilities;
import wiki.WikipediaOperations;

public class LabelWithSearchAlign extends URIAlignment{
	
	private Levenstein levensteinCompare = new Levenstein();
	private OWLData owldata;
	private Utilities util;
	
	LabelWithSearchAlign(String ontology){
		this.owldata = new OWLData(ontology);
		this.util = new Utilities(ontology, "search");
	}
	
	public void labelWithSearchAlign(URI uri1, URI uri2, String name1, String name2, PrintWriter output) throws Exception{
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
			System.out.println(e.toString());
			System.exit(0);
		}
		int i = 0;
		for (OWLEntity a : aEntities){
			if(i % 10 == 0){
				System.out.println("i = " + i);
//				util.saveCache();
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
//				if((a.isOWLClass() && !b.isOWLClass()) || (b.isOWLClass() && !a.isOWLClass())) continue;
				
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
				
				double confidence = computeConfidence(dataA, dataB);
//				double levensteinScore = (1 - Math.abs(levensteinCompare.score(labelA, labelB)) / (Math.max(labelA.length(), labelB.length()))) ;
				//compare the terms for a and b, store if find a match
//				if (levensteinScore >= 0.9){
//					owldata.storeCell(a, b, levensteinScore);
//					output.println("EQUAL MATCH FOUND: " + labelA + "  -  " + labelB);
//				} 
				if (labelA.equalsIgnoreCase(labelB)){
					owldata.storeCell(a, b, 1.0);
//					output.println("EQUAL MATCH FOUND: " + labelA + "  -  " + labelB);
				}
//				else if ((!dataA.isEmpty() && !dataB.isEmpty()) && (dataA.containsAll(dataB) || dataB.containsAll(dataA))){
//					owldata.storeCell(a, b, 0.9);
//					output.println("WIKI MATCH FOUND: " + labelA + "  -  " + labelB);
//					output.println(dataA);
//					output.println(dataB);
////				}
				else if ((!dataA.isEmpty() && !dataB.isEmpty()) &&  confidence > 0.7){
					owldata.storeCell(a, b, confidence);
					output.println("WIKI MATCH FOUND: " + labelA + "  -  " + labelB);
					output.println(dataA);
					output.println(dataB);
				}
				
			}
		}
		owldata.completeAlignment(name1, name2);
		owldata.cleanUp();
		util.saveCache();
		util.printCache();
		
	}
	
	private double computeConfidence(ArrayList<String> x, ArrayList<String> y){
		if( x.size() == 0 || y.size() == 0 ) {
            return 0.0;
        }
        
        Set<String> unionXY = new HashSet<String>(x);
        unionXY.addAll(y);
        
        Set<String> intersectionXY = new HashSet<String>(x);
        intersectionXY.retainAll(y);
        
        double confidence = (double) intersectionXY.size() / (double) unionXY.size();
		if (confidence == 1.0){
			confidence = 0.99;
		}
        return confidence;
	}
	
	private ArrayList<String> getData(String label, PrintWriter output) {
		ArrayList<String> data = processTerms(label, WikipediaOperations.getRedirectsAlias(label));
		for( int i = 0; (data.size() == 1 && data.get(0).equals("") && i < 2); i++){
			try {
				Thread.sleep(250);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
			data = processTerms(label, WikipediaOperations.getRedirectsAlias(label));
		}
		if (data.size() <= 1){
			data = processTerms(label, WikipediaOperations.getSearchOptions(label));
		}
		if (data.size() == 0){
			data.add(label);
		}
		output.print("Label: " + label + "    TERMS: ");
		output.println(data);
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
			System.exit(0);
		}
		if (data.get(0).equals("INTERNAL-ERROR")){
			System.out.println("Internal Error for: " + label + ". Added the label to the label; list and moving on.");
			newdata.add(label);
			return newdata;
		} 
		if (data.get(0).equals("PAGE-STATUS::missing")){
			// Need to now search using the open search thing...but for now just return  
			newdata.add(label);
			return newdata;
		}
		if (data.get(0).equals("PAGE-STATUS::disambiguation")){
			//For now just keep all the possible links.. change later to something else..
			data.remove(0);
			for (String s : data){
				s = s.replaceAll("\\(.*?\\) ?", "");
				newdata.add(Preprocessing.preprocess(s));
			}
			return newdata;
		}
		if (data.get(0).equals("PAGE-STATUS::found") || data.get(0).equals("PAGE-STATUS::redirected")){
			data.remove(0);
			for (String s : data){
				s = s.replaceAll("\\(.*?\\) ?", "");
				newdata.add(Preprocessing.preprocess(s));
			}
			return newdata;
		}
		if (data.get(0).equals("PAGE-STATUS::search")){
			data.remove(0);
			for (String s : data){
				if (s.startsWith("T:")){
					s = s.replaceAll("T:", "");
					s = s.replaceAll("\\(.*?\\) ?", "");
					newdata.add(Preprocessing.preprocess(s));
				}
			}
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
		LabelWithSearchAlign aligner = new LabelWithSearchAlign("conference_v1");
		
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

				aligner.labelWithSearchAlign(f1.toURI(), f2.toURI(), name1, name2, output);
				
			}
		}
		output.close();
	}
	
	public static void doAnatomy() throws Exception{
		FileOutputStream os1 = new FileOutputStream("./logging/anatomy_logging.txt");
		PrintWriter output = new PrintWriter(new OutputStreamWriter(os1,"UTF-8"), true);
		
		LabelWithSearchAlign aligner = new LabelWithSearchAlign("anatomy");
		
		File f1 = new File("./data/anatomy/mouse.owl");
		File f2 = new File("./data/anatomy/human.owl");
				
		String s1 = f1.toString();
		String s2 = f2.toString();
		String name1 = s1.substring(s1.lastIndexOf("/") + 1,s1.lastIndexOf("."));
		String name2 = s2.substring(s2.lastIndexOf("/") + 1,s2.lastIndexOf("."));

		System.out.println("\tAligning " + name1 + " and " + name2);

		aligner.labelWithSearchAlign(f1.toURI(), f2.toURI(), name1, name2, output);
				
		output.close();
	}
	
	public static void doGeo() throws Exception{
		FileOutputStream os1 = new FileOutputStream("./logging/geo_logging.txt");
		PrintWriter output = new PrintWriter(new OutputStreamWriter(os1,"UTF-8"), true);
		
		LabelWithSearchAlign aligner = new LabelWithSearchAlign("geo");
		
		File f1 = new File("./data/geo/envo.owl");
		File f2 = new File("./data/geo/whole_realm.owl");
				
		String s1 = f1.toString();
		String s2 = f2.toString();
		String name1 = s1.substring(s1.lastIndexOf("/") + 1,s1.lastIndexOf("."));
		String name2 = s2.substring(s2.lastIndexOf("/") + 1,s2.lastIndexOf("."));

		System.out.println("\tAligning " + name1 + " and " + name2);

		aligner.labelWithSearchAlign(f1.toURI(), f2.toURI(), name1, name2, output);
				
		output.close();
	}
	
	public static void doHydro() throws Exception{
		FileOutputStream os1 = new FileOutputStream("./logging/hydro_logging.txt");
		PrintWriter output = new PrintWriter(new OutputStreamWriter(os1,"UTF-8"), true);
		
		LabelWithSearchAlign aligner = new LabelWithSearchAlign("hydro");
		
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

				aligner.labelWithSearchAlign(f1.toURI(), f2.toURI(), name1, name2, output);
			}
		}
				
		output.close();
	}
	
	
	public static void main(String[] args) throws Exception {
//		doConference();
//		doAnatomy();
//		doGeo();
		doHydro();
	}
}
