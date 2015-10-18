package edu.uab.ccts.nlp.uima.annotator.brat;

import java.io.File;
import java.util.Comparator;
import java.util.Hashtable;
import java.util.TreeMap;

import org.apache.uima.analysis_engine.AnalysisEngineDescription;
import org.apache.uima.analysis_engine.AnalysisEngineProcessException;
import org.apache.uima.cas.CASException;
import org.apache.uima.cas.CASRuntimeException;
import org.apache.uima.fit.component.JCasAnnotator_ImplBase;
import org.apache.uima.fit.factory.AnalysisEngineFactory;
import org.apache.uima.jcas.JCas;
import org.apache.uima.jcas.cas.FSArray;
import org.apache.uima.jcas.tcas.Annotation;
import org.apache.uima.resource.ResourceInitializationException;
import org.apache.uima.util.Level;
import org.apache.ctakes.typesystem.type.refsem.OntologyConcept;
import org.apache.ctakes.typesystem.type.relation.BinaryTextRelation;
import org.apache.ctakes.typesystem.type.relation.RelationArgument;
import org.apache.ctakes.typesystem.type.structured.DocumentID;
import org.cleartk.util.ViewUriUtil;

import edu.uab.ccts.nlp.brat.BratConfiguration;
import edu.uab.ccts.nlp.brat.BratConfigurationImpl;
import edu.uab.ccts.nlp.brat.BratConstants;
import edu.uab.ccts.nlp.brat.BratEntity;
import edu.uab.ccts.nlp.shared_task.SemEval2015Constants;
import brat.type.DiscontinousBratAnnotation;

public class BratParserAnnotator extends JCasAnnotator_ImplBase {

	BratConfiguration bratconfig;

	//These are re-initialized each time in process method
	TreeMap<String,String> bratKeyDict = null; //Key Txx (entity id), Value Entity Row
	Hashtable<String,DiscontinousBratAnnotation> uimaDiseaseDict  = null; //Key Txxx, Value brat diseases
	Hashtable<String,DiscontinousBratAnnotation> uimaNotDiseaseDict  = null; //Key Txxx, Value brat non-disease
	boolean verbose = true; 
	boolean print_problem_files_only = false;


	@Override
	/**
	 * All annotations should go into BratConstants.TEXT_VIEW
	 */
	public void process(JCas jcas) throws AnalysisEngineProcessException {
		//uimaKeyDict doesn't need to be sorted
		uimaDiseaseDict = new Hashtable<String,DiscontinousBratAnnotation>(); //Key Txxx, Value brat diseases
		uimaNotDiseaseDict = new Hashtable<String,DiscontinousBratAnnotation>(); //Key Txxx, Value brat diseases
		bratKeyDict = new TreeMap<String,String>(new Comparator<String>(){
			public int compare (String x, String y) {
				if(!x.substring(0, 1).equals(y.substring(0,1))) {
					return x.compareTo(y)*-1; //Reverse the order, want to put # annotations at end
				}
				return Integer.compare(Integer.parseInt(x.substring(1, x.length())),
						Integer.parseInt(y.substring(1, y.length())));
			}
		});

		int semeval_cuiless_count=0;
		int brat_cuiless_count=0;
		int brat_annotated_count=0;
		int unannotated_count=0; //Unannotated entities
		int extra_annotations=0; //Annotations that do not map to one of the CUI-less diseases
		int help_cui_count=0; //Annotated with un-parsable CUIs, need to be looked at
		String unannotated_summary="";
		String help_summary="";
		String extra_summary="";

		/**
		 * Parse our configuration file
		 */
		JCas configView = null, anView = null, textView = null, semevalPipeView = null;
		BratConfiguration bratconfig = null;
		try {
			anView = jcas.getView(BratConstants.ANN_VIEW);
			textView = jcas.getView(BratConstants.TEXT_VIEW);
			configView = jcas.getView(BratConstants.CONFIG_VIEW);
			bratconfig = new BratConfigurationImpl(configView.getDocumentText());
		} catch (CASException e) { e.printStackTrace();
		} catch (ResourceInitializationException e) { e.printStackTrace(); throw new AnalysisEngineProcessException(e);}

		//Computer annotator and document name from URI
		String[] pathbits = (ViewUriUtil.getURI(jcas)).toString().split(File.separator);
		String docname = pathbits[pathbits.length-1];
		DocumentID id = new DocumentID(anView);
		id.setDocumentID(docname);
		id.addToIndexes(); id.addToIndexes(textView);
		String datasetname = pathbits[pathbits.length-2];
		String annotator_name = pathbits[pathbits.length-2].split("_")[0];

		//System.out.println("Processing "+annotator_name+" "+docname);
		semeval_cuiless_count = getSemEvalCUIlessCount(jcas,
				semeval_cuiless_count, semevalPipeView);

		//Populate bratKeyDict
		if(anView.getDocumentText().length()!=0) {
			//Create hashed version of annotation file
			String[] lines = anView.getDocumentText().split(System.getProperty("line.separator"));
			for(String line : lines){
				String[] fields = line.split("\t");
				if(fields.length<2) {
					System.out.println(line+" has no tabs!");
					System.out.flush();
				}
				String key = fields[0].trim();
				String value = fields[1].trim();
				if(fields.length>2) {
					for(int i=2;i<fields.length;i++){
						value+= "\t"+fields[i].trim();
					}
				}
				bratKeyDict.put(key,value);
			}
		} else {
			System.err.println("No annotation view?!");
		}

		//System.out.println(bratKeyDict);


		//Get all our entities (diseases) first
		for(String key : bratKeyDict.keySet()){
			if(key.trim().startsWith("T")) {
				String[] tabfields = bratKeyDict.get(key).split("\t");
				//if(!tabfields[0].startsWith("Disease")) continue;
				DiscontinousBratAnnotation dba = new DiscontinousBratAnnotation(textView);
				dba.setAnnotatorName(annotator_name);
				dba.setDocName(docname);
				//System.out.println("KEY"+key); System.out.flush();
				dba.setId(Integer.parseInt(key.substring(1, key.length())));
				String text = tabfields[1];
				String[] entfields = tabfields[0].split(" ");
				BratEntity bratent = bratconfig.getEntityByName(entfields[0].trim());
				dba.setTypeID(bratent.getTypeId());
				if(tabfields[0].indexOf(";")==-1){
					dba.setBegin(Integer.parseInt(entfields[1].trim()));
					dba.setEnd(Integer.parseInt(entfields[2].trim()));
				} else {
					String[] span_fields = tabfields[0].split(" |;");
					FSArray thespans = new FSArray(textView,span_fields.length-1);
					for(int i=1;i<span_fields.length;i=i+2){
						Annotation span = new Annotation(textView);
						if(i==1) dba.setBegin(Integer.parseInt(span_fields[i]));
						span.setBegin(Integer.parseInt(span_fields[i]));
						int end = Integer.parseInt(span_fields[i+1]);
						span.setEnd(end);
						dba.setEnd(end);
						thespans.set(i/2,span);
					}
					dba.setSpans(thespans);
				}
				dba.setDiscontinousText(text);
				if(!tabfields[0].startsWith("Disease")) { 
					uimaNotDiseaseDict.put(key, dba);
				} else { uimaDiseaseDict.put(key,dba); }
			}
		}
		brat_cuiless_count=uimaDiseaseDict.size();
		//System.out.println(docname+" has uimaDiseaseDict size of "+uimaDiseaseDict.size()); //T15 is in there, T44, T13

		int last_pre_existing_disease_start = 0; //-1 indicates no more pre-existing diseases
		for(String key : bratKeyDict.keySet()){
			if(key.startsWith("R")){
				BinaryTextRelation btr = new BinaryTextRelation(textView);
				String[] tabfields = bratKeyDict.get(key).split("\t");
				String[] span_fields = tabfields[0].split(" |:");
				String reltype = span_fields[0];
				btr.setCategory(reltype);
				//if(reltype.indexOf(BratConstants.NER_TYPE.BODYLOCATION.getName())!=-1) {
				RelationArgument one = new RelationArgument(textView);
				one.setArgument(uimaDiseaseDict.get(span_fields[2]));
				btr.setArg1(one);
				RelationArgument two = new RelationArgument(textView);
				two.setArgument(uimaDiseaseDict.get(span_fields[4]));
				btr.setArg2(two);
				btr.addToIndexes(textView);
			} else if(key.trim().startsWith("T")) { 
				//Check to identify novel annotated stuff
				DiscontinousBratAnnotation dis = uimaDiseaseDict.get(key.trim());
				if(dis==null) { 
					//Not a disease
					//System.out.println("Got null in "+docname+":"+key.trim()+" size "+uimaKeyDict.size());
					continue; 
				} 
				int cur = Integer.parseInt(bratKeyDict.get(key).split("\t")[0].split(" ")[1]);
				this.getContext().getLogger().log(Level.FINEST,"cur:"+cur+" LastPreExist"+last_pre_existing_disease_start);
				if(last_pre_existing_disease_start>=0 && cur>=last_pre_existing_disease_start){
					last_pre_existing_disease_start=cur;
					dis.setIsNovelEntity(false);
					//System.out.println(docname+" -pre-exist- "+key+" -- "+cur);
				} else {
					dis.setIsNovelEntity(true);
					last_pre_existing_disease_start=-1;
					this.getContext().getLogger().log(Level.FINE,"Detected novel disease:"+dis.getId());
				}
			} else if(key.startsWith("#")) {
				String[] tabfields = bratKeyDict.get(key).split("\t");
				String[] span_fields = tabfields[0].split(" ");
				//Retrieve Entity
				DiscontinousBratAnnotation annotated = uimaDiseaseDict.get(span_fields[1]);
				if(annotated==null) {
					DiscontinousBratAnnotation notdisease = uimaNotDiseaseDict.get(span_fields[1]);
					if(notdisease==null) {
						extra_summary+="EXTRA-"+span_fields[1];
						extra_annotations++;
					}
					continue;
				}
				//Add CUIs
				String[] cuis = tabfields[1].split(" |,");
				assert(cuis.length>0);
				if(!(cuis[0].startsWith("C") && cuis.length<6)){
					help_cui_count++;
					help_summary+="HELP-"+(bratKeyDict.get(key))+uimaDiseaseDict.get(key);
					continue;
				}
				FSArray ontarray = new FSArray(textView,cuis.length);
				int k=0;
				if(cuis.length==0) { cuis = new String[1]; cuis[0]="MISSED_CUI"; }
				for(String cui : cuis) {
					//System.out.println("Dealing with cui "+cui+" at k:"+k);
					OntologyConcept oc = new OntologyConcept(textView);
					oc.setCode(cui);
					ontarray.set(k, oc);
					oc.addToIndexes(textView);
					k++;
				}
				ontarray.addToIndexes(textView);
				if(ontarray!=null) annotated.setOntologyConceptArr(ontarray);
				annotated.addToIndexes(textView);
				uimaDiseaseDict.remove(span_fields[1]);
				brat_annotated_count++;
			}
		}
		unannotated_count=uimaDiseaseDict.size();
		if(unannotated_count>0) {
			for(String s : uimaDiseaseDict.keySet()) {
				unannotated_summary += s+":"+bratKeyDict.get(s).split("\t")[1]+"\t";
			}
			if(unannotated_count>=brat_cuiless_count) unannotated_summary = "NOT_ANNOTATED";
			if(unannotated_count>=semeval_cuiless_count) unannotated_summary = "NOT_ANNOTATED";
		}
		//System.out.println("Unannotated_count:"+unannotated_count+" Extra_annotations:"+extra_annotations);
		printTableLine(semeval_cuiless_count, brat_cuiless_count,
				brat_annotated_count, unannotated_count, extra_annotations,
				help_cui_count, docname, annotator_name, unannotated_summary,
				extra_summary,help_summary,datasetname);
	}

	private int getSemEvalCUIlessCount(JCas jcas, int semeval_cuiless_count,
			JCas semevalPipeView) {
		try {
			semeval_cuiless_count=0;
			semevalPipeView = jcas.getView(SemEval2015Constants.PIPED_VIEW);
			String[] pipelines = semevalPipeView.getDocumentText().split(System.getProperty("line.separator"));
			String[] prev_chunks = null;
			for(String line : pipelines) {
				String[] chunks = line.split("\\|");
				if(chunks[2].equals("CUI-less")){
					if(prev_chunks!=null && (
							(!prev_chunks[2].equals("CUI-less")) &&
							(!prev_chunks[1].equals(chunks[1])))) {
					}
					semeval_cuiless_count++;
				}
				prev_chunks = chunks;
			}
		} catch (CASRuntimeException e) { 
			this.getContext().getLogger().log(Level.INFO,"No PIPE_VIEW found");
		} catch (Exception e) { 
			if(semevalPipeView.getDocumentText()==null){
				System.out.println("Could not find original semeval input file");
			} 
		}
		return semeval_cuiless_count;
	}

	private void printTableLine(int semeval_cuiless_count,
			int brat_cuiless_count, int brat_annotated_count,
			int unannotated_count, int extra_annotations, int help_cui_count,
			String docname, String annotator_name, String unannotated_summary,
			String extra_summary, String help_summary, String datasetname) {
		String tableline = (annotator_name+"\t"+semeval_cuiless_count+"\t"+
				brat_cuiless_count+"\t"+brat_annotated_count+"\t"+unannotated_count+
				"\t"+extra_annotations+"\t"+help_cui_count+"\t"+datasetname+"\t"+docname+
				"\t"+unannotated_summary+"\t"+extra_summary+"\t"+help_summary+"\n");
		if(verbose) {
			if(print_problem_files_only) {
				if( (unannotated_count>0) || extra_annotations>0) {
					System.out.print(tableline);
					return;
				}
			} else System.out.print(tableline);
		}
	}

	public static AnalysisEngineDescription getDescription() throws ResourceInitializationException {
		return AnalysisEngineFactory.createEngineDescription(BratParserAnnotator.class);
	}

	public static boolean isWellFormedCUI(String cui) {
		if(cui.startsWith("C") && cui.length()==8) return true;
		return false;
	}

}
