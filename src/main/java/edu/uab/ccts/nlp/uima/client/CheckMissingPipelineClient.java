package edu.uab.ccts.nlp.uima.client;

import java.io.File;
import java.util.Collection;
import java.util.Hashtable;

import org.apache.commons.io.FileUtils;
import org.apache.uima.UIMAException;
import org.apache.uima.collection.CollectionReaderDescription;
import org.apache.uima.fit.factory.CollectionReaderFactory;
import org.apache.uima.fit.pipeline.SimplePipeline;
import org.apache.uima.fit.util.JCasUtil;
import org.apache.uima.jcas.JCas;
import org.apache.uima.resource.ResourceInitializationException;
import org.uimafit.factory.AggregateBuilder;

import com.google.common.collect.HashMultiset;

import brat.type.DiscontinousBratAnnotation;
import edu.uab.ccts.nlp.brat.BratConstants;
import edu.uab.ccts.nlp.shared_task.SemEval2015Constants;
import edu.uab.ccts.nlp.uima.annotator.brat.BratParserAnnotator;
import edu.uab.ccts.nlp.uima.annotator.cuiless.AnnotatorStatistics;
import edu.uab.ccts.nlp.uima.collection_readers.BRATCollectionReader;
import edu.uab.ccts.nlp.uima.collection_readers.SemEval2015BratCompareCollectionReader;
import edu.uab.ccts.nlp.uima.annotator.shared_task.SemEval2015ViewCreatorAnnotator;


public class CheckMissingPipelineClient {
	static boolean isTraining = true;

	static String brat_annotation_root = ClientConfiguration.resourceDirPath + "devel/devel_updated_v2/";

	static Hashtable<String,Hashtable<String,HashMultiset<String>>> annotation_results = 
	new Hashtable<String,Hashtable<String,HashMultiset<String>>>();

	public static void main(String... args)
	{
		Collection<File> semFiles = null;
		if(isTraining) {
			System.out.println("Checking missing data in training data set");
			brat_annotation_root = ClientConfiguration.resourceDirPath + "training_clean/";
			semFiles = FileUtils.listFiles(new File(ClientConfiguration.semeval2015_old_train_root),
				SemEval2015Constants.semevalExtensions, true);
		} else {
			semFiles = FileUtils.listFiles(new File(ClientConfiguration.semeval2015_updated_devel_root),
				SemEval2015Constants.semevalExtensions, true);
		}
		System.out.println(brat_annotation_root); System.out.flush();

		Collection<File> inputFiles = FileUtils.listFiles(new File(brat_annotation_root),
				BratConstants.bratExtensions, true);
		System.out.println("Got "+inputFiles.size()+" brat input files for check missing pipeline...");
		System.out.println("Got "+semFiles.size()+" semeval input files for check missing pipeline...");
		apply(inputFiles,semFiles);

	}
	
	public static void apply(Collection<File> files, Collection<File> semfiles) 
	{
		try {
		/*
			CollectionReader reader = CollectionReaderFactory.createReader(
					SemEval2015BratCompareCollectionReader.class,
					BRATCollectionReader.PARAM_FILES,
					files,
					SemEval2015BratCompareCollectionReader.PARAM_SEMEVAL_FILES,
					semfiles
			);

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
		builder.add(SemEval2015ViewCreatorAnnotator.createAnnotatorDescription(ClientConfiguration.semeval2015_old_train_root));
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
			JCas annView = jcas.getView(BratConstants.TEXT_VIEW);
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
