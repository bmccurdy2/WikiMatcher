package util;

import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.URI;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;

import org.semanticweb.owl.align.Alignment;
import org.semanticweb.owl.align.AlignmentException;
import org.semanticweb.owl.align.AlignmentVisitor;
import org.semanticweb.owl.align.Cell;
import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.model.IRI;
import org.semanticweb.owlapi.model.OWLAnnotation;
import org.semanticweb.owlapi.model.OWLAnnotationProperty;
import org.semanticweb.owlapi.model.OWLClass;
import org.semanticweb.owlapi.model.OWLDataProperty;
import org.semanticweb.owlapi.model.OWLEntity;
import org.semanticweb.owlapi.model.OWLNamedIndividual;
import org.semanticweb.owlapi.model.OWLObjectProperty;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLOntologyManager;

import fr.inrialpes.exmo.align.impl.URIAlignment;
import fr.inrialpes.exmo.align.impl.renderer.RDFRendererVisitor;
import uk.ac.manchester.cs.owl.owlapi.OWLAnnotationPropertyImpl;
import uk.ac.manchester.cs.owl.owlapi.OWLDataFactoryImpl;

public class OWLData{
	private String ontology = "";
	private URIAlignment dummyAlignment = new URIAlignment();
	private HashMap<OWLEntity, ArrayList<Cell>> matched = new HashMap<>();
	private OWLOntology ontA;
	private OWLOntology ontB;
	
	public OWLData(String ontology){
		this.ontology = ontology;
	}
	
	public Set<OWLEntity> getData(URI file, String AorB) throws Exception {

		OWLOntologyManager manager = OWLManager.createOWLOntologyManager();
		IRI iri = IRI.create(file);
		OWLOntology ont = manager.loadOntologyFromOntologyDocument(iri);
		
		if (AorB.equals("A")){
			this.ontA = ont;
		} else if (AorB.equals("B")){
			this.ontB = ont;
		}
		Set<OWLEntity> entities = null;
		if (this.ontology.equals("conference_v1")){
			entities = getConferenceEntities(ont);
		} else if (this.ontology.equals("anatomy")){
			entities = getAnatomyEntities(ont);
		} else if (this.ontology.equals("geo")){
			if (AorB.equals("A")){
				entities = getEnvoEntities(ont);
			} else if (AorB.equals("B")){
				entities = getAnatomyEntities(ont);
			}
		} else if (this.ontology.equals("hydro")){
			entities = getConferenceEntities(ont);
		}
		
		return entities;
	}
	
	private Set<OWLEntity> getAnatomyEntities(OWLOntology ont) {
			return ont.getSignature();
	}
	
	private Set<OWLEntity> getEnvoEntities(OWLOntology ont) {
		Set<OWLObjectProperty> aObjectProps = ont.getObjectPropertiesInSignature();
		Set<OWLDataProperty> aDataProps = ont.getDataPropertiesInSignature();
		Set<OWLAnnotationProperty> aAnnotationProps = ont.getAnnotationPropertiesInSignature();
		Set<OWLClass> aClasses = ont.getClassesInSignature();
		
		Set<OWLEntity> aEntities = new HashSet<>();
		aEntities.addAll(aObjectProps);
		aEntities.addAll(aDataProps);
		aEntities.addAll(aAnnotationProps);
		aEntities.addAll(aClasses);
		return aEntities;
	}

	private Set<OWLEntity> getConferenceEntities(OWLOntology ont){
		Set<OWLObjectProperty> aObjectProps = ont.getObjectPropertiesInSignature();
		Set<OWLDataProperty> aDataProps = ont.getDataPropertiesInSignature();
		Set<OWLAnnotationProperty> aAnnotationProps = ont.getAnnotationPropertiesInSignature();
		Set<OWLClass> aClasses = ont.getClassesInSignature();
		Set<OWLNamedIndividual> aNamedIndividual = ont.getIndividualsInSignature();

		Set<OWLEntity> aEntities = new HashSet<>();
		aEntities.addAll(aObjectProps);
		aEntities.addAll(aDataProps);
		aEntities.addAll(aAnnotationProps);
		aEntities.addAll(aClasses);
		aEntities.addAll(aNamedIndividual);
		
		return aEntities;
	}
	
	public void cleanUp(){
		this.ontA = null;
		this.ontB = null;
	}
	
	public void storeCell(OWLEntity a, OWLEntity b, double confidence) throws AlignmentException{
		Cell cell = this.dummyAlignment.addAlignCell(a.getIRI().toURI(), b.getIRI().toURI(), "=", confidence);

		if (matched.containsKey(a)) {
			ArrayList<Cell> temp = matched.get(a);

			if (temp.get(0).getStrength() == confidence) {
				temp.add(cell);
				matched.put(a, temp);
			} else if (temp.get(0).getStrength() < confidence) {
				temp.clear();
				temp.add(cell);
				matched.put(a, temp);
			}
		} else {
			ArrayList<Cell> temp = new ArrayList<>();
			temp.add(cell);
			matched.put(a, temp);
		}
	}
	
	public void completeAlignment(String name1, String name2) throws AlignmentException{
		URIAlignment alignment = new URIAlignment();
		HashSet<String> done = new HashSet<>();
		for (OWLEntity e : matched.keySet()) {
			if (matched.containsKey(e)) {
				ArrayList<Cell> temp = matched.get(e);
				if (temp.size() == 1) {
					Cell cell = temp.get(0);
					String key = cell.getObject1AsURI() + "|" + cell.getObject2AsURI();
					if (!done.contains(key)) {
						alignment.addAlignCell(cell.getObject1AsURI(),cell.getObject2AsURI(), "=", cell.getStrength());
						System.out.println(cell.getObject1AsURI() + " == " + cell.getObject2AsURI());
						done.add(key);
					}
				}
			}
		}
		writeAlignmentToFile(alignment, name1, name2);
		matched = new HashMap<>();
	}
	
//	public void completeAlignment(String name1, String name2) throws AlignmentException{
//		URIAlignment alignment = new URIAlignment();
//		HashSet<String> done = new HashSet<>();
//		for (OWLEntity e : matched.keySet()) {
//			if (matched.containsKey(e)) {
//				ArrayList<Cell> temp = matched.get(e);
//				for(Cell cell : temp){
////				Cell cell = temp.get(0);
//					String key = cell.getObject1AsURI() + "|" + cell.getObject2AsURI();
//					
//					alignment.addAlignCell(cell.getObject1AsURI(),cell.getObject2AsURI(), "=", cell.getStrength());
//					System.out.println(cell.getObject1AsURI() + " == " + cell.getObject2AsURI());
//					done.add(key);
//				}
//				
//			}
//		}
//		writeAlignmentToFile(alignment, name1, name2);
//		matched = new HashMap<>();
//	}
	
	public void writeAlignmentToFile(Alignment alignment, String name1, String name2){
		File answers = new File("./data/" + this.ontology + "/results/" + name1 + "-" + name2 + ".rdf");
		
		try {
			FileOutputStream os = new FileOutputStream(answers);
			PrintWriter writer = new PrintWriter(new OutputStreamWriter(os, "UTF-8"), true);

			AlignmentVisitor renderer = new RDFRendererVisitor(writer);
			alignment.render(renderer);
			writer.close();
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} 	
	}
	
	public String preprocessLabel(OWLEntity e, String AorB){
		if (this.ontology.equals("conference_v1")){
			if (AorB.equals("A")){
				return Preprocessing.preprocess(e, this.ontA);
			} else if (AorB.equals("B")){
				return Preprocessing.preprocess(e, this.ontB);
			}
		} else if (this.ontology.equals("anatomy")){
			if (AorB.equals("A")){
				return Preprocessing.preprocess(getLabelAnatomy(e, this.ontA));
			} else if (AorB.equals("B")){
				return Preprocessing.preprocess(getLabelAnatomy(e, this.ontB));
			}
		} else if (this.ontology.equals("geo")){
			if (AorB.equals("A")){
				return Preprocessing.preprocess(getGeoLabel(e, this.ontA));
			} else if (AorB.equals("B")){
//				System.out.println(Preprocessing.preprocess(e, this.ontB));
				return Preprocessing.preprocess(e, this.ontB);
			}
		} else if (this.ontology.equals("hydro")){
			if (AorB.equals("A")){
				return getLabelHydro(Preprocessing.preprocess(e, this.ontA));
			} else if (AorB.equals("B")){
				return getLabelHydro(Preprocessing.preprocess(e, this.ontB));
			}
		}
		
		return null;
	}
	
	private static String getLabelHydro(String label){
		label = label.toLowerCase();
		label = label.replace("_", " ");
		if (label.length() > 1){
			label = label.substring(0, 1).toUpperCase() + label.substring(1);
		}
		return label;
	}
	
	private String getLabelAnatomy(OWLEntity e, OWLOntology ont) {
		
		String label = e.getIRI().toString();
		
    	Set<OWLAnnotation> labels = e.getAnnotations(ont, new OWLAnnotationPropertyImpl(OWLDataFactoryImpl.getInstance(), IRI.create("http://www.w3.org/2000/01/rdf-schema#label")));
    	
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
	
}
