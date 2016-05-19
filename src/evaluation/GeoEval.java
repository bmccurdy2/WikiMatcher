package evaluation;

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

public class GeoEval {
	
	public static void doGeo() throws Exception{
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
				
				File answers = new File( dir + "/article-results/" + name1 + "-" + name2 + ".rdf"); 
				Alignment matcherAlignment = parser.parse(new FileReader(answers));
				Enumeration<Cell> cells = matcherAlignment.getElements();
				while (cells.hasMoreElements()) {		
					Cell ans = cells.nextElement();
					if(envoList.containsKey(ans.getObject1AsURI().toString())){
//						System.out.println(ans.getObject1AsURI().toString());
						allMatches.add("http://purl.obolibrary.org/obo/envo.owl#" + getGeoLabel(envoList.get(ans.getObject1AsURI().toString()), ontA).replace(" ", "_") + "|" + ans.getObject2AsURI()); 
					}
				}
//				File answers1 = new File( dir + "/snippet-results/" + name1 + "-" + name2 + ".rdf"); 
//				Alignment matcherAlignment1 = parser.parse(new FileReader(answers1));
//				Enumeration<Cell> cells1 = matcherAlignment1.getElements();
//				while (cells1.hasMoreElements()) {		
//					Cell ans = cells1.nextElement();
//					if(envoList.containsKey(ans.getObject1AsURI().toString())){
////						System.out.println(ans.getObject1AsURI().toString());
//						allMatches.add("http://purl.obolibrary.org/obo/envo.owl#" + getGeoLabel(envoList.get(ans.getObject1AsURI().toString()), ontA).replace(" ", "_") + "|" + ans.getObject2AsURI()); 
//					}
//				}
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
//				File answers2 = new File( dir + "/levenstein-results/" + name1 + "-" + name2 + ".rdf"); 
//				Alignment matcherAlignment2 = parser.parse(new FileReader(answers2));
//				Enumeration<Cell> cells2 = matcherAlignment2.getElements();
//				while (cells2.hasMoreElements()) {		
//					Cell ans = cells2.nextElement();
//					if(envoList.containsKey(ans.getObject1AsURI().toString())){
////						System.out.println(ans.getObject1AsURI().toString());
//						allMatches.add("http://purl.obolibrary.org/obo/envo.owl#" + getGeoLabel(envoList.get(ans.getObject1AsURI().toString()), ontA).replace(" ", "_") + "|" + ans.getObject2AsURI()); 
//					}
//					
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
				} else {
					fp ++;
				}
			} else {
				System.out.println("Could not find: " + x.toLowerCase().trim());
			}
		}
		System.out.println("TP: " + tp);
		System.out.println("FP: " + fp);
		
		System.out.println("TP Exact: " + tpnumExact);
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
	
	public static void main(String args[]) throws Exception{
		doGeo();
	}
}
