package edu.uab.ccts.nlp.jbratuimatools.client;

import java.io.File;
import java.util.Collection;
import java.util.Hashtable;

import org.apache.commons.io.FileUtils;
import org.apache.ctakes.typesystem.type.relation.BinaryTextRelation;
import org.apache.uima.collection.CollectionReaderDescription;
import org.apache.uima.fit.factory.CollectionReaderFactory;
import org.apache.uima.fit.pipeline.SimplePipeline;
import org.apache.uima.fit.util.JCasUtil;
import org.apache.uima.jcas.JCas;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.uimafit.factory.AggregateBuilder;

import com.google.common.collect.HashMultiset;

import brat.type.DiscontinousBratAnnotation;
import edu.uab.ccts.nlp.brat.BratConstants;
import edu.uab.ccts.nlp.jbratuimatools.uima.BRATCollectionReader;
import edu.uab.ccts.nlp.jbratuimatools.uima.annotator.BratParserAnnotator;
import edu.uab.ccts.nlp.jbratuimatools.util.AnnotatorStatistics;
import edu.uab.ccts.nlp.shared_task.semeval2015.uima.annotator.SemEval2015ViewCreatorAnnotator;

/**
 * This should check for missing annotations in both the train and devel/testing
 * data sets.
 * @author ozborn
 *
 */
public class CheckMissingPipelineClient {

	private static final Logger LOG  = LoggerFactory.getLogger(CheckMissingPipelineClient.class);
	static boolean isTraining = true;

	static String brat_annotation_root = ClientConfiguration.cuilessDataDirPath + "devel/devel_updated_v2/";

	static Hashtable<String,Hashtable<String,HashMultiset<String>>> annotation_results = 
			new Hashtable<String,Hashtable<String,HashMultiset<String>>>();

	public static void main(String... args)
	{
		if(args.length!=1) {
			LOG.warn("Provide argument train/devl to indicate where to check for missing annotations");
			System.exit(0);
		} else {
			if(args[0].equalsIgnoreCase("train")) {
				LOG.info("Checking missing data in training data set");
				brat_annotation_root = ClientConfiguration.cuilessDataDirPath + "training_clean/";
			} else {
				isTraining=false;
				brat_annotation_root = ClientConfiguration.cuilessDataDirPath + "devel/mdanila/";
			}
		}
		LOG.info("Checking for missing annotations in:"+brat_annotation_root);

		Collection<File> inputFiles = FileUtils.listFiles(new File(brat_annotation_root),
				BratConstants.bratExtensions, true);
		LOG.info("Got "+inputFiles.size()+" brat input files for check missing pipeline...");
		apply(inputFiles);

	}

	public static void apply(Collection<File> files)
	{
		try {

			CollectionReaderDescription crd = CollectionReaderFactory.createReaderDescription(
					BRATCollectionReader.class,
					BRATCollectionReader.PARAM_FILES,
					files
					);


			AggregateBuilder builder = new AggregateBuilder();
			if(isTraining) {
				builder.add(SemEval2015ViewCreatorAnnotator.createAnnotatorDescription(ClientConfiguration.getSemeval2015OldTrainRoot()));
			} else {
				builder.add(SemEval2015ViewCreatorAnnotator.createAnnotatorDescription(ClientConfiguration.getSemeval2015UpdatedDevelRoot()));
			}
			builder.add(BratParserAnnotator.getDescription());

			AnnotatorStatistics annotatorstats = new AnnotatorStatistics();
			for (JCas jcas : SimplePipeline.iteratePipeline(crd, builder.createAggregateDescription()))
			{
				JCas annView = jcas.getView(BratConstants.TEXT_VIEW);
				Collection<DiscontinousBratAnnotation> brats = JCasUtil.select(annView, DiscontinousBratAnnotation.class);
				//String[] pathbits = (ViewUriUtil.getURI(annView)).toString().split(File.separator);
				//System.out.println("Got "+brats.size()+" brat annotations for "+pathbits[pathbits.length-1]);
				Collection<BinaryTextRelation> rels = JCasUtil.select(annView, BinaryTextRelation.class);
				annotatorstats.add(brats,rels); 
			}
			annotatorstats.print();
			annotatorstats.printSemanticTypeDistribution();
			//SimplePipeline.runPipeline(reader, builder.createAggregate());
		} catch (Exception e) {
			e.printStackTrace();
		}	
	}
}
