package edu.uab.ccts.nlp.jbratuimatools.client;

import java.io.File;
import java.util.Collection;

import org.apache.commons.io.FileUtils;
import org.apache.uima.UIMAException;
import org.apache.uima.collection.CollectionReaderDescription;
import org.apache.uima.fit.factory.AggregateBuilder;
import org.apache.uima.fit.factory.CollectionReaderFactory;
import org.apache.uima.fit.pipeline.SimplePipeline;
import org.apache.uima.jcas.JCas;
import org.apache.uima.resource.ResourceInitializationException;

import edu.uab.ccts.nlp.jbratuimatools.uima.annotator.Semeval2CUIlessBRATAnnotator;
import edu.uab.ccts.nlp.shared_task.semeval2015.SemEval2015Constants;
import edu.uab.ccts.nlp.shared_task.semeval2015.uima.SemEval2015CollectionReader;
import edu.uab.ccts.nlp.shared_task.semeval2015.uima.annotator.SemEval2015GoldAttributeParserAnnotator;


/**
 * Converts SemEval Task 14 data into the BRAT annotation format for annotating CUIless
 * concepts
 * @author ozborn
 *
 */
public class SemevalCUIless2BratClient {
	protected static String resourceDirPath = "/Users/ozborn/code/repo/cuilessdata/";
	protected static String brat_annotation_root = resourceDirPath + "training_clean/";
	public final static String brat_devel_output_root = resourceDirPath + "input/devel_updated";

	
	/**
	 * Hard coding devel set, since we already annotated the training set with the 
	 * old code
	 * @param args
	 */
	public static void main(String... args)
	{
		String st[] = {SemEval2015Constants.SEMEVAL_PIPED_EXTENSION};
		Collection<File> semFiles = FileUtils.listFiles(new File(ClientConfiguration.getSemeval2015UpdatedDevelRoot()),
		st, true);
		System.out.println("Got "+semFiles.size()+" semeval input files...");
		apply(ClientConfiguration.getSemeval2015UpdatedDevelRoot(),semFiles);

	}

	
	public static void apply(String outputdir, Collection<File> files) 
	//public static void apply(String outputdir, String inputdir) 
	{
		try {
    
		CollectionReaderDescription crd = CollectionReaderFactory.createReaderDescription(
				SemEval2015CollectionReader.class
				/*SemEval2015CollectionReader.class,
					SemEval2015CollectionReader.PARAM_FILES,
					files */
			);

		AggregateBuilder builder = new AggregateBuilder();
		builder.add(SemEval2015GoldAttributeParserAnnotator.getTrainingDescription());
		builder.add(Semeval2CUIlessBRATAnnotator.getDescription());
		//SimplePipeline.runPipeline(crd, builder.createAggregate());

		for (JCas jcas : SimplePipeline.iteratePipeline(crd, builder.createAggregateDescription()))
		{
			//JCas annView = jcas.getView(BratConstants.ANN_VIEW);
			//Collection<DiscontinousBratAnnotation> brats = JCasUtil.select(annView, DiscontinousBratAnnotation.class);
		}
		
		
		} catch (ResourceInitializationException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (UIMAException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
}
