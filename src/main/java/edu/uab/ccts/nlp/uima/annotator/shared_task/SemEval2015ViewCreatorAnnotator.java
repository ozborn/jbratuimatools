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

/**
 * This file reads in SemEval files into the Semeval PIPED_VIEW and the SEMEVAL_TEXT_VIEW
 * @author ozborn
 *
 */
public class SemEval2015ViewCreatorAnnotator extends JCasAnnotator_ImplBase {

	static final String defaultTrainingPath = "/Users/ozborn/Dropbox/Public_NLP_Data/semeval-2015-task-14_old/semeval-2015-task-14/subtask-c/data/train";
	static final String defaultDevelPath = "/home/ozborn/Dropbox/Public_NLP_Data/semeval-2015-task-14_updated/data/devel";


	/**
	 * FIXME Does not populate below
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
			pipedView = jcas.createView(SemEval2015Constants.PIPED_VIEW);
			semevalTextView = jcas; //Use the default/gold view
		} catch (CASException e)
		{
			e.printStackTrace();
			System.exit(0);
		}
		String name = new File(ViewUriUtil.getURI(jcas).getPath()).getName();
		//System.out.println(ViewUriUtil.getURI(jcas).getPath()); System.out.flush();
		String[] bits = name.split("-");
		String prefix = bits[0]+"-"+bits[1];
		//System.out.println("Prefix was:"+prefix);System.out.flush();
		String type = "discharge"; //Default type
		if(bits.length>=3){
			type = bits[2];
			if(type.toLowerCase().startsWith("discharge")) type="discharge";
			if(type.toLowerCase().startsWith("ecg")) type="ecg";
			if(type.toLowerCase().startsWith("echo")) type="echo";
			if(type.toLowerCase().startsWith("radio")) type="radiology";
		} 
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
				//System.out.println("Did not find training text file:"+ofile);
				ofile = new File(defaultDevelPath+File.separator+type+
						File.separator+prefix.split("\\.")[0]+"."+SemEval2015Constants.SEMEVAL_TEXT_FILE_EXTENSION);
				if(ofile.exists()){
					String otext = FileUtils.readFileToString(ofile);
					semevalTextView.setDocumentText(otext);
				} else {
					System.err.println("Could not find expected devel text file:"+ofile.getPath());
				}
			}
			File pfile = new File(pipefilename);
			if(pfile.exists()) {
				String ptext = FileUtils.readFileToString(pfile);
				pipedView.setDocumentText(ptext);
			} else {
				//System.out.println("Could not find expected pipe file:"+pfile.getPath());
				pfile = new File(defaultDevelPath+File.separator+type+
						File.separator+prefix.split("\\.")[0]+"."+SemEval2015Constants.SEMEVAL_PIPED_EXTENSION);
				if(pfile.exists()) {
					String ptext = FileUtils.readFileToString(pfile);
					pipedView.setDocumentText(ptext);
				} else {
					System.err.println("Could not find expected pipe file:"+pfile.getPath());
				}
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
