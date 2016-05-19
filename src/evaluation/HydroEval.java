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

import org.semanticweb.owl.align.Alignment;
import org.semanticweb.owl.align.Cell;

import fr.inrialpes.exmo.align.parser.AlignmentParser;

public class HydroEval {
	
	public static void doHydro() throws Exception{

		AlignmentParser parser = new AlignmentParser(0);
//		HashSet<String> allMatches = new HashSet<>();
		ArrayList<String> allMatches = new ArrayList<>();
		
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
				
//				File answers = new File( dir + "/article-results/" + name1 + "-" + name2 + ".rdf"); 
//				Alignment matcherAlignment = parser.parse(new FileReader(answers));
//				Enumeration<Cell> cells = matcherAlignment.getElements();
//				while (cells.hasMoreElements()) {		
//					Cell ans = cells.nextElement();
//					allMatches.add(ans.getObject1AsURI() + "|" + ans.getObject2AsURI());
//				}
//				File answers1 = new File( dir + "/snippet-results/" + name1 + "-" + name2 + ".rdf"); 
//				Alignment matcherAlignment1 = parser.parse(new FileReader(answers1));
//				Enumeration<Cell> cells1 = matcherAlignment1.getElements();
//				while (cells1.hasMoreElements()) {		
//					Cell ans = cells1.nextElement();
//					allMatches.add(ans.getObject1AsURI() + "|" + ans.getObject2AsURI());
//				}
//				File answers2 = new File( dir + "/terms-results/" + name1 + "-" + name2 + ".rdf"); 
//				Alignment matcherAlignment2 = parser.parse(new FileReader(answers2));
//				Enumeration<Cell> cells2 = matcherAlignment2.getElements();
//				while (cells2.hasMoreElements()) {		
//					Cell ans = cells2.nextElement();
//					allMatches.add(ans.getObject1AsURI() + "|" + ans.getObject2AsURI());
//				}
				File answers2 = new File( dir + "/levenstein-results/" + name1 + "-" + name2 + ".rdf"); 
				Alignment matcherAlignment2 = parser.parse(new FileReader(answers2));
				Enumeration<Cell> cells2 = matcherAlignment2.getElements();
				while (cells2.hasMoreElements()) {		
					Cell ans = cells2.nextElement();
					allMatches.add(ans.getObject1AsURI() + "|" + ans.getObject2AsURI());
				}
				
			}
		}
		
//		BufferedReader br = new BufferedReader(new FileReader("./data/hydro/turkResults/True-False Relationships.csv"));
//		String line = "";
//		HashMap<String, Boolean> results = new HashMap<>();
//		while ((line = br.readLine()) != null) {
//			String[] parts = line.split(",");
//			String match = parts[0];
//			match = match.replace("\"", "");
//			String[] matchparts = match.split("\\|");
//			int correct = Integer.parseInt(parts[2].replace("\"", ""));
//			int incorrect = Integer.parseInt(parts[3].replace("\"", ""));
//			if (correct > incorrect){
//				results.put(("http://" + matchparts[0] + "|" + "http://" + matchparts[1]).toLowerCase(), true);
//				System.out.println(("http://" + matchparts[0] + "|" + "http://" + matchparts[1]).toLowerCase());
//			} else {
//				results.put(("http://" + matchparts[0] + "|" + "http://" + matchparts[1]).toLowerCase(), false);
//				System.out.println(("http://" + matchparts[0] + "|" + "http://" + matchparts[1]).toLowerCase());
//			}
//		}
//		br.close();
		
		BufferedReader br = new BufferedReader(new FileReader("./HydroQuestions.txt"));
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
		
		int tp = 0;
		int fp = 0;		
		int tpnumExact = 0;
		for(String x : allMatches){
			if (results.containsKey(x.toLowerCase())){
//				System.out.println(x.toLowerCase());
				String[] parts = x.toLowerCase().trim().split("\\|");
				String labelA = parts[0];
				if(labelA.contains("#")){
					String[] partsA = labelA.split("#");
					labelA = partsA[1];
				} else {
					String[] partsA = labelA.split("/");
					labelA = partsA[partsA.length -1];
				}
				labelA = labelA.replace("_", "");
				
				String labelB = parts[1];
				if(labelB.contains("#")){
					String[] partsB = labelB.split("#");
					labelB = partsB[1];
				} else {
					String[] partsB = labelB.split("/");
					labelB = partsB[partsB.length -1];
				}
				labelB = labelB.replace("_", "");
				
//				System.out.println("A - " +labelA);
//				System.out.println("B - " +labelB);
				if(labelA.equals(labelB)){
					tpnumExact++;
				}
				if(results.get(x.toLowerCase())){
					tp ++;
				} else {
					fp ++;
				}
			} else {
				System.out.println("Could not find: " + x.toLowerCase());
			}
		}
		System.out.println("TP: " + tp);
		System.out.println("FP: " + fp);
		System.out.println("TP Exact: " + tpnumExact);
	}
	
	public static void main(String args[]) throws Exception{
		doHydro();
	}
}
