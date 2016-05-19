package util;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;

public class EntityData implements Serializable{
	private String label = "";
	private String article = "";
	private String snippet = "";
	private String[] terms = null;
	private String pageStatus = "";
	private double[] tfidfScoreArticle = null;
	private double[] tfidfScoreSnippet = null;
	private static final long serialVersionUID = 7526471155622776147L;
	
	public EntityData(String label){
		this.label = label;
	}
	
	public EntityData(){
		
    }

	
	public String getPageStatus() {
		return pageStatus;
	}

	public void setPageStatus(String status) {
		this.pageStatus = status;
	}
	
	public String getLabel() {
		return label;
	}

	public void setLabel(String label) {
		this.label = label;
	}

	public String getArticle() {
		return article;
	}

	public void setArticle(String article) {
		this.article = article;
	}

	public String getSnippet() {
		return snippet;
	}

	public void setSnippet(String snippet) {
		this.snippet = snippet;
	}
	
	public void setSnippet(ArrayList<String> snippet) {
		String newS = "";
		for (String s : snippet){
			newS += s + " ";
		}
		this.snippet = newS;
	}
	
	public String[] getTerms() {
		return terms;
	}

	public void setTerms(ArrayList<String> terms) {
		String[] tmp = new String[terms.size()];
		tmp = terms.toArray(tmp);
		this.terms = tmp;
	}

	@Override
	public String toString() {
		return "[label=" + label + ", \n\tpageStatus=" + pageStatus + ", \n\tarticle=" + article
				+ ", \n\tsnippet=" + snippet + ", \n\tterms=" + Arrays.toString(terms) + "]";
	}

	public double[] getTfidfScoreArticle() {
		return tfidfScoreArticle;
	}

	public void setTfidfScoreArticle(double[] tfidfScoreThing) {
		this.tfidfScoreArticle = tfidfScoreThing;
	}
	
	public double[] getTfidfScoreSnippet() {
		return tfidfScoreSnippet;
	}

	public void setTfidfScoreSnippet(double[] tfidfScoreThing) {
		this.tfidfScoreSnippet = tfidfScoreThing;
	}
}
