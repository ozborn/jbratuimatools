package edu.uab.ccts.nlp.uima.annotator.cuiless;

import java.io.Serializable;
import java.util.Collection;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import com.google.common.collect.HashMultiset;
import com.google.common.collect.Multisets;

import edu.uab.ccts.nlp.brat.BratConstants;
import edu.uab.ccts.nlp.uima.annotator.brat.BratParserAnnotator;
import edu.uab.ccts.nlp.umls.tools.UMLSTools;

import org.apache.ctakes.typesystem.type.refsem.OntologyConcept;
import org.apache.ctakes.typesystem.type.refsem.UmlsConcept;
import org.apache.ctakes.typesystem.type.textsem.IdentifiedAnnotation;
import org.apache.uima.analysis_engine.AnalysisEngineProcessException;
import org.apache.uima.cas.FeatureStructure;
import org.apache.uima.fit.util.JCasUtil;
import org.apache.uima.jcas.JCas;
import org.apache.uima.jcas.cas.FSArray;
import org.cleartk.util.ViewUriUtil;

import brat.type.DiscontinousBratAnnotation;


public class AnnotatorStatistics implements Serializable {

	public static final String ALL_ANNOTATORS = "ALL_ANNOTATORS";
	private static final long serialVersionUID = 1L;
	private static Hashtable<String,Hashtable<String,HashMultiset<String>>> anno_results = null;
	private static Hashtable<String,String> map_type_hash = null; //Key docname+T+id, value = CUIs string (comma separated)
	private static Hashtable<String,String> text_type_hash = null; //Key annotator_name+doc_name+entity_id - Value = text
	private static Hashtable<String,Hashtable<DiscontinousBratAnnotation,Integer>> brat_ctakes_matching_cui_count;
	private static Hashtable<String,Hashtable<DiscontinousBratAnnotation,Set<String>>> brat_ctakes_failed_cuis;
	private static HashSet<String> wrong_vocabulary_cuis = null;
	static Hashtable<String,Hashtable<String,Hashtable<String,String>>> exact_results
	= new Hashtable<String,Hashtable<String,Hashtable<String,String>>>();

	public Hashtable<String, Hashtable<String, HashMultiset<String>>> getAnnotatorStats() {
		return anno_results;
	}

	public AnnotatorStatistics(){
		//Contains key annotator name, value Hashtable with key discontinous
		//text and value HashMultiset containing elements that are strings
		//of ordered, comma separated CUIs
		anno_results = new Hashtable<String,Hashtable<String,HashMultiset<String>>>();
		map_type_hash = new Hashtable<String,String>();
		text_type_hash = new Hashtable<String,String>(); 
		Hashtable<String,HashMultiset<String>> all = new Hashtable<String,HashMultiset<String>>();
		anno_results.put(ALL_ANNOTATORS, all);
		wrong_vocabulary_cuis = new HashSet<String>();
		brat_ctakes_matching_cui_count = new Hashtable<String,Hashtable<DiscontinousBratAnnotation,Integer>>(); //Key document_name, Value = Hashtable with key DiscontinousBratAnnotation, Values Count of Matching CTAKES CUIS
		brat_ctakes_failed_cuis = new Hashtable<String,Hashtable<DiscontinousBratAnnotation,Set<String>>>(); //Key document_name, Value = Hashtable with key DiscontinousBratAnnotation, Values Set of failed to match CUIS
		//Exact Results contains key annotator name, value Hashtable with
		//key document_id and value Hashtable with key entity 
		//identifier (Txx) and value comma separated CUIs
		exact_results = new Hashtable<String,Hashtable<String,Hashtable<String,String>>>();
	}

	/**
	 */
	public void addCtakesCUIs(JCas annview, JCas ctakesview) throws AnalysisEngineProcessException {
		Collection<DiscontinousBratAnnotation> brats = JCasUtil.select(annview, DiscontinousBratAnnotation.class);
		Hashtable<DiscontinousBratAnnotation,Integer> cui_matches = new Hashtable<DiscontinousBratAnnotation,Integer>();
		Hashtable<DiscontinousBratAnnotation,Set<String>> failed_matches = new Hashtable<DiscontinousBratAnnotation,Set<String>>();
		String docname = ViewUriUtil.getURI(annview).toString();
		for(DiscontinousBratAnnotation dba : brats) {
			docname = dba.getDocName();
			TreeSet<String> bratcuis =  new TreeSet<String>();
			Integer cui_match_count = new Integer(0);
			String commacui = getCUIs(dba);
			String commabratcuis[] = commacui.split(",");
			for(int i=0;i<commabratcuis.length;i++) { bratcuis.add(commabratcuis[i]);}
			System.out.println("Doc:"+docname+" with brat:"+dba.getCoveredText()+ " and cuis:"+commacui);
			Collection<IdentifiedAnnotation> overlap = JCasUtil.selectCovered(ctakesview,
					IdentifiedAnnotation.class,dba.getBegin(),dba.getEnd());
			for(Iterator<IdentifiedAnnotation> it=overlap.iterator();it.hasNext();) {
				IdentifiedAnnotation ia = it.next();
				FSArray fsArray = ia.getOntologyConceptArr();
				if(fsArray == null) break;
				for(FeatureStructure featureStructure : fsArray.toArray()) {
					OntologyConcept ontologyConcept = (OntologyConcept) featureStructure;
					if(ontologyConcept instanceof UmlsConcept) {
						UmlsConcept umlsConcept = (UmlsConcept) ontologyConcept;
						String code = umlsConcept.getCui();
						if(bratcuis.contains(code)) { cui_match_count++; }
						else {
							System.out.println(ctakesview.getDocumentText().substring(ia.getBegin(), ia.getEnd())+" with "+code+" does not match");
							if(failed_matches.isEmpty()) failed_matches.put(dba, new TreeSet<String>());
							Set<String> fcuis = failed_matches.get(dba);
							fcuis.add(code);
						}
					} else {
						String ucode = ontologyConcept.getCode();
						if(ucode.startsWith("C")) {
							if(bratcuis.contains(ucode)) { cui_match_count++; }
							else {
								if(failed_matches.isEmpty()) failed_matches.put(dba, new TreeSet<String>());
								Set<String> fcuis = failed_matches.get(dba);
								fcuis.add(ucode);
								System.out.println(ia.getCoveredText()+" with "+ucode+" does not match");
							}
						}
					}
				}
			}	
			if(cui_match_count>=bratcuis.size()) {
				System.out.println("CTakes has CUI");
			} else {
				System.out.println("CTakes lacks CUI/s");
			}
			cui_matches.put(dba, cui_match_count);
		}
		System.out.println("Total count of CUI matches:"+cui_matches.size());
		brat_ctakes_matching_cui_count.put(docname, cui_matches);
		brat_ctakes_failed_cuis.put(docname, failed_matches);
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
			if(dba.getIsNovelEntity()==true) {
				//System.out.println("Skipping "+dba.getAnnotatorName()+"--"+
				//dba.getDocName()+"--T"+dba.getId()+"---"+dba.getCoveredText());
				continue;
			}
			String allcuis = getCUIs(dba);
			buildAnnotationHash(annotator_name, text_key, allcuis,anno_results);
			map_type_hash.put(dba.getDocName()+"T"+dba.getId(), allcuis);
			text_type_hash.put(annotator_name+":"+dba.getDocName()+":T"+dba.getId(),text_key);
			buildExactResults(dba,allcuis);
		}
	}


	//Exact Results contains key annotator name, value Hashtable with
	//key document_id and value Hashtable with key entity 
	//identifier (Txx) and value comma separated CUIs
	private void buildExactResults(DiscontinousBratAnnotation dba, String allcuis){
		Hashtable<String,Hashtable<String,String>> docid_hash = exact_results.get(dba.getAnnotatorName());
		if(docid_hash==null) docid_hash = new Hashtable<String,Hashtable<String,String>>();
		Hashtable<String,String> entid_hash = docid_hash.get(dba.getDocName());
		if(entid_hash==null) entid_hash = new Hashtable<String,String>();
		entid_hash.put("T"+dba.getId(), allcuis);
		docid_hash.put(dba.getDocName(), entid_hash);
		exact_results.put(dba.getAnnotatorName(), docid_hash);
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



	/**
	 * Returns comma separated CUI string
	 * @param dba
	 * @return
	 */
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
		printGlobalTypeCounts(map_type_hash);
	}

	public void print(){
		print(anno_results);
	}



	/**
	 * Prints distribution of CUIs
	 * @param cuishash
	 */
	private void printGlobalTypeCounts(Hashtable<String,String> cuishash){
		int single_map=0, double_map=0,triple_map=0,quad_map=0, total_count=0;
		Collection<String> maps = cuishash.values();
		for(String cui : maps){
			total_count++;
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
		System.out.println("Global Single:"+single_map+" Double:"+double_map+
				" Triple:"+triple_map+" Quad:"+quad_map+" Total:"+total_count);

	}


	public void printSemanticTypeDistribution(){
		//Key is CUI, Value is Hashmultiset of semantic types with counts
		Hashtable<String,HashMultiset<String>> dist = new Hashtable<String,HashMultiset<String>>();
		Hashtable<String,String> cuistypehash = new Hashtable<String,String>(); //Key CUI, Value , separated semantic types
		HashMultiset<String> stdist = HashMultiset.create();
		HashMultiset<String> doublestdist = HashMultiset.create();
		int bad_count=0, bad_form_count=0, total_cui_count=0;
		try {
			//for(String cuis : map_type_hash.values()) {
			for(Map.Entry<String,String> mapentry : map_type_hash.entrySet()) {
				String cuis = mapentry.getValue();
				String[] cs = cuis.split(",");
				if(BratParserAnnotator.isWellFormedCUI(cs[0])) { doublestdist.add(cuis); }
				//Iterate through all the cuis for this mapping
				for(int i=0;i<cs.length;i++){
					String cui = cs[i].trim();
					if(!BratParserAnnotator.isWellFormedCUI(cui)) {
						System.out.println(mapentry.getKey()+" has badly formed cui:"+cui);
						bad_form_count++;
						continue;
					}
					total_cui_count++;
					HashMultiset<String> exist = dist.get(cui);
					if(exist==null){
						exist = HashMultiset.create();
						String[] cuiinfo = UMLSTools.fetchCUIInfo(cui, BratConstants.UMLS_DB_CONNECT_STRING);
						cuistypehash.put(cui,cuiinfo[2]);
						if(cuiinfo[3].indexOf("SNOMEDCT")==-1) {
							System.out.println(mapentry.getKey()+" has non-SNOMED-CT CUI "+cui+" from ontology "+cuiinfo[3]);
							wrong_vocabulary_cuis.add(cui);
							bad_count++;
						}
						HashMultiset<String> clean = cleanSemanticTypes(cuiinfo[2],stdist);
						exist.addAll(clean);
					} 
					else {
						String sts = cuistypehash.get(cui);
						HashMultiset<String> clean = cleanSemanticTypes(sts,stdist);
						exist.addAll(clean);
					}
					dist.put(cui, exist);
				}
			}
		} catch (Exception e) { e.printStackTrace(); }
		for(String cui : dist.keySet()){
			Set<String> stypes = dist.get(cui).elementSet();
			String values = "";
			for(String st : stypes){
				values += st+":"+dist.get(cui).count(st);
			}
			System.out.println(cui+"-"+values);
		}
		for (String type : Multisets.copyHighestCountFirst(stdist).elementSet()) {
			System.out.println(type + ": " + stdist.count(type));
		}
		for (String type : Multisets.copyHighestCountFirst(doublestdist).elementSet()) {
			System.out.println(type + ": " + doublestdist.count(type));
		}
		System.out.println("Total Unique CUI count was:"+dist.keySet().size());
		System.out.println("Total CUI count was:"+total_cui_count);
		System.out.println("Badly formed CUI count was:"+bad_form_count);
		System.out.println("Total wrong vocabulary CUI count was:"+bad_count);
		System.out.println("Total unique wrong vocabulary CUI count was:"+wrong_vocabulary_cuis.size());
	}


	private HashMultiset<String> cleanSemanticTypes(String raw, HashMultiset<String> histogram){
		String[] stypes = raw.split(",");
		HashMultiset<String> clean = HashMultiset.create();
		if(stypes.length==0) clean.add("not_available");
		for(int j=0;j<stypes.length;j++){
			String addme = stypes[j].trim();
			if(stypes[j].trim().equals("")) {
				addme = "cui_not_in_database";
			} 
			clean.add(addme);
			histogram.add(addme);
		}
		return clean;
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


	/**
	 * Counts the agreements between annotators
	 * @return
	 */
	public double calculateAgreement(){
		double agreements = 0.0, disagreements = 0.0;
		Object[] annotators = exact_results.keySet().toArray();
		System.out.println(annotators[0]+" first annotator"); System.out.flush();
		Hashtable<String,Hashtable<String,String>> reftable = 
				exact_results.get(annotators[0]);
		Set<String> refdocs = reftable.keySet();
		for(String rdoc : refdocs){
			//System.out.println("Looking at document:"+rdoc);
			Hashtable<String,String> refents = reftable.get(rdoc);
			Hashtable<String,String> testents = 
					exact_results.get(annotators[1]).get(rdoc);
			if(testents==null) {
				//System.out.println(annotators[1]+" did not complete "+rdoc);
				continue;
			}
			//Iterate through each entity in the document
			//Check to see how many documents are missing
			for(Iterator<String> entiter = refents.keySet().iterator(); entiter.hasNext();){
				String refentity = entiter.next();
				//System.out.println("Looking at entity:"+refentity);
				String refcuis = refents.get(refentity);
				String testcuis = testents.get(refentity);
				if(refcuis==null && testcuis==null) continue;
				if(refcuis!=null && testcuis==null) {
					System.out.println(annotators[1]+" has null value for "+refentity);
					continue;
				}
				if(refcuis==null && testcuis!=null) {
					System.out.println(annotators[0]+" has null value for "+refentity);
					continue;
				}

				if(refcuis.trim().equalsIgnoreCase(testcuis.trim())) {
					agreements++;
				} else {
					String refnames = UMLSTools.fetchBestConceptName(refcuis, BratConstants.UMLS_DB_CONNECT_STRING);
					String testnames = UMLSTools.fetchBestConceptName(testcuis, BratConstants.UMLS_DB_CONNECT_STRING);
					disagreements++;
					String typekey = annotators[0]+":"+rdoc+":"+refentity;
					String distext = text_type_hash.get(typekey);
					System.out.println("DISAGREE\t"+rdoc+"\t"+refentity+"\t"+distext+"\t"
							+refcuis+"\t"+refnames+"\t"+testcuis+"\t"+testnames);
				}
			}
		}
		System.out.println(disagreements+" disagreements");
		return agreements/(agreements+disagreements);
	}

	/**
	 * Prints out the text for multiple CUIs regardless of annotator
	 */
	public void printMultipleCUIText(){
		HashMultiset<String> stype_combinations = HashMultiset.create();
		StringBuffer mutlicuis = new StringBuffer();
		mutlicuis.append("");
		Hashtable<String,HashMultiset<String>> alls = anno_results.get(ALL_ANNOTATORS);
		Set<String> texts = alls.keySet();
		for(String text : texts) {
			HashMultiset<String> test = alls.get(text);
			String any = test.iterator().next();
			for (String s : test.elementSet()) {
				String[] cuiarray = s.split(",");
				if(cuiarray.length<2) continue;
				String bname = null;
				bname = UMLSTools.fetchBestConceptName(s, BratConstants.UMLS_DB_CONNECT_STRING);
				if(bname.indexOf("||")==-1) continue;
				String[] pieces = bname.split("\\|\\|");
				StringBuffer key = new StringBuffer();
				for(String comp : pieces) {
					//System.out.println(comp);
					if(comp!=null && comp.length()>5) {
						key.append(comp.substring(comp.length()-5, comp.length()-1)); 
					}
					key.append(" ");
				}
				stype_combinations.add(key.toString());
				mutlicuis.append("\t"+text);
				mutlicuis.append("\t"+bname);
				mutlicuis.append("\t"+s);
				mutlicuis.append("\t"+test.count(s));
				mutlicuis.append("\n");
			}
		}
		System.out.print(mutlicuis.toString());
		for(String s : stype_combinations.elementSet()){
			System.out.println(s+"\t"+stype_combinations.count(s));
		}
		System.out.print(stype_combinations);
	}


	public void printCtakesBratSummary(){
		Integer all_failed=0,all_matched=0;
		for(Iterator<String> it = brat_ctakes_matching_cui_count.keySet().iterator();it.hasNext();){
			String docname = it.next();
			Hashtable<DiscontinousBratAnnotation,Integer> matches = brat_ctakes_matching_cui_count.get(docname);
			for(Iterator<DiscontinousBratAnnotation> bratit = matches.keySet().iterator();it.hasNext();) {
				DiscontinousBratAnnotation brat = bratit.next();
				Hashtable<DiscontinousBratAnnotation,Set<String>> failed = brat_ctakes_failed_cuis.get(docname);
				Set<String> fcuis = failed.get(brat);
				all_failed+=fcuis.size();
				all_matched += matches.get(brat);
				String bratcuis="";
				for(int i=0;i<brat.getOntologyConceptArr().size();i++) { bratcuis+=brat.getOntologyConceptArr(i);}
				System.out.println("Doc:"+docname+" with text:"+brat.getCoveredText()+
				" with CUIs:"+bratcuis+" and failed cuis:"+fcuis);
			}
		}
		System.out.println("All matched:"+all_matched+" All failed:"+all_failed);
	}


	public String getDiscrepancies() throws Exception { 
		return getDiscrepancies(anno_results) ; 
	}

}
