package extratools;

import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Scanner;
import java.util.Set;

import org.semanticweb.owl.align.Alignment;
import org.semanticweb.owl.align.AlignmentException;
import org.semanticweb.owl.align.Cell;
import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.model.IRI;
import org.semanticweb.owlapi.model.OWLAnnotation;
import org.semanticweb.owlapi.model.OWLEntity;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLOntologyManager;

import fr.inrialpes.exmo.align.parser.AlignmentParser;
import uk.ac.manchester.cs.owl.owlapi.OWLAnnotationPropertyImpl;
import uk.ac.manchester.cs.owl.owlapi.OWLDataFactoryImpl;
import util.Preprocessing;
import wiki.WikipediaOperations;

public class WikiStatsConference {
	
	public static void main(String[] args) throws Exception {
		
		ArrayList<String> myCodeFN = new ArrayList<>();
		ArrayList<String> myCodeFP = new ArrayList<>();
		ArrayList<String> myCodeTP = new ArrayList<>();
		HashMap<String, String> allLabelsA = new HashMap<>();
		HashMap<String, String> allLabelsB = new HashMap<>();
		
		FileOutputStream os1 = new FileOutputStream("./extratools/Conference_myCodeAlignWikiStats.txt");
		PrintWriter output = new PrintWriter(new OutputStreamWriter(os1, "UTF-8"), true);
		
		AlignmentParser parser = new AlignmentParser(0);

		File dir = new File("./data/conference_v1/");

		String[] files = {"./data/conference_v1/cmt.owl", "./data/conference_v1/conference.owl", "./data/conference_v1/confOf.owl",  "./data/conference_v1/edas.owl", "./data/conference_v1/ekaw.owl", "./data/conference_v1/iasted.owl", "./data/conference_v1/sigkdd.owl"};
		
		for (int i=0; i<files.length; i++) {
			File f1 = new File(files[i]);
			if (!f1.toString().endsWith(".owl")) continue;
			for (int j=i; j<files.length; j++) {
				File f2 = new File(files[j]);
				if (!f2.toString().endsWith(".owl") || f1.toString().equals(f2.toString())) continue;
				
				String s1 = f1.toString();
				String s2 = f2.toString();
				String name1 = s1.substring(s1.lastIndexOf("/")+1, s1.lastIndexOf("."));
				String name2 = s2.substring(s2.lastIndexOf("/")+1, s2.lastIndexOf("."));
				
				File answers = new File(dir + "/snippet_results/" + name1 + "-" + name2 + ".rdf"); 
				
				File refFile = new File(dir + "/reference-alignment/" + name1 + "-" + name2 + ".rdf"); 
				
				Alignment matcherAlignment = parser.parse(new FileReader(answers));
				Alignment refAlignment = parser.parse(new FileReader(refFile));
				
				OWLOntologyManager manager = OWLManager.createOWLOntologyManager();
				IRI iriA = IRI.create(f1);
				OWLOntology ontA = manager.loadOntologyFromOntologyDocument(iriA);
				Set<OWLEntity> a = ontA.getSignature();
				for (OWLEntity en : a){
					allLabelsA.put(en.getIRI().toURI().toString(), Preprocessing.preprocess(en, ontA));
				}
				IRI iriB = IRI.create(f2);
				OWLOntology ontB = manager.loadOntologyFromOntologyDocument(iriB);
				Set<OWLEntity> b = ontB.getSignature();
				for (OWLEntity en : b){
					allLabelsB.put(en.getIRI().toURI().toString(), Preprocessing.preprocess(en, ontB));
				}

				// true positives: things in answers that are also in reference
				Enumeration<Cell> cells = matcherAlignment.getElements();
				while (cells.hasMoreElements()) {
					Cell ans = cells.nextElement();
					Enumeration<Cell> refs = refAlignment.getElements();
					while (refs.hasMoreElements()) {
						Cell ref = refs.nextElement();
						if (match(ans, ref)) {
							myCodeTP.add(ans.getObject1AsURI() + " = " + ans.getObject2AsURI() + " : " + ans.getStrength());
							break;
						}
					}
				}
				
				
				// false positives: things in answers that are not in reference
				cells = matcherAlignment.getElements();
				while (cells.hasMoreElements()) {
					Cell ans = cells.nextElement();
					Enumeration<Cell> refs = refAlignment.getElements();
					boolean found = false;
					while (refs.hasMoreElements()) {
						Cell ref = refs.nextElement();
						if (match(ans, ref)) {
							found = true;
							break;
						}
					}
					if (!found) {
						System.out.println("Added FP");
						myCodeFP.add(ans.getObject1AsURI() + " = " + ans.getObject2AsURI() + " : " + ans.getStrength());
					}
				}
				
				// false negatives: things in reference that are not in answers
				cells = refAlignment.getElements();
				while (cells.hasMoreElements()) {
					Cell ans = cells.nextElement();
					Enumeration<Cell> refs = matcherAlignment.getElements();
					boolean found = false;
					while (refs.hasMoreElements()) {
						Cell ref = refs.nextElement();
						if (match(ans, ref)) {
							found = true;
							break;
						}
					}
					if (!found) {
						System.out.println("Added FN");
						myCodeFN.add(ans.getObject1AsURI() + " = " + ans.getObject2AsURI());
					}
				}
			}
		}
				

		int ff_count = 0;
		int mm_count = 0;
		int fm_count = 0;
		int rr_count = 0;
		int fr_count = 0;
		int mr_count = 0;
		int dd_count = 0;
		int fd_count = 0;
		int md_count = 0;
		int rd_count = 0;
		int ss_count = 0;
		int fs_count = 0;
		int ms_count = 0;
		int rs_count = 0;
		int ds_count = 0;
		int other = 0;
		int equal = 0;
		FileOutputStream os2 = new FileOutputStream("./conference_tp.txt");
		PrintWriter output2 = new PrintWriter(new OutputStreamWriter(os2,"UTF-8"), true);		

		System.out.println(myCodeTP.size());
		for (int i = 0; i < myCodeTP.size(); i++) {
			if (i % 10 == 0){
				System.out.println("i = " + i);
			}
			String tmp = myCodeTP.get(i);
			String mousepart = tmp.substring(0, tmp.indexOf(" "));
			String tmp2 = tmp.substring(tmp.indexOf(" ")+1, tmp.length());
			String humanpart = tmp2.substring(tmp2.indexOf(" ") + 1, tmp2.indexOf(" : "));
			String score = tmp2.substring(tmp2.lastIndexOf(" "), tmp2.length());
			if (allLabelsA.get(mousepart).equals(allLabelsB.get(humanpart))){
				equal ++;
			} else {
				ArrayList<String> dataA = getData(allLabelsA.get(mousepart));
				ArrayList<String> dataB = getData(allLabelsB.get(humanpart));
				output.println(allLabelsA.get(mousepart) + " " + dataA.get(0) + " = " + allLabelsB.get(humanpart) + " " + dataB.get(0) + " : " + score);
				if (dataA.get(0).equals("PAGE-STATUS::found") && dataB.get(0).equals("PAGE-STATUS::found")){
					ff_count ++;
				} else if (dataA.get(0).equals("PAGE-STATUS::missing") && dataB.get(0).equals("PAGE-STATUS::missing")){
					mm_count ++;
				} else if ((dataA.get(0).equals("PAGE-STATUS::found") && dataB.get(0).equals("PAGE-STATUS::missing")) || (dataA.get(0).equals("PAGE-STATUS::missing") && dataB.get(0).equals("PAGE-STATUS::found"))){
					fm_count ++;
				} else if (dataA.get(0).equals("PAGE-STATUS::redirected") && dataB.get(0).equals("PAGE-STATUS::redirected")){
					rr_count ++;
				} else if ((dataA.get(0).equals("PAGE-STATUS::found") && dataB.get(0).equals("PAGE-STATUS::redirected")) || (dataA.get(0).equals("PAGE-STATUS::redirected") && dataB.get(0).equals("PAGE-STATUS::found"))){
					fr_count ++;
					output2.println(mousepart + " " + dataA.get(0) + " = " + humanpart + " " + dataB.get(0) + " : " + score);
					
				} else if ((dataA.get(0).equals("PAGE-STATUS::missing") && dataB.get(0).equals("PAGE-STATUS::redirected")) || (dataA.get(0).equals("PAGE-STATUS::redirected") && dataB.get(0).equals("PAGE-STATUS::missing"))){
					mr_count ++;
				} else if (dataA.get(0).equals("PAGE-STATUS::disambiguation") && dataB.get(0).equals("PAGE-STATUS::disambiguation")){
					dd_count ++;
				} else if ((dataA.get(0).equals("PAGE-STATUS::found") && dataB.get(0).equals("PAGE-STATUS::disambiguation")) || (dataA.get(0).equals("PAGE-STATUS::disambiguation") && dataB.get(0).equals("PAGE-STATUS::found"))){
					fd_count ++;
				} else if ((dataA.get(0).equals("PAGE-STATUS::missing") && dataB.get(0).equals("PAGE-STATUS::disambiguation")) || (dataA.get(0).equals("PAGE-STATUS::disambiguation") && dataB.get(0).equals("PAGE-STATUS::missing"))){
					md_count ++;
				} else if ((dataA.get(0).equals("PAGE-STATUS::redirected") && dataB.get(0).equals("PAGE-STATUS::disambiguation")) || (dataA.get(0).equals("PAGE-STATUS::disambiguation") && dataB.get(0).equals("PAGE-STATUS::redirected"))){
					rd_count ++;
				} else if (dataA.get(0).equals("PAGE-STATUS::search") && dataB.get(0).equals("PAGE-STATUS::search")){
					ss_count ++;
					output2.println(mousepart + " " + dataA.get(0) + " = " + humanpart + " " + dataB.get(0) + " : " + score);
					
				} else if ((dataA.get(0).equals("PAGE-STATUS::found") && dataB.get(0).equals("PAGE-STATUS::search")) || (dataA.get(0).equals("PAGE-STATUS::search") && dataB.get(0).equals("PAGE-STATUS::found"))){
					fs_count ++;
				} else if ((dataA.get(0).equals("PAGE-STATUS::missing") && dataB.get(0).equals("PAGE-STATUS::search")) || (dataA.get(0).equals("PAGE-STATUS::search") && dataB.get(0).equals("PAGE-STATUS::missing"))){
					ms_count ++;
				} else if ((dataA.get(0).equals("PAGE-STATUS::redirected") && dataB.get(0).equals("PAGE-STATUS::search")) || (dataA.get(0).equals("PAGE-STATUS::search") && dataB.get(0).equals("PAGE-STATUS::redirected"))){
					rs_count ++;
				} else if ((dataA.get(0).equals("PAGE-STATUS::disambiguation") && dataB.get(0).equals("PAGE-STATUS::search")) || (dataA.get(0).equals("PAGE-STATUS::search") && dataB.get(0).equals("PAGE-STATUS::disambiguation"))){
					ds_count ++;
				} else {
					other ++;
				}
			}
		}
		System.out.println("TP\n");
		System.out.println("Found-Found: " + ff_count);
		System.out.println("Missing-Missing: " + mm_count);
		System.out.println("Found-Missing: " + fm_count);
		System.out.println("Redirected-Redirected: " + rr_count);
		System.out.println("Found-Redirected: " + fr_count);
		System.out.println("Missing-Redirected: " + mr_count);
		System.out.println("Disambiguation-Disambiguation: " + dd_count);
		System.out.println("Found-Disambiguation: " + fd_count);
		System.out.println("Missing-Disambiguation: " + md_count);
		System.out.println("Redirected-Disambiguation: " + rd_count);
		System.out.println("Search-Search: " + ss_count);
		System.out.println("Found-Search: " + fs_count);
		System.out.println("Missing-Search: " + ms_count);
		System.out.println("Redirected-Search: " + rs_count);
		System.out.println("Disambiguation-Search: " + ds_count);
		System.out.println("Other: " + other);
		System.out.println("Equal: " + equal);
		
		
		ff_count = 0;
		 mm_count = 0;
		 fm_count = 0;
		 rr_count = 0;
		 fr_count = 0;
		 mr_count = 0;
		 dd_count = 0;
		 fd_count = 0;
		 md_count = 0;
		 rd_count = 0;
		 ss_count = 0;
		 fs_count = 0;
		 ms_count = 0;
		 rs_count = 0;
		 ds_count = 0;
		 other = 0;
		 equal = 0;
		System.out.println(myCodeFP.size());
		for (int i = 0; i < myCodeFP.size(); i++) {
			if (i % 10 == 0){
				System.out.println("i = " + i);
			}
			String tmp = myCodeFP.get(i);
			String mousepart = tmp.substring(0, tmp.indexOf(" "));
			String tmp2 = tmp.substring(tmp.indexOf(" ")+1, tmp.length());
			String humanpart = tmp2.substring(tmp2.indexOf(" ") + 1, tmp2.indexOf(" : "));
			String score = tmp2.substring(tmp2.lastIndexOf(" "), tmp2.length());
			if (allLabelsA.get(mousepart).equals(allLabelsB.get(humanpart))){
				equal ++;
			} else {
				ArrayList<String> dataA = getData(allLabelsA.get(mousepart));
				ArrayList<String> dataB = getData(allLabelsB.get(humanpart));
				output.println(allLabelsA.get(mousepart) + " " + dataA.get(0) + " = " + allLabelsB.get(humanpart) + " " + dataB.get(0) + " : " + score);
				if (dataA.get(0).equals("PAGE-STATUS::found") && dataB.get(0).equals("PAGE-STATUS::found")){
					ff_count ++;
				} else if (dataA.get(0).equals("PAGE-STATUS::missing") && dataB.get(0).equals("PAGE-STATUS::missing")){
					mm_count ++;
				} else if ((dataA.get(0).equals("PAGE-STATUS::found") && dataB.get(0).equals("PAGE-STATUS::missing")) || (dataA.get(0).equals("PAGE-STATUS::missing") && dataB.get(0).equals("PAGE-STATUS::found"))){
					fm_count ++;
				} else if (dataA.get(0).equals("PAGE-STATUS::redirected") && dataB.get(0).equals("PAGE-STATUS::redirected")){
					rr_count ++;
				} else if ((dataA.get(0).equals("PAGE-STATUS::found") && dataB.get(0).equals("PAGE-STATUS::redirected")) || (dataA.get(0).equals("PAGE-STATUS::redirected") && dataB.get(0).equals("PAGE-STATUS::found"))){
					fr_count ++;
				} else if ((dataA.get(0).equals("PAGE-STATUS::missing") && dataB.get(0).equals("PAGE-STATUS::redirected")) || (dataA.get(0).equals("PAGE-STATUS::redirected") && dataB.get(0).equals("PAGE-STATUS::missing"))){
					mr_count ++;
				} else if (dataA.get(0).equals("PAGE-STATUS::disambiguation") && dataB.get(0).equals("PAGE-STATUS::disambiguation")){
					dd_count ++;
				} else if ((dataA.get(0).equals("PAGE-STATUS::found") && dataB.get(0).equals("PAGE-STATUS::disambiguation")) || (dataA.get(0).equals("PAGE-STATUS::disambiguation") && dataB.get(0).equals("PAGE-STATUS::found"))){
					fd_count ++;
				} else if ((dataA.get(0).equals("PAGE-STATUS::missing") && dataB.get(0).equals("PAGE-STATUS::disambiguation")) || (dataA.get(0).equals("PAGE-STATUS::disambiguation") && dataB.get(0).equals("PAGE-STATUS::missing"))){
					md_count ++;
				} else if ((dataA.get(0).equals("PAGE-STATUS::redirected") && dataB.get(0).equals("PAGE-STATUS::disambiguation")) || (dataA.get(0).equals("PAGE-STATUS::disambiguation") && dataB.get(0).equals("PAGE-STATUS::redirected"))){
					rd_count ++;
				} else if (dataA.get(0).equals("PAGE-STATUS::search") && dataB.get(0).equals("PAGE-STATUS::search")){
					ss_count ++;
				} else if ((dataA.get(0).equals("PAGE-STATUS::found") && dataB.get(0).equals("PAGE-STATUS::search")) || (dataA.get(0).equals("PAGE-STATUS::search") && dataB.get(0).equals("PAGE-STATUS::found"))){
					fs_count ++;
				} else if ((dataA.get(0).equals("PAGE-STATUS::missing") && dataB.get(0).equals("PAGE-STATUS::search")) || (dataA.get(0).equals("PAGE-STATUS::search") && dataB.get(0).equals("PAGE-STATUS::missing"))){
					ms_count ++;
				} else if ((dataA.get(0).equals("PAGE-STATUS::redirected") && dataB.get(0).equals("PAGE-STATUS::search")) || (dataA.get(0).equals("PAGE-STATUS::search") && dataB.get(0).equals("PAGE-STATUS::redirected"))){
					rs_count ++;
				} else if ((dataA.get(0).equals("PAGE-STATUS::disambiguation") && dataB.get(0).equals("PAGE-STATUS::search")) || (dataA.get(0).equals("PAGE-STATUS::search") && dataB.get(0).equals("PAGE-STATUS::disambiguation"))){
					ds_count ++;
				} else {
					other ++;
				}
			}
		}
		System.out.println("\n\nFP:\n");
		System.out.println("Found-Found: " + ff_count);
		System.out.println("Missing-Missing: " + mm_count);
		System.out.println("Found-Missing: " + fm_count);
		System.out.println("Redirected-Redirected: " + rr_count);
		System.out.println("Found-Redirected: " + fr_count);
		System.out.println("Missing-Redirected: " + mr_count);
		System.out.println("Disambiguation-Disambiguation: " + dd_count);
		System.out.println("Found-Disambiguation: " + fd_count);
		System.out.println("Missing-Disambiguation: " + md_count);
		System.out.println("Redirected-Disambiguation: " + rd_count);
		System.out.println("Search-Search: " + ss_count);
		System.out.println("Found-Search: " + fs_count);
		System.out.println("Missing-Search: " + ms_count);
		System.out.println("Redirected-Search: " + rs_count);
		System.out.println("Disambiguation-Search: " + ds_count);
		System.out.println("Other: " + other);
		System.out.println("Equal: " + equal);
		
		
		 ff_count = 0;
		 mm_count = 0;
		 fm_count = 0;
		 rr_count = 0;
		 fr_count = 0;
		 mr_count = 0;
		 dd_count = 0;
		 fd_count = 0;
		 md_count = 0;
		 rd_count = 0;
		 ss_count = 0;
		 fs_count = 0;
		 ms_count = 0;
		 rs_count = 0;
		 ds_count = 0;
		 other = 0;
		 equal = 0;
		System.out.println(myCodeFN.size());
		for (int i = 0; i < myCodeFN.size(); i++) {
			if (i % 10 == 0){
				System.out.println("i = " + i);
			}
			String tmp = myCodeFN.get(i);
			String mousepart = tmp.substring(0, tmp.indexOf(" "));
			String humanpart = tmp.substring(tmp.indexOf("=")+2, tmp.length());
			if (allLabelsA.get(mousepart).equals(allLabelsB.get(humanpart))){
				equal ++;
			} else {
				ArrayList<String> dataA = getData(allLabelsA.get(mousepart));
				ArrayList<String> dataB = getData(allLabelsB.get(humanpart));
				output.println(allLabelsA.get(mousepart) + " " + dataA.get(0) + " = " + allLabelsB.get(humanpart) + " " + dataB.get(0));
				if (dataA.get(0).equals("PAGE-STATUS::found") && dataB.get(0).equals("PAGE-STATUS::found")){
					ff_count ++;
				} else if (dataA.get(0).equals("PAGE-STATUS::missing") && dataB.get(0).equals("PAGE-STATUS::missing")){
					mm_count ++;
				} else if ((dataA.get(0).equals("PAGE-STATUS::found") && dataB.get(0).equals("PAGE-STATUS::missing")) || (dataA.get(0).equals("PAGE-STATUS::missing") && dataB.get(0).equals("PAGE-STATUS::found"))){
					fm_count ++;
				} else if (dataA.get(0).equals("PAGE-STATUS::redirected") && dataB.get(0).equals("PAGE-STATUS::redirected")){
					rr_count ++;
				} else if ((dataA.get(0).equals("PAGE-STATUS::found") && dataB.get(0).equals("PAGE-STATUS::redirected")) || (dataA.get(0).equals("PAGE-STATUS::redirected") && dataB.get(0).equals("PAGE-STATUS::found"))){
					fr_count ++;
				} else if ((dataA.get(0).equals("PAGE-STATUS::missing") && dataB.get(0).equals("PAGE-STATUS::redirected")) || (dataA.get(0).equals("PAGE-STATUS::redirected") && dataB.get(0).equals("PAGE-STATUS::missing"))){
					mr_count ++;
				} else if (dataA.get(0).equals("PAGE-STATUS::disambiguation") && dataB.get(0).equals("PAGE-STATUS::disambiguation")){
					dd_count ++;
				} else if ((dataA.get(0).equals("PAGE-STATUS::found") && dataB.get(0).equals("PAGE-STATUS::disambiguation")) || (dataA.get(0).equals("PAGE-STATUS::disambiguation") && dataB.get(0).equals("PAGE-STATUS::found"))){
					fd_count ++;
				} else if ((dataA.get(0).equals("PAGE-STATUS::missing") && dataB.get(0).equals("PAGE-STATUS::disambiguation")) || (dataA.get(0).equals("PAGE-STATUS::disambiguation") && dataB.get(0).equals("PAGE-STATUS::missing"))){
					md_count ++;
				} else if ((dataA.get(0).equals("PAGE-STATUS::redirected") && dataB.get(0).equals("PAGE-STATUS::disambiguation")) || (dataA.get(0).equals("PAGE-STATUS::disambiguation") && dataB.get(0).equals("PAGE-STATUS::redirected"))){
					rd_count ++;
				} else if (dataA.get(0).equals("PAGE-STATUS::search") && dataB.get(0).equals("PAGE-STATUS::search")){
					ss_count ++;
				} else if ((dataA.get(0).equals("PAGE-STATUS::found") && dataB.get(0).equals("PAGE-STATUS::search")) || (dataA.get(0).equals("PAGE-STATUS::search") && dataB.get(0).equals("PAGE-STATUS::found"))){
					fs_count ++;
				} else if ((dataA.get(0).equals("PAGE-STATUS::missing") && dataB.get(0).equals("PAGE-STATUS::search")) || (dataA.get(0).equals("PAGE-STATUS::search") && dataB.get(0).equals("PAGE-STATUS::missing"))){
					ms_count ++;
				} else if ((dataA.get(0).equals("PAGE-STATUS::redirected") && dataB.get(0).equals("PAGE-STATUS::search")) || (dataA.get(0).equals("PAGE-STATUS::search") && dataB.get(0).equals("PAGE-STATUS::redirected"))){
					rs_count ++;
				} else if ((dataA.get(0).equals("PAGE-STATUS::disambiguation") && dataB.get(0).equals("PAGE-STATUS::search")) || (dataA.get(0).equals("PAGE-STATUS::search") && dataB.get(0).equals("PAGE-STATUS::disambiguation"))){
					ds_count ++;
				} else {
					other ++;
				}
			}
		}
		System.out.println("\n\nFN:\n");
		System.out.println("Found-Found: " + ff_count);
		System.out.println("Missing-Missing: " + mm_count);
		System.out.println("Found-Missing: " + fm_count);
		System.out.println("Redirected-Redirected: " + rr_count);
		System.out.println("Found-Redirected: " + fr_count);
		System.out.println("Missing-Redirected: " + mr_count);
		System.out.println("Disambiguation-Disambiguation: " + dd_count);
		System.out.println("Found-Disambiguation: " + fd_count);
		System.out.println("Missing-Disambiguation: " + md_count);
		System.out.println("Redirected-Disambiguation: " + rd_count);
		System.out.println("Search-Search: " + ss_count);
		System.out.println("Found-Search: " + fs_count);
		System.out.println("Missing-Search: " + ms_count);
		System.out.println("Redirected-Search: " + rs_count);
		System.out.println("Disambiguation-Search: " + ds_count);
		System.out.println("Other: " + other);
		System.out.println("Equal: " + equal);
		
		

		output.close();
	}	
	
	private static ArrayList<String> getData(String label) {
		ArrayList<String> x = WikipediaOperations.getRedirectsAlias(label);
		ArrayList<String> data = processTerms(label, x);
		for( int i = 0; (data.size() == 1 && data.get(0).equals("") && i < 2); i++){
			try {
				Thread.sleep(250);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
			data = processTerms(label, WikipediaOperations.getRedirectsAlias(label));
		}
		if (data.size() == 0 || data.get(0).equals("INTERNAL-ERROR") || data.get(0).equals("PAGE-STATUS::missing")){
			data = processTerms(label, WikipediaOperations.getSearchOptions(label));
		}
		if (data.size() == 0){
			data.add(0,"NOTHING");
		}

		return data;
	}

	private static ArrayList<String> processTerms(String label, ArrayList<String> data) {
		ArrayList<String> newdata = new ArrayList<String>();
		if (data.get(0).equals("PAGE-STATUS::API-ERROR")){
			System.out.println("There is a timeout or some other error with API.");
			System.out.println("Logging: label error occured: " + label);
		}
		if (data.get(0).equals("INTERNAL-ERROR")){

			return data;
		} 
		if (data.get(0).equals("PAGE-STATUS::missing")){
			// Need to now search using the open search thing...but for now just return  
			
			return data;
		}
		if (data.get(0).equals("PAGE-STATUS::disambiguation")){
			//For now just keep all the possible links.. change later to something else..
			return data;
		}
		if (data.get(0).equals("PAGE-STATUS::found") || data.get(0).equals("PAGE-STATUS::redirected")){

			return data;
		}
		if (data.get(0).equals("PAGE-STATUS::search")){

			return data;
		}
		System.out.println(data);
		System.out.println("ISSUE WITH: " + label + ". Added the label to the labellist and moving on.");
		newdata.add(0,"INTERNAL-ERROR");
		return newdata;
	}
	
	private static boolean match(Cell c1, Cell c2) throws AlignmentException {
		return c1.getObject1AsURI().toString().equals(c2.getObject1AsURI().toString()) && 
				c1.getObject2AsURI().toString().equals(c2.getObject2AsURI().toString());
	}
	
	
}
