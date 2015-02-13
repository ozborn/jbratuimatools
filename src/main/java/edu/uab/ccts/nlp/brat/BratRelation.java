package edu.uab.ccts.nlp.brat;

public interface BratRelation {
	
	public String getName();
	public BratEntity getArg1();
	public BratEntity getArg2();

}
