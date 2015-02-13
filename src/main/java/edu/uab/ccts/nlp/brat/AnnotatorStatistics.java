package edu.uab.ccts.nlp.brat;

import java.io.Serializable;
import java.util.Collection;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.Set;
import java.util.TreeSet;

import com.google.common.collect.HashMultiset;

import org.apache.ctakes.typesystem.type.refsem.OntologyConcept;
import org.apache.uima.jcas.cas.FSArray;

import brat.type.DiscontinousBratAnnotation;


public class AnnotatorStatistics implements Serializable {
	
	private static final long serialVersionUID = 1L;
	private static final String ALL_ANNOTATORS = "ALL_ANNOTATORS";
	private Hashtable<String,Set<DiscontinousBratAnnotation>> consistency_data = null; //KEY is covered text

	Hashtable<String,String> single_cuis = new Hashtable<String,String>();
	Hashtable<String,String> missing_cuis = new Hashtable<String,String>();
	Hashtable<String,String> multi_cuis = new Hashtable<String,String>();
	
	Hashtable<String,Hashtable<String,HashMultiset<String>>> anno_results = null;

	public AnnotatorStatistics(){
		//Contains key annotator name, value Hashtable with key discontinous
		//text and value HashMultiset containing elements that are strings
		//of ordered, comma separated CUIs
		anno_results = new Hashtable<String,Hashtable<String,HashMultiset<String>>>();

		//Legacy
		consistency_data = new Hashtable<String,Set<DiscontinousBratAnnotation>>();
	}
	
	public void add(Collection<DiscontinousBratAnnotation> dbas) {
		for(DiscontinousBratAnnotation dba : dbas) {
			FSArray ontarray = dba.getOntologyConceptArr();
			assert(dba.getDocName()!=null);
			assert(dba.getAnnotatorName()!=null);
			assert(ontarray!=null);
			if(ontarray.size()>5) {System.out.println(ontarray.size()); }
			assert(ontarray.size()<6);
			assert(dba.getDocName().length()>4);
			String annotator_name = dba.getAnnotatorName();
			String text_key = dba.getDiscontinousText();

			Hashtable<String,HashMultiset<String>> alltexthash = 
			anno_results.get(ALL_ANNOTATORS);
			Hashtable<String,HashMultiset<String>> dtexthash = 
			anno_results.get(annotator_name);
			if(alltexthash==null) alltexthash = new Hashtable<String,HashMultiset<String>>();
			if(dtexthash==null) dtexthash = new Hashtable<String,HashMultiset<String>>();
			HashMultiset<String> cuiset = dtexthash.get(text_key);
			if(cuiset==null) cuiset = HashMultiset.create();
			cuiset.add(getCUIs(dba));
			dtexthash.put(text_key,cuiset);
			alltexthash.put(text_key,cuiset);
			anno_results.put(annotator_name, dtexthash);
			anno_results.put(ALL_ANNOTATORS, dtexthash);
			
			//Legacy Stuff
			Set<DiscontinousBratAnnotation> current = consistency_data.get(text_key);
			if(current==null) current = new HashSet<DiscontinousBratAnnotation>();
			current.add(dba);
			consistency_data.put(text_key, current);
			populateCUIs(dba, text_key);
		}
	}
	
	
	private String getCUIs(DiscontinousBratAnnotation dba) {
		if(dba.getOntologyConceptArr()==null) { assert(false); }
		int size = dba.getOntologyConceptArr().size();
		if(size==0) {
			return "MISSED_CUIS";
		} else if(size==1) {
			return dba.getOntologyConceptArr(0).getCode();
		} else {
			TreeSet<String> ts = new TreeSet<String>();
			for(int i=0;i<size;i++) {
				ts.add(dba.getOntologyConceptArr(i).getCode());
			}
			String big_string = "";
			for(java.util.Iterator<String> it = ts.iterator();it.hasNext();){
				big_string+=(it.next());
				if(it.hasNext()) big_string+=",";
			}
			return big_string;
		}
		
	}

	private void populateCUIs(DiscontinousBratAnnotation dba, String key) {
		if(dba.getOntologyConceptArr()==null) { missing_cuis.put(key, ""); }
		int size = dba.getOntologyConceptArr().size();
		if(size==0) {
			missing_cuis.put(key, "");
		} else if(size==1) {
			single_cuis.put(key, dba.getOntologyConceptArr(0).getCode());
		} else {
			TreeSet<String> ts = new TreeSet<String>();
			for(int i=0;i<size;i++) {
				ts.add(dba.getOntologyConceptArr(i).getCode());
			}
			String big_string = "";
			for(String s : ts) big_string+=("-"+s);
			multi_cuis.put(key, big_string);
		}
	}
	
	
	public void print(){
		System.out.println(missing_cuis);
		System.out.println(single_cuis);
		System.out.println(multi_cuis);
		System.out.println(anno_results);
		HashMultiset<String> test = anno_results.get(ALL_ANNOTATORS).get("redness");
		test.add("CFAKE");
		System.out.println(anno_results.get(ALL_ANNOTATORS));
		printDiscrepancies();
	}
	
	public void printDiscrepancies(){
		System.out.println("Discrepancies");
		Hashtable<String,HashMultiset<String>> alls = anno_results.get(ALL_ANNOTATORS);
		Set<String> texts = alls.keySet();
		for(String text : texts) {
			HashMultiset<String> test = alls.get(text);
			if(test.size()>1) {
				String any = test.iterator().next();
				if(test.count(any)!=test.size()){
					System.out.println(text+"("+test.size()+") :: "+test);
				}
			}
		}
	}
	
	
	
	

}
