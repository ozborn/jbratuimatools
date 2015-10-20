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
import edu.uab.ccts.nlp.shared_task.SemEval2015Constants;
import edu.uab.ccts.nlp.uima.annotator.brat.BratParserAnnotator;
import edu.uab.ccts.nlp.uima.annotator.cuiless.AnnotatorStatistics;
import edu.uab.ccts.nlp.uima.collection_readers.BRATCollectionReader;
import edu.uab.ccts.nlp.uima.annotator.shared_task.SemEval2015ViewCreatorAnnotator;


/**
 * TODO (Incomplete)
 * Calculates annotator agreement in the devel dataset between multiple annotators
 * but currently just 2 annotators. FIXME - awaiting Maio's results
 * @author ozborn
 *
 */
public class AnnotatorAgreementClient {
	protected static String resourceDirPath = ClientConfiguration.resourceDirPath;
	protected static String brat_annotation_root = resourceDirPath + "devel_updated/";

	
	/**
	 * Calculates annotator agreement between 2 annotators
	 * Does not correct for agreement due to chance as there are 400K+ classes
	 * and the effect will be too small
	 * @param args annotator1_dataset, annotator2_dataset
	 */
	public static void main(String... args)
	{
		if(args.length<2) System.out.println("Need 2 annotator datasets");
		String ann1files = brat_annotation_root+args[0];
		String ann2files = brat_annotation_root+args[1];
		System.out.println(ann1files+" and \n"+ann2files+" being compared"); 
		System.out.flush();
		//Input files are manual files
		Collection<File> a1files = FileUtils.listFiles(new File(ann1files),
				BratConstants.bratExtensions, true);
		Collection<File> a2files = FileUtils.listFiles(new File(ann2files),
				BratConstants.bratExtensions, true);
		Collection<File> semFiles = FileUtils.listFiles(new File(ClientConfiguration.semeval2015_updated_devel_root),
				SemEval2015Constants.semevalExtensions, true);
		System.out.println("Got "+a1files.size()+" annotator1 input files for check missing pipeline...");
		System.out.println("Got "+a2files.size()+" annotator2 input files for check missing pipeline...");
		System.out.println("Got "+semFiles.size()+" semeval input files for checking annotator agreement...");
		a1files.addAll(a2files);
		apply(a1files,semFiles);
			

	}
	
	public static void apply(Collection<File> files, Collection<File> semfiles) 
	{
		try {
    
		/*
		CollectionReaderDescription crd = CollectionReaderFactory.createReaderDescription(
				SemEval2015BratCompareCollectionReader.class,
					BRATCollectionReader.PARAM_FILES,
					files,
					SemEval2015BratCompareCollectionReader.PARAM_SEMEVAL_FILES,
					semfiles
			);
			*/
			CollectionReaderDescription crd = CollectionReaderFactory.createReaderDescription(
					BRATCollectionReader.class,
					BRATCollectionReader.PARAM_FILES,
					files
					);


		AggregateBuilder builder = new AggregateBuilder();
		builder.add(SemEval2015ViewCreatorAnnotator.createAnnotatorDescription(ClientConfiguration.semeval2015_updated_devel_root));
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
		System.out.println(annotatorstats.calculateAgreement()+" annotator agreement");
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
