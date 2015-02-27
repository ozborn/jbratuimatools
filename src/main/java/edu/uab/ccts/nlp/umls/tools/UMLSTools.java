package edu.uab.ccts.nlp.umls.tools;

import java.io.File;
import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;

import oracle.jdbc.OracleResultSet;

import org.apache.commons.io.FileUtils;

import java.sql.DriverManager;

public class UMLSTools {
	
	static String query_file_path=
	"/Users/ozborn/code/workspaces/brat_workspace/jbratuimatools/src/main/resources/sql/oracle/select_best_umls_concept_name.sql";
	static String query_stypes_file_path=
	"/Users/ozborn/code/workspaces/brat_workspace/jbratuimatools/src/main/resources/sql/oracle/select_stypes_sab.sql";
	static String query_sql, query_stypes_sql;
	static Connection con;
	
	static {
		File f = new File(query_file_path);
		File f2 = new File(query_stypes_file_path);
		try {
			query_sql = FileUtils.readFileToString(f,"UTF-8");
			query_stypes_sql = FileUtils.readFileToString(f2,"UTF-8");
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	
	public UMLSTools(String connection_string) throws Exception {
		con = DriverManager.getConnection(connection_string);
	}
	
	
	/**
	 * Returns the best concept name as defined by UMLS precedence for an input
	 * CUI
	 * @param cui
	 * @return String 
	 */
	public static String fetchBestConceptName(String cuis, String connection_string) {
		String goodname="";
		try {
			con = DriverManager.getConnection(connection_string); //Yes, I know this is slow...
			PreparedStatement st = con.prepareStatement(query_sql);
			String[] cuiarray = cuis.split(",");
			for(int i=0;i<cuiarray.length;i++){
				st.setString(1,cuiarray[i]);
				st.setString(2,cuiarray[i]);
				OracleResultSet resultset = (OracleResultSet) st.executeQuery();
				if(resultset.next()) {
					String abbrevs = resultset.getString(3);
					if(i==0) goodname = resultset.getString(2);
					else goodname += "||"+resultset.getString(2);
					goodname+="("+abbrevs+")";
				}
				resultset.close();
			}
			con.close();
		} catch (Exception e) {
			e.printStackTrace();
		}
		return goodname;
		
	}

	
		
	/**
	 * Returns the best concept name as defined by UMLS precedence for an input
	 * CUI
	 * @param cuis - collection of CUIs
	 * @return String array, 1st value CUI, 2nd value best name, 3rd value semantic type, 4th value vocabulary
	 */
	public static String[] fetchCUIInfo(String cuis, String connection_string) {
		String[] array = new String[4];
		array[0]=cuis;
		String goodname="";
		String abbrevs="";
		String vocab="";
		try {
			con = DriverManager.getConnection(connection_string); //Yes, I know this is slow...
			PreparedStatement st = con.prepareStatement(query_stypes_sql);
			String[] cuiarray = cuis.split(",");
			for(int i=0;i<cuiarray.length;i++){
				st.setString(1,cuiarray[i]);
				st.setString(2,cuiarray[i]);
				OracleResultSet resultset = (OracleResultSet) st.executeQuery();
				if(resultset.next()) {
					if(i==0) abbrevs = resultset.getString(3);
					else abbrevs += "|"+resultset.getString(3);
					if(i==0) goodname = resultset.getString(2);
					else goodname += "||"+resultset.getString(2);
					if(i==0) vocab = resultset.getString(4);
					else vocab += "||"+resultset.getString(4);
					//vocab+="("+abbrevs+")";
				}
				resultset.close();
			}
			con.close();
		} catch (Exception e) {
			e.printStackTrace();
		}
		array[1] = goodname;
		array[2] = abbrevs;
		array[3] = vocab;
		return array;
		
	}

	
	
	
}
