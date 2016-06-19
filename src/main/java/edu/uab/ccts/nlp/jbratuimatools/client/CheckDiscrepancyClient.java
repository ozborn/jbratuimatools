package edu.uab.ccts.nlp.jbratuimatools.client;

import java.io.File;
import java.util.Collection;

import org.apache.commons.io.FileUtils;
import org.apache.ctakes.typesystem.type.relation.BinaryTextRelation;
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
import edu.uab.ccts.nlp.jbratuimatools.uima.BRATCollectionReader;
import edu.uab.ccts.nlp.jbratuimatools.uima.annotator.BratParserAnnotator;
import edu.uab.ccts.nlp.jbratuimatools.util.AnnotatorStatistics;
import edu.uab.ccts.nlp.shared_task.semeval2015.uima.annotator.SemEval2015ViewCreatorAnnotator;


/**
 * Checking for discrepancies, still using old data (FIXME to work for devel which is new format)
 * @author ozborn
 *
 */
public class CheckDiscrepancyClient {

	static String semeval2015_updated_train_root, semeval2015_old_train_root;
	static String semeval_dir_root,brat_annotation_root;
	static String datasetType="train";


	/**
	 * 
	 * @param args train|devel(train) cuilessdatadirpath(user.home/code/repo/cuilessdata) dropboxpublicdatapath 
	 */
	public static void main(String... args)
	{
		brat_annotation_root = ClientConfiguration.getCuilessDataDirPath() + "training_clean/";
		if(args.length>0){
			if(args[0].equals("train")) {
				brat_annotation_root+="training_clean/";
				semeval_dir_root = ClientConfiguration.getSemeval2015OldTrainRoot(); //FIXME if time, update to new
			} else {
				brat_annotation_root="devel/consensus/";
				datasetType="devel";
				semeval_dir_root = ClientConfiguration.getSemeval2015UpdatedDevelRoot();
			}
			
			
		}
		System.out.println("Using:\n Brat Annotation Root Directory:"+brat_annotation_root+
		"\nSemeval Input Root Directory:"+semeval_dir_root); System.out.flush();
		
		/*
				if(args.length>1) {
					ClientConfiguration.setCuilessDataDirPath(args[0]);
					System.out.println("Set resourceDirPath to:"+ClientConfiguration.cuilessDataDirPath);
					if(args.length>2) {
						ClientConfiguration.setDropboxPublicDataPath(args[1]);
						System.out.println("Set dropboxPublicDataPath to:"+ClientConfiguration.getDropboxPublicDataPath());
					}
				}
		*/
				Collection<File> inputFiles = FileUtils.listFiles(new File(brat_annotation_root),
						BratConstants.bratExtensions, true);
				//System.out.println("Got "+inputFiles.size()+" input files for check missing pipeline...");
				System.out.println("\nBrat Input Files:"+ inputFiles.size());
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
			builder.add(SemEval2015ViewCreatorAnnotator.createAnnotatorDescription(semeval_dir_root));
			builder.add(BratParserAnnotator.getDescription());

			AnnotatorStatistics annotatorstats = new AnnotatorStatistics();
			for (JCas jcas : SimplePipeline.iteratePipeline(crd, builder.createAggregateDescription()))
			{
				JCas annView = jcas.getView(BratConstants.TEXT_VIEW);
				Collection<DiscontinousBratAnnotation> brats = JCasUtil.select(annView, DiscontinousBratAnnotation.class);
				Collection<BinaryTextRelation> rels = JCasUtil.select(annView, BinaryTextRelation.class);
				annotatorstats.add(brats,rels); 
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
