package edu.uab.ccts.nlp.umls.tools;

import java.util.Hashtable;
import java.util.Properties;
import java.util.regex.Pattern;

import org.apache.uima.util.Level;
import org.apache.uima.util.Logger;
import org.cleartk.semeval2015.type.DiseaseDisorderAttribute;

public class CleanUtils {


	public CleanUtils(){}

	/**
	 * Cleans up errors in original semeval dataset, thecuis is modified
	 * @param index
	 * @param thecuis
	 * @param replacements
	 * @param uimalogger
	 * @param dda
	 */
	public void getCuisFromDiseaseDisorderAttributes(int index,
			Hashtable<String,String> thecuis,Properties replacements, Logger uimalogger,
			DiseaseDisorderAttribute dda) {
		String norm = dda.getNorm().trim();
		if(dda.getAttributeType().equals("BodyLocation")) {
			boolean b = Pattern.matches("C\\d\\d\\d\\d\\d\\d\\d*",norm.trim());
			if(b) { //Add in body locations, uniquely identified by having a CUI as a norm
				thecuis.put("T"+index,norm); 
			} else { //Fix body location errors
				boolean bad = Pattern.matches("\\d\\d\\d\\d\\d\\d\\d*",norm.trim());	
				if(bad){
					uimalogger.log(Level.WARNING,"Fixed CUI without C:"+norm);
					dda.setNorm("C"+norm);
					thecuis.put("T"+index,"C"+norm);
				} else {
					if(norm.equalsIgnoreCase("cuiless")|| norm.equalsIgnoreCase("cui-less")) {
						dda.setNorm("CUI-less");
						thecuis.put("T"+index, "CUI-less");
					} else if(norm.equals("NULL")){
						//Whole body, just ignore
					} else System.err.println("Failed to get a body location cui for:"+norm);
				}
			}
		} else if(dda.getAttributeType().equals("Conditional")) {
			if(norm.equalsIgnoreCase("true"))  thecuis.put("T"+index,"C0278254");
		} else if(dda.getAttributeType().equals("Uncertainity")) {
			if(norm.equalsIgnoreCase("yes"))  thecuis.put("T"+index,"C0087130");
		} else if(dda.getAttributeType().equals("Generic")) {
			if(norm.equalsIgnoreCase("yes"))  thecuis.put("T"+index,"C0277545");
		} else if(dda.getAttributeType().equals("Negation")) {
			if(norm.equalsIgnoreCase("yes"))  thecuis.put("T"+index,"C0205160");
		} else {
			String attcui = replacements.getProperty(norm);
			if(attcui!=null) thecuis.put("T"+index,attcui.trim());
			else uimalogger.log(Level.WARNING,"No CUI for:"+norm);
		}
	}
}  

