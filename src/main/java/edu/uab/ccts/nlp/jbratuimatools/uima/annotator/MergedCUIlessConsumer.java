package edu.uab.ccts.nlp.jbratuimatools.uima.annotator;

import edu.uab.ccts.nlp.brat.BratConfiguration;
import edu.uab.ccts.nlp.brat.BratConfigurationImpl;
import edu.uab.ccts.nlp.brat.BratConstants;
import edu.uab.ccts.nlp.jbratuimatools.util.AnnotatorStatistics;
import edu.uab.ccts.nlp.shared_task.semeval2015.SemEval2015Constants;
import edu.uab.ccts.nlp.shared_task.semeval2015.uima.annotator.SemEval2015Task2Consumer;
import edu.uab.ccts.nlp.umls.tools.CleanUtils;

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
import org.cleartk.semeval2015.type.DiseaseDisorderAttribute;
import org.apache.uima.fit.component.JCasAnnotator_ImplBase;
import org.apache.uima.fit.factory.AnalysisEngineFactory;
import org.uimafit.util.JCasUtil;

import brat.type.DiscontinousBratAnnotation;

import java.io.IOException;
import java.net.URL;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
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
	static Properties semeval2umls;

	public void initialize(UimaContext context) throws ResourceInitializationException
	{
		super.initialize(context);
		if(consensusLines!=null) this.getContext().getLogger().log(Level.CONFIG,"Using consensus file:"+consensusLines);
		else this.getContext().getLogger().log(Level.INFO,"No consensus file used...");
		if(negSeparatedCuis) this.getContext().getLogger().log(Level.CONFIG,"Using negation separated cuis");
		else this.getContext().getLogger().log(Level.INFO,"Negation included in cuis");
		URL u1 = this.getClass().getResource("/semeval2umls.properties");
		semeval2umls = new Properties();
		try {
			semeval2umls.load(u1.openStream());
		} catch (IOException ioe) { throw new ResourceInitializationException(ioe); }
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
				if(ds.getCuis().get(0).equalsIgnoreCase("CUI-less")) {
					semeval_cuiless_size++;
					FSArray atts = SemEval2015Task2Consumer.associateSpans(semevalView, ds);
					CleanUtils cleanutils = new CleanUtils();
					Hashtable<String,String> thecuis = new Hashtable<String,String>();
					Set<String> cuis = new HashSet<String>();
					for(int i=0;i<atts.size();i++){
						DiseaseDisorderAttribute dda = (DiseaseDisorderAttribute) atts.get(i);
						this.getContext().getLogger().log(Level.FINER,docid+" at:"+ds.getBegin()+"-"+ds.getEnd()+" ; getting cuis from attribute:"+dda.getAttributeType()+ " with norm:"+dda.getNorm());
						cleanutils.getCuisFromDiseaseDisorderAttributes(i,thecuis,semeval2umls,
								this.getContext().getLogger(),dda);
					}
					cuis.addAll(thecuis.values());
					if(cuis.size()>0) this.getContext().getLogger().log(Level.FINE,"Got "+cuis.size()+" attribute cuis:"+cuis);

					updateWithConsensusCUI(docid,bratView,ds,docoffsethash,cuis);
				}
			}

			this.getContext().getLogger().log(Level.FINE,"Found an input semeval CUI-less collection of size "
					+semeval_cuiless_size);
			this.getContext().getLogger().log(Level.FINE,"Found a annotated brat disease collection of size "+onlyDisease.size());
			if(semeval_cuiless_size!= onlyDisease.size() || both_brat_semeval!=semeval_cuiless_size){
				if(semeval_cuiless_size<onlyDisease.size()) {
					this.getContext().getLogger().log(Level.WARNING,docid+" Original semeval annotation"+
							"may have annotated a CUI-less concept with a CUI that was originally CUI-less in our"+
							"early dataset.");
				}
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
			DiseaseDisorder dd, Map<String,String>conhash, Set<String> attcuis) {
		boolean matched = false;
		String cuiless = "CUI-less";
		StringArray cuisa = null;
		String replacement_cui = null;
		this.getContext().getLogger().log(Level.FINE,"Doc:"+docname+" trying to find a consensus match for semeval cui-less concept: "
				+dd.getCoveredText()+" start/end "+dd.getBegin()+"/"+dd.getEnd());
		String failures = "";
		for (DiscontinousBratAnnotation brat: JCasUtil.select(bratview, DiscontinousBratAnnotation.class)) {
			if(brat.getEnd()==dd.getEnd() && brat.getBegin()==dd.getBegin()) {
				if(brat.getIsNovelEntity()==false) { 
					matched = true; 
					this.getContext().getLogger().log(Level.FINER, "Matched "+dd+" to brat:"+brat); 
				}
				else { this.getContext().getLogger().log(Level.WARNING, "Matched novel annotation?!"); }

				FSArray cuis = brat.getOntologyConceptArr();
				replacement_cui = getConsensusReplaceCuis(docname, conhash, brat,attcuis);
				if(replacement_cui!=null) {
					this.getContext().getLogger().log(Level.FINE,"Doc:"+docname+" with text:"+
				    dd.getCoveredText()+" --> Got consensus CUI:"+replacement_cui);
					break;
				}
				replacement_cui = getBratAnnotatedCuis(cuis);
				this.getContext().getLogger().log(Level.FINE,"Doc:"+docname+" with text:"+dd.getCoveredText()+
				"   --> Got brat (AGREE) CUI:"+replacement_cui);
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
		if(replacement_cui==null || replacement_cui.isEmpty()) { 
			cuisa = new StringArray(bratview,1); cuisa.set(0, cuiless); dd.setCuis(cuisa); }
		else {
			String multicuis[] = replacement_cui.split(",");
			cuisa = new StringArray(bratview,multicuis.length);
			for(int i=0;i<multicuis.length;i++) { cuisa.set(i, multicuis[i]); }
			dd.setCuis(cuisa);
		}
		return matched;
	}


	/**
	 * @param replacement_cui
	 * @param cuis
	 * @return
	 */
	private String getBratAnnotatedCuis(FSArray cuis) {
		StringBuilder sb = new StringBuilder();
		for(int i=0;i<cuis.size();i++){
			OntologyConcept oc = (OntologyConcept) cuis.get(i);
			String cui = oc.getCode();
			if(i==cuis.size()-1) {
				sb.append(cui);
			} else {
				sb.append(cui);
				sb.append(",");
			}
		}
		return sb.toString();
	}


	/**
	 * @param docname
	 * @param conhash
	 * @param replacement_cui
	 * @param brat
	 * @return
	 */
	private String getConsensusReplaceCuis(String docname, Map<String, String> conhash,
			DiscontinousBratAnnotation brat, Set<String> attcuis) {
		String replacementCui = null;
		String bratOffSetString = AnnotatorStatistics.getOffsets(brat);
		this.getContext().getLogger().log(Level.FINER, "In doc:("+docname+
				") looking through "+conhash.size()+" consensus agreements to find "+bratOffSetString);
		for (Map.Entry<String,String> entry : conhash.entrySet()){
			String spanstring = entry.getKey().split("=")[1];
			String condoc = entry.getKey().split("=")[0];
			this.getContext().getLogger().log(Level.FINER, "docname:"+docname+" condoc:"+condoc+
					" looking to match "+spanstring+" with "+bratOffSetString);
			if(condoc.substring(0,condoc.indexOf('.')).equals(docname.substring(0,docname.indexOf('.')))&& spanstring.equals(bratOffSetString)) {
				replacementCui = entry.getValue();
				this.getContext().getLogger().log(Level.FINE, "Doc:"+docname+" found replacement CUI "
						+replacementCui+" from "+spanstring);
				break;
			}
		}
		if(replacementCui==null) {
			this.getContext().getLogger().log(Level.FINE,"No replacement for this CUI, must be agreement?");
			return null; //No replacement found
		}
		if(attcuis.size()==0) {
			this.getContext().getLogger().log(Level.FINE,"Att size is zero?! No replacement CUI.");
			return replacementCui; //No attributes need to be replaced
		}

		//Must replace attributes mixed in with consensus data
		String[] concuis = null;
		HashSet<String> concuishash = new HashSet<String>();
		if(replacementCui.indexOf(",")==-1) concuishash.add(replacementCui);
		else {
			concuis = replacementCui.split(",");
			concuishash = new HashSet<String>(Arrays.asList(concuis));
		}
		this.getContext().getLogger().log(Level.FINE,"Doc:"+docname+" attributes to get rid of:"+attcuis);
		concuishash.removeAll(attcuis);
		Iterator<String> it = concuishash.iterator();
		StringBuilder sb = new StringBuilder();
		while(it.hasNext()){
			sb.append(it.next());
			if(it.hasNext()) sb.append(",");
		}
		replacementCui = sb.toString();
		this.getContext().getLogger().log(Level.FINE,"Doc:("+docname+") Brat Annotation:"+
		brat.getCoveredText()+" Consensus Replacement CUI:"+replacementCui);
		return replacementCui;
	}



	/**
	 * Using a tab separated "Action" sheet, it builds up a map between key DocumentName-Offset and the
	 * CUI Set for that (disease) entity
	 * @param filledCuis
	 */
	private Map<String,String> buildFileOffsetMap(String docname){
		Map<String,String> CUIS2Use = new Hashtable<String,String>();
		Logger logger = this.getContext().getLogger();
		if(consensusLines==null || consensusLines.length==0) {
			this.getContext().getLogger().log(Level.FINE,"Consensus lines count is:"+consensusLines.length);
		}
		for(String line : consensusLines) {
			String negIncludedCuis = null;
			String negSeparateCuis = null;
			String[] fields = line.split("\\t",-1);
			String correctsuffix = docname.replaceAll(".text", ".txt");
			this.getContext().getLogger().log(Level.FINER,"Matching on field 1"+fields[1]+
					" and correctsuffix "+correctsuffix);
			if(fields[1].endsWith(correctsuffix)){
				if(fields.length<6) {
					logger.log(Level.WARNING,fields[1]+" at "+fields[2]+" has only "+fields.length+" fields");
					continue;
				}
				String id = fields[1]+"="+fields[2];
				this.getContext().getLogger().log(Level.FINER,"GOt id for hash of:"+id);
				if(fields[4]!=null && !fields[4].isEmpty()) { 
					negIncludedCuis = fields[4].replaceAll("\"", "");
					negSeparateCuis = negIncludedCuis;
				} else logger.log(Level.WARNING,"No negIncluded CUI for:"+line);
				if(fields.length>=6 && fields[5]!=null && !fields[5].isEmpty()) {
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
