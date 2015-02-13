package edu.uab.ccts.nlp.uima.annotator.shared_task;

import java.io.File;
import java.io.IOException;

import org.apache.commons.io.FileUtils;
import org.apache.uima.analysis_engine.AnalysisEngineDescription;
import org.cleartk.util.ViewUriUtil;
import org.apache.uima.analysis_engine.AnalysisEngineProcessException;
import org.apache.uima.cas.CASException;
import org.apache.uima.fit.component.JCasAnnotator_ImplBase;
import org.apache.uima.fit.factory.AnalysisEngineFactory;
import org.apache.uima.jcas.JCas;
import org.apache.uima.resource.ResourceInitializationException;
import org.uimafit.descriptor.ConfigurationParameter;

import edu.uab.ccts.nlp.uima.shared_task.SemEval2015Constants;

public class SemEval2015ViewCreatorAnnotator extends JCasAnnotator_ImplBase {

	static final String defaultTrainingPath = "/Users/ozborn/Dropbox/Public_NLP_Data/semeval-2015-task-14_old/semeval-2015-task-14/subtask-c/data/train";
	/**
	 * Does not populate below
	 */
	public static final String PARAM_TRAINING_PATH = "SemEval2015TrainingPath";
	@ConfigurationParameter(
			name = PARAM_TRAINING_PATH,
			description = "path to training directory",
			defaultValue = defaultTrainingPath)
	private String SemEval2015TrainingPath = defaultTrainingPath; //Needed default not working FIXME

	@Override
	public void process(JCas jcas) throws AnalysisEngineProcessException {
		JCas pipedView = null, semevalTextView = null;
		try
		{
			pipedView = jcas.createView(SemEval2015Constants.SEMEVAL_PIPED_VIEW);
			semevalTextView = jcas.createView(SemEval2015Constants.SEMEVAL_TEXT_VIEW);
		} catch (CASException e)
		{
			e.printStackTrace();
			System.exit(0);
		}
		String name = new File(ViewUriUtil.getURI(jcas).getPath()).getName();
		String[] bits = name.split("-");
		String prefix = bits[0]+"-"+bits[1];
		String type = bits[2];
		if(type.toLowerCase().startsWith("discharge")) type="discharge";
		if(type.toLowerCase().startsWith("ecg")) type="ecg";
		if(type.toLowerCase().startsWith("echo")) type="echo";
		if(type.toLowerCase().startsWith("radio")) type="radiology";
		//System.out.println("Type:"+type);
		//System.out.println("Prefix:"+prefix);
		//System.out.println("Type:"+type);
		//Fails, FIXME
		//System.out.println("Not working?! - SemPath:"+SemEval2015TrainingPath);
		String original_textfile_name = defaultTrainingPath+File.separator+type+
				File.separator+prefix+"."+SemEval2015Constants.SEMEVAL_TEXT_FILE_EXTENSION;
		String pipefilename = defaultTrainingPath+File.separator+type+
				File.separator+prefix+"."+SemEval2015Constants.SEMEVAL_PIPED_EXTENSION;
		try {
			File ofile = new File(original_textfile_name);
			if(ofile.exists()) {
				String otext = FileUtils.readFileToString(ofile);
				semevalTextView.setDocumentText(otext);
			} else {
				System.err.println("Could not find expected file:"+ofile.getPath());
			}
			File pfile = new File(pipefilename);
			if(pfile.exists()) {
				String ptext = FileUtils.readFileToString(pfile);
				pipedView.setDocumentText(ptext);
			} else {
				System.err.println("Could not find expected file:"+ofile.getPath());
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
	}


	public static AnalysisEngineDescription createAnnotatorDescription(String training_path)
			throws ResourceInitializationException {

		return AnalysisEngineFactory.createEngineDescription(
				SemEval2015ViewCreatorAnnotator.class,
				SemEval2015ViewCreatorAnnotator.PARAM_TRAINING_PATH,
				//SemEval2015ViewCreatorAnnotator.defaultTrainingPath
				training_path
				);
	}


}
