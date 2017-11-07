package edu.uab.ccts.nlp.umls.tools;

import java.util.Hashtable;
import java.util.Set;
import java.util.regex.Pattern;

import org.apache.uima.UIMAFramework;
import org.apache.uima.resource.ResourceInitializationException;
import org.apache.uima.util.Level;
import org.cleartk.semeval2015.type.DiseaseDisorderAttribute;

import edu.uab.ccts.nlp.shared_task.semeval2015.SemEval2015Constants;
import edu.uab.ccts.nlp.shared_task.semeval2015.SemEvalConfig;

public class CleanUtils {

	SemEvalConfig sec;

	public CleanUtils() throws ResourceInitializationException{
		sec = new SemEvalConfig();
	}

	/**
	 * Cleans up errors in original semeval dataset, thecuis is modified
	 * @param index
	 * @param thecuis
	 * @param replacements
	 * @param uimalogger
	 * @param dda
	 */
	public void getCuisFromDiseaseDisorderAttributes(int index,
			Hashtable<String,String> thecuis, DiseaseDisorderAttribute dda) {
		String norm = dda.getNorm().trim();
		Set<String> somecuis = sec.semevalNorm2Cui(dda.getAttributeType(), norm);
		if(!somecuis.isEmpty()) {
			String acui = somecuis.iterator().next();
			thecuis.put("T"+index, acui);
			UIMAFramework.getLogger().log(Level.INFO,"Got "+acui+" out of "+somecuis.size());
		}

		// Legacy Side effect, keep for now
		if(dda.getAttributeType().equals(SemEval2015Constants.BODY_RELATION)) {
			boolean b = isWellFormedCUI(norm.trim());
			if(!b) { //Add in body locations, uniquely identified by having a CUI as a norm
				cleanAttributeNorm(norm, dda);
			}
		} 
		/*
		else if(dda.getAttributeType().equals(SemEval2015Constants.CONDITIONAL_RELATION)) {
			if(norm.equalsIgnoreCase("true"))  thecuis.put("T"+index,"C0278254");
		} else if(dda.getAttributeType().equals(SemEval2015Constants.UNCERTAINTY_RELATION)) {
			if(norm.equalsIgnoreCase("yes"))  thecuis.put("T"+index,"C87131");
		} else if(dda.getAttributeType().equals(SemEval2015Constants.GENERIC_RELATION)) {
			if(norm.equalsIgnoreCase("yes")||norm.equalsIgnoreCase("true"))  thecuis.put("T"+index,"C0277545");
		} else if(dda.getAttributeType().equals(SemEval2015Constants.NEGATION_RELATION)) {
			if(norm.equalsIgnoreCase("yes"))  thecuis.put("T"+index,"C0205160");
		} else {
			String attcui = replacements.getProperty(norm);
			if(attcui!=null) thecuis.put("T"+index,attcui.trim());
			else uimalogger.log(Level.WARNING,"No CUI for:"+norm);
		}
		 */
	}


	/** Adds missing leading 'C' to CUIs, normalizes spelling of CUI-less */
	public void cleanAttributeNorm(String norm,DiseaseDisorderAttribute dda){
		boolean bad = Pattern.matches("\\d\\d\\d\\d\\d\\d\\d*",norm.trim());	
		if(bad){
			UIMAFramework.getLogger().log(Level.WARNING,"Fixed CUI without C:"+norm);
			dda.setNorm("C"+norm);
		} else {
			if(norm.equalsIgnoreCase("cuiless")|| norm.equalsIgnoreCase("cui-less")) {
				dda.setNorm(SemEval2015Constants.CUILESS);
			}
		}
	}


	public static boolean isWellFormedCUI(String cuistring) {
		if(Pattern.matches("C\\d\\d\\d\\d\\d\\d\\d*",cuistring)) return true;
		if(cuistring.trim().equals(SemEval2015Constants.CUILESS)) return true;
		return false;
	}
}  

