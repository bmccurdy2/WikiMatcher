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
import java.util.Scanner;
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


public class GetResultsList  {
	public static void main(String[] args) throws Exception{
		doHydro();
	}
	
	public static void doHydro() throws Exception{

		FileOutputStream os1 = new FileOutputStream("./HydroResults22.txt");

		PrintWriter output = new PrintWriter (
				new OutputStreamWriter(os1, "UTF-8" ), true);
		
		AlignmentParser parser = new AlignmentParser(0);
		HashSet<String> allMatches = new HashSet<>();
//		ArrayList<String> allMatches = new ArrayList<>();
		
		File dir = new File("./data/hydro/");
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
					allMatches.add(ans.getObject1AsURI() + "|" + ans.getObject2AsURI());
				}
				File answers1 = new File( dir + "/snippet-results/" + name1 + "-" + name2 + ".rdf"); 
				Alignment matcherAlignment1 = parser.parse(new FileReader(answers1));
				Enumeration<Cell> cells1 = matcherAlignment1.getElements();
				while (cells1.hasMoreElements()) {		
					Cell ans = cells1.nextElement();
					allMatches.add(ans.getObject1AsURI() + "|" + ans.getObject2AsURI());
				}
				File answers2 = new File( dir + "/terms-results/" + name1 + "-" + name2 + ".rdf"); 
				Alignment matcherAlignment2 = parser.parse(new FileReader(answers2));
				Enumeration<Cell> cells2 = matcherAlignment2.getElements();
				while (cells2.hasMoreElements()) {		
					Cell ans = cells2.nextElement();
					allMatches.add(ans.getObject1AsURI() + "|" + ans.getObject2AsURI());
				}
				
			}
		}
		
		for(String x : allMatches){
			output.println(x);
		}
		output.close();
	}
	
	public static void doGeo() throws Exception {
		
		FileOutputStream os1 = new FileOutputStream("./GeoResults-withLeven2.txt");

		PrintWriter output = new PrintWriter (
				new OutputStreamWriter(os1, "UTF-8" ), true);
		
		AlignmentParser parser = new AlignmentParser(0);

		File f1 = new File("./data/geo/envo.owl");
		File f2 = new File("./data/geo/whole_realm.owl");
		
		System.out.println("Evaluating results...");
				
		String s1 = f1.toString();
		String s2 = f2.toString();
		String name1 = s1.substring(s1.lastIndexOf("/")+1, s1.lastIndexOf("."));
		String name2 = s2.substring(s2.lastIndexOf("/")+1, s2.lastIndexOf("."));
		
		HashSet<String> allMatches = new HashSet<>();
//		ArrayList<String> allMatches = new ArrayList<>();
		
		OWLOntologyManager manager = OWLManager.createOWLOntologyManager();
		IRI iriA = IRI.create(f1);
		OWLOntology ontA = manager.loadOntologyFromOntologyDocument(iriA);
		Set<OWLEntity> envo = getEnvoEntities(ontA);
		HashMap<String, OWLEntity> envoList = new HashMap<>();
		for(OWLEntity e : envo){
			System.out.println(e.getIRI().toString());
			envoList.put(e.getIRI().toString(), e);
		}
		
		File answers = new File("./data/geo/article-results/" + name1 + "-" + name2 + ".rdf"); 	
		Alignment matcherAlignment = parser.parse(new FileReader(answers));
		Enumeration<Cell> cells = matcherAlignment.getElements();
		while (cells.hasMoreElements()) {		
			Cell ans = cells.nextElement();
			System.out.println(ans.getObject1AsURI().toString());
			if(envoList.containsKey(ans.getObject1AsURI().toString())){
				allMatches.add("http:://purl.obolibrary.org/obo/envo.owl#" + getGeoLabel(envoList.get(ans.getObject1AsURI().toString()), ontA).replace(" ", "_") + "|" + ans.getObject2AsURI());
			} else {
				System.out.println("FUCK");
			}
//			OWLEntity e1 = ontA.getEntitiesInSignature(IRI.create(ans.getObject1AsURI())).iterator().next();
//			allMatches.add(getGeoLabel(e1, ontA) + "|" + ans.getObject2AsURI());
		}
		
		File answers1 = new File("./data/geo/terms-results/" + name1 + "-" + name2 + ".rdf"); 	
		Alignment matcherAlignment1 = parser.parse(new FileReader(answers1));
		Enumeration<Cell> cells1 = matcherAlignment1.getElements();
		while (cells1.hasMoreElements()) {		
			Cell ans = cells1.nextElement();
			allMatches.add("http:://purl.obolibrary.org/obo/envo.owl#" + getGeoLabel(envoList.get(ans.getObject1AsURI().toString()), ontA).replace(" ", "_") + "|" + ans.getObject2AsURI());
//			OWLEntity e1 = ontA.getEntitiesInSignature(IRI.create(ans.getObject1AsURI())).iterator().next();
//			allMatches.add(getGeoLabel(e1, ontA) + "|" + ans.getObject2AsURI());
		}
		
		File answers2 = new File("./data/geo/snippet-results/" + name1 + "-" + name2 + ".rdf"); 	
		Alignment matcherAlignment2 = parser.parse(new FileReader(answers2));
		Enumeration<Cell> cells2 = matcherAlignment2.getElements();
		while (cells2.hasMoreElements()) {		
			Cell ans = cells2.nextElement();
			allMatches.add("http:://purl.obolibrary.org/obo/envo.owl#" + getGeoLabel(envoList.get(ans.getObject1AsURI().toString()), ontA).replace(" ", "_") + "|" + ans.getObject2AsURI());
//			OWLEntity e1 = ontA.getEntitiesInSignature(IRI.create(ans.getObject1AsURI())).iterator().next();
//			allMatches.add(getGeoLabel(e1, ontA) + "|" + ans.getObject2AsURI());
		}
		
		File answers3 = new File("./data/geo/levenstein-results/" + name1 + "-" + name2 + ".rdf"); 	
		Alignment matcherAlignment3 = parser.parse(new FileReader(answers3));
		Enumeration<Cell> cells3 = matcherAlignment3.getElements();
		while (cells3.hasMoreElements()) {		
			Cell ans = cells3.nextElement();
			allMatches.add("http:://purl.obolibrary.org/obo/envo.owl#" + getGeoLabel(envoList.get(ans.getObject1AsURI().toString()), ontA).replace(" ", "_") + "|" + ans.getObject2AsURI());
//			OWLEntity e1 = ontA.getEntitiesInSignature(IRI.create(ans.getObject1AsURI())).iterator().next();
//			allMatches.add(getGeoLabel(e1, ontA) + "|" + ans.getObject2AsURI());
		}
		
//		ArrayList<String> done = new ArrayList<>();
		for(String x : allMatches){
//			if (!done.contains(x)){
				output.println(x);
//				done.add(x);
//			} 
		}
//		System.out.println(done.size());
		output.close();
	}
	
	public static void doGeoRel() throws Exception {
		
		FileOutputStream os1 = new FileOutputStream("./GeoRelations-changed.txt");

		PrintWriter output = new PrintWriter (
				new OutputStreamWriter(os1, "UTF-8" ), true);
		
		AlignmentParser parser = new AlignmentParser(0);

		File f1 = new File("./data/geo/envo.owl");
		
		System.out.println("Evaluating results...");
				
		String s1 = f1.toString();
		String name1 = s1.substring(s1.lastIndexOf("/")+1, s1.lastIndexOf("."));
		
		OWLOntologyManager manager = OWLManager.createOWLOntologyManager();
		IRI iriA = IRI.create(f1);
		OWLOntology ontA = manager.loadOntologyFromOntologyDocument(iriA);
		Set<OWLEntity> envo = getEnvoEntities(ontA);
		HashMap<String, OWLEntity> envoList = new HashMap<>();
		for(OWLEntity e : envo){
			envoList.put(e.getIRI().toString(), e);
		}
			
		Scanner in = new Scanner(new FileReader("./data/geo/geoRelations.txt"));
		while(in.hasNextLine()){
			String line = in.nextLine();
			if (line.contains("http://purl.obolibrary.org/obo/")){ 
				line = line.trim();
				if(envoList.containsKey(line)){
					output.println("http://purl.obolibrary.org/obo/envo.owl#" + getGeoLabel(envoList.get(line), ontA).replace(" ", "_"));
				} else {
					System.out.println("FUCK");
				}
			} else if (line.contains("https://purl.obolibrary.org/obo/")){ 
				line = line.trim();
				if(envoList.containsKey(line)){
					output.println("http://purl.obolibrary.org/obo/envo.owl#" + getGeoLabel(envoList.get(line), ontA).replace(" ", "_"));
				} else {
					System.out.println("FUCK");
				}
			} else if (line.contains("http://sweet.jpl.nasa.gov/")){
				output.println(line.trim());
			} else {
				output.println("\t" + line.trim());
			}
		}
		
		
		output.close();
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
}
