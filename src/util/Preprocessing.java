package util;



import java.util.ArrayList;
import java.util.Set;

import org.semanticweb.owlapi.model.IRI;
import org.semanticweb.owlapi.model.OWLAnnotation;
import org.semanticweb.owlapi.model.OWLEntity;
import org.semanticweb.owlapi.model.OWLOntology;

import uk.ac.manchester.cs.owl.owlapi.OWLAnnotationPropertyImpl;
import uk.ac.manchester.cs.owl.owlapi.OWLDataFactoryImpl;

// The methods in this utility class are used to pull the labels from ontology
// entities and sort of homogenize them so that they can be compared in an 
// apples-to-apples way by the alignment algorithm. I tried a bunch of different
// homogenizing strategies, but in the end I'm just pulling out the entity label,
// tokenizing it, removing special characters, and putting it into lowercase.

public class Preprocessing {
	
	// pulls the label from the URI of the entity and tokenizes it; tokens 
	// are all lowercase and delimited by a single space
	public static String preprocess(OWLEntity x, OWLOntology ontologyX) {
		String s = getString(x, ontologyX);
		
		s = s.replaceAll("wordnet_", "");
		s = s.replaceAll("wikicategory_", "");
		
		s = Preprocessing.stringTokenize(s, true);
		return s;
	}
	
	// same as the above
	public static String preprocess(String label) {
		String s = getString(label);
		
		s = s.replaceAll("wordnet_", "");
		s = s.replaceAll("wikicategory_", "");
		
		s = Preprocessing.stringTokenize(s, true);
		return s;
	}
	
	// tokenization splits the string into words; word breaks are identified by
	// whitespace, underscores, or hyphens. special characters (e.g. /, \, >)
	// are removed.
	private static ArrayList<String> tokenize(String s, boolean lowercase) {
		if (s == null) {
			return null;
		}

		ArrayList<String> strings = new ArrayList<String>();

		String current = "";
		Character prevC = 'x';

		for (Character c: s.toCharArray()) {

			if ((Character.isLowerCase(prevC) && Character.isUpperCase(c)) || 
					c == '_' || c == '-' || c == ' ' || c == '/' || c == '\\' || c == '>') {

				current = current.trim();

				if (current.length() > 0) {
					if (lowercase) 
						strings.add(current.toLowerCase());
					else
						strings.add(current);
				}

				current = "";
			}

			if (c != '_' && c != '-' && c != '/' && c != '\\' && c != '>') {
				current += c;
				prevC = c;
			}
		}

		current = current.trim();

		if (current.length() > 0) {
			// this check is to handle the id numbers in YAGO
			if (!(current.length() > 4 && Character.isDigit(current.charAt(0)) && 
					Character.isDigit(current.charAt(current.length()-1)))) {
				strings.add(current.toLowerCase());
			}
		}

		return strings;
	}

	// takes the ArrayList of tokens and creates a single string, with tokens
	// separated by a single space
	private static String stringTokenize(String s, boolean lowercase) {
		String result = "";

		ArrayList<String> tokens = tokenize(s, lowercase);
		for (String token: tokens) {
			result += token + " ";
		}

		return result.trim();
	}
	
	// gets the label for an entity based on its URI; first choice is the text after
	// the last # in the URI. if there is no # in the URI, the text after the last / 
	// is used. if that doesn't exist, the rdf-schema label attribute value is used.
	// if that's not there, we give up and return the full URI.
	public static String getString(OWLEntity e, OWLOntology ontology) {
		
		String label = e.getIRI().toString();
		
		if (label.contains("#")) {
			label = label.substring(label.indexOf('#')+1);
			return label;
		}
		
		if (label.contains("/")) {
			label = label.substring(label.lastIndexOf('/')+1);
			return label;
		}
		
    	Set<OWLAnnotation> labels = e.getAnnotations(
    			ontology, new OWLAnnotationPropertyImpl(
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
	
	// same as the above, but cannot check for the rdf-schema label attribute value
	public static String getString(String label) {
		
		if (label.contains("#")) {
			label = label.substring(label.indexOf('#')+1);
			return label;
		}
		
		if (label.contains("/")) {
			label = label.substring(label.lastIndexOf('/')+1);
			return label;
		}
    	
    	return label;
	}

}
