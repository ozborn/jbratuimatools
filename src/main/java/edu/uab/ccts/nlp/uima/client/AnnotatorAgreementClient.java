package edu.uab.ccts.nlp.uima.client;

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
import edu.uab.ccts.nlp.uima.annotator.brat.BratParserAnnotator;
import edu.uab.ccts.nlp.uima.annotator.cuiless.AnnotatorStatistics;
import edu.uab.ccts.nlp.uima.collection_readers.BRATCollectionReader;
import edu.uab.ccts.nlp.uima.collection_readers.SemEval2015BratCompareCollectionReader;
import edu.uab.ccts.nlp.uima.annotator.shared_task.SemEval2015Constants;
import edu.uab.ccts.nlp.uima.annotator.shared_task.SemEval2015ViewCreatorAnnotator;


/**
 * Calculates annotator agreement in the devel dataset between multiple annotators
 * @author ozborn
 *
 */
public class AnnotatorAgreementClient {
	protected static String resourceDirPath = "/Users/ozborn/code/repo/cuilessdata/";
	protected static String brat_annotation_root = resourceDirPath + "devel_updated/";
	protected static String semeval2015_updated_devel_root = 
			"/Users/ozborn/Dropbox/Public_NLP_Data/semeval-2015-task-14_updated/data/devel/discharge";
	//protected static String semeval2015_old_train_root = 
	//		"/Users/ozborn/Dropbox/Public_NLP_Data/semeval-2015-task-14_old/semeval-2015-task-14/subtask-c/data/train";
	public static final String[] bratExtensions = {
			BratConstants.BRAT_CONFIG_FILE_EXTENSION,BratConstants.BRAT_TEXT_FILE_EXTENSION};
	public static final String[] semevalExtensions = {
			SemEval2015Constants.SEMEVAL_TEXT_FILE_EXTENSION};

	public static void main(String... args)
	{
		System.out.println(brat_annotation_root); System.out.flush();
		//Input files are manual files
		Collection<File> inputFiles = FileUtils.listFiles(new File(brat_annotation_root),
				bratExtensions, true);
		//Collection<File> semFiles = FileUtils.listFiles(new File(semeval2015_old_train_root),
		//These ar ethe reference files
		Collection<File> semFiles = FileUtils.listFiles(new File(semeval2015_updated_devel_root),
				semevalExtensions, true);
		//System.out.println("Got "+inputFiles.size()+" input files for check missing pipeline...");
		System.out.println("Got "+semFiles.size()+" semeval input files for checking annotator agreement...");
		apply(inputFiles,semFiles);

	}
	
	public static void apply(Collection<File> files, Collection<File> semfiles) 
	{
		try {
    
		CollectionReaderDescription crd = CollectionReaderFactory.createReaderDescription(
				SemEval2015BratCompareCollectionReader.class,
					BRATCollectionReader.PARAM_FILES,
					files,
					SemEval2015BratCompareCollectionReader.PARAM_SEMEVAL_FILES,
					semfiles
			);

		AggregateBuilder builder = new AggregateBuilder();
		builder.add(SemEval2015ViewCreatorAnnotator.createAnnotatorDescription(semeval2015_updated_devel_root));
		builder.add(BratParserAnnotator.getDescription());

		AnnotatorStatistics annotatorstats = new AnnotatorStatistics();
		for (JCas jcas : SimplePipeline.iteratePipeline(crd, builder.createAggregateDescription()))
		{
			JCas annView = jcas.getView(BratConstants.TEXT_VIEW);
			Collection<DiscontinousBratAnnotation> brats = JCasUtil.select(annView, DiscontinousBratAnnotation.class);
			annotatorstats.add(brats); //FIXME add or use an "annotator/user" field for each Discontinous Annotation
		}
		annotatorstats.print(annotatorstats.getAnnotatorStats());
		System.out.println("Annotator stats:"+annotatorstats.getAnnotatorStats());
		System.out.println(annotatorstats.getDiscrepancies());
		
		
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
