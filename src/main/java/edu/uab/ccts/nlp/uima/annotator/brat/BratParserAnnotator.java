package edu.uab.ccts.nlp.uima.annotator.brat;

import java.io.File;
import java.util.Hashtable;

import org.apache.uima.analysis_engine.AnalysisEngineDescription;
import org.apache.uima.analysis_engine.AnalysisEngineProcessException;
import org.apache.uima.cas.CASException;
import org.apache.uima.fit.component.JCasAnnotator_ImplBase;
import org.apache.uima.fit.factory.AnalysisEngineFactory;
import org.apache.uima.jcas.JCas;
import org.apache.uima.jcas.cas.FSArray;
import org.apache.uima.jcas.tcas.Annotation;
import org.apache.uima.resource.ResourceInitializationException;
import org.apache.ctakes.typesystem.type.refsem.OntologyConcept;
import org.apache.ctakes.typesystem.type.relation.BinaryTextRelation;
import org.apache.ctakes.typesystem.type.relation.RelationArgument;
import org.cleartk.util.ViewUriUtil;

import edu.uab.ccts.nlp.brat.BratConfiguration;
import edu.uab.ccts.nlp.brat.BratConfigurationImpl;
import edu.uab.ccts.nlp.brat.BratConstants;
import edu.uab.ccts.nlp.brat.BratEntity;
import edu.uab.ccts.nlp.uima.shared_task.SemEval2015Constants;
import brat.type.DiscontinousBratAnnotation;

public class BratParserAnnotator extends JCasAnnotator_ImplBase {

	BratConfiguration bratconfig;

	//These are re-initialized each time in process method
	Hashtable<String,String> bratKeyDict = null;
	Hashtable<String,DiscontinousBratAnnotation> uimaKeyDict  = null;

	//public void initialize(UimaContext context) throws ResourceInitializationException{ }


	@Override
	public void process(JCas jcas) throws AnalysisEngineProcessException {

		uimaKeyDict = new Hashtable<String,DiscontinousBratAnnotation>();
		bratKeyDict = new Hashtable<String,String>();
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
		JCas configView = null, annView = null, textView = null, semevalPipeView = null;
		BratConfiguration bratconfig = null;
		try {
			annView = jcas.getView(BratConstants.ANN_VIEW);
			//System.out.println("Got annView of size"+annView.getDocumentText().length());
			textView = jcas.getView(BratConstants.TEXT_VIEW);
			configView = jcas.getView(BratConstants.CONFIG_VIEW);
			semevalPipeView = jcas.getView(SemEval2015Constants.SEMEVAL_PIPED_VIEW);
			//System.out.println("Got semevalpipe of size"+semevalPipeView.getDocumentText().length());
			bratconfig = new BratConfigurationImpl(configView.getDocumentText());
		} catch (CASException e) { e.printStackTrace();
		} catch (ResourceInitializationException e) { e.printStackTrace(); throw new AnalysisEngineProcessException(e);}

		//Computer annotator and document name from URI
		String[] pathbits = (ViewUriUtil.getURI(jcas)).toString().split(File.separator);
		String docname = pathbits[pathbits.length-1];
		String annotator_name = pathbits[pathbits.length-2];

		if(semevalPipeView.getDocumentText()==null){
			System.out.println("Could not find original semeval input file");
			semeval_cuiless_count=-1;
		} else {
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
		}


		if(annView.getDocumentText().length()!=0) {
			//Create hashed version of annotation file
			String[] lines = annView.getDocumentText().split(System.getProperty("line.separator"));
			for(String line : lines){
				String[] fields = line.split("\t");
				String key = fields[0].trim();
				String value = fields[1].trim();
				if(fields.length>2) {
					for(int i=2;i<fields.length;i++){
						value+= "\t"+fields[i].trim();
					}
				}
				bratKeyDict.put(key,value);
			}
		}


		//Get all our entities first
		for(String key : bratKeyDict.keySet()){
			if(key.trim().startsWith("T")) {
				String[] tabfields = bratKeyDict.get(key).split("\t");
				if(!tabfields[0].startsWith("Disease")) continue;
				DiscontinousBratAnnotation dba = new DiscontinousBratAnnotation(annView);
				dba.setAnnotatorName(annotator_name);
				dba.setDocName(docname);
				String text = tabfields[1];
				String[] entfields = tabfields[0].split(" ");
				BratEntity bratent = bratconfig.getEntityByName(entfields[0].trim());
				dba.setTypeID(bratent.getTypeId());
				if(tabfields[0].indexOf(";")==-1){
					dba.setBegin(Integer.parseInt(entfields[1].trim()));
					dba.setEnd(Integer.parseInt(entfields[2].trim()));
				} else {
					String[] span_fields = tabfields[0].split(" |;");
					FSArray thespans = new FSArray(annView,span_fields.length-1);
					for(int i=1;i<span_fields.length;i=i+2){
						Annotation span = new Annotation(annView);
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
				uimaKeyDict.put(key,dba);
			}
		}
		brat_cuiless_count=uimaKeyDict.size();


		for(String key : bratKeyDict.keySet()){
			if(key.startsWith("R")){
				BinaryTextRelation btr = new BinaryTextRelation(annView);
				String[] tabfields = bratKeyDict.get(key).split("\t");
				String[] span_fields = tabfields[0].split(" |:");
				String reltype = span_fields[0];
				btr.setCategory(reltype);
				//if(reltype.indexOf(BratConstants.NER_TYPE.BODYLOCATION.getName())!=-1) {
				RelationArgument one = new RelationArgument(annView);
				one.setArgument(uimaKeyDict.get(span_fields[2]));
				btr.setArg1(one);
				RelationArgument two = new RelationArgument(annView);
				two.setArgument(uimaKeyDict.get(span_fields[4]));
				btr.setArg2(two);
				btr.addToIndexes(annView);
			} else if(key.trim().startsWith("T")) { continue;
			} else if(key.startsWith("#")) {
				String[] tabfields = bratKeyDict.get(key).split("\t");
				String[] span_fields = tabfields[0].split(" ");
				//Retrieve Entity
				DiscontinousBratAnnotation annotated = uimaKeyDict.get(span_fields[1]);
				if(annotated==null) {
					extra_summary+="EXTRA-"+span_fields[1];
					extra_annotations++;
					continue;
				}
				//Add CUIs
				String[] cuis = tabfields[1].split(" |,");
				assert(cuis.length>0);
				if(!(cuis[0].startsWith("C") && cuis.length<6)){
					help_cui_count++;
					help_summary+="HELP-"+(bratKeyDict.get(key))+uimaKeyDict.get(key);
					continue;
				}
				FSArray ontarray = new FSArray(annView,cuis.length);
				int k=0;
				if(cuis.length==0) { cuis = new String[1]; cuis[0]="MISSED_CUI"; }
				for(String cui : cuis) {
					//System.out.println("Dealing with cui "+cui+" at k:"+k);
					OntologyConcept oc = new OntologyConcept(annView);
					oc.setCode(cui);
					ontarray.set(k, oc);
					oc.addToIndexes(annView);
					k++;
				}
				ontarray.addToIndexes(annView);
				if(ontarray!=null) annotated.setOntologyConceptArr(ontarray);
				annotated.addToIndexes(annView);
				uimaKeyDict.remove(span_fields[1]);
				brat_annotated_count++;
			}
		}
		unannotated_count=uimaKeyDict.size();
		if(unannotated_count>0) {
			for(String s : uimaKeyDict.keySet()) {
				unannotated_summary += s+":"+bratKeyDict.get(s).split("\t")[1]+"\t";
			}
			if(unannotated_count>=brat_cuiless_count) unannotated_summary = "NOT_ANNOTATED";
			if(unannotated_count>=semeval_cuiless_count) unannotated_summary = "NOT_ANNOTATED";
		}
		printTableLine(semeval_cuiless_count, brat_cuiless_count,
				brat_annotated_count, unannotated_count, extra_annotations,
				help_cui_count, docname, annotator_name, unannotated_summary,
				extra_summary,help_summary);
	}

	private void printTableLine(int semeval_cuiless_count,
			int brat_cuiless_count, int brat_annotated_count,
			int unannotated_count, int extra_annotations, int help_cui_count,
			String docname, String annotator_name, String unannotated_summary,
			String extra_summary, String help_summary) {
		String tableline = (annotator_name+"\t"+semeval_cuiless_count+"\t"+
				brat_cuiless_count+"\t"+brat_annotated_count+"\t"+unannotated_count+
				"\t"+extra_annotations+"\t"+help_cui_count+"\t"+docname+
				"\t"+unannotated_summary+"\t"+extra_summary+"\t"+help_summary);
		System.out.println(tableline);
	}

	public static AnalysisEngineDescription getDescription() throws ResourceInitializationException {
		return AnalysisEngineFactory.createEngineDescription(BratParserAnnotator.class);
	}

}
