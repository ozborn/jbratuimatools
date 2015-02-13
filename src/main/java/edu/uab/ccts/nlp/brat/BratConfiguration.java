package edu.uab.ccts.nlp.brat;

import java.util.Set;

public interface BratConfiguration {
	
	public Set<BratEntity> getEntities();
	public Set<BratRelation> getRelations();
	public BratEntity getEntityByName(String s);

}
