package edu.uab.ccts.nlp.jbratuimatools.uima.annotator;

import edu.uab.ccts.nlp.brat.BratConstants;
import edu.uab.ccts.nlp.shared_task.semeval2015.SemEval2015Constants;

import org.apache.ctakes.typesystem.type.refsem.OntologyConcept;
import org.apache.ctakes.typesystem.type.structured.DocumentID;
import org.apache.uima.analysis_engine.AnalysisEngineDescription;
import org.apache.uima.analysis_engine.AnalysisEngineProcessException;
import org.apache.uima.jcas.JCas;
import org.apache.uima.jcas.cas.FSArray;
import org.apache.uima.jcas.cas.StringArray;
import org.apache.uima.resource.ResourceInitializationException;
import org.apache.uima.util.Level;
import org.cleartk.semeval2015.type.DiseaseDisorder;
import org.uimafit.component.JCasAnnotator_ImplBase;
import org.uimafit.factory.AnalysisEngineFactory;
import org.uimafit.util.JCasUtil;

import brat.type.DiscontinousBratAnnotation;

import java.util.Collection;

/**
 * Updated DiseaseDisorders in default/GOLD_VIEW to use the CUI mappings done by the annotators
 * for the CUI-less concepts which are stored as DiscontinousBratAnnotations in BRAT_TEXT_VIEW 
 * 
 * @author ozborn
 *
 */
public class MergedCUIlessConsumer extends JCasAnnotator_ImplBase {
	public static boolean VERBOSE = false;

	// Counts annotation type by origin (semeval, brat or both)
	int only_brat, only_semeval, both_brat_semeval;
	int brat_annotation_size, semeval_annotation_size, semeval_cuiless_size;
	JCas disorderView = null, bratView = null;

	
	@Override
	public void process(JCas aJCas) throws AnalysisEngineProcessException
	{
		setupViews(aJCas);
		only_brat=0; only_semeval=0; both_brat_semeval=0;
		brat_annotation_size=0; semeval_annotation_size=0; semeval_cuiless_size=0;

		String docid = null;
		for (DocumentID di : JCasUtil.select(disorderView, DocumentID.class)) {
			docid = di.getDocumentID(); break; 
		}

		try {
			//CasCopier copier = new CasCopier(aJCas.getCas(),aJCas.getCas());
			Collection<DiscontinousBratAnnotation> col = JCasUtil.select(bratView, DiscontinousBratAnnotation.class);

			for (DiseaseDisorder ds : JCasUtil.select(disorderView, DiseaseDisorder.class))
			{
				semeval_annotation_size++;
				//SemEval2015Task2Consumer.associateSpans(disorderView, ds); //May need this to get body CUIs?
				if(ds.getCuis().get(0).equalsIgnoreCase("CUI-less")) {
					semeval_cuiless_size++;
					updateAnnotatedCUI(bratView,ds);
				}
			}

			this.getContext().getLogger().log(Level.FINE,"Found an input semeval CUI-less collection of size "
					+semeval_cuiless_size);
			this.getContext().getLogger().log(Level.FINE,"Found a annotated brat collection of size "+col.size());
			if(semeval_cuiless_size!= col.size() || both_brat_semeval!=semeval_cuiless_size){
				this.getContext().getLogger().log(Level.WARNING,docid+" has input cuiless:"+
						semeval_cuiless_size+"  and annotated cuiless:"+col.size());
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
			disorderView = aJCas.getView(SemEval2015Constants.GOLD_VIEW);
			if(!JCasUtil.exists(disorderView, DiseaseDisorder.class)) {
				this.getContext().getLogger().log(Level.SEVERE,
						"Can not find DiseaseDisorders to check BratAnnotations with in"
								+disorderView.getViewName());
			}
		} catch (Exception e) {
			this.getContext().getLogger().log(Level.SEVERE,"Can not get required view!");
			throw(new AnalysisEngineProcessException(e));
		}
	}


	/**
	 * Fetches the BRAT annotated CUI/s, updates CUI-less with out new CUIs
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



	public static AnalysisEngineDescription getDescription() throws ResourceInitializationException {
		return 
				AnalysisEngineFactory.createPrimitiveDescription(MergedCUIlessConsumer.class);
	}



}
