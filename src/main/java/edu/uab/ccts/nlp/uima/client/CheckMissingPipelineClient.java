package edu.uab.ccts.nlp.uima.client;

import java.io.File;
import java.util.Collection;
import java.util.Hashtable;

import org.apache.commons.io.FileUtils;
import org.apache.uima.UIMAException;
import org.apache.uima.collection.CollectionReader;
import org.apache.uima.collection.CollectionReaderDescription;
import org.apache.uima.fit.factory.CollectionReaderFactory;
import org.apache.uima.fit.factory.ConfigurationParameterFactory;
import org.apache.uima.fit.factory.ConfigurationParameterFactory.ConfigurationData;
import org.apache.uima.fit.pipeline.JCasIterable;
import org.apache.uima.fit.pipeline.SimplePipeline;
import org.apache.uima.fit.util.JCasUtil;
import org.apache.uima.jcas.JCas;
import org.apache.uima.resource.ResourceInitializationException;
import org.cleartk.util.ViewUriUtil;
import org.uimafit.factory.AggregateBuilder;
import org.uimafit.factory.AnalysisEngineFactory;

import com.google.common.collect.HashMultiset;

import brat.type.DiscontinousBratAnnotation;
import edu.uab.ccts.nlp.brat.BratConstants;
import edu.uab.ccts.nlp.uima.annotator.brat.BratParserAnnotator;
import edu.uab.ccts.nlp.uima.annotator.cuiless.AnnotatorStatistics;
import edu.uab.ccts.nlp.uima.collection_readers.BRATCollectionReader;
import edu.uab.ccts.nlp.uima.collection_readers.SemEval2015BratCompareCollectionReader;
import edu.uab.ccts.nlp.uima.shared_task.SemEval2015Constants;
import edu.uab.ccts.nlp.uima.annotator.shared_task.SemEval2015ViewCreatorAnnotator;


public class CheckMissingPipelineClient {
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
	static Hashtable<String,Hashtable<String,HashMultiset<String>>> annotation_results = 
	new Hashtable<String,Hashtable<String,HashMultiset<String>>>();

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
		CollectionReader reader;
		try {
			reader = CollectionReaderFactory.createReader(
					SemEval2015BratCompareCollectionReader.class,
					BRATCollectionReader.PARAM_FILES,
					files,
					SemEval2015BratCompareCollectionReader.PARAM_SEMEVAL_FILES,
					semfiles
			);

		/*
		ConfigurationData config_data1 = ConfigurationParameterFactory.createConfigurationData(
                BRATCollectionReader.PARAM_FILES,
                files
        );
		ConfigurationData config_data2 = ConfigurationParameterFactory.createConfigurationData(
				SemEval2015BratCompareCollectionReader.PARAM_SEMEVAL_FILES,
				semfiles
        );
		ConfigurationData config_data1 = ConfigurationParameterFactory.createConfigurationData(
                BRATCollectionReader.PARAM_FILE_PATH,
                brat_annotation_root
        );
		ConfigurationData config_data2 = ConfigurationParameterFactory.createConfigurationData(
				SemEval2015BratCompareCollectionReader.PARAM_SEMEVAL_PATH,
				semeval2015_old_train_root
        );
        */
    
		CollectionReaderDescription crd = CollectionReaderFactory.createReaderDescription(
				SemEval2015BratCompareCollectionReader.class,
					BRATCollectionReader.PARAM_FILES,
					files,
					SemEval2015BratCompareCollectionReader.PARAM_SEMEVAL_FILES,
					semfiles
			);

		AggregateBuilder builder = new AggregateBuilder();
		builder.add(SemEval2015ViewCreatorAnnotator.createAnnotatorDescription(semeval2015_old_train_root));
		builder.add(BratParserAnnotator.getDescription());
		/*
		builder.add(AnalysisEngineFactory.createPrimitiveDescription(SemEval2015TaskCGoldAnnotator.class,
				SemEval2015TaskCGoldAnnotator.PARAM_TRAINING,
				true,
				SemEval2015TaskCGoldAnnotator.PARAM_CUI_MAP,
				TrainTestPipeline.cuiMapFile));
				*/

		/*Fails to pass Collection<Files? parameter
		builder.add(AnalysisEngineFactory.createPrimitiveDescription(SemEval2015ViewCreatorAnnotator.class,
				SemEval2015ViewCreatorAnnotator.PARAM_SEMEVAL_FILES,
				semfiles
				));
		*/

		AnnotatorStatistics annotatorstats = new AnnotatorStatistics();
		for (JCas jcas : SimplePipeline.iteratePipeline(crd, builder.createAggregateDescription()))
		{
			JCas annView = jcas.getView(BratConstants.ANN_VIEW);
			Collection<DiscontinousBratAnnotation> brats = JCasUtil.select(annView, DiscontinousBratAnnotation.class);
			//String[] pathbits = (ViewUriUtil.getURI(annView)).toString().split(File.separator);
			//System.out.println("Got "+brats.size()+" brat annotations for "+pathbits[pathbits.length-1]);
			annotatorstats.add(brats);
		}
		annotatorstats.print();
		annotatorstats.printSemanticTypeDistribution();
		
		
		//SimplePipeline.runPipeline(reader, builder.createAggregate());
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
