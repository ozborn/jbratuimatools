package edu.uab.ccts.nlp.uima.client;


public class ClientConfiguration {
	static String resourceDirPath = System.getProperty("user.home")+"/code/repo/cuilessdata/";
	static String dropboxPublicDataPath = System.getProperty("user.home")+"/Dropbox/Public_NLP_Data/";
	
	static String semeval2015_updated_train_root = dropboxPublicDataPath+
			"semeval-2015-task-14_updated/data/train";
	static String semeval2015_updated_devel_root = dropboxPublicDataPath+
			"semeval-2015-task-14_updated/data/devel/discharge";
	static String semeval2015_old_train_root = dropboxPublicDataPath+
			"semeval-2015-task-14_old/semeval-2015-task-14/subtask-c/data/train";
	
	static String brat_annotated_training_data = resourceDirPath + "training_clean/";
	static String brat_annotated_devel_data;


}
