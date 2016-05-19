package evaluation;

import java.io.File;
import java.io.FileReader;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Set;

import org.semanticweb.owl.align.Alignment;
import org.semanticweb.owl.align.AlignmentException;
import org.semanticweb.owl.align.Cell;
import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.model.IRI;
import org.semanticweb.owlapi.model.OWLEntity;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLOntologyManager;

import fr.inrialpes.exmo.align.parser.AlignmentParser;
import util.Preprocessing;


public class ConferenceEvaluation {
	
	public static void main(String[] args) throws Exception {
		
		AlignmentParser parser = new AlignmentParser(0);

		File dir = new File("./data/conference_v1/");
		
		
		String[] files = {"./data/conference_v1/cmt.owl", "./data/conference_v1/conference.owl", "./data/conference_v1/confOf.owl",  "./data/conference_v1/edas.owl", "./data/conference_v1/ekaw.owl", "./data/conference_v1/iasted.owl", "./data/conference_v1/sigkdd.owl"};
		
		int tp = 0;
		int fp = 0;
		int fn = 0;
		int tpnumExact = 0;
		int fnnumExact = 0;
		int tpProp = 0;
		int fpProp = 0;
		int fnProp = 0;
		
		int tpClass = 0;
		int fpClass = 0;
		int fnClass = 0;
		
		System.out.println("Evaluating results...");
		
		for (int i=0; i<files.length; i++) {
			
			//File f1 = files[i];
			File f1 = new File(files[i]);
			
			if (!f1.toString().endsWith(".owl")) continue;
			
			for (int j=i; j<files.length; j++) {
				
				//File f2 = files[j];
				File f2 = new File(files[j]);
				
				if (!f2.toString().endsWith(".owl") || 
						f1.toString().equals(f2.toString())) continue;
				
				String s1 = f1.toString();
				String s2 = f2.toString();
				String name1 = s1.substring(s1.lastIndexOf("/")+1, s1.lastIndexOf("."));
				String name2 = s2.substring(s2.lastIndexOf("/")+1, s2.lastIndexOf("."));
				
				File answers = new File(
						dir + "/results/" + name1 + "-" + name2 + ".rdf"); 
				
				File refFile = new File(
						dir + "/reference-alignment/" + name1 + "-" + name2 + ".rdf"); 
				
				Alignment matcherAlignment = parser.parse(new FileReader(answers));
				Alignment refAlignment = parser.parse(new FileReader(refFile));
				
				OWLOntologyManager manager = OWLManager.createOWLOntologyManager();
				IRI iriA = IRI.create(f1);
				OWLOntology ontA = manager.loadOntologyFromOntologyDocument(iriA);
				
				IRI iriB = IRI.create(f2);
				OWLOntology ontB = manager.loadOntologyFromOntologyDocument(iriB);
			
				// true positives: things in answers that are also in reference
				Enumeration<Cell> cells = matcherAlignment.getElements();
				while (cells.hasMoreElements()) {
					
					Cell ans = cells.nextElement();
					Enumeration<Cell> refs = refAlignment.getElements();
					
					while (refs.hasMoreElements()) {
						Cell ref = refs.nextElement();
						
						if (match(ans, ref)) {

							tp++;
//							System.out.println(Preprocessing.preprocess(ans.getObject1AsURI().toString()));
//							System.out.println(Preprocessing.preprocess(ans.getObject2AsURI().toString()));
							if(Preprocessing.preprocess(ans.getObject1AsURI().toString()).equals(Preprocessing.preprocess(ans.getObject2AsURI().toString()))){
								tpnumExact++;
							}
							if (involvesProperty(ans, ontA, ontB)) {
								tpProp++;
//								System.out.println(ans.getObject1AsURI() + " = " + 
//								ans.getObject2AsURI() + " -- right");
							} else {
								tpClass++;
							}
							
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
						
						fp++;
						
						if (involvesProperty(ans, ontA, ontB)) {
							fpProp++;
//							System.out.println(ans.getObject1AsURI() + " = " + 
//							ans.getObject2AsURI() + " -- false positive");
						} else {
							fpClass++;
						}
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
						
						fn++;
						
						String labelA = Preprocessing.preprocess(ans.getObject1AsURI().toString());
						String labelB = Preprocessing.preprocess(ans.getObject2AsURI().toString());
						System.out.println(labelA);
						System.out.println(labelB);
						if(labelA.equals(labelB)){
							System.out.println("FUCKING HERE");
							fnnumExact++;
						}
						if (involvesProperty(ans, ontA, ontB)) {
							fnProp++;
//							System.out.println(ans.getObject1AsURI() + " = " + 
//							ans.getObject2AsURI() + " -- false negative");
						} else {
							fnClass++;
						}
					}
				}
			}
		}
				
		System.out.println("tp: " + tp);
		System.out.println("fp: " + fp);
		System.out.println("fn: " + fn);
		
		System.out.println("tpExact: " + tpnumExact);
		System.out.println("fnExact: " + fnnumExact);
		
		System.out.println("tpClass: " + tpClass);
		System.out.println("fpClass: " + fpClass);
		System.out.println("fnClass: " + fnClass);
		
		System.out.println("tpProp: " + tpProp);
		System.out.println("fpProp: " + fpProp);
		System.out.println("fnProp: " + fnProp);
		
		double precision = tp / ((float) (tp + fp));
		double recall = tp / ((float) (tp + fn));
		double fmeasure = (2 * precision * recall) / (precision + recall);
		
		double recallPlus = (tp-tpnumExact) / ((float) ((tp-tpnumExact) + (fn-fnnumExact)));
		
		double classPrecision = tpClass / ((float) (tpClass + fpClass));
		double classRecall = tpClass / ((float) (tpClass + fnClass));
		
		double propPrecision = tpProp / ((float) (tpProp + fpProp));
		double propRecall = tpProp / ((float) (tpProp + fnProp));

		System.out.println("f-measure: " + Math.round(fmeasure * 100)/100.0);
		System.out.println("precision: " + Math.round(precision * 100)/100.0);
		System.out.println("recall: " + Math.round(recall * 100)/100.0);
		System.out.println("recall+: " + Math.round(recallPlus * 100)/100.0);
		System.out.println();
		
		System.out.println("precision (class): " + Math.round(classPrecision * 100)/100.0);
		System.out.println("recall (class): " +  Math.round(classRecall * 100)/100.0);
		System.out.println();
		
		System.out.println("precision (property): " +  Math.round(propPrecision * 100)/100.0);
		System.out.println("recall (property): " +  Math.round(propRecall * 100)/100.0);
		System.out.println();
		
		System.out.println(tpProp + " out of " + (tpProp + fpProp) + " guesses");	
	}
	
	
	private static boolean match(Cell c1, Cell c2) throws AlignmentException {
		return c1.getObject1AsURI().toString().equals(c2.getObject1AsURI().toString()) && 
				c1.getObject2AsURI().toString().equals(c2.getObject2AsURI().toString());
	}
	
	
	private static boolean involvesProperty(Cell c, OWLOntology ont1, OWLOntology ont2) 
			throws AlignmentException {
		
		Set<OWLEntity> ents1 = ont1.getEntitiesInSignature(IRI.create(c.getObject1AsURI()));
		Set<OWLEntity> ents2 = ont2.getEntitiesInSignature(IRI.create(c.getObject2AsURI()));
		
		for (OWLEntity ent: ents1) {
			if (ent.isOWLAnnotationProperty() || ent.isOWLDataProperty() || 
					ent.isOWLObjectProperty()) {
				return true;
			}
		}
		
		for (OWLEntity ent: ents2) {
			if (ent.isOWLAnnotationProperty() || ent.isOWLDataProperty() || 
					ent.isOWLObjectProperty()) {
				return true;
			}
		}
		
		return false;
	}
}