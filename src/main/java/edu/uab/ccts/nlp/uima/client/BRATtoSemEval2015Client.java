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
import edu.uab.ccts.nlp.uima.annotator.shared_task.SemEval2015ParserAnnotator;
import edu.uab.ccts.nlp.uima.annotator.shared_task.MergedCUIlessConsumer;
import edu.uab.ccts.nlp.uima.annotator.shared_task.SemEval2015Task2Consumer;
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
	static String output_directory = "target"+File.separator+"cuiless"+File.separator;
	static boolean roundtrip_test = false; //True if testing ability to roundtrip files without cui-less adjustment

	public static void main(String... args)
	{
		if(args.length>0 && args[0].equalsIgnoreCase("devel")) {
			output_directory+="devel";
			brat_annotation_root = ClientConfiguration.brat_annotated_devel_data;
		} else { 
			output_directory+="train";
			brat_annotation_root = ClientConfiguration.brat_annotated_training_data; 
		}
		if(args.length>1 && args[1].equalsIgnoreCase("roundtrip")) roundtrip_test=true;
		File outdir = new File(output_directory); 
		if(!outdir.exists()){
			if(!outdir.mkdirs()) {
				System.err.println("Can not create needed output directory");
				System.exit(0);
			}
		}
		System.out.println("Writing cuiless annotations for:"+brat_annotation_root); System.out.flush();

		Collection<File> inputFiles = FileUtils.listFiles(new File(brat_annotation_root),
				BratConstants.bratExtensions, true);
		System.out.println("Got "+inputFiles.size()+" brat input files for converting to SemEval 2015 format...");
		apply(inputFiles);

	}

	public static void apply(Collection<File> files){
		{
			try {
				CollectionReaderDescription crd = CollectionReaderFactory.createReaderDescription(
						BRATCollectionReader.class,
						BRATCollectionReader.PARAM_FILES,
						files
						);

				AggregateBuilder builder = new AggregateBuilder();
				builder.add(SemEval2015ViewCreatorAnnotator.createAnnotatorDescription(ClientConfiguration.semeval2015_old_train_root));
				builder.add(SemEval2015ParserAnnotator.getDescription());
				builder.add(BratParserAnnotator.getDescription());
				if(!roundtrip_test) builder.add(MergedCUIlessConsumer.getDescription());
				builder.add(SemEval2015Task2Consumer.getCuilessDescription(output_directory));

				for (JCas jcas : SimplePipeline.iteratePipeline(crd, builder.createAggregateDescription()))
				{}

				//SimplePipeline.runPipeline(reader, builder.createAggregate());
			} catch (ResourceInitializationException e) {
				e.printStackTrace();
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	}
}
