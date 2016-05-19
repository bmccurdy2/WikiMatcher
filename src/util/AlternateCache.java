package util;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Set;

public class AlternateCache {
	
	private HashMap<String, EntityData> cache = null;
	private String dataset = "";
	private String alignment = "";
	
	public AlternateCache(String dataset, String alignment){
		this.dataset = dataset;
		this.alignment = alignment;
		cache = readCache();
	}
	
	@SuppressWarnings("unchecked")
	private HashMap<String, EntityData> readCache() {
		HashMap<String, EntityData> me = null;
		try {
			File cache = new File("./logging/" + this.dataset + "_" +  this.alignment + "_wiki.out");
			if (!cache.exists()) {
				System.out.println("Cache does not exist");
				ObjectOutputStream out = new ObjectOutputStream(
						new FileOutputStream("./logging/" + this.dataset + "_" +  this.alignment + "_wiki.out"));
				out.writeObject(me);
				out.close();
			} else {
				System.out.println("Cache exists - reading in");
				ObjectInputStream in = new ObjectInputStream(
						new FileInputStream("./logging/" + this.dataset + "_" +  this.alignment + "_wiki.out"));
				me = (HashMap<String, EntityData>) in.readObject();
				in.close();
			}
		} catch (Exception ex) {
			ex.printStackTrace();
		}
		if (me == null) {
			System.out.println("Creating new HashMap");
			me = new HashMap<String, EntityData>();
		}
		return me;
	}

	public void saveCache() {
		try {
			ObjectOutputStream out = new ObjectOutputStream(
					new FileOutputStream("./logging/" + this.dataset + "_" +  this.alignment + "_wiki.out"));
			out.writeObject(this.cache);
			out.close();
			System.out.println("Cache Size: " + cache.size());

		} catch (IOException ex) {
			ex.printStackTrace();
		}
	}
	
	public void printCache() {
		try {
			FileOutputStream os1 = new FileOutputStream("./logging/" + this.dataset + "_" +  this.alignment + "_cacheOutput.txt");
			PrintWriter out = new PrintWriter(new OutputStreamWriter(os1,
					"UTF-8"), true);
			for (String key : cache.keySet()) {
				out.println("Label: " + key + " : " + cache.get(key).toString());
			}
			out.close();

		} catch (IOException ex) {
			ex.printStackTrace();
		}
	}
	
	public EntityData getValue(String label){
		if (cache.containsKey(label)) {
			return cache.get(label);
		} 
		return null;
	}
	
	public Set<String> allKeys(){
		return cache.keySet();
	}
	
	public void storeValue(String label, EntityData data){
		cache.put(label, data);
	}
	
	public boolean hasValue(String label){
		return this.cache.containsKey(label);
	}
}
