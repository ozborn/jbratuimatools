package edu.uab.ccts.nlp.brat;

public class BratRelationImpl implements BratRelation {
	String name = null;
	BratEntity arg1 = null;
	BratEntity arg2 = null;
	
	public BratRelationImpl(BratConfiguration bratconfig, String line){
		String[] fields = line.split(" ");
		name = fields[0];
		String arg1temp = fields[1].split(":")[0].trim();
		String arg1name = arg1temp.substring(0, arg1temp.length()-1);
		arg1 = bratconfig.getEntityByName(arg1name);
		String arg2name = fields[2].split(":")[0].trim();
		arg2 = bratconfig.getEntityByName(arg2name);
		
	}
	
	static public boolean isParseableRelation(String line) {
		if(line.startsWith("has")) return true;
		return false;
	}

	@Override
	public String getName() {
		return name;
	}

	@Override
	public BratEntity getArg1() {
		return arg1;
	}

	@Override
	public BratEntity getArg2() {
		return arg2;
	}

}
