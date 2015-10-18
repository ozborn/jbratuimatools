package edu.uab.ccts.nlp.uima.client;

import java.io.File;
import java.util.Collection;
import java.util.Hashtable;

import org.apache.commons.io.FileUtils;
import org.apache.uima.collection.CollectionReaderDescription;
import org.apache.uima.fit.factory.CollectionReaderFactory;
import org.apache.uima.fit.pipeline.SimplePipeline;
import org.apache.uima.jcas.JCas;
import org.apache.uima.resource.ResourceInitializationException;
import org.uimafit.factory.AggregateBuilder;

import com.google.common.collect.HashMultiset;

import edu.uab.ccts.nlp.brat.BratConstants;
import edu.uab.ccts.nlp.uima.annotator.brat.BratParserAnnotator;
import edu.uab.ccts.nlp.uima.collection_readers.BRATCollectionReader;
import edu.uab.ccts.nlp.uima.collection_readers.SemEval2015BratCompareCollectionReader;
import edu.uab.ccts.nlp.uima.annotator.shared_task.SemEval2015Constants;
import edu.uab.ccts.nlp.uima.annotator.shared_task.SemEval2015ParserAnnotator;
import edu.uab.ccts.nlp.uima.annotator.shared_task.MergedCUIlessConsumer;
import edu.uab.ccts.nlp.uima.annotator.shared_task.SemEval2015ViewCreatorAnnotator;


/**
 * This should read in the annotated BRAT files with multiple CUIs, and read in the SemEval2015
 * updated (Dec 3, 2014 version) training and test data and overlay the multiple CUI annotations for
 * the CUIless concepts to generate a new output file suitable for use as training/test data.
 * 
 * Basically this generates the final data set for training data, integrating user
 * annotations for CUIless concepts.
 * 
 * For now it can use the old dataset for the training data, but the updated set for
 * the devel/testing data
 * 
 * @author ozborn
 *
 */
public class BRATtoSemEval2015Client {
	static Hashtable<String,Hashtable<String,HashMultiset<String>>> annotation_results = 
	new Hashtable<String,Hashtable<String,HashMultiset<String>>>();
	
	static String brat_annotation_root = null;

	public static void main(String... args)
	{
		if(args.length>0 && args[0].equalsIgnoreCase("devel")) {
			brat_annotation_root = ClientConfiguration.brat_annotated_devel_data;
		} else { brat_annotation_root = ClientConfiguration.brat_annotated_training_data; }

		System.out.println("Writing final annotation for:"+brat_annotation_root); System.out.flush();

		Collection<File> inputFiles = FileUtils.listFiles(new File(brat_annotation_root),
				BratConstants.bratExtensions, true);
		Collection<File> semFiles = FileUtils.listFiles(new File(ClientConfiguration.semeval2015_updated_train_root),
				SemEval2015Constants.semevalExtensions, true);
		System.out.println("Got "+inputFiles.size()+" brat input files for converting to SemEvall 2015 format...");
		System.out.println("Got "+semFiles.size()+" semeval input files for reference...");
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
		builder.add(SemEval2015ViewCreatorAnnotator.createAnnotatorDescription(ClientConfiguration.semeval2015_old_train_root));
		builder.add(SemEval2015ParserAnnotator.getDescription());
		builder.add(BratParserAnnotator.getDescription());
		//Need add annotator to do the merging?
		builder.add(MergedCUIlessConsumer.getDescription());
		for (JCas jcas : SimplePipeline.iteratePipeline(crd, builder.createAggregateDescription()))
		{}
	
		//SimplePipeline.runPipeline(reader, builder.createAggregate());
		} catch (ResourceInitializationException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
}
