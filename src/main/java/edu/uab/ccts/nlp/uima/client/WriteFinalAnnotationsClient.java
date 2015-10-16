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
 * updated (Dec 3, 2014 version) training data and overlay the multiple CUI annotations for
 * the CUIless concepts to generate a new output file suitable for use as training data.
 * 
 * Basically this generates the final data set for training data, integrating user
 * annotations for CUIless concepts.
 * @author ozborn
 *
 */
public class WriteFinalAnnotationsClient {
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
		Collection<File> semFiles = FileUtils.listFiles(new File(semeval2015_updated_train_root),
				semevalExtensions, true);
		//System.out.println("Got "+inputFiles.size()+" input files for check missing pipeline...");
		System.out.println("Got "+semFiles.size()+" semeval input files for check missing pipeline...");
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
		builder.add(SemEval2015ViewCreatorAnnotator.createAnnotatorDescription(semeval2015_old_train_root));
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
