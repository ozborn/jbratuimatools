package edu.uab.ccts.nlp.uima.collection_readers;

import java.io.File;
import java.util.Collection;

import org.apache.commons.io.FileUtils;
import org.apache.uima.UimaContext;
import org.apache.uima.fit.descriptor.ConfigurationParameter;
import org.apache.uima.resource.ResourceInitializationException;

import edu.uab.ccts.nlp.shared_task.SemEval2015Constants;

/**
 * For my own SemEval comparison, not needed for generic BRAT stuff
 * @author ozborn
 *
 */
public class SemEval2015BratCompareCollectionReader extends
		BRATCollectionReader {
	
	
	public static final String PARAM_SEMEVAL_FILES = "semeval_files";
	@ConfigurationParameter(
			name = PARAM_SEMEVAL_FILES,
			description = "points to a data directory containing semeval piped and text files")
	protected Collection<File> semevalFiles;
	
	
	public static final String PARAM_SEMEVAL_PATH = "semeval_files";
	@ConfigurationParameter(
			name = PARAM_SEMEVAL_PATH,
			description = "points to a data directory containing semeval piped and text files")
	protected String semeval_path;
	
	public void initialize(UimaContext context) throws ResourceInitializationException {
		super.initialize(context);
		if(semevalFiles==null | semevalFiles.size()==0){
			System.out.println("Semeval Files were null or empty");
			semevalFiles = FileUtils.listFiles(new File(semeval_path),
				SemEval2015Constants.semevalExtensions, true);
		}
	}

}
