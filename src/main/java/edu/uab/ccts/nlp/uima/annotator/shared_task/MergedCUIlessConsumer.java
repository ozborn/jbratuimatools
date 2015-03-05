package edu.uab.ccts.nlp.uima.annotator.shared_task;

import edu.uab.ccts.nlp.brat.BratConstants;
import edu.uab.ccts.nlp.uima.annotator.shared_task.SemEval2015Constants;

import org.apache.ctakes.typesystem.type.refsem.OntologyConcept;
import org.apache.ctakes.typesystem.type.structured.DocumentID;
import org.apache.uima.UimaContext;
import org.apache.uima.analysis_engine.AnalysisEngineDescription;
import org.apache.uima.analysis_engine.AnalysisEngineProcessException;
import org.apache.uima.cas.CASRuntimeException;
import org.apache.uima.jcas.JCas;
import org.apache.uima.jcas.cas.FSArray;
import org.apache.uima.resource.ResourceInitializationException;
import org.apache.uima.util.Level;
import org.cleartk.semeval2015.type.DiseaseDisorder;
import org.cleartk.semeval2015.type.DiseaseDisorderAttribute;
import org.cleartk.semeval2015.type.DisorderRelation;
import org.cleartk.semeval2015.type.DisorderSpan;
import org.uimafit.component.JCasAnnotator_ImplBase;
import org.uimafit.descriptor.ConfigurationParameter;
import org.uimafit.factory.AnalysisEngineFactory;
import org.uimafit.util.JCasUtil;

import brat.type.DiscontinousBratAnnotation;

import java.io.File;
import java.io.FileWriter;
import java.io.Writer;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * Writes out SemEval2015 annotations in piped format, overwrites CUI-less
 * CUIs for disorders if BRAT annotations are present
 * @author ozborn
 *
 */
public class MergedCUIlessConsumer extends JCasAnnotator_ImplBase {

	public static final String PARAM_OUTPUT_DIRECTORY = "outputDir";
	public static String resourceDirPath = "src/main/resources/";
	public static final String defaultPipedOutputDir = resourceDirPath+"template_results/";
	@ConfigurationParameter(
			name = PARAM_OUTPUT_DIRECTORY,
			description = "Path to the output directory for Task 2",
			defaultValue="src/main/resources/template_results/")
	private String outputDir = resourceDirPath+"template_results/";
	public static boolean VERBOSE = false;

	public static AnalysisEngineDescription createAnnotatorDescription()
			throws ResourceInitializationException
	{

		return AnalysisEngineFactory.createPrimitiveDescription(
				MergedCUIlessConsumer.class,
				MergedCUIlessConsumer.PARAM_OUTPUT_DIRECTORY,
				resourceDirPath + "semeval-2015-task-14/subtask-c/data");
	}

	public void initialize(UimaContext context) throws ResourceInitializationException
	{
		super.initialize(context);
		try {
			File out = new File(outputDir);
			if (!out.exists())
			{
				if (!out.mkdir()) System.out.println("Could not make directory " + outputDir);
			} else
			{
				if (VERBOSE) System.out.println(outputDir + " exists!");
			}
		} catch (Exception e)
		{
			e.printStackTrace();
		}
	}

	@Override
	public void process(JCas aJCas) throws AnalysisEngineProcessException
	{
		JCas disorderView = null, bratView = null;
		try {
			bratView = aJCas.getView(BratConstants.TEXT_VIEW);
			disorderView = aJCas.getView(SemEval2015Constants.GOLD_VIEW);
		} catch (CASRuntimeException e1) {
			try {
				disorderView = aJCas.getView(SemEval2015Constants.APP_VIEW);
			} catch (CASRuntimeException e2) {
				this.getContext().getLogger().log(Level.SEVERE,"Can not get a view with disorders to output");
			} catch (Exception e) { e.printStackTrace(); }
		} catch (Exception e) { e.printStackTrace(); }
		if(!JCasUtil.exists(disorderView, DiseaseDisorder.class)) {
			this.getContext().getLogger().log(Level.SEVERE,
					"Can not find DiseaseDisorders to output in"+disorderView.getViewName());
		}

		String docid = null;
		for (DocumentID di : JCasUtil.select(disorderView, DocumentID.class))
		{
			docid = di.getDocumentID();
			break;
		}

		String filepath = outputDir + File.separator +
				docid.substring(0, docid.length() - 4) + "pipe";

		try
		{
			Collection<DiscontinousBratAnnotation> col = JCasUtil.select(bratView, DiscontinousBratAnnotation.class);
			this.getContext().getLogger().log(Level.FINE,"Found a annotated brat collection of size "+col.size());
			int input_cuiless_count = 0;
			for(DiseaseDisorder indis : JCasUtil.select(disorderView, DiseaseDisorder.class)){
				if(indis.getCui().equalsIgnoreCase("CUI-less")) input_cuiless_count++;
			}
			Writer writer = new FileWriter(filepath);
			for (DiseaseDisorder ds : JCasUtil.select(disorderView, DiseaseDisorder.class))
			{
				associateSpans(disorderView, ds);
				String results = getDiseaseDisorderSemEval2015Format(bratView, docid, ds);
				if (VERBOSE) System.out.println(results);
				writer.write(results + "\n");
			}
			writer.close();
			if(input_cuiless_count!= col.size()){
				this.getContext().getLogger().log(Level.WARNING,docid+" has input cuiless:"+
			input_cuiless_count+"  and annotated cuiless:"+col.size());
			}
		} catch (Exception e)
		{
			e.printStackTrace();
		}
	}

	/**
	 * FIXME Need to handle multiple lines
	 */
	private String getDiseaseDisorderSemEval2015Format(JCas jcas, String docid, DiseaseDisorder dd)
	{
		StringBuffer output_lines = new StringBuffer(2000);
		output_lines.append(docid);
		output_lines.append(SemEval2015Constants.OUTPUT_SEPERATOR);
		FSArray spans = dd.getSpans();
		for (int i = 0; i < spans.size(); i++)
		{
			DisorderSpan ds = (DisorderSpan) spans.get(i);
			output_lines.append(ds.getBegin() + "-" + ds.getEnd());
			if (i != spans.size() - 1) output_lines.append(",");
			//			System.out.print(ds.getCoveredText() + "\t");
		}
		output_lines.append(SemEval2015Constants.OUTPUT_SEPERATOR);
		//If CUI-less, look for the CUI in BRAT annotations
		if(dd.getCui().equalsIgnoreCase("cui-less")){
			updateAnnotatedCUI(jcas,dd);
		} 
		output_lines.append(dd.getCui());
		output_lines.append(SemEval2015Constants.OUTPUT_SEPERATOR);
		FSArray atts = dd.getAttributes();
		output_lines.append(fetchAttributeString(atts, SemEval2015Constants.NEGATION_RELATION));
		output_lines.append(fetchAttributeString(atts, SemEval2015Constants.SUBJECT_RELATION));
		output_lines.append(fetchAttributeString(atts, SemEval2015Constants.UNCERTAINITY_RELATION));
		output_lines.append(fetchAttributeString(atts, SemEval2015Constants.COURSE_RELATION));
		output_lines.append(fetchAttributeString(atts, SemEval2015Constants.SEVERITY_RELATION));
		output_lines.append(fetchAttributeString(atts, SemEval2015Constants.CONDITIONAL_RELATION));
		output_lines.append(fetchAttributeString(atts, SemEval2015Constants.GENERIC_RELATION));
		output_lines.append(fetchAttributeString(atts, SemEval2015Constants.BODY_RELATION));
		//output_lines.append(fetchAttributeString(atts, SemEval2015Constants.DOCTIME_RELATION));
		//output_lines.append(fetchAttributeString(atts, SemEval2015Constants.TEMPORAL_RELATION));
		//		System.out.println();
		return output_lines.toString();
	}



	/**
	 * Fetches the BRAT annotated CUI/s
	 */
	private boolean updateAnnotatedCUI(JCas bratview, DiseaseDisorder dd) {
		boolean matched = false;
		String cuiless = "CUI-less";
		this.getContext().getLogger().log(Level.FINE,"Trying to find a match for cui-less concept: "
		+dd.getCoveredText()+" start/end "+dd.getBegin()+"/"+dd.getEnd());
		String failures = "";
		for (DiscontinousBratAnnotation brat: JCasUtil.select(bratview, DiscontinousBratAnnotation.class)) {
			if(brat.getEnd()==dd.getEnd() &&
					brat.getBegin()==dd.getBegin()) {
				matched = true;
				FSArray cuis = brat.getOntologyConceptArr();
				for(int i=0;i<cuis.size();i++){
					OntologyConcept oc = (OntologyConcept) cuis.get(i);
					String cui = oc.getCode();
					if(i==cuis.size()-1) {
						cuiless += cui;
					} else cuiless += cui+",";
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
			System.out.println(docid+" - failed to find a match for:"+dd.getCoveredText()+" in:\n"+failures);
		} 
		dd.setCui(cuiless);
		return matched;
	}

	/**
	 * Too bad UIMA doesn't have built in hashtables...
	 */
	private String fetchAttributeString(FSArray atts, String type)
	{
		String norm = SemEval2015Constants.defaultNorms.get(type);
		String cue = "null";
		if (atts != null)
		{
			for (int i = 0; i < atts.size(); i++)
			{
				DiseaseDisorderAttribute dda = (DiseaseDisorderAttribute) atts.get(i);
				if (type.equals(dda.getAttributeType()))
				{
					norm = dda.getNorm();
					if (!type.equals(SemEval2015Constants.DOCTIME_RELATION))
					{
						FSArray attspans = dda.getSpans();
						if (attspans == null)
						{
							//							System.out.println(dda.getBegin() + " to " + dda.getEnd() + " has no atts!!!!");
							continue;
						}
						for (int j = 0; j < attspans.size(); j++)
						{
							DisorderSpan ds = (DisorderSpan) attspans.get(j);
							if (j == 0) cue = (ds.getBegin() + "-" + ds.getEnd());
							else
							{
								cue = cue + "," + ds.getBegin() + "-" + ds.getEnd();
							}
							//							System.out.print(ds.getCoveredText() + "\t");

						}
						String out = norm + SemEval2015Constants.OUTPUT_SEPERATOR + cue;
						//						if (type.equals(SemEval2015Constants.BODY_RELATION))
						out += SemEval2015Constants.OUTPUT_SEPERATOR;
						return out;
					} else if (!type.equals(SemEval2015Constants.BODY_RELATION)){
						return norm;
					} else
					{
						return norm + SemEval2015Constants.OUTPUT_SEPERATOR;
					}
				}
			}
		}
		return norm + SemEval2015Constants.OUTPUT_SEPERATOR + cue + SemEval2015Constants.OUTPUT_SEPERATOR;
	}

	public static void associateSpans(JCas jCas, DiseaseDisorder dd)
	{
		List<DiseaseDisorderAttribute> atts = new ArrayList<>();
		for (DisorderRelation rel: JCasUtil.select(jCas, DisorderRelation.class))
		{
			DisorderSpan s = (DisorderSpan) rel.getArg2().getArgument();

			for (DisorderSpan span : JCasUtil.select(dd.getSpans(), DisorderSpan.class))
			{
				if (span == s)
				{
					atts.add((DiseaseDisorderAttribute) rel.getArg1().getArgument());
				}
			}
		}
		FSArray relSpans = new FSArray(jCas, atts.size());
		int min_begin = -1, max_end = -1;
		for (int i = 0; i < atts.size(); i++)
		{
			DiseaseDisorderAttribute ds = atts.get(i);
			if (ds.getBegin() < min_begin || min_begin == -1) min_begin = ds.getBegin();
			if (ds.getEnd() > max_end) max_end = ds.getEnd();
			relSpans.set(i, ds);
		}
		dd.setAttributes(relSpans);
	}


	public static AnalysisEngineDescription getDescription() throws ResourceInitializationException {
		return 
				AnalysisEngineFactory.createPrimitiveDescription(MergedCUIlessConsumer.class,
						MergedCUIlessConsumer.PARAM_OUTPUT_DIRECTORY,
						defaultPipedOutputDir);
	}

}
