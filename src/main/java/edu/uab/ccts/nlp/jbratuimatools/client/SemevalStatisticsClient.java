package edu.uab.ccts.nlp.jbratuimatools.client;

import java.io.File;
import java.util.Collection;

import org.apache.commons.io.FileUtils;
import org.apache.uima.UIMAException;
import org.apache.uima.collection.CollectionReaderDescription;
import org.apache.uima.fit.factory.CollectionReaderFactory;
import org.apache.uima.fit.pipeline.SimplePipeline;
import org.apache.uima.jcas.JCas;
import org.apache.uima.resource.ResourceInitializationException;
import org.uimafit.factory.AggregateBuilder;

import edu.uab.ccts.nlp.shared_task.semeval2015.SemEval2015Constants;
import edu.uab.ccts.nlp.shared_task.semeval2015.uima.SemEval2015CollectionReader;
import edu.uab.ccts.nlp.shared_task.semeval2015.uima.annotator.SemEval2015AttributeCounter;
import edu.uab.ccts.nlp.shared_task.semeval2015.uima.annotator.SemEval2015GoldAttributeParserAnnotator;


/**
 * Reads in SemEval Format (updated to handle more CUI disorders) and prints out some statistics
 * concepts
 * @author ozborn
 *
 */
public class SemevalStatisticsClient {
	protected static String resourceDirPath = "/Users/ozborn/code/repo/cuilessdata/devel/consensus_neg_excluded/";

	
	/**
	 * @param args
	 */
	public static void main(String... args)
	{
		System.out.println("ARGS 0:"+args[0]);
		String st[] = {SemEval2015Constants.SEMEVAL_PIPED_EXTENSION};
		//Collection<File> semFiles = FileUtils.listFiles(new File(ClientConfiguration.getSemeval2015UpdatedDevelRoot()),
		//st, true);
		Collection<File> semFiles = FileUtils.listFiles(new File(args[0]),
		st, true);
		System.out.println("Got "+semFiles.size()+" semeval input files...");
		apply(args[0],semFiles);

	}

	
	public static void apply(String outputdir, Collection<File> files) 
	{
		try {
    
		CollectionReaderDescription crd = CollectionReaderFactory.createReaderDescription(
				SemEval2015CollectionReader.class,
					SemEval2015CollectionReader.PARAM_FILES,
					files 
			);

		AggregateBuilder builder = new AggregateBuilder();
		builder.add(SemEval2015GoldAttributeParserAnnotator.getTrainingDescription());
		builder.add(SemEval2015AttributeCounter.getDescription());

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
