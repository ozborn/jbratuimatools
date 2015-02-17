package edu.uab.ccts.nlp.uima.annotator.cuiless;

import java.io.Serializable;
import java.sql.Connection;
import java.util.Collection;
import java.util.Hashtable;
import java.util.Set;
import java.util.TreeSet;

import com.google.common.collect.HashMultiset;

import edu.uab.ccts.nlp.brat.BratConstants;
import edu.uab.ccts.nlp.umls.tools.UMLSTools;

import org.apache.uima.jcas.cas.FSArray;

import brat.type.DiscontinousBratAnnotation;


public class AnnotatorStatistics implements Serializable {
	
	public static final String ALL_ANNOTATORS = "ALL_ANNOTATORS";
	private static final long serialVersionUID = 1L;
	private static Hashtable<String,Hashtable<String,HashMultiset<String>>> anno_results = null;

	public Hashtable<String, Hashtable<String, HashMultiset<String>>> getAnnotatorStats() {
		return anno_results;
	}

	public AnnotatorStatistics(){
		//Contains key annotator name, value Hashtable with key discontinous
		//text and value HashMultiset containing elements that are strings
		//of ordered, comma separated CUIs
		anno_results = new Hashtable<String,Hashtable<String,HashMultiset<String>>>();
		Hashtable<String,HashMultiset<String>> all = new Hashtable<String,HashMultiset<String>>();
		anno_results.put(ALL_ANNOTATORS, all);

	}

	
	public void add(Collection<DiscontinousBratAnnotation> dbas){
		for(DiscontinousBratAnnotation dba : dbas) {
			//System.out.println("Processing "+dba.getDiscontinousText()+" from "+
			//dba.getDocName()+"annotated by"+dba.getAnnotatorName());
			FSArray ontarray = dba.getOntologyConceptArr();
			assert(dba.getDocName()!=null);
			assert(dba.getAnnotatorName()!=null);
			assert(ontarray!=null);
			if(ontarray.size()>5) {System.out.println(ontarray.size()); }
			assert(ontarray.size()<6);
			assert(dba.getDocName().length()>4);
			String annotator_name = dba.getAnnotatorName();
			String text_key = dba.getDiscontinousText();
			String allcuis = getCUIs(dba);
			buildAnnotationHash(annotator_name, text_key, allcuis,anno_results);
		}
	}

	private void buildAnnotationHash(String annotator_name, String text_key,
			String allcuis, 
			Hashtable<String,Hashtable<String,HashMultiset<String>>>  bighash){
		Hashtable<String,HashMultiset<String>> alltexthash = 
		bighash.get(ALL_ANNOTATORS); //Always there
		//System.out.println("Words in alltexthash are:"+alltexthash.size());
		Hashtable<String,HashMultiset<String>> dtexthash = 
		bighash.get(annotator_name);
		if(dtexthash==null) {
			dtexthash = new Hashtable<String,HashMultiset<String>>();
			bighash.put(annotator_name, dtexthash);
		}
		
		HashMultiset<String> dcuiset = dtexthash.get(text_key);
		if(dcuiset==null) {
			dcuiset = HashMultiset.create();
			dtexthash.put(text_key,dcuiset);
		}
		dcuiset.add(allcuis);
	
		HashMultiset<String> acuiset = alltexthash.get(text_key);
		if(acuiset==null) {
			acuiset = HashMultiset.create();
			alltexthash.put(text_key,acuiset);
		}
		acuiset.add(allcuis);
		
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

	
	public void print(Hashtable<String,Hashtable<String,HashMultiset<String>>> ghash){
		//System.out.println(ghash);
		//System.out.println(ghash.get(ALL_ANNOTATORS));
		printMappingTypeCounts(ghash);
	}
	
	public void print(){
		print(anno_results);
	}
	
	
	
	
	/**
	 * Prints out the number of single CUIs, double CUIs, etc...
	 */
	void printMappingTypeCounts(Hashtable<String,Hashtable<String,HashMultiset<String>>> ghash){
		int single_map=0, double_map=0,triple_map=0,quad_map=0;
		Hashtable<String,HashMultiset<String>> wordhash = ghash.get(ALL_ANNOTATORS);
		Set<String> texts = wordhash.keySet();
		System.out.println("Distinct text:"+wordhash.size());
		for(String s : texts) {
			HashMultiset<String> maps = wordhash.get(s);
			for(String cui : maps.elementSet()){
				//System.out.println(cui);
				String[] cuis = cui.split("\\s+|,");
				switch (cuis.length) {
				case(0):
					System.out.println("Weird:"+cui);
					break;
				case(1):
					single_map++;
					break;
				case(2):
					double_map++;
					break;
				case(3):
					triple_map++;
					break;
				case(4):
					quad_map++;
					break;
				default:
					System.out.println("Too big?:"+cui);
				}
			}
		}
		System.out.println("Single:"+single_map+" Double:"+double_map+
		" Triple:"+triple_map+" Quad:"+quad_map);
	}
	
	
	public String getDiscrepancies(Hashtable<String,Hashtable<String,HashMultiset<String>>> ghash) throws Exception{
		if(ghash==null) ghash = anno_results;
		StringBuffer discrepancies = new StringBuffer();
		discrepancies.append("");
		int dcount = 0, agree_count=0, unique_count=0;
		Hashtable<String,HashMultiset<String>> alls = ghash.get(ALL_ANNOTATORS);
		Set<String> texts = alls.keySet();
		for(String text : texts) {
			HashMultiset<String> test = alls.get(text);
			if(test.size()>1) {
				String any = test.iterator().next();
				if(test.count(any)!=test.size()){
					discrepancies.append(text+"\t"+test.size());
					for (String s : test.elementSet()) {
						String bname = null;
						bname = UMLSTools.fetchBestConceptName(s, BratConstants.UMLS_DB_CONNECT_STRING);
						discrepancies.append("\t"+bname);
						discrepancies.append("\t"+s);
						discrepancies.append("\t"+test.count(s));
					}
					discrepancies.append("\n");
					dcount++;
				} else { agree_count++; }
			} else { unique_count++; }
		}
		System.out.println(agree_count+" agreements, "+dcount+" discrepancies, "+unique_count+" untestable");
		return discrepancies.toString();
	}
	
	public String getDiscrepancies() throws Exception { 
		return getDiscrepancies(anno_results) ; 
	}

}
