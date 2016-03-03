package edu.uab.ccts.nlp.jbratuimatools.client;

import java.io.File;
import java.util.Collection;

import org.apache.commons.io.FileUtils;
import org.apache.uima.UIMAException;
import org.apache.uima.collection.CollectionReaderDescription;
import org.apache.uima.fit.factory.CollectionReaderFactory;
import org.apache.uima.fit.pipeline.SimplePipeline;
import org.apache.uima.fit.util.JCasUtil;
import org.apache.uima.jcas.JCas;
import org.apache.uima.resource.ResourceInitializationException;
import org.uimafit.factory.AggregateBuilder;

import brat.type.DiscontinousBratAnnotation;
import edu.uab.ccts.nlp.brat.BratConstants;
import edu.uab.ccts.nlp.jbratuimatools.uima.BRATCollectionReader;
import edu.uab.ccts.nlp.jbratuimatools.uima.annotator.BratParserAnnotator;
import edu.uab.ccts.nlp.jbratuimatools.util.AnnotatorStatistics;
import edu.uab.ccts.nlp.shared_task.semeval2015.SemEval2015Constants;
import edu.uab.ccts.nlp.shared_task.semeval2015.uima.annotator.SemEval2015ViewCreatorAnnotator;


/**
 * This analyzes the train data and prints out disease text, the CUIs and their
 * semantic types, the number of times they have occured for all annotations have
 * use multi CUIs. It also prints the distribution of semantic types for all
 * multi-CUI annotations.
 * @author ozborn
 *
 */
public class PostCordDistributionClient {
	protected static String resourceDirPath = "/Users/ozborn/code/repo/cuilessdata/";
	protected static String brat_annotation_root = resourceDirPath + "training_clean/";
	protected static String semeval2015_updated_train_root = 
			"/Users/ozborn/Dropbox/Public_NLP_Data/semeval-2015-task-14_updated/data/train";
	protected static String semeval2015_old_train_root = 
			"/Users/ozborn/Dropbox/Public_NLP_Data/semeval-2015-task-14_old/semeval-2015-task-14/subtask-c/data/train";
	public static final String[] bratExtensions = {
		BratConstants.BRAT_CONFIG_FILE_EXTENSION,BratConstants.BRAT_TEXT_FILE_EXTENSION};
	public static final String[] semevalExtensions = {
		SemEval2015Constants.SEMEVAL_TEXT_FILE_EXTENSION};

	public static void main(String... args)
	{
		System.out.println(brat_annotation_root); System.out.flush();
		Collection<File> inputFiles = FileUtils.listFiles(new File(brat_annotation_root),
				bratExtensions, true);
		Collection<File> semFiles = FileUtils.listFiles(new File(semeval2015_old_train_root),
				semevalExtensions, true);
		//System.out.println("Got "+inputFiles.size()+" input files for check missing pipeline...");
		System.out.println("Got "+semFiles.size()+" semeval input files for check missing pipeline...");
		apply(inputFiles,semFiles);

	}

	public static void apply(Collection<File> files, Collection<File> semfiles) 
	{
		try {

			CollectionReaderDescription crd = CollectionReaderFactory.createReaderDescription(
					BRATCollectionReader.class,
					BRATCollectionReader.PARAM_FILES,
					files
					);



			AggregateBuilder builder = new AggregateBuilder();
			builder.add(SemEval2015ViewCreatorAnnotator.createAnnotatorDescription(semeval2015_old_train_root));
			builder.add(BratParserAnnotator.getDescription());

			AnnotatorStatistics annotatorstats = new AnnotatorStatistics();
			for (JCas jcas : SimplePipeline.iteratePipeline(crd, builder.createAggregateDescription()))
			{
				JCas annView = jcas.getView(BratConstants.TEXT_VIEW);
				Collection<DiscontinousBratAnnotation> brats = JCasUtil.select(annView, DiscontinousBratAnnotation.class);
				annotatorstats.add(brats);
			}
			annotatorstats.print(annotatorstats.getAnnotatorStats());
			System.out.println("Annotator stats:"+annotatorstats.getAnnotatorStats());
			annotatorstats.printMultipleCUIText();


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
