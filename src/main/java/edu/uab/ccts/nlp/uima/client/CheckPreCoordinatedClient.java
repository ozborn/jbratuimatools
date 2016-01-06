package edu.uab.ccts.nlp.uima.client;

import java.io.File;
import java.util.Collection;

import org.apache.commons.io.FileUtils;
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
import edu.uab.ccts.nlp.uima.annotator.brat.BratParserAnnotator;
import edu.uab.ccts.nlp.uima.annotator.cuiless.AnnotatorStatistics;
import edu.uab.ccts.nlp.uima.collection_readers.BRATCollectionReader;
import edu.uab.ccts.nlp.uima.annotator.shared_task.SemEval2015ViewCreatorAnnotator;


/**
 * Checks for pre-coordinated annotations among the CUI-less annotations (single mappings)
 * Provides a suggested post-coordinated if that text was post-coordinated elsewhere
 * Sorts by most frequent pre-coordinated CUIs to least frequent
 * Used to create set of CUIs for Matt Neu/Maio Danila to semantically decompose
 * @author ozborn
 *
 */
public class CheckPreCoordinatedClient {

	static String semeval2015_updated_train_root, semeval2015_old_train_root, semeval_dir_root,brat_annotation_root;



	public static void main(String... args)
	{
		brat_annotation_root = ClientConfiguration.resourceDirPath + "training_clean/";
		semeval_dir_root = ClientConfiguration.semeval2015_old_train_root;
		System.out.println("Using:\n Brat Annotation Root Directory:"+brat_annotation_root+
				"\nSemeval Input Root Directory:"+semeval_dir_root); System.out.flush();
				if(args.length>0) {
					ClientConfiguration.resourceDirPath = args[0];
					System.out.println("Set resourceDirPath to:"+ClientConfiguration.resourceDirPath);
					if(args.length>1) {
						ClientConfiguration.dropboxPublicDataPath = args[1];
						System.out.println("Set dropboxPublicDataPath to:"+ClientConfiguration.dropboxPublicDataPath);
					}
				}
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
				annotatorstats.add(brats);
			}
			annotatorstats.print(annotatorstats.getAnnotatorStats());
			System.out.println("Annotator stats:"+annotatorstats.getAnnotatorStats());
			annotatorstats.printPreCoordinated();


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
