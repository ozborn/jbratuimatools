package edu.uab.ccts.nlp.jbratuimatools.uima.annotator;

import edu.uab.ccts.nlp.brat.BratConfiguration;
import edu.uab.ccts.nlp.brat.BratConfigurationImpl;
import edu.uab.ccts.nlp.brat.BratConstants;
import edu.uab.ccts.nlp.jbratuimatools.util.AnnotatorStatistics;
import edu.uab.ccts.nlp.shared_task.semeval2015.SemEval2015Constants;

import org.apache.ctakes.typesystem.type.refsem.OntologyConcept;
import org.apache.ctakes.typesystem.type.structured.DocumentID;
import org.apache.uima.UimaContext;
import org.apache.uima.analysis_engine.AnalysisEngineDescription;
import org.apache.uima.analysis_engine.AnalysisEngineProcessException;
import org.apache.uima.cas.CASException;
import org.apache.uima.fit.descriptor.ConfigurationParameter;
import org.apache.uima.jcas.JCas;
import org.apache.uima.jcas.cas.FSArray;
import org.apache.uima.jcas.cas.StringArray;
import org.apache.uima.resource.ResourceInitializationException;
import org.apache.uima.util.Level;
import org.apache.uima.util.Logger;
import org.cleartk.semeval2015.type.DiseaseDisorder;
import org.apache.uima.fit.component.JCasAnnotator_ImplBase;
import org.apache.uima.fit.factory.AnalysisEngineFactory;
import org.uimafit.util.JCasUtil;

import brat.type.DiscontinousBratAnnotation;

import java.util.Collection;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.Map;
import java.util.Vector;

/**
 * Updated DiseaseDisorders in default/GOLD_VIEW to use the CUI mappings done by the annotators
 * for the CUI-less concepts which are stored as DiscontinousBratAnnotations in BRAT_TEXT_VIEW 
 * 
 * @author ozborn
 *
 */
public class MergedCUIlessConsumer extends JCasAnnotator_ImplBase {
	public static final String PARAM_CONSENSUS_LINES="consensusLines";
	public static final String PARAM_NEG_SEPARATED_CUIS = "negSeparatedCuis";

	// Counts annotation type by origin (semeval, brat or both)
	int only_brat, only_semeval, both_brat_semeval;
	int brat_annotation_size, semeval_annotation_size, semeval_cuiless_size;
	JCas semevalView = null, bratView = null;
	Map<String,String> entityConsensusCuis = new Hashtable<String,String>();

	@ConfigurationParameter(
			name = PARAM_CONSENSUS_LINES,
			description = "file to read consensus annotations from for double annotated dataset")
	protected String[] consensusLines = null;
	
	@ConfigurationParameter(
			name = PARAM_NEG_SEPARATED_CUIS,
			description = "Whether to use consensus CUIs resolved such that negation is included as a separate term")
	protected boolean negSeparatedCuis = false;

	public void initialize(UimaContext context) throws ResourceInitializationException
	{
		super.initialize(context);
		if(consensusLines!=null) this.getContext().getLogger().log(Level.CONFIG,"Using consensus file:"+consensusLines);
		else this.getContext().getLogger().log(Level.INFO,"No consensus file used...");
		if(negSeparatedCuis) this.getContext().getLogger().log(Level.CONFIG,"Using negation separated cuis");
		else this.getContext().getLogger().log(Level.INFO,"Negation included in cuis");
	}

	
	@Override
	public void process(JCas aJCas) throws AnalysisEngineProcessException {
	
		setupViews(aJCas);
		only_brat=0; only_semeval=0; both_brat_semeval=0;
		brat_annotation_size=0; semeval_annotation_size=0; semeval_cuiless_size=0;

		String docid = null;
		for (DocumentID di : JCasUtil.select(semevalView, DocumentID.class)) {
			docid = di.getDocumentID(); break; 
		}
		JCas configView;
		BratConfiguration bratconfig;
		try {
			configView = aJCas.getView(BratConstants.CONFIG_VIEW);
			bratconfig = new BratConfigurationImpl(configView.getDocumentText());
		} catch (CASException|ResourceInitializationException e1) {
			e1.printStackTrace();
			throw new AnalysisEngineProcessException(e1);
		}

		try {
			//CasCopier copier = new CasCopier(aJCas.getCas(),aJCas.getCas());
			Collection<DiscontinousBratAnnotation> col = JCasUtil.select(bratView, DiscontinousBratAnnotation.class);
			//Remove non-Disease annotations
			Collection<DiscontinousBratAnnotation> onlyDisease = new Vector<DiscontinousBratAnnotation>();
			Iterator<DiscontinousBratAnnotation> dbait = col.iterator();
			while(dbait.hasNext()) {
				DiscontinousBratAnnotation dba = dbait.next();
				if(dba.getTypeID()==bratconfig.getIdFromType("Disease")) onlyDisease.add(dba);
			}
			
			Map<String,String> docoffsethash = null;
			if(consensusLines!=null) {
				docoffsethash = buildFileOffsetMap(docid);
			}

			for (DiseaseDisorder ds : JCasUtil.select(semevalView, DiseaseDisorder.class))
			{
				semeval_annotation_size++;
				//SemEval2015Task2Consumer.associateSpans(disorderView, ds); //May need this to get body CUIs?
				if(ds.getCuis().get(0).equalsIgnoreCase("CUI-less")) {
					semeval_cuiless_size++;
					//updateAnnotatedCUI(bratView,ds);
					updateWithConsensusCUI(docid,bratView,ds,docoffsethash);
				}
			}

			this.getContext().getLogger().log(Level.FINE,"Found an input semeval CUI-less collection of size "
					+semeval_cuiless_size);
			this.getContext().getLogger().log(Level.FINE,"Found a annotated brat disease collection of size "+onlyDisease.size());
			if(semeval_cuiless_size!= onlyDisease.size() || both_brat_semeval!=semeval_cuiless_size){
				this.getContext().getLogger().log(Level.WARNING,docid+" has input cuiless:"+
						semeval_cuiless_size+"  and annotated cuiless:"+onlyDisease.size());
				this.getContext().getLogger().log(Level.INFO,"Both SemEval and Brat:"+both_brat_semeval);
			}
			if(only_semeval!=0) this.getContext().getLogger().log(Level.INFO,"SemEval Only Size:"+only_semeval);

		} catch (Exception e)
		{
			e.printStackTrace();
		}
	}


	private void setupViews(JCas aJCas) throws AnalysisEngineProcessException {
		try {
			bratView = aJCas.getView(BratConstants.TEXT_VIEW);
			semevalView = aJCas.getView(SemEval2015Constants.GOLD_VIEW);
			if(!JCasUtil.exists(semevalView, DiseaseDisorder.class)) {
				this.getContext().getLogger().log(Level.SEVERE,
						"Can not find DiseaseDisorders to check BratAnnotations with in"
								+semevalView.getViewName());
			}
		} catch (Exception e) {
			this.getContext().getLogger().log(Level.SEVERE,"Can not get required view!");
			throw(new AnalysisEngineProcessException(e));
		}
	}
	
	

	private boolean updateWithConsensusCUI(String docname,JCas bratview, 
			DiseaseDisorder dd, Map<String,String>conhash) {
		boolean matched = false;
			String cuiless = "CUI-less";
		StringArray cuisa = null;
		String replacement_cui = "";
		this.getContext().getLogger().log(Level.FINE,"Trying to find a match for cui-less concept: "
				+dd.getCoveredText()+" start/end "+dd.getBegin()+"/"+dd.getEnd());
		String failures = "";
		for (DiscontinousBratAnnotation brat: JCasUtil.select(bratview, DiscontinousBratAnnotation.class)) {
			if(brat.getEnd()==dd.getEnd() && brat.getBegin()==dd.getBegin()) {
				if(brat.getIsNovelEntity()==false) { matched = true; }
				else { this.getContext().getLogger().log(Level.WARNING, "Matched novel annotation?!"); }
				/*
				FSArray cuis = brat.getOntologyConceptArr();
				for(int i=0;i<cuis.size();i++){
					OntologyConcept oc = (OntologyConcept) cuis.get(i);
					String cui = oc.getCode();
					if(i==cuis.size()-1) {
						replacement_cui += cui;
					} else replacement_cui += cui+",";
				}
				*/
				String bratOffSetString = AnnotatorStatistics.getOffsets(brat);
				for (Map.Entry<String,String> entry : conhash.entrySet()){
					String spanstring = entry.getKey().split("=")[1];
					String condoc = entry.getKey().split("=")[0];
					if(condoc.equals(docname)&& spanstring.equals(bratOffSetString)) {
						replacement_cui = entry.getValue();
						break;
					}
				}
				break;
			} else {
				failures += "No Match "+brat.getDiscontinousText()+" start/end "+brat.getBegin()+"/"+brat.getEnd()+"\n";
			}
		}
		if(!matched){
			String docid = "";
			for (DocumentID di : JCasUtil.select(bratview, DocumentID.class)) {
				docid = di.getDocumentID();
				break;
			}
			only_semeval++;
			this.getContext().getLogger().log(Level.WARNING, 
					docid+" - failed to find a match for:"+dd.getCoveredText()+" in:\n"+failures);
		} else { both_brat_semeval++; } 
		if(replacement_cui.isEmpty()) { cuisa = new StringArray(bratview,1); cuisa.set(0, cuiless); dd.setCuis(cuisa); }
		else {
			String multicuis[] = replacement_cui.split(" ");
			cuisa = new StringArray(bratview,multicuis.length);
			for(int i=0;i<multicuis.length;i++) { cuisa.set(i, multicuis[i]); }
			dd.setCuis(cuisa);
		}
	
		
		return matched;
	}

	/**
	 * Fetches the BRAT annotated CUI/s, updates CUI-less with our new CUIs from BRAT Annotation
	 */
	private boolean updateAnnotatedCUI(JCas bratview, DiseaseDisorder dd) {
		boolean matched = false;
		String cuiless = "CUI-less";
		StringArray cuisa = null;
		String replacement_cui = "";
		this.getContext().getLogger().log(Level.FINE,"Trying to find a match for cui-less concept: "
				+dd.getCoveredText()+" start/end "+dd.getBegin()+"/"+dd.getEnd());
		String failures = "";
		for (DiscontinousBratAnnotation brat: JCasUtil.select(bratview, DiscontinousBratAnnotation.class)) {
			if(brat.getEnd()==dd.getEnd() && brat.getBegin()==dd.getBegin()) {
				if(brat.getIsNovelEntity()==false) { matched = true; }
				else { this.getContext().getLogger().log(Level.WARNING, "Matched novel annotation?!"); }
				FSArray cuis = brat.getOntologyConceptArr();
				for(int i=0;i<cuis.size();i++){
					OntologyConcept oc = (OntologyConcept) cuis.get(i);
					String cui = oc.getCode();
					if(i==cuis.size()-1) {
						replacement_cui += cui;
					} else replacement_cui += cui+",";
				}
				break;
			} else {
				failures += "No Match "+brat.getDiscontinousText()+" start/end "+brat.getBegin()+"/"+brat.getEnd()+"\n";
			}
		}
		if(!matched){
			String docid = "";
			for (DocumentID di : JCasUtil.select(bratview, DocumentID.class)) {
				docid = di.getDocumentID();
				break;
			}
			only_semeval++;
			this.getContext().getLogger().log(Level.WARNING, 
					docid+" - failed to find a match for:"+dd.getCoveredText()+" in:\n"+failures);
		} else { both_brat_semeval++; } 
		if(replacement_cui.isEmpty()) { cuisa = new StringArray(bratview,1); cuisa.set(0, cuiless); dd.setCuis(cuisa); }
		else {
			String multicuis[] = replacement_cui.split(" ");
			cuisa = new StringArray(bratview,multicuis.length);
			for(int i=0;i<multicuis.length;i++) { cuisa.set(i, multicuis[i]); }
			dd.setCuis(cuisa);
		}
		return matched;
	}
	
	
	/**
	 * DELETE ME
	 * Using a tab separated sheet, it builds up a map between key DocumentName-EntityID and the
	 * CUI Set for that (disease) entity
	 * @deprecated
	 * @param filledCuis
	 */
	private void buildConsensusHash(String docname){
		Logger logger = this.getContext().getLogger();
		for(String line : consensusLines) {
			String negIncludedCuis = null;
			String negSeparateCuis = null;
			String[] fields = line.split("\\t");
			String correctsuffix = docname.replaceAll(".text", ".txt");
			if(fields[2].endsWith(correctsuffix)){
				if(fields.length<12) {
					logger.log(Level.WARNING,fields[3]+" at "+fields[4]+" has only "+fields.length+" fields");
					continue;
				}
				String id = fields[4];
				if(fields[11]!=null) {
					negIncludedCuis = fields[11].replaceAll("\"", "");
					negSeparateCuis = negIncludedCuis;
				} else logger.log(Level.WARNING,"No negIncluded CUI for:"+line);
				if(fields.length>=13 && fields[12]!=null) {
					negSeparateCuis = fields[12].replaceAll("\"", "");
				}
				if(negSeparatedCuis) entityConsensusCuis.put(id,negSeparateCuis);
				else entityConsensusCuis.put(id, negIncludedCuis);
			}
		}
	}
	
	
	
	
	/**
	 * Using a tab separated "Action" sheet, it builds up a map between key DocumentName-Offset and the
	 * CUI Set for that (disease) entity
	 * @param filledCuis
	 */
	private Map<String,String> buildFileOffsetMap(String docname){
		Map<String,String> CUIS2Use = new Hashtable<String,String>();
		Logger logger = this.getContext().getLogger();
		for(String line : consensusLines) {
			String negIncludedCuis = null;
			String negSeparateCuis = null;
			String[] fields = line.split("\\t");
			String correctsuffix = docname.replaceAll(".text", ".txt");
			if(fields[1].endsWith(correctsuffix)){
				if(fields.length<6) {
					logger.log(Level.WARNING,fields[1]+" at "+fields[2]+" has only "+fields.length+" fields");
					continue;
				}
				String id = fields[1]+"="+fields[2];
				StringBuilder replacement_cuis = new StringBuilder();
				if(fields[4]!=null) { 
					negIncludedCuis = fields[4].replaceAll("\"", "");
					negSeparateCuis = negIncludedCuis;
				} else logger.log(Level.WARNING,"No negIncluded CUI for:"+line);
				if(fields.length>=6 && fields[5]!=null) {
					negSeparateCuis = fields[5].replaceAll("\"", "");
				}
				CUIS2Use.put(id, negSeparateCuis);
			}
		}
		return CUIS2Use;
		
	}

	



	public static AnalysisEngineDescription getDescription() throws ResourceInitializationException {
		return 
				AnalysisEngineFactory.createEngineDescription(MergedCUIlessConsumer.class);
	}

	public static AnalysisEngineDescription getDescription(String[] lines, boolean negSepCuis) throws ResourceInitializationException {
		return 
				//AnalysisEngineFactory.createPrimitiveDescription(MergedCUIlessConsumer.class
				AnalysisEngineFactory.createEngineDescription(
						MergedCUIlessConsumer.class
						,PARAM_CONSENSUS_LINES
						,lines
						,PARAM_NEG_SEPARATED_CUIS
						,negSepCuis
						);
	}



}
