package edu.uab.ccts.nlp.jbratuimatools.client;

import java.io.File;
import java.io.FileWriter;
import java.io.Writer;
import java.util.Collection;

import org.apache.commons.io.FileUtils;
import org.apache.uima.collection.CollectionReaderDescription;
import org.apache.uima.fit.factory.AggregateBuilder;
import org.apache.uima.fit.factory.CollectionReaderFactory;
import org.apache.uima.fit.pipeline.SimplePipeline;
import org.apache.uima.jcas.JCas;
import org.apache.uima.resource.ResourceInitializationException;

import edu.uab.ccts.nlp.shared_task.semeval2015.SemEval2015Constants;
import edu.uab.ccts.nlp.shared_task.semeval2015.uima.SemEval2015CollectionReader;
import edu.uab.ccts.nlp.shared_task.semeval2015.uima.annotator.SemEval2015AttributeCounter;
import edu.uab.ccts.nlp.shared_task.semeval2015.uima.annotator.SemEval2015GoldAttributeParserAnnotator;


/**
 * Reads in SemEval Format (updated to handle more CUI disorders) and prints out some statistics
 * concepts. This will generate Table 4 from the paper. Just need to:
 * 1) Concatenate pipe files
 * 2) Remove headers in vim via %s/DocID\(.\)*\n//g
 * 3) Put in Excel
 * 
 * @author ozborn
 *
 */
public class SemevalStatisticsClient {
	//protected static String resourceDirPath = "/Users/ozborn/code/repo/cuilessdata/devel/consensus_neg_excluded_2016ab/";
	static String resourceDirPath = "/Users/ozborn/code/repo/cuilessdata/CUILESS/training_fixed_semeval_cuiless_only";
	static String fileName=null;


	/**
	 * @param args
	 */
	public static void main(String... args)
	{
		if(args[0]==null) {
			System.err.println("Input directory with semeval datda to calculate stats on, ex)"+
					resourceDirPath);
		} else resourceDirPath = args[0];
		if(args[1]==null) {
			fileName="allCounts.txt";
		} else fileName=args[1];
		System.out.println("Using files from resourceDirPath:"+resourceDirPath
				+"\nOutputing results to:"+fileName);
		String st[] = {SemEval2015Constants.SEMEVAL_PIPED_EXTENSION};
		Collection<File> semFiles = FileUtils.listFiles(new File(resourceDirPath),
				st, true);
		System.out.println("Got "+semFiles.size()+" semeval input files...");

		try {
			File out = new File(resourceDirPath);
			if (!out.exists())
			{
				if(!out.mkdir()) {
					System.out.println("Could not make directory " + resourceDirPath);
					System.exit(0);
				}
			} else { System.out.println(resourceDirPath+" pre-exists, make sure clean..."); }
			fileName = resourceDirPath+File.separator+fileName;
			out = new File(fileName);
			out.createNewFile();
			try (Writer allwriter = new FileWriter(fileName)){
				allwriter.write("DocID|Spans|Disorder|Negation|Subject|Uncertainity|Course|Severity|Conditional|Generic|Body|\n");
			}
		} catch (Exception e) { e.printStackTrace(); }
		apply(resourceDirPath,semFiles);
	}


	public static void apply(String outputdir, Collection<File> files) 
	{
		try {

			CollectionReaderDescription crd = CollectionReaderFactory.createReaderDescription(
					SemEval2015CollectionReader.class,
					SemEval2015CollectionReader.PARAM_FILES,
					files 
					);

			AggregateBuilder builder = new AggregateBuilder();
			builder.add(SemEval2015GoldAttributeParserAnnotator.getTrainingDescription()); //Training, still wants cui file
			builder.add(SemEval2015AttributeCounter.getDescription(
					"target"+File.separator+"Semeval2015CountResults",fileName));

			for (@SuppressWarnings("unused") JCas jcas : SimplePipeline.iteratePipeline(crd, builder.createAggregateDescription()))
			{
				//JCas annView = jcas.getView(BratConstants.ANN_VIEW);
				//Collection<DiscontinousBratAnnotation> brats = JCasUtil.select(annView, DiscontinousBratAnnotation.class);
			}

		} catch (ResourceInitializationException e) {
			e.printStackTrace();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
}
