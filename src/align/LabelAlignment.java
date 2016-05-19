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

public class LabelAlignment  extends URIAlignment{
	
	private OWLData owldata;
	private Utilities util;
	
	LabelAlignment(String ontology){
		this.owldata = new OWLData(ontology);
		this.util = new Utilities(ontology, "label");
	}
	
	public void labelAlign(URI uri1, URI uri2, String name1, String name2, PrintWriter output) throws Exception{
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
			i++;
			String labelA = owldata.preprocessLabel(a, "A");
			ArrayList<String> dataA = null;
			if (this.util.hasValue(labelA)){
				dataA = this.util.getValue(labelA);
				if (dataA.size() == 1 && dataA.get(0).equals("")){
					dataA = processTerms(labelA, WikipediaOperations.getRedirectsAlias(labelA));
					output.print("Label: " + labelA + "    TERMS: ");
					output.println(dataA);
					this.util.storeValue(labelA, dataA);
				}
			} else {
				dataA = processTerms(labelA, WikipediaOperations.getRedirectsAlias(labelA));
				output.print("Label: " + labelA + "    TERMS: ");
				output.println(dataA);
				this.util.storeValue(labelA, dataA);
			}
			
			for (OWLEntity b : bEntities){
				String labelB = owldata.preprocessLabel(b, "B");
				ArrayList<String> dataB = null;
				if (this.util.hasValue(labelB)){
					dataB = this.util.getValue(labelB);
					if (dataB.size() == 1 && dataB.get(0).equals("")){
						dataB = processTerms(labelB, WikipediaOperations.getRedirectsAlias(labelB));
						output.print("Label: " + labelB + "    TERMS: ");
						output.println(dataB);
						this.util.storeValue(labelB, dataB);
					}
				} else {
					dataB = processTerms(labelB, WikipediaOperations.getRedirectsAlias(labelB));
					output.print("Label: " + labelB + "    TERMS: ");
					output.println(dataB);
					this.util.storeValue(labelB, dataB);
				}
				
				
				//compare the terms for a and b, store if find a match
				if (labelA.equals(labelB)){
					owldata.storeCell(a, b, 1.0);
					output.println("EQUAL MATCH FOUND: " + labelA + "  -  " + labelB);
				} 
//				else if (dataA.contains(labelB) && dataB.contains(labelA)){
//					owldata.storeCell(a, b, 0.9);
//					output.println("WIKI MATCH FOUND: " + labelA + "  -  " + labelB);
//					System.out.println("WIKI MATCH FOUND: " + labelA + "  -  " + labelB);
//					System.out.println(dataA);
//					System.out.println(dataB);
//				}
				else if ((!dataA.isEmpty() && !dataB.isEmpty()) && (dataA.containsAll(dataB) || dataB.containsAll(dataA))){
					owldata.storeCell(a, b, 0.9);
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
	
	private ArrayList<String> processTerms(String label, ArrayList<String> data) {
		ArrayList<String> newdata = new ArrayList<String>();
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
		System.out.println(data);
		System.out.println("ISSUE WITH: " + label + ". Added the label to the labellist and moving on.");
		newdata.add(label);
		return newdata;
	}
	
	public static void doConference() throws Exception{
		FileOutputStream os1 = new FileOutputStream("./logging/conference_logging.txt");
		PrintWriter output = new PrintWriter(new OutputStreamWriter(os1,"UTF-8"), true);
		
		String[] files = {"./data/conference_v1/cmt.owl", "./data/conference_v1/conference.owl", "./data/conference_v1/confOf.owl",  "./data/conference_v1/edas.owl", "./data/conference_v1/ekaw.owl", "./data/conference_v1/iasted.owl", "./data/conference_v1/sigkdd.owl"};
		LabelAlignment aligner = new LabelAlignment("conference_v1");
		
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

				aligner.labelAlign(f1.toURI(), f2.toURI(), name1, name2, output);
				
			}
		}
		output.close();
	}
	
	public static void doAnatomy() throws Exception{
		FileOutputStream os1 = new FileOutputStream("./logging/anatomy_logging.txt");
		PrintWriter output = new PrintWriter(new OutputStreamWriter(os1,"UTF-8"), true);
		
		LabelAlignment aligner = new LabelAlignment("anatomy");
		
		File f1 = new File("./data/anatomy/mouse.owl");
		File f2 = new File("./data/anatomy/human.owl");
				
		String s1 = f1.toString();
		String s2 = f2.toString();
		String name1 = s1.substring(s1.lastIndexOf("/") + 1,s1.lastIndexOf("."));
		String name2 = s2.substring(s2.lastIndexOf("/") + 1,s2.lastIndexOf("."));

		System.out.println("\tAligning " + name1 + " and " + name2);

		aligner.labelAlign(f1.toURI(), f2.toURI(), name1, name2, output);
				
		output.close();
	}
	
	
	public static void main(String[] args) throws Exception {
		doConference();
//		doAnatomy();
	}
}
