package edu.uab.ccts.nlp.jbratuimatools.client;

import java.io.File;
import java.util.Collection;
import java.util.Hashtable;

import org.apache.commons.io.FileUtils;
import org.apache.uima.analysis_engine.AnalysisEngineDescription;
import org.apache.uima.collection.CollectionReaderDescription;
import org.apache.uima.fit.factory.AggregateBuilder;
import org.apache.uima.fit.factory.CollectionReaderFactory;
import org.apache.uima.fit.pipeline.SimplePipeline;
import org.apache.uima.jcas.JCas;
import org.apache.uima.resource.ResourceInitializationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.HashMultiset;

import edu.uab.ccts.nlp.brat.BratConstants;
import edu.uab.ccts.nlp.jbratuimatools.uima.BRATCollectionReader;
import edu.uab.ccts.nlp.jbratuimatools.uima.annotator.BratParserAnnotator;
import edu.uab.ccts.nlp.jbratuimatools.uima.annotator.MergedCUIlessConsumer;
import edu.uab.ccts.nlp.shared_task.semeval2015.uima.annotator.SemEval2015GoldAttributeParserAnnotator;
import edu.uab.ccts.nlp.shared_task.semeval2015.uima.annotator.SemEval2015Task2Consumer;
import edu.uab.ccts.nlp.shared_task.semeval2015.uima.annotator.SemEval2015ViewCreatorAnnotator;


/**
 * This reads annotated BRAT files with multiple CUIs, and read 
 * in the SemEval2015 old or updated (Dec 3, 2014 version) training data and 
 * overlay the multiple CUI annotations for the CUIless concepts to 
 * generate a new output file suitable for use as training data. Alternatively
 * it can generate a training data set with only the original 'CUI-less'
 * annotations
 * 
 * Basically this generates the final data set for training data, integrating user
 * annotations for CUIless concepts.
 * 
 * This is NOT used for the double annotated DEV dataset (dataset D) which uses a 
 * consensus file to resolve conflicts between the annotators.
 * To generate that, use DevelConsensus2SemEval2015Client
 * 
 * @author ozborn
 *
 */
public class BRATtoSemEval2015Client {
	static Hashtable<String,Hashtable<String,HashMultiset<String>>> annotation_results = 
			new Hashtable<String,Hashtable<String,HashMultiset<String>>>();

	private static final Logger LOG  = LoggerFactory.getLogger(BRATtoSemEval2015Client.class);
	static String brat_annotation_root = ClientConfiguration.brat_annotated_training_data;
	static String output_directory = "target"+File.separator+"cuiless"+
	File.separator+"train"+File.separator;
	static boolean roundtrip_test = false; //True if testing ability to roundtrip files without cui-less adjustment

	public static void main(String... args)
	{
		if(args.length>0 && args[0].equalsIgnoreCase("old")) {
			output_directory+="old";
		} else { 
			output_directory+="updated";
		}
		if(args.length>1 && args[1].equalsIgnoreCase("roundtrip")) roundtrip_test=true;
		File outdir = new File(output_directory); 
		if(!outdir.exists()){
			if(!outdir.mkdirs()) {
				System.err.println("Can not create needed output directory");
				LOG.error("Can not create needed output directory");
				System.exit(0);
			}
		}
		String notice = "Writing cuiless annotations using:"+brat_annotation_root+
		" as input to output directory:"+output_directory;
		System.out.println(notice);System.out.flush();

		Collection<File> inputFiles = FileUtils.listFiles(new File(brat_annotation_root),
				BratConstants.bratExtensions, true);
		LOG.info(notice);
		LOG.info("Got "+inputFiles.size()+" brat input files for converting to SemEval 2015 format...");
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
				AnalysisEngineDescription aed = null;
				if(output_directory.endsWith("old")) {
					aed=SemEval2015ViewCreatorAnnotator.createDescription(
							ClientConfiguration.getSemeval2015OldTrainRoot(),true);
				}
				else if(output_directory.endsWith("updated")) {
					aed=SemEval2015ViewCreatorAnnotator.createDescription(
							ClientConfiguration.getSemeval2015UpdatedTrainRoot(),true);
				}
				builder.add(aed);
				builder.add(SemEval2015GoldAttributeParserAnnotator.getTrainingDescription());
				builder.add(BratParserAnnotator.getDescription());
				String[] noConsensus = null;
				if(!roundtrip_test) builder.add(MergedCUIlessConsumer.getDescription(noConsensus,false));
				builder.add(SemEval2015Task2Consumer.getDescription(output_directory));

				for (@SuppressWarnings("unused") JCas jcas : SimplePipeline.iteratePipeline(crd, builder.createAggregateDescription()))
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
