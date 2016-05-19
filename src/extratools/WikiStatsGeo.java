package extratools;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;

import org.semanticweb.owl.align.Alignment;
import org.semanticweb.owl.align.Cell;
import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.model.IRI;
import org.semanticweb.owlapi.model.OWLAnnotation;
import org.semanticweb.owlapi.model.OWLAnnotationProperty;
import org.semanticweb.owlapi.model.OWLClass;
import org.semanticweb.owlapi.model.OWLDataProperty;
import org.semanticweb.owlapi.model.OWLEntity;
import org.semanticweb.owlapi.model.OWLObjectProperty;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLOntologyManager;

import fr.inrialpes.exmo.align.parser.AlignmentParser;
import uk.ac.manchester.cs.owl.owlapi.OWLAnnotationPropertyImpl;
import uk.ac.manchester.cs.owl.owlapi.OWLDataFactoryImpl;
import util.OWLData;
import wiki.WikipediaOperations;

public class WikiStatsGeo {
	
	public static void main(String[] args) throws Exception {
		File f = new File("./data/geo/envo.owl");
		OWLOntologyManager manager = OWLManager.createOWLOntologyManager();
		IRI iriA = IRI.create(f);
		OWLOntology ontA = manager.loadOntologyFromOntologyDocument(iriA);
		Set<OWLEntity> envo = getEnvoEntities(ontA);
		HashMap<String, OWLEntity> envoList = new HashMap<>();
		for(OWLEntity e : envo){
			envoList.put(e.getIRI().toString(), e);
		}
		
		AlignmentParser parser = new AlignmentParser(0);
//		HashSet<String> allMatches = new HashSet<>();
		ArrayList<String> allMatches = new ArrayList<>();
		
		File dir = new File("./data/geo/");
		File[] files = dir.listFiles();
		for (int i=0; i<files.length; i++) {
			File f1 = files[i];
			if (!f1.toString().endsWith(".owl")) continue;
			for (int j=i; j<files.length; j++) {
				File f2 = files[j];
				if (!f2.toString().endsWith(".owl") || f1.toString().equals(f2.toString())) continue;
				
				String s1 = f1.toString();
				String s2 = f2.toString();
				String name1 = s1.substring(s1.lastIndexOf("/")+1, s1.lastIndexOf("."));
				String name2 = s2.substring(s2.lastIndexOf("/")+1, s2.lastIndexOf("."));
				
//				File answers = new File( dir + "/article-results/" + name1 + "-" + name2 + ".rdf"); 
//				Alignment matcherAlignment = parser.parse(new FileReader(answers));
//				Enumeration<Cell> cells = matcherAlignment.getElements();
//				while (cells.hasMoreElements()) {		
//					Cell ans = cells.nextElement();
//					if(envoList.containsKey(ans.getObject1AsURI().toString())){
////						System.out.println(ans.getObject1AsURI().toString());
//						allMatches.add("http://purl.obolibrary.org/obo/envo.owl#" + getGeoLabel(envoList.get(ans.getObject1AsURI().toString()), ontA).replace(" ", "_") + "|" + ans.getObject2AsURI()); 
//					}
//				}
				File answers1 = new File( dir + "/snippet-results/" + name1 + "-" + name2 + ".rdf"); 
				Alignment matcherAlignment1 = parser.parse(new FileReader(answers1));
				Enumeration<Cell> cells1 = matcherAlignment1.getElements();
				while (cells1.hasMoreElements()) {		
					Cell ans = cells1.nextElement();
					if(envoList.containsKey(ans.getObject1AsURI().toString())){
//						System.out.println(ans.getObject1AsURI().toString());
						allMatches.add("http://purl.obolibrary.org/obo/envo.owl#" + getGeoLabel(envoList.get(ans.getObject1AsURI().toString()), ontA).replace(" ", "_") + "|" + ans.getObject2AsURI()); 
					}
				}
//				File answers2 = new File( dir + "/terms-results/" + name1 + "-" + name2 + ".rdf"); 
//				Alignment matcherAlignment2 = parser.parse(new FileReader(answers2));
//				Enumeration<Cell> cells2 = matcherAlignment2.getElements();
//				while (cells2.hasMoreElements()) {		
//					Cell ans = cells2.nextElement();
//					if(envoList.containsKey(ans.getObject1AsURI().toString())){
////						System.out.println(ans.getObject1AsURI().toString());
//						allMatches.add("http://purl.obolibrary.org/obo/envo.owl#" + getGeoLabel(envoList.get(ans.getObject1AsURI().toString()), ontA).replace(" ", "_") + "|" + ans.getObject2AsURI()); 
//					}
//				}
				
			}
		}
		
		BufferedReader br = new BufferedReader(new FileReader("./GeoQuestions.txt"));
		String line = "";
		HashMap<String, Boolean> results = new HashMap<>();
		String match = "";
		boolean getAnswer = false;
		while ((line = br.readLine()) != null) {
			if(line.contains("MATCH")){
				match = line.trim().replace("MATCH: ", "");
				match = match.toLowerCase();
				match = match.replace("::", ":");
			} else if (line.contains("ANSWER")){
				getAnswer =true;
			} else if (getAnswer){
				if (line.contains("TRUE")){
					results.put(match, true);
				} else {
					results.put(match, false);
				}
				match = "";
				getAnswer = false;
			}
			
		}
		br.close();
		
//		for (String x : results.keySet()){
//			System.out.println(x);
//		}
		
		ArrayList<String> myCodeFP = new ArrayList<>();
		ArrayList<String> myCodeTP = new ArrayList<>();
		int tp = 0;
		int tpnumExact = 0;
		int fp = 0;		
		for(String x : allMatches){
			if (results.containsKey(x.toLowerCase().trim())){
//				System.out.println(x.toLowerCase().trim());
				String[] parts = x.toLowerCase().trim().split("\\|");
				String labelA = parts[0].replace("http://purl.obolibrary.org/obo/envo.owl#", "");
				labelA = labelA.replace("_", "");
				String[] partsB = parts[1].split("#");
				String labelB = partsB[1];
				System.out.println("A - " +labelA);
				System.out.println("B - " +labelB);
				if(labelA.equals(labelB)){
					tpnumExact++;
				}
				
				if(results.get(x.toLowerCase().trim())){
					tp ++;
					myCodeTP.add(labelA +"="+ labelB);
				} else {
					fp ++;
					myCodeFP.add(labelA +"="+ labelB);
				}
			} else {
				System.out.println("Could not find: " + x.toLowerCase().trim());
			}
		}
		System.out.println("TP: " + tp);
		System.out.println("FP: " + fp);
		
		System.out.println("TP Exact: " + tpnumExact);
		
		FileOutputStream os2 = new FileOutputStream("./geo-tp-matches.txt");
		PrintWriter output2 = new PrintWriter(new OutputStreamWriter(os2,"UTF-8"), true);
		
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
		System.out.println(myCodeTP.size());
		for (int i = 0; i < myCodeTP.size(); i++) {
			if (i % 10 == 0){
				System.out.println("i = " + i);
			}
			String tmp = myCodeTP.get(i);
			String labelA = tmp.split("=")[0];
			
			String labelB = tmp.split("=")[1];
			if (labelA.equals(labelB)){
				equal ++;
			} else {
				ArrayList<String> dataA = getData(labelA);
				ArrayList<String> dataB = getData(labelB);
//				output2.println(dataA.get(0) + " - " + dataB.get(0) + "  -  " + labelA + " - " + labelB);
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
					output2.println(dataA.get(0) + " - " + dataB.get(0) + "  -  " + labelA + " - " + labelB);
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
					output2.println(dataA.get(0) + " - " + dataB.get(0) + "  -  " + labelA + " - " + labelB);
				} else if ((dataA.get(0).equals("PAGE-STATUS::found") && dataB.get(0).equals("PAGE-STATUS::search")) || (dataA.get(0).equals("PAGE-STATUS::search") && dataB.get(0).equals("PAGE-STATUS::found"))){
					fs_count ++;
					output2.println(dataA.get(0) + " - " + dataB.get(0) + "  -  " + labelA + " - " + labelB);
				} else if ((dataA.get(0).equals("PAGE-STATUS::missing") && dataB.get(0).equals("PAGE-STATUS::search")) || (dataA.get(0).equals("PAGE-STATUS::search") && dataB.get(0).equals("PAGE-STATUS::missing"))){
					ms_count ++;
				} else if ((dataA.get(0).equals("PAGE-STATUS::redirected") && dataB.get(0).equals("PAGE-STATUS::search")) || (dataA.get(0).equals("PAGE-STATUS::search") && dataB.get(0).equals("PAGE-STATUS::redirected"))){
					rs_count ++;
				} else if ((dataA.get(0).equals("PAGE-STATUS::disambiguation") && dataB.get(0).equals("PAGE-STATUS::search")) || (dataA.get(0).equals("PAGE-STATUS::search") && dataB.get(0).equals("PAGE-STATUS::disambiguation"))){
					ds_count ++;
					output2.println(dataA.get(0) + " - " + dataB.get(0) + "  -  " + labelA + " - " + labelB);
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
			String labelA = tmp.split("=")[0];
			
			String labelB = tmp.split("=")[1];
			if (labelA.equals(labelB)){
				equal ++;
			} else {
				ArrayList<String> dataA = getData(labelA);
				ArrayList<String> dataB = getData(labelB);
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
		System.out.println("FP\n");
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
	}
	
	public static void doBasic() throws Exception {
		
		HashSet<String> allLabelsA = new HashSet<>();
		HashMap<String, String> results = new HashMap<>();
		
		AlignmentParser parser = new AlignmentParser(0);

		File f1 = new File("./data/geo/envo.owl");
		File f2 = new File("./data/geo/whole_realm.owl");
		OWLData owldata = new OWLData("geo");
		
		Set<OWLEntity> a = owldata.getData(f1.toURI(), "A");
		for (OWLEntity en : a){
			allLabelsA.add(owldata.preprocessLabel(en, "A"));
		}
		
		Set<OWLEntity> b = owldata.getData(f2.toURI(), "B");
		for (OWLEntity en : b){
			allLabelsA.add(owldata.preprocessLabel(en, "B"));
		}
				

		int f_count = 0;
		int m_count = 0;
		int r_count = 0;
		int d_count = 0;
		int s_count = 0;
		int other = 0;
		System.out.println(allLabelsA.size());
		int i = 0;
		
		for (String label : allLabelsA) {
			if (i % 10 == 0){
				System.out.println("i = " + i);
			}
			i++;
			
			ArrayList<String> dataA = getData(label);
			if (dataA.get(0).equals("PAGE-STATUS::found")){
				f_count ++;
				results.put(label, "PAGE-STATUS::found");
			} else if (dataA.get(0).equals("PAGE-STATUS::missing")){
				m_count ++;
				results.put(label, "PAGE-STATUS::missing");
			} else if (dataA.get(0).equals("PAGE-STATUS::redirected")){
				r_count ++;
				results.put(label, "PAGE-STATUS::redirected");
			} else if (dataA.get(0).equals("PAGE-STATUS::disambiguation")){
				d_count ++;
				results.put(label, "PAGE-STATUS::disambiguation");
			} else if (dataA.get(0).equals("PAGE-STATUS::search")){
				s_count ++;
				results.put(label, "PAGE-STATUS::search");
			} else {
				other ++;
				results.put(label, "PAGE-STATUS::other");
			}
		}
		
		System.out.println("TP\n");
		System.out.println("Found: " + f_count);
		System.out.println("Missing: " + m_count);
		System.out.println("Redirected: " + r_count);
		System.out.println("Disambiguation: " + d_count);
		System.out.println("Search: " + s_count);
		System.out.println("Other: " + other);
	}	
	
	private static Set<OWLEntity> getEnvoEntities(OWLOntology ontA) {
		Set<OWLObjectProperty> aObjectProps = ontA.getObjectPropertiesInSignature();
		Set<OWLDataProperty> aDataProps = ontA.getDataPropertiesInSignature();
		Set<OWLAnnotationProperty> aAnnotationProps = ontA.getAnnotationPropertiesInSignature();
		Set<OWLClass> aClasses = ontA.getClassesInSignature();
		
		Set<OWLEntity> aEntities = new HashSet<>();
		aEntities.addAll(aObjectProps);
		aEntities.addAll(aDataProps);
		aEntities.addAll(aAnnotationProps);
		aEntities.addAll(aClasses);
		return aEntities;
	}
	
	private static String getGeoLabel(OWLEntity e, OWLOntology ont) {
		
		String label = e.getIRI().toString();
		
    	Set<OWLAnnotation> labels = e.getAnnotations(
    			ont, new OWLAnnotationPropertyImpl(
    			OWLDataFactoryImpl.getInstance(), 
    			IRI.create("http://www.w3.org/2000/01/rdf-schema#label")));
    	
        if (labels != null && labels.size() > 0) {
    		label = ((OWLAnnotation) labels.toArray()[0]).getValue().toString();
    		if (label.startsWith("\"")) {
    			label = label.substring(1);
    		}
    		
    		if (label.contains("\"")) {
    			label = label.substring(0, label.lastIndexOf('"'));
    		}
    		
    	}
    	
    	return label;
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
//		System.out.println(data);
//		System.out.println("ISSUE WITH: " + label + ". Added the label to the labellist and moving on.");
//		newdata.add(0,"INTERNAL-ERROR");
		try {
			Thread.sleep(3000);
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return newdata;
	}
	
	
	
}
