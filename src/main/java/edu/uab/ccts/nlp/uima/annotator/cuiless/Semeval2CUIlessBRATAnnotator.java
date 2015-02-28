package edu.uab.ccts.nlp.uima.annotator.cuiless;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Hashtable;
import java.util.Iterator;

import org.apache.uima.UimaContext;
import org.apache.uima.analysis_engine.AnalysisEngineDescription;
import org.apache.uima.analysis_engine.AnalysisEngineProcessException;
import org.apache.uima.cas.FSIndex;
import org.apache.uima.jcas.JCas;
import org.apache.uima.jcas.tcas.Annotation;
import org.apache.uima.resource.ResourceInitializationException;
import org.apache.uima.util.Level;
import org.uimafit.component.JCasAnnotator_ImplBase;
import org.uimafit.util.JCasUtil;
import org.apache.ctakes.typesystem.type.textsem.IdentifiedAnnotation;
import org.apache.ctakes.typesystem.type.refsem.OntologyConcept;
import org.apache.ctakes.typesystem.type.refsem.UmlsConcept;
import org.apache.uima.jcas.cas.FSArray;
import org.apache.uima.cas.FeatureStructure;
import org.apache.uima.fit.factory.AnalysisEngineFactory;
import org.cleartk.semeval2015.type.DiseaseDisorder;
import org.cleartk.semeval2015.type.DiseaseDisorderAttribute;
import org.cleartk.semeval2015.type.DisorderSpan;
import org.cleartk.util.ViewUriUtil;

import edu.uab.ccts.nlp.brat.BratConstants;
import edu.uab.ccts.nlp.uima.annotator.brat.BratParserAnnotator;



/**
 * This is a general file output class that writes documents in the format needed for various
 * shared tasks. It also annotates each MetaMapExtendedHit into a SharClef2013annotation
 * @author ozborn
 *
 */
public class Semeval2CUIlessBRATAnnotator extends JCasAnnotator_ImplBase{
	static final String BRAT_FILE_PATH = "BratOutputDirectory";
	private String _dirInputpath, _bratOutputPath;
	boolean _removeNonOverlapping=false;
	private String _edited_doc_text = null;
	Hashtable<String,String> entities = new Hashtable<String,String>();
	String replace_regex = "\n";
	int _relid = 1;

	//Key Disease Identifier, String CUI list (space separated) of suggested applicable CUIs with concept names
	//Hashtable<String,String> diseaseNoteMap = null;
	final String UmlsConnectionString = BratConstants.UMLS_DB_CONNECT_STRING;


	public void initialize(UimaContext aContext) throws ResourceInitializationException {
		try {
			super.initialize(aContext);
			_bratOutputPath = (String) aContext.getConfigParameterValue(BRAT_FILE_PATH);
			aContext.getLogger().log(Level.CONFIG,"Brat Output Directory: "+_bratOutputPath+"\n");

			//UmlsConnectionString = (String) aContext.getConfigParameterValue(ConfigurationSingleton.PARAM_UMLS_DB_URL);
			aContext.getLogger().log(Level.INFO,"Oracle UMLS URL is: "+UmlsConnectionString+"\n");
		} catch (Exception e) {
			throw new ResourceInitializationException(e);
		}
	}



	@Override
	public void process(JCas jcas) throws AnalysisEngineProcessException {
		String uri = ViewUriUtil.getURI(jcas).toString();
		String txtfilename = _bratOutputPath+File.separator+
				uri.substring(uri.lastIndexOf(File.separator),uri.lastIndexOf(".")+1)+"txt";
		_edited_doc_text = jcas.getDocumentText();
		StringBuffer sb = shareClef20142Brat(jcas);
		String annfilename = _bratOutputPath+File.separator+
				uri.substring(uri.lastIndexOf(File.separator),uri.lastIndexOf(".")+1)+"ann";
		try {
			writeBratFile(annfilename,sb);
			writeBratFile(txtfilename,new StringBuffer(_edited_doc_text));
		} catch(IOException ioe) { throw new AnalysisEngineProcessException(ioe); }
	}


	private void writeBratFile(String name,StringBuffer sb) throws IOException {
		if(sb==null) return;
		File f = new File(_bratOutputPath);
		try {
			if(f.exists() && f.canWrite()){
				FileWriter fw = new FileWriter(name);
				this.getContext().getLogger().log(Level.INFO,"Writing file named "+f);
				fw.write(sb.toString().trim());
				fw.close();
			} else {
				this.getContext().getLogger().log(Level.SEVERE,f+" does not exist or not writable");
			}
		} catch (IOException e) { throw e; }

	}



	private StringBuffer shareClef20142Brat(JCas jcas) {
		StringBuffer brat_annotation=null;
		int identifier = 1, prev_identifier=0;
		try {
			brat_annotation = new StringBuffer();
			if( (JCasUtil.select(jcas, DiseaseDisorder.class)).size()==0){
				this.getContext().getLogger().log(Level.WARNING,
						"Could not find DiseaseDisorder with JCasUtil in view "+jcas.getViewName());
			}
			FSIndex<Annotation> dIndex = jcas.getAnnotationIndex(DiseaseDisorder.type);
			Iterator<Annotation> dIter = dIndex.iterator();
			if(!dIter.hasNext()) {
				this.getContext().getLogger().log(Level.WARNING,
						"Could not find DiseaseDisorder disease with annotation index in view "+jcas.getViewName());
			}
			String prev_discontinous_offset =  null;
			//diseaseNoteMap = new Hashtable<String,String>();
			while(dIter.hasNext()) {
				DiseaseDisorder sd = (DiseaseDisorder) dIter.next();
				boolean all_cuiless = true;
				String discontinous_offset = "";
				int[] forbrat = new int[sd.getSpans().size()*2];
				for (int i=0 ;i < sd.getSpans().size();i++){
					DisorderSpan ds = (DisorderSpan) sd.getSpans(i);
					forbrat[i*2] = ds.getBegin();
					forbrat[(i*2)+1] = ds.getEnd();
					discontinous_offset += discontinous_offset+ds.getBegin()+" "+ds.getEnd();
					if(!((i+1)==sd.getSpans().size())) discontinous_offset = discontinous_offset+";";
					if(!ds.getCui().equalsIgnoreCase("CUIless")) { all_cuiless = false; break; }
				}
				if(!all_cuiless) continue;

				String diseaseid = null;
				diseaseid = "T"+identifier;
				brat_annotation.append(diseaseid+"\tDisease ");
				brat_annotation.append(discontinous_offset+"\t");
				brat_annotation.append(getBratFromSpan(_edited_doc_text
						, forbrat));
				brat_annotation.append("\n");
				/*
				String suggestions = addSuggestedCUIs(jcas,sd,forbrat);
				if(suggestions!=null && !suggestions.isEmpty()) {
					diseaseNoteMap.put(diseaseid,suggestions);
					this.getContext().getLogger().log(Level.FINER,
							"Added "+suggestions+" to disease note map");
				}
				*/
				prev_discontinous_offset = discontinous_offset;
				prev_identifier = identifier;
				identifier++;

				for(int j=0;j<sd.getAttributes().size();j++){
					DiseaseDisorderAttribute datt = (DiseaseDisorderAttribute) sd.getAttributes(j);
					identifier = appendDiseaseDisorderAttribute(brat_annotation,
							identifier, datt,datt.getAttributeType(),diseaseid);
				}
			}

			this.getContext().getLogger().log(Level.FINEST,
					brat_annotation.toString());
		} catch (Exception e ) {
			e.printStackTrace();
		}
		//getAnnotatorNotes(brat_annotation); // DO NOT PRE-POPULATE
		brat_annotation.append("\n");
		return brat_annotation;
	}



	private int appendDiseaseDisorderAttribute(StringBuffer brat_annotation,
			int identifier, DiseaseDisorderAttribute theatt,String typename,
			String diseaseid) {
		if(theatt!=null && (theatt.getBegin()!=0 && theatt.getEnd()!=0)){
			String attid = "T"+identifier;
			this.getContext().getLogger().log(Level.FINE,"Set attid:"+attid);
			String attrange = theatt.getBegin()+" "+theatt.getEnd();
			//if(entities.get(attrange)==null) {
			brat_annotation.append(attid+"\t"+typename+" ");
			brat_annotation.append(attrange+"\t");
			brat_annotation.append(theatt.getCoveredText()+"\n");
			entities.put(attrange,attid);
			identifier++;

			//Make the relationship
			brat_annotation.append("R"+_relid+"\tHas"+typename+" Arg1:"+diseaseid
					+" Arg2:"+attid+"\n");
			_relid++;
		}
		return identifier;
	}



	private String getBratFromSpan(String doc,int[] array){
		StringBuffer sb = new StringBuffer();
		for(int i = 0; i<array.length;i=i+2){
			this.getContext().getLogger().log(Level.FINEST,"Span:"+array[i]+":"+array[i+1]);
			if(i==0) {
				String added = doc.substring(array[i], array[i+1]);
				added = replaceNewlineWithSpace(_edited_doc_text,
						added, array[i], array[i+1]);
				sb.append(added);
			}
			else {
				sb.append(" ");
				String added = doc.substring(array[i], array[i+1]);
				added = replaceNewlineWithSpace(_edited_doc_text,
						added, array[i], array[i+1]);
				sb.append(added);
			}
		}
		return sb.toString();
	}



	/** Assume only one newline */
	private String replaceNewlineWithSpace(String doc, String s, int start, int stop){
		int replace = s.indexOf(replace_regex);
		if(s.indexOf(replace_regex)==-1) return s;
		this.getContext().getLogger().log(Level.FINE,
				"Found instance of regex to replace in "+s+" at "+replace);
		s = s.substring(0, replace)+" "+s.substring(replace+1);
		this.getContext().getLogger().log(Level.FINEST,
				"Now s looks like "+s);
		String newdoc = doc.substring(0, start+replace)+" ";
		newdoc += doc.substring(start+replace+1);
		this.getContext().getLogger().log(Level.FINEST,
				"Original doc length:"+doc.length()+" New Doc Length:"+newdoc.length());
		assert(newdoc.length()==doc.length());
		_edited_doc_text=newdoc;
		return s;
	}


	/** Can not seem to use JCasUtil.selectCovering here */
	private String addSuggestedCUIs(JCas jcas, DiseaseDisorder sd, int[] array){
		StringBuffer codes = new StringBuffer();
		codes.append("");
		for(int i = 0; i<array.length;i=i+2){
			int outerleft=-1,outerright=-1;
			for(IdentifiedAnnotation ia : JCasUtil.select(jcas, IdentifiedAnnotation.class)){
				if(ia.getBegin()<= array[i+1] && ia.getEnd()>=array[i]) {
					getContext().getLogger().log(Level.INFO,
							"Potential Annotation("+ia.getBegin()+"-"+ia.getEnd()+"):"+
									ia.getCoveredText()+" for:"+sd.getCoveredText()+"("+array[i]+"-"+array[i+1]+")");
					if(outerleft==-1 && outerright==-1) {
						outerleft = ia.getBegin(); outerright = ia.getEnd();
					}
					if(ia.getBegin()>=outerleft && ia.getEnd()<outerright) {
						if( !(ia.getBegin()==outerleft && ia.getEnd()==outerright)) {
							getContext().getLogger().log(Level.FINE,
									"Ignored annotation as it is subset of larger ");
							continue; //Subset of larger annotation
						}
					}
				} else { continue; }   
				FSArray fsArray = ia.getOntologyConceptArr();
				if(fsArray == null) break;
				for(FeatureStructure featureStructure : fsArray.toArray()) {
					OntologyConcept ontologyConcept = (OntologyConcept) featureStructure;
					if(ontologyConcept.getDisambiguated()==false) {
						getContext().getLogger().log(Level.INFO,
								"Ignored disambiguationed "+ontologyConcept.getCode());
						continue;
					} else {
						getContext().getLogger().log(Level.INFO,
								"Kept disambiguationed "+ontologyConcept.getCode());
					}
					if(ontologyConcept instanceof UmlsConcept) {
						UmlsConcept umlsConcept = (UmlsConcept) ontologyConcept;
						String code = umlsConcept.getCui();
						codes.append(code); codes.append(" ");
						getContext().getLogger().log(Level.INFO,
								"Got CUI for notes! "+ia.getCoveredText());
					} else {
						getContext().getLogger().log(Level.INFO,
								"Not a UMLS concept! "+ontologyConcept.getCode());
						String ucode = ontologyConcept.getCode();
						if(ucode.startsWith("C")) { codes.append(ucode); codes.append(" "); }
					}
				}
			}
		}
		return codes.toString();
	}


	//Code below was likely for pre-populating which annotars didn't like
	/*
	private void getAnnotatorNotes(StringBuffer brat_annotation){
		int annot_counter=1;
		Connection con = null;
		try {
			con = DriverManager.getConnection(UmlsConnectionString);
			PreparedStatement st = con.prepareStatement(_query_sql);
			Iterator<String> didit = diseaseNoteMap.keySet().iterator();
			while(didit.hasNext()){
				String did = didit.next();
				String cuis[] = diseaseNoteMap.get(did).split(" ");
				//for(String cui : cuis){
					for(int i=0;i< cuis.length;i++){
						String cui = cuis[i];
						st.setString(1,cui);
						st.setString(2,cui);
						OracleResultSet resultset = (OracleResultSet) st.executeQuery();
						if(resultset.next()) {
							String goodname = resultset.getString(2);
							String suggest = cui+":"+goodname+" ";
							//String abbrevs = resultset.getString(3);
							if(i==0) {
								brat_annotation.append("#"+annot_counter+
										"\tAnnotatorNotes "+did+"\t");
							}
							brat_annotation.append(suggest);
							if(i==cuis.length-1) brat_annotation.append("\n");
							else { brat_annotation.append("\t"); }
						}
						resultset.close();
					}
					annot_counter++;
			}   
			con.close();
		} catch (Exception e) {
			e.printStackTrace();
			this.getContext().getLogger().log(Level.SEVERE,"Failed to get good concept name to suggest from UMLS");
		}   
	}
	*/
	
	public static AnalysisEngineDescription getDescription() throws ResourceInitializationException {
		return AnalysisEngineFactory.createEngineDescription(Semeval2CUIlessBRATAnnotator.class);
	}
}
