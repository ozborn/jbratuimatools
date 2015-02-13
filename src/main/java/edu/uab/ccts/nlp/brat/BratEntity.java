package edu.uab.ccts.nlp.brat;

public interface BratEntity {
	
	public String getName();
	/** Get the type id from the bratEntityTypes.properties file */
	public int getTypeId();
	public BratEntity getParent();
	public void setParent(BratEntity be);

}
