package edu.uab.ccts.nlp.brat;


public class BratEntityImpl implements BratEntity {
	String name = null;
	BratEntity parent = null;
	int entity_id = 0;
	
	public BratEntityImpl(String n, int random_id) { 
		name = n; 
		entity_id = random_id;
	}
	
	public static boolean isParseableEntity(String line) {
		if(line.trim().length()>0) return true;
		return false;
	}

	@Override
	public String getName() { return name; }
	public int getTypeId() { return entity_id; }

	@Override
	public BratEntity getParent() { return parent; }
	
	public void setParent(BratEntity p) { parent = p; }

}
