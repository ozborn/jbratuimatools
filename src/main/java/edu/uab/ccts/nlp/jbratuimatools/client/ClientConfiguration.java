package edu.uab.ccts.nlp.jbratuimatools.client;

/**
 * TODO - make static variables private non-static variables
 * @author ozborn
 *
 */
public class ClientConfiguration {

	static String cuilessDataDirPath = System.getProperty("user.home")+"/code/repo/cuilessdata/";
	public static String getCuilessDataDirPath() {
		return cuilessDataDirPath;
	}
	public static void setCuilessDataDirPath(String cuilessDataDirPath) {
		ClientConfiguration.cuilessDataDirPath = cuilessDataDirPath;
	}
	
	static String dropboxPublicDataPath = System.getProperty("user.home")+"/Dropbox/Public_NLP_Data/";
	public static String getDropboxPublicDataPath() {
		return dropboxPublicDataPath;
	}
	public static void setDropboxPublicDataPath(String dropboxPublicDataPath) {
		ClientConfiguration.dropboxPublicDataPath = dropboxPublicDataPath;
	}

	
	private static String semeval2015OldTrainRoot = getDropboxPublicDataPath()+
			"semeval-2015-task-14_old/semeval-2015-task-14/subtask-c/data/train";
	public static String getSemeval2015OldTrainRoot() {
		return semeval2015OldTrainRoot;
	}
	static void setSemeval2015OldTrainRoot(String semeval2015_old_train_root) {
		ClientConfiguration.semeval2015OldTrainRoot = semeval2015_old_train_root;
	}

	private static String semeval2015UpdatedTrainRoot = getDropboxPublicDataPath()+
			"semeval-2015-task-14_updated/data/train";
	static String getSemeval2015UpdatedTrainRoot() {
		return semeval2015UpdatedTrainRoot;
	}
	static void setSemeval2015UpdatedTrainRoot(String semeval2015UpdatedTrainRoot) {
		ClientConfiguration.semeval2015UpdatedTrainRoot = semeval2015UpdatedTrainRoot;
	}

	
	private static String semeval2015UpdatedDevelRoot = getDropboxPublicDataPath()+
			"semeval-2015-task-14_updated/data/devel/discharge";
	public static String getSemeval2015UpdatedDevelRoot() {
		return semeval2015UpdatedDevelRoot;
	}
	static void setSemeval2015UpdatedDevelRoot(String semeval2015UpdatedDevelRoot) {
		ClientConfiguration.semeval2015UpdatedDevelRoot = semeval2015UpdatedDevelRoot;
	}

	
	static String brat_annotated_training_data = getCuilessDataDirPath() + "training_clean/";
	static String brat_annotated_devel_data = getCuilessDataDirPath() + "devel/matt_neu_only"; //TODO replace with adjuciated data from both

	

}
