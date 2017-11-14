package edu.uab.nlp.jbratuimatools.test.integration;

import static org.junit.Assert.*;

import java.io.FileInputStream;
import java.util.Properties;
import java.util.Set;

import org.junit.BeforeClass;
import org.junit.Test;

import edu.uab.ccts.nlp.jbratuimatools.util.AnnotatorStatistics;

public class AnnotatorStatisticsIT {
	AnnotatorStatistics as = new AnnotatorStatistics();
	static Properties props = new Properties();

	@BeforeClass
	public static void setUpBeforeClass() throws Exception {
		try(FileInputStream in = new FileInputStream("src/test/resources/integrationTest.properties")){
			props.load(in);
		} catch (Exception e) { e.printStackTrace(); }
		System.out.println("umlsJdbcString="+props.getProperty("umlsJdbcString"));
	}

	@Test
	public void testGetCuiSnomedAncestors() {
		Set<String> test = as.getCuiSnomedAncestors("C0011847",props.getProperty("umlsJdbcString"));	
		assertTrue(test.size()>1); //Two types of diabetes
	}

	@Test
	public void testGetCuisFromSnomedAuis() {
		Set<String> auis = as.getCuiSnomedAncestors("C0011847",props.getProperty("umlsJdbcString"));
		System.out.println("All Diabetes AUIs "+auis);
		Set<String> cuis = as.getCuisFromSnomedAuis(auis,props.getProperty("umlsJdbcString"));
		System.out.println("All Diabetes Ancestor CUIs "+cuis);
		assertTrue(cuis.size()>0);
	}

}
