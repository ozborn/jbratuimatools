package edu.uab.ccts.nlp.jbratuimatools.util;

import java.io.Serializable;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import com.google.common.collect.HashMultiset;
import com.google.common.collect.Multiset;
import com.google.common.collect.Multisets;

import edu.uab.ccts.nlp.brat.BratConfiguration;
import edu.uab.ccts.nlp.umls.tools.CleanUtils;
import edu.uab.ccts.nlp.umls.tools.UMLSTools;

import org.apache.ctakes.typesystem.type.refsem.OntologyConcept;
import org.apache.ctakes.typesystem.type.refsem.UmlsConcept;
import org.apache.ctakes.typesystem.type.relation.BinaryTextRelation;
import org.apache.ctakes.typesystem.type.textsem.IdentifiedAnnotation;
import org.apache.uima.analysis_engine.AnalysisEngineProcessException;
import org.apache.uima.cas.FeatureStructure;
import org.apache.uima.fit.util.JCasUtil;
import org.apache.uima.jcas.JCas;
import org.apache.uima.jcas.cas.FSArray;
import org.apache.uima.jcas.tcas.Annotation;
import org.cleartk.util.ViewUriUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import brat.type.DiscontinousBratAnnotation;


public class AnnotatorStatistics implements Serializable {

	private static final Logger LOG  = LoggerFactory.getLogger(AnnotatorStatistics.class);
	public static final String ALL_ANNOTATORS = "ALL_ANNOTATORS";
	private static final long serialVersionUID = 1L;
	BratConfiguration bratconfig = null;

	//Contains key annotator name, value Hashtable with key discontinous
	//text and value HashMultiset containing elements that are strings
	//of ordered, comma separated CUIs
	private static Hashtable<String,Hashtable<String,HashMultiset<String>>> anno_results = null;
	private static Hashtable<String,String> map_type_hash = null; //Key docname+T+id, value = CUIs string (comma separated)
	private static Hashtable<String,String> text_type_hash = null; //Key annotator_name+doc_name+entity_id - Value = text
	private static Hashtable<String,String> bratDBAOffsets = null;
	private static Hashtable<String,Hashtable<DiscontinousBratAnnotation,Set<String>>> brat_ctakes_failed_cuis;
	private static Hashtable<String,Hashtable<DiscontinousBratAnnotation,Set<String>>> brat_ctakes_matched_cuis;
	private static HashSet<String> wrong_vocabulary_cuis = null;
	static Hashtable<String,Hashtable<String,Hashtable<String,String>>> exact_results
	= new Hashtable<String,Hashtable<String,Hashtable<String,String>>>();
	static Hashtable<String,Hashtable<String,Hashtable<String,String>>> related_cui_results
	= new Hashtable<String,Hashtable<String,Hashtable<String,String>>>();
	static Hashtable<String,Hashtable<String,Hashtable<String,Set<String>>>> ancestor_related_cui_results
	= new Hashtable<String,Hashtable<String,Hashtable<String,Set<String>>>>();
	static Hashtable<String,Hashtable<String,Hashtable<String,Set<String>>>> ancestor_exact_cui_results
	= new Hashtable<String,Hashtable<String,Hashtable<String,Set<String>>>>();
	private static Hashtable<String,String> datasetHash = new Hashtable<String,String>();//Key filename, String dataset

	public Hashtable<String, Hashtable<String, HashMultiset<String>>> getAnnotatorStats() {
		return anno_results;
	}

	public AnnotatorStatistics(){
		anno_results = new Hashtable<String,Hashtable<String,HashMultiset<String>>>();
		Hashtable<String,HashMultiset<String>> all = new Hashtable<String,HashMultiset<String>>();
		anno_results.put(ALL_ANNOTATORS, all);
		map_type_hash = new Hashtable<String,String>();
		text_type_hash = new Hashtable<String,String>(); 
		bratDBAOffsets = new Hashtable<String,String>(); //Key annotator_name+doc_name+entity_id : Value OFfsets
		wrong_vocabulary_cuis = new HashSet<String>();
		brat_ctakes_failed_cuis = new Hashtable<String,Hashtable<DiscontinousBratAnnotation,Set<String>>>(); //Key document_name, Value = Hashtable with key DiscontinousBratAnnotation, Values Set of failed to match CUIS
		brat_ctakes_matched_cuis = new Hashtable<String,Hashtable<DiscontinousBratAnnotation,Set<String>>>(); //Key document_name, Value = Hashtable with key DiscontinousBratAnnotation, Values Set of matching CUIS
		//Exact Results contains key annotator name, value Hashtable with
		//key document_id and value Hashtable with key entity 
		//identifier (Txx) and value comma separated CUIs
		exact_results = new Hashtable<String,Hashtable<String,Hashtable<String,String>>>();
		related_cui_results = new Hashtable<String,Hashtable<String,Hashtable<String,String>>>();
		ancestor_related_cui_results = new Hashtable<String,Hashtable<String,Hashtable<String,Set<String>>>>();
		ancestor_exact_cui_results = new Hashtable<String,Hashtable<String,Hashtable<String,Set<String>>>>();
	}


	/**
	 */
	public void addCtakesCUIs(JCas annview, JCas ctakesview) throws AnalysisEngineProcessException {
		Collection<DiscontinousBratAnnotation> brats = JCasUtil.select(annview, DiscontinousBratAnnotation.class);
		Hashtable<DiscontinousBratAnnotation,Set<String>> failed_matches = new Hashtable<DiscontinousBratAnnotation,Set<String>>();
		Hashtable<DiscontinousBratAnnotation,Set<String>> passed_matches = new Hashtable<DiscontinousBratAnnotation,Set<String>>();
		String docname = ViewUriUtil.getURI(annview).toString();
		for(DiscontinousBratAnnotation dba : brats) {
			docname = dba.getDocName();
			TreeSet<String> bratcuis =  new TreeSet<String>();
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
						if(bratcuis.contains(code)) { 
							addCuiMatchOrMismatch(passed_matches, dba, code);
						} else {
							addCuiMatchOrMismatch(failed_matches, dba, code);
							System.out.println(ctakesview.getDocumentText().substring(ia.getBegin(), ia.getEnd())+" with "+code+" does not match");
						}
					} else {
						String ucode = ontologyConcept.getCode();
						if(ucode.startsWith("C")) {
							if(bratcuis.contains(ucode)) { 
								addCuiMatchOrMismatch(passed_matches, dba, ucode);
							} else {
								addCuiMatchOrMismatch(failed_matches, dba, ucode);
								System.out.println(ia.getCoveredText()+" with "+ucode+" does not match");
							}
						}
					}
				}
			}	
		}
		brat_ctakes_failed_cuis.put(docname, failed_matches);
		brat_ctakes_matched_cuis.put(docname, passed_matches);
	}

	private void addCuiMatchOrMismatch(Hashtable<DiscontinousBratAnnotation, Set<String>> matches,
			DiscontinousBratAnnotation dba, String code) {
		Set<String> fcuis;
		if(matches.get(dba)==null) {
			fcuis = new TreeSet<String>();
			matches.put(dba, fcuis);
		}
		fcuis = matches.get(dba);
		fcuis.add(code);
	}


	public void add(Collection<DiscontinousBratAnnotation> dbas,
			Collection<BinaryTextRelation> rels, String filepath){
		
		//Only add in Disease Annotations
		for(DiscontinousBratAnnotation dba : dbas) {

			datasetHash.put(dba.getDocName(), filepath);
			if(dba.getTypeID()!=0) continue; //Only disease get processed
			//System.out.print("Processing ("+dba.getBegin()+"-"+dba.getEnd()+")"+dba.getDiscontinousText()+" from "+
			//dba.getDocName()+" annotated by "+dba.getAnnotatorName()+" ");
			assert(dba.getDocName()!=null);
			assert(dba.getAnnotatorName()!=null);
			assert(dba.getDocName().length()>4);
			String annotator_name = dba.getAnnotatorName();
			String text_key = dba.getDiscontinousText();
			if(dba.getIsNovelEntity()==true) {
				//System.out.println("Skipping "+dba.getAnnotatorName()+"--"+
				//dba.getDocName()+"--T"+dba.getId()+"---"+dba.getCoveredText());
				continue;
			}
			String allcuis = getCUIs(dba);
			//System.out.println("with allcuis of"+allcuis);
			//Populate anno_results to track which mentions associate with which CUIs
			buildAnnotationHash(annotator_name, text_key, allcuis,anno_results);
			map_type_hash.put(dba.getDocName()+"T"+dba.getId(), allcuis);
			text_type_hash.put(annotator_name+":"+dba.getDocName()+":T"+dba.getId(),text_key);
			bratDBAOffsets.put(annotator_name+":"+dba.getDocName()+":T"+dba.getId(),getOffsets(dba));
			Set<String> diseaseLeafCuis = buildExactResults(dba,allcuis);
			ancestor_exact_cui_results = buildAncestorResults(dba,diseaseLeafCuis,ancestor_exact_cui_results);
			String relatedcuis = getRelatedCUIs(dba,rels);
			Set<String> leafcuis= buildRelatedResults(dba,allcuis,relatedcuis);
			ancestor_related_cui_results = buildAncestorResults(dba,leafcuis,ancestor_related_cui_results);
		}
	}

	
	/*
	  Related Results contains key annotator name, value Hashtable with
	  key document_id and value Hashtable with key entity 
	  identifier (Txx) and value comma separated CUIs related to the disease and all related CUIs
	  
	  @returns String of comma-separated related CUIS, both disease CUIs and related CUIs (body location,negation, etc...)
	 */
	private Set<String> buildRelatedResults(DiscontinousBratAnnotation dba, String allcuis,
		String related_cuis){
		Hashtable<String,Hashtable<String,String>> docid_hash = related_cui_results.get(dba.getAnnotatorName());
		if(docid_hash==null) docid_hash = new Hashtable<String,Hashtable<String,String>>();
		Hashtable<String,String> entid_hash = docid_hash.get(dba.getDocName());
		if(entid_hash==null) entid_hash = new Hashtable<String,String>();
		StringBuilder mergedstring = new StringBuilder();
		HashSet<String> nodupsDisease = new HashSet<String>(Arrays.asList(allcuis.split(",")));
		if(related_cuis!=null) {
			HashSet<String> nodupsNotDisease = new HashSet<String>(Arrays.asList(related_cuis.split(",")));
			nodupsDisease.addAll(nodupsNotDisease);
		}
		List<String> mergedList = new ArrayList<String>(nodupsDisease);
		Collections.sort(mergedList);
		for(int i=0;i<mergedList.size();i++){ 
			if(i==mergedList.size()-1) {
				mergedstring.append(mergedList.get(i)); 
			} else { mergedstring.append(mergedList.get(i)); mergedstring.append(","); }
		}
		entid_hash.put("T"+dba.getId(), mergedstring.toString());
		docid_hash.put(dba.getDocName(), entid_hash);
		related_cui_results.put(dba.getAnnotatorName(), docid_hash);
		return nodupsDisease;
	}


	//Exact Results contains key annotator name, value Hashtable with
	//key document_id and value Hashtable with key entity 
	//identifier (Txx) and value comma separated CUIs
	private Set<String>  buildExactResults(DiscontinousBratAnnotation dba, String allcuis){
		Hashtable<String,Hashtable<String,String>> docid_hash = exact_results.get(dba.getAnnotatorName());
		if(docid_hash==null) docid_hash = new Hashtable<String,Hashtable<String,String>>();
		Hashtable<String,String> entid_hash = docid_hash.get(dba.getDocName());
		if(entid_hash==null) entid_hash = new Hashtable<String,String>();
		entid_hash.put("T"+dba.getId(), allcuis);
		docid_hash.put(dba.getDocName(), entid_hash);
		exact_results.put(dba.getAnnotatorName(), docid_hash);
		HashSet<String> nodupsDisease = new HashSet<String>(Arrays.asList(allcuis.split(",")));
		return nodupsDisease;
	}


	//Ancestor Results contains key annotator name, value Hashtable with
	//key document_id and value Hashtable with key entity 
	//identifier (Txx) and value comma separated CUIs
	private Hashtable<String,Hashtable<String,Hashtable<String,Set<String>>>> 
	buildAncestorResults(DiscontinousBratAnnotation dba, Set<String> leafCuis,
			Hashtable<String,Hashtable<String,Hashtable<String,Set<String>>>> rstore
			){
		Hashtable<String,Hashtable<String,Set<String>>> docid_hash = rstore.get(dba.getAnnotatorName());
		if(docid_hash==null) docid_hash = new Hashtable<String,Hashtable<String,Set<String>>>();
		Hashtable<String,Set<String>> entid_hash = docid_hash.get(dba.getDocName());
		if(entid_hash==null) entid_hash = new Hashtable<String,Set<String>>();
		//Convert leaf to ancestor CUIs
		HashSet<String> ancestorAuis = new HashSet<String>();
		for(String cui : leafCuis) {
			ancestorAuis.addAll(getCuiSnomedAncestors(cui,UMLSTools.getUmlsConnectionString()));
		}
		Set<String> ancestorCuis = new HashSet<String>();
		if(ancestorAuis.isEmpty()) {
			System.err.println("No AUIs in "+leafCuis);
		} else ancestorCuis = getCuisFromSnomedAuis(ancestorAuis,UMLSTools.getUmlsConnectionString());
		ancestorCuis.addAll(leafCuis);
		entid_hash.put("T"+dba.getId(), ancestorCuis);
		docid_hash.put(dba.getDocName(), entid_hash);
		rstore.put(dba.getAnnotatorName(), docid_hash);
		return rstore;
	}


	/**
	 * Construct annotation hash to track which mentions track with which CUIs 
	 * @param annotator_name
	 * @param text_key
	 * @param allcuis
	 * @param bighash
			Contains key annotator name (or ALL_ANNOTATORS), value Hashtable with key discontinous
			text and value HashMultiset containing elements that are strings
			of ordered, comma separated CUIs
	 */
	private void buildAnnotationHash(String annotator_name, String text_key,
			String allcuis, 
			Hashtable<String,Hashtable<String,HashMultiset<String>>>  bighash){
		Hashtable<String,HashMultiset<String>> alltexthash = 
				bighash.get(ALL_ANNOTATORS); //Always there
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
	public static String getCUIs(DiscontinousBratAnnotation dba) {
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
	
	
	public static String getOffsets(DiscontinousBratAnnotation dba) {
		StringBuilder sb = new StringBuilder();
		FSArray thespans = dba.getSpans();
		if(thespans!=null) {
			for(int i=0;i<thespans.size();i++){
				Annotation anno = (Annotation) thespans.get(i);
				if(i==thespans.size()-1) sb.append(anno.getBegin()+"-"+anno.getEnd());
				else sb.append(anno.getBegin()+"-"+anno.getEnd()+";");
			}
		} else {
			System.out.println("Should NOT HAPPEN!");
			sb.append(dba.getBegin()+"-"+dba.getEnd());
		}	
		return sb.toString();
	}
	
	
	public static String getRelatedCUIs(DiscontinousBratAnnotation dba,
			Collection<BinaryTextRelation> rels
		) {
		String related_cuis = null;
		//System.out.println("Looking at entity:"+dba.getDiscontinousText()+" of type:"+dba.getTypeID());
		for(BinaryTextRelation btr : rels) {
			if(btr.getArg1().getArgument()==dba) {
				DiscontinousBratAnnotation object = (DiscontinousBratAnnotation)btr.getArg2().getArgument();
				//System.out.println("Found relation to:"+object.getDiscontinousText()+" at "+object.getBegin());
				FSArray oa = object.getOntologyConceptArr();
				if(oa==null || oa.size()==0) {
					LOG.warn("Failed to find needed related ontology concept in object :"+object.getDiscontinousText()+" at "+object.getBegin());
					continue;
				}
				if(related_cuis==null) related_cuis = object.getOntologyConceptArr(0).getCode();
				else related_cuis += ","+object.getOntologyConceptArr(0).getCode();
				//System.out.println("Found relation to:"+object.getDiscontinousText()+" with code "+related_cuis);
			}
		}
		return related_cuis;
	}


	public void print(){
		printGlobalTypeCounts(map_type_hash);
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
				if(CleanUtils.isWellFormedCUI(cs[0])) { doublestdist.add(cuis); }
				else{ LOG.warn(cs[0]+" is not a well-formed CUI");}
				//Iterate through all the cuis for this mapping
				for(int i=0;i<cs.length;i++){
					String cui = cs[i].trim();
					if(!CleanUtils.isWellFormedCUI(cui)) {
						LOG.warn(mapentry.getKey()+" has badly formed cui:"+cui);
						bad_form_count++;
						continue;
					}
					total_cui_count++;
					HashMultiset<String> exist = dist.get(cui);
					if(exist==null){
						exist = HashMultiset.create();
						String[] cuiinfo = UMLSTools.fetchCUIInfo(cui, UMLSTools.getUmlsConnectionString());
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
						bname = UMLSTools.fetchBestConceptName(s, UMLSTools.getUmlsConnectionString());
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
	public double calculateAgreement(String agreementType){
		double agreements = 0.0, disagreements = 0.0;
		Hashtable<String,Hashtable<String,Hashtable<String,String>>> theResults;
		if(agreementType.equalsIgnoreCase("exact")) {
			theResults = exact_results;
		}
		else if(agreementType.equalsIgnoreCase("related")) {
			theResults=related_cui_results;
		} else {
			System.err.println("Agreement type not exact or related");
			return 0.0;
		}
		Object[] annotators = theResults.keySet().toArray();
		System.out.println("Reference annotator:"+annotators[0]); System.out.flush();

		Hashtable<String,Hashtable<String,String>> reftable = 
				theResults.get(annotators[0]);
		Set<String> refdocs = reftable.keySet();
		for(String rdoc : refdocs){
			Hashtable<String,String> refents = reftable.get(rdoc);
			Hashtable<String,String> testents = 
					theResults.get(annotators[1]).get(rdoc);
			if(testents==null) {
				System.err.println(annotators[1]+" did not complete "+rdoc);
				continue;
			}
			//Iterate through each entity in the document
			//Check to see how many documents are missing
			for(Iterator<String> entiter = refents.keySet().iterator(); entiter.hasNext();){
				String refentity = entiter.next();
				String refcuis = refents.get(refentity);
				String testcuis = testents.get(refentity);
				if(refcuis==null && testcuis==null) continue;
				if(refcuis!=null && testcuis==null) {
					System.err.println(annotators[1]+" has null value for "+refentity+" in "+rdoc);
					continue;
				}
				if(refcuis==null && testcuis!=null) {
					System.err.println(annotators[0]+" has null value for "+refentity+" in "+rdoc);
					continue;
				}

				String refnames = UMLSTools.fetchBestConceptName(refcuis, UMLSTools.getUmlsConnectionString());
				String testnames = UMLSTools.fetchBestConceptName(testcuis, UMLSTools.getUmlsConnectionString());
				String typekey = annotators[0]+":"+rdoc+":"+refentity;
				String distext = text_type_hash.get(typekey);
				String filepath = datasetHash.get(rdoc);
				String printline = "";
				String myoffsets = bratDBAOffsets.get(typekey);

				if(refcuis.trim().equalsIgnoreCase(testcuis.trim())) {
					agreements++;
					printline=printline+"AGREE\t";
				} else {
					disagreements++;
					printline=printline+"DISAGREE\t";
				}
				printline=printline+filepath+"\t"+rdoc+"\t"+myoffsets+"\t"+
						distext+"\t"+refentity+"\t"+annotators[0]+"\t"+refcuis+"\t"
						+refnames+"\t"+annotators[1]+"\t"+testcuis+"\t"+testnames;
				System.out.println(printline);
			}
		}
		System.out.println(disagreements+" disagreements");
		return agreements/(agreements+disagreements);
	}
	
	
	public double calculateAncestorAgreement(String agreementType){
		double proportional_agreement = 0.0;
		int agreement_comparison_count=0;
		Hashtable<String,Hashtable<String,Hashtable<String,Set<String>>>> theResults;
		
		if(agreementType.equalsIgnoreCase("exact")) {
			theResults = ancestor_exact_cui_results;
		}
		else if(agreementType.equalsIgnoreCase("related")) {
			theResults=ancestor_related_cui_results;
		} else {
			System.err.println("Agreement type not specified");
			return 0.0;
		}
		Object[] annotators = theResults.keySet().toArray();
		System.out.println("Reference annotator:"+annotators[0]); System.out.flush();
		Hashtable<String,Hashtable<String,Set<String>>> reftable = 
				theResults.get(annotators[0]);
		Set<String> refdocs = reftable.keySet();
		for(String rdoc : refdocs){
			Hashtable<String,Set<String>> refents = reftable.get(rdoc);
			Hashtable<String,Set<String>> testents = 
					theResults.get(annotators[1]).get(rdoc);
			if(testents==null) {
				System.err.println(annotators[1]+" did not complete "+rdoc);
				continue;
			}
			//Iterate through each entity in the document
			//Check to see how many documents are missing
			for(Iterator<String> entiter = refents.keySet().iterator(); entiter.hasNext();){
				String refentity = entiter.next();
				Set<String> refcuis = refents.get(refentity);
				Set<String> testcuis = testents.get(refentity);
				if(refcuis==null && testcuis==null) continue;
				if(refcuis!=null && testcuis==null) {
					System.err.println(annotators[1]+" has null value for "+refentity+" in "+rdoc);
					continue;
				}
				if(refcuis==null && testcuis!=null) {
					System.err.println(annotators[0]+" has null value for "+refentity+" in "+rdoc);
					continue;
				}

				String typekey = annotators[0]+":"+rdoc+":"+refentity;
				String distext = text_type_hash.get(typekey);
				String filepath = datasetHash.get(rdoc);
				String printline = "";
				String myoffsets = bratDBAOffsets.get(typekey);
				String refnames = "ConceptNameRetrievalFailure";
				String error_string=filepath+"\t"+myoffsets+"\t"+distext+"\t"+refentity;

				try {
					refnames = UMLSTools.fetchBestConceptName(refcuis, UMLSTools.getUmlsConnectionString());
				} catch (Exception e) { 
					System.err.println("No best name for ref:"+error_string);
					e.printStackTrace(); 
				}
				String testnames = "ConceptNameRetrievalFailure";
				try {
					testnames = UMLSTools.fetchBestConceptName(testcuis, UMLSTools.getUmlsConnectionString());
				} catch (Exception e) { 
					System.err.println("No best name for test of ref:"+error_string);
					e.printStackTrace(); 
				}
			
				//Compute Intersection between test and reference
				if(refcuis.size()==0) {
					System.err.println("No CUIS for ref "+error_string);
					continue;
				}
				agreement_comparison_count++;
				Set<String> union = new HashSet<String>(refcuis);
				union.addAll(testcuis);
				double allAncestorCount=(double) union.size();
				Set<String> intersect = new HashSet<String>(refcuis);
				printline="INITIAL SET SIZE: REF="+refcuis.size()+" TEST="+testcuis.size();
				intersect.retainAll(testcuis);
				//double agreement = (double)intersect.size()/(double)refcuis.size();
				double agreement = (double)intersect.size()/allAncestorCount;
				proportional_agreement+=agreement;
				printline+="AGREE PROPORTION ("+intersect.size()+"/"+refcuis.size()+")("+agreement+")\t";
				printline=printline+filepath+"\t"+rdoc+"\t"+myoffsets+"\t"+
						distext+"\t"+refentity+"\t"+annotators[0]+"\t"+refcuis+"\t"
						+refnames+"\t"+annotators[1]+"\t"+testcuis+"\t"+testnames;
				System.out.println(printline);
			}
		}
		return proportional_agreement/agreement_comparison_count;
		
	}
	
	
	/**
	 * For the input CUI, get a set of ancestral UMLS SNOMED Atomic Elements from MRHIER
	 * @param cui
	 * @return
	 */
	public Set<String> getCuiSnomedAncestors(String cui, String jdbcconnect) {
		Set<String> allauis = new HashSet<String>();
		if(cui.trim().equalsIgnoreCase("CUI-less")) {
			allauis.add("A0000000"); //Not used in UMLS 2016AB, probably not used at all
			return allauis;
		}
		String query ="SELECT PTR FROM MRHIER WHERE CUI=?";
		try (Connection con = DriverManager.getConnection(jdbcconnect);
				PreparedStatement ps = con.prepareStatement(query);
				){
			ps.setString(1, cui);
			try(ResultSet rs = ps.executeQuery()){
				while(rs.next()) {
					String ptr = rs.getString(1);
					String[] ancestors = ptr.split("\\.");
					allauis.addAll(Arrays.asList(ancestors));
				}
			} catch (Exception rse) { rse.printStackTrace(); }
		} catch (Exception e) { e.printStackTrace(); }
		if(allauis.isEmpty()) {
			query ="SELECT AUI FROM MRCONSO WHERE CUI=? AND SAB LIKE 'SNOMEDCT_US_%'";
			try (Connection con = DriverManager.getConnection(jdbcconnect);
					PreparedStatement ps = con.prepareStatement(query);
					){
				ps.setString(1, cui);
				try(ResultSet rs = ps.executeQuery()){
					while(rs.next()) {
						String aui = rs.getString(1);
						allauis.add(aui);
						break;
					}
				} catch (Exception rse) { rse.printStackTrace(); }
			} catch (Exception e) { e.printStackTrace(); }
		}
		return allauis;
	}
	
	/**
	 * For the input set of AUIS, get a set of CUIs corresponding to the set
	 * @param cui
	 * @return
	 */
	public Set<String> getCuisFromSnomedAuis(Set<String> auis, String jdbcconnect) {
		Set<String> allcuis = new HashSet<String>();
		if(auis.size()==1 && auis.iterator().next().equalsIgnoreCase("A0000000")) {
			allcuis.add("Cui-less");
			return allcuis;
		}
		String query ="SELECT DISTINCT CUI FROM MRCONSO WHERE AUI IN";
		StringBuilder sb = new StringBuilder("(");
		for(Iterator<String> it = auis.iterator();it.hasNext();) {
			sb.append("\'");
			sb.append(it.next());
			sb.append("\'");
			if(it.hasNext())sb.append(",");
		}
		sb.append(")");
		query+=sb.toString();
		try (Connection con = DriverManager.getConnection(jdbcconnect);
			 PreparedStatement ps = con.prepareStatement(query);
			 ResultSet rs = ps.executeQuery()	
				) {
			while(rs.next()) {
				String cui = rs.getString(1);
				allcuis.add(cui);
			}
			
		} catch (Exception e) { e.printStackTrace();}
		return allcuis;
	}
	
	
	
	private String getDisagreementType(String refcuis, String testcuis) {
		String disagreeType="DISAGREE";
		
		return disagreeType;
		
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
				bname = UMLSTools.fetchBestConceptName(s, UMLSTools.getUmlsConnectionString());
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
	
	
	public void printPreCoordinated(){
		Hashtable<String,HashMultiset<String>> cuimentions = new Hashtable<String,HashMultiset<String>>();
		anno_results.remove(ALL_ANNOTATORS);
		for(Hashtable<String,HashMultiset<String>> h : anno_results.values()) {
			for(String text : h.keySet()) {
				HashMultiset<String> cuiset = h.get(text);
				for(String commacuis : cuiset.elementSet()) {
					if(commacuis.indexOf(",")==-1) { //Pre-coordinated
						HashMultiset<String> cuitest = cuimentions.get(commacuis);
						if(cuitest==null) cuitest = HashMultiset.create();
						cuitest.add(text);
						cuimentions.put(commacuis,cuitest);
					}
				}
			}
		}
		for(String cui : cuimentions.keySet()) {
			Set<Multiset.Entry<String>> histogram = cuimentions.get(cui).entrySet();
			Integer total_count = 0;
			StringBuffer allmentions = new StringBuffer(); allmentions.append("");
			for(Multiset.Entry<String> mention : histogram){
				total_count += mention.getCount();
				allmentions.append(mention.getElement()+"\t"+mention.getCount()+"\t");
			}
			System.out.println(cui+"\t"+total_count+"\t"+allmentions.toString());
		}
	}


	public String getDiscrepancies() throws Exception { 
		return getDiscrepancies(anno_results) ; 
	}

}
