package edu.uab.nlp.jbratuimatools.test.integration;

import static org.junit.Assert.*;

import java.util.Set;

import org.junit.BeforeClass;
import org.junit.Test;

import edu.uab.ccts.nlp.jbratuimatools.util.AnnotatorStatistics;
import edu.uab.ccts.nlp.umls.tools.UMLSTools;

public class AnnotatorStatisticsIT {
	AnnotatorStatistics as = new AnnotatorStatistics();
	static String umlsConnectionString;

	@BeforeClass
	public static void setUpBeforeClass() throws Exception {
		umlsConnectionString = UMLSTools.getUmlsConnectionString();
		System.out.println("umlsJdbcString="+umlsConnectionString);
	}

	@Test
	public void testGetCuiSnomedAncestors() {
		if(umlsConnectionString==null) fail("No UMLS database specified in src/main/resources/umlsDB.properties to test against...");
		Set<String> test = as.getCuiSnomedAncestors("C0011847",UMLSTools.getUmlsConnectionString());
		assertTrue(test.size()>1); //Two types of diabetes
	}

	@Test
	public void testGetCuisFromSnomedAuis() {
		if(umlsConnectionString==null) fail("No UMLS database specified in src/main/resources/umlsDB.properties to test against...");
		Set<String> auis = as.getCuiSnomedAncestors("C0011847",UMLSTools.getUmlsConnectionString());
		System.out.println("All Diabetes AUIs "+auis);
		Set<String> cuis = as.getCuisFromSnomedAuis(auis,UMLSTools.getUmlsConnectionString());
		System.out.println("All Diabetes Ancestor CUIs "+cuis);
		assertTrue(cuis.size()>0);
	}

}
