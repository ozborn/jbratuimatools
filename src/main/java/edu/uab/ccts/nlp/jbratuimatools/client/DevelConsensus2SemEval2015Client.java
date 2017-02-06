package edu.uab.ccts.nlp.jbratuimatools.client;

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Collection;
import java.util.Hashtable;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.commons.io.FileUtils;
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
 * 
 * This should read in the annotated BRAT files with multiple CUIs, and read in the SemEval2015
 * updated (Dec 3, 2014 version) devel data and overlay the multiple CUI annotations for
 * the CUIless concepts to generate a new output file suitable for use as devel/test data.
 * 
 * Basically this generates the final data set for devel data, integrating:
 * 1) User BRAT annotations from Matt or Maio for CUIless concepts where they agree
 * 2) Consensus annotation from Matt and Maio where they initially disagreed
 * 
 * For the training set CUI-less concepts see BRATtoSemEval2015Client
 * 
 * @author ozborn
 *
 */
public class DevelConsensus2SemEval2015Client {
	static Hashtable<String,Hashtable<String,HashMultiset<String>>> annotation_results = 
			new Hashtable<String,Hashtable<String,HashMultiset<String>>>();

	private static final Logger LOG  = LoggerFactory.getLogger(DevelConsensus2SemEval2015Client.class);
	static String brat_annotation_root = null;
	static String output_directory = "target"+File.separator+"cuiless"+File.separator;
	static String tab_consensus_filepath=System.getProperty("user.home")+
				"/code/repo/cuilessdata/analysis/SemEvalCUIlessUpdates2016ab.txt";
	static boolean roundtrip_test = false; //True if testing ability to roundtrip files without cui-less adjustment
	static boolean separate_negations=true;
	static String[] consensusArray;

	public static void main(String... args)
	{
		output_directory+="devel/consensus";
		brat_annotation_root = ClientConfiguration.brat_annotated_devel_data;
		
		if(args.length>0) tab_consensus_filepath=args[0];
		if(args.length>1) {
			if(args[1].indexOf("include_negated_cuis")!=-1) separate_negations=false;
		}
		if(args.length>2 && args[2].equalsIgnoreCase("roundtrip")) roundtrip_test=true;
		File outdir = new File(output_directory); 
		if(!outdir.exists()){
			if(!outdir.mkdirs()) {
				System.err.println("Can not create needed output directory");
				System.exit(0);
			}
		}
		LOG.info("Writing cuiless annotations for:"+brat_annotation_root);
		LOG.info("Usng consensus annotation file:"+tab_consensus_filepath);
		
		
		try (Stream<String> stream = Files.lines(Paths.get(tab_consensus_filepath),Charset.forName("Cp1252"))) {
			List<String> consensusList = stream
					//.filter(line -> line.startsWith("DISAGREE")).collect(Collectors.toList());
					.filter(line -> line.startsWith("UPDATE_DISORDER"))
					.collect(Collectors.toList());
			consensusArray = new String[consensusList.size()];
			consensusArray = consensusList.toArray(consensusArray);
			LOG.info("Pulled "+consensusList.size()+" consensus annotations");
		} catch (IOException ioe) { ioe.printStackTrace(); }

		Collection<File> inputFiles = FileUtils.listFiles(new File(brat_annotation_root),
				BratConstants.bratExtensions, true);
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
				builder.add(SemEval2015ViewCreatorAnnotator.createDescription(ClientConfiguration.getSemeval2015UpdatedDevelRoot(),true));
				builder.add(SemEval2015GoldAttributeParserAnnotator.getTrainingDescription());
				builder.add(BratParserAnnotator.getDescription());
				if(!roundtrip_test) builder.add(MergedCUIlessConsumer.getDescription(consensusArray,separate_negations));
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
