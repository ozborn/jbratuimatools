package edu.uab.ccts.nlp.uima.collection_readers;

import org.apache.commons.io.FileUtils;
import org.apache.uima.UimaContext;
import org.apache.uima.cas.CASException;
import org.apache.uima.collection.CollectionException;
import org.apache.uima.jcas.JCas;
import org.apache.uima.resource.ResourceInitializationException;
import org.apache.uima.util.Progress;
import org.apache.uima.util.ProgressImpl;
import org.cleartk.util.ViewUriUtil;
import org.apache.uima.fit.component.JCasCollectionReader_ImplBase;
import org.apache.uima.fit.descriptor.ConfigurationParameter;

import edu.uab.ccts.nlp.brat.BratConstants;
import edu.uab.ccts.nlp.uima.client.CheckMissingPipelineClient;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Hashtable;
import java.util.List;

public class BRATCollectionReader extends JCasCollectionReader_ImplBase
{

	public static final String PARAM_FILES = "brat_files";
	@ConfigurationParameter(
			name = PARAM_FILES,
			description = "points to a collection of files including annotation.conf and BRAT (.ann) files")
	protected Collection<File> files;

	public static final String PARAM_FILE_PATH = "brat_file_path";
	@ConfigurationParameter(
			name = PARAM_FILE_PATH,
			description = "points to a data directory containing annotation.conf and BRAT (.ann) and text files",
			defaultValue = ".")
	protected String brat_file_path;

	protected List<File> annFiles = new ArrayList<File>();
	protected List<File> textFiles = new ArrayList<File>();
	protected Hashtable<String,File> hashedConfigFiles = 
			new Hashtable<String,File>();
	protected int totalFiles = 0;

	public void initialize(UimaContext context) throws ResourceInitializationException
	{
		if(files==null || files.size()==0) {
			//Attempt to get from file path
		files = FileUtils.listFiles(new File(brat_file_path),
				CheckMissingPipelineClient.bratExtensions, true);
		}
		System.out.println("Total file count for BRATCollectionReader is "+files.size());
		
		
		//Files can be text files (hopefully with associated ann files) or the config file
		for (File f : files)
		{
			if(f.getName().equals(BratConstants.BRAT_CONFIG_FILE_NAME)) {
				hashedConfigFiles.put(f.getParent(), f);
			} else {
				textFiles.add(f);
				String annpath = f.getPath().replace(
						BratConstants.BRAT_TEXT_FILE_EXTENSION, 
						BratConstants.BRAT_ANN_FILE_EXTENSION
				);
				File annFile = new File(annpath);
				if(annFile.exists()) { annFiles.add(annFile); }
			}
		}
		totalFiles = textFiles.size();
	}

	public void getNext(JCas jCas) throws IOException, CollectionException
	{
		JCas annView,textView;
		JCas configView;
		try
		{
			annView = jCas.createView(BratConstants.ANN_VIEW);
			configView = jCas.createView(BratConstants.CONFIG_VIEW);
			textView = jCas.createView(BratConstants.TEXT_VIEW);
		} catch (CASException ce)
		{
			throw new CollectionException(ce);
		}

		File annFile = annFiles.remove(0);
		String annotations = FileUtils.readFileToString(annFile);
		//System.out.println("Got ann file of size:"+annotations.length());
		File textFile = textFiles.remove(0);
		String fileText = FileUtils.readFileToString(textFile);
		String configdirpath = textFile.getParent();
		String configtext = FileUtils.readFileToString(hashedConfigFiles.get(configdirpath));

		//jCas.setDocumentText(fileText);
		ViewUriUtil.setURI(textView, textFile.toURI());
		textView.setDocumentText(fileText);
		annView.setDocumentText(annotations);
		configView.setDocumentText(configtext);
	}

	public boolean hasNext() throws IOException, CollectionException
	{
		return (annFiles.size() > 0);
	}

	public Progress[] getProgress()
	{
		return new Progress[]{
				new ProgressImpl(totalFiles - annFiles.size(),
						totalFiles,
						Progress.ENTITIES)};
	}
	
	
}
