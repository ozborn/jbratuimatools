package edu.uab.ccts.nlp.brat;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.StringReader;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.Set;

import org.apache.uima.resource.ResourceInitializationException;

public class BratConfigurationImpl implements BratConfiguration {

	Hashtable<String,BratEntity> entities = new Hashtable<String,BratEntity>();
	Hashtable<String,BratRelation> relations = new Hashtable<String,BratRelation>();
	
	//Key is entity name, value is the integer assigned to that type
	Hashtable<String,Integer> typeMap = new Hashtable<String,Integer>();

	public enum Mode { ENTITIES ("[entities]"),
				RELATIONS ("[relations]"),
				EVENTS ("[events]"),
				ATTRIBUTES ("[attributes]");
		private final String idstring;
		Mode(String s){
			this.idstring = s;
		}
	};
	
	
	/**
	 * Creates our brat configuration by parsing the annotation.conf file
	 * TODO Add support for something other than relations and entities
	 * EntityID is assigned based not on a standard ontology (there is none)
	 * but the order in which types are found in the .conf file. For example,
	 * negation may be 3, condition 4, etc...
	 * @param config_file_text
	 * @throws ResourceInitializationException
	 */
	public BratConfigurationImpl (String config_file_text) throws ResourceInitializationException {
		Mode curmode = null;
   		Deque<BratEntity> stack = new ArrayDeque<BratEntity>();
   		int lastdepth = -1;
   		int entity_count = 0;
		try(BufferedReader br = new BufferedReader(new StringReader(config_file_text))) {
		    for(String line; (line = br.readLine()) != null; ) {
		        if(line.trim().equals(Mode.ENTITIES.idstring)) {
		        	curmode = Mode.ENTITIES;
		        }
		        else if(line.trim().equals(Mode.ATTRIBUTES.idstring)) {
		        	curmode = Mode.ATTRIBUTES;
		        }
		        else if(line.trim().equals(Mode.EVENTS.idstring)) {
		        	curmode = Mode.EVENTS;
		        }
		        else if(line.trim().equals(Mode.RELATIONS.idstring)) {
		        	curmode = Mode.RELATIONS;
		        } else {
		        	if(curmode==null) continue;
		        	if(curmode == Mode.ENTITIES) {
		        		if(!BratEntityImpl.isParseableEntity(line)) continue; 
		        		typeMap.put(line.trim(), entity_count);
		        		BratEntityImpl bei = new BratEntityImpl(line.trim(),entity_count++);
		        		entities.put(line.trim(), bei);
	        			int depth = line.length() - line.replace("\t","").length();
	        			if(depth>lastdepth) { //Add to queue
		        			if(!stack.isEmpty()) bei.setParent(stack.peek());
	        			} else if (depth==lastdepth) { //Replace end of queue
	        				stack.pop(); //Get rid of old queue end
	        				bei.setParent(stack.peek());
	        			} else { //Remove end of queue
	        				stack.pop();
	        				stack.pop();
	        				bei.setParent(stack.peek());
	        			}
	        			String message = "Created entity "+bei.getName();
	        			if(bei.getParent()!=null) message+=" with parent "+bei.getParent().getName();
	        			//System.out.println(message);
        				stack.push(bei); //Now we are end of queue
        				lastdepth = depth;
		        	} else if(curmode == Mode.RELATIONS) {
		        		if(!BratRelationImpl.isParseableRelation(line)) continue;
		        		BratRelationImpl bri = new BratRelationImpl(this, line.trim());
	        			System.out.println("Created relation "+bri.getName());
		        		relations.put(bri.getName(), bri);
		        	}
		        	
		        }
		    }
		} catch (IOException e) {
			e.printStackTrace();
			throw new ResourceInitializationException(e);
		}
		
	}

	@Override
	public Set<BratEntity> getEntities() {
		return new HashSet<BratEntity>(entities.values());
	}

	@Override
	public Set<BratRelation> getRelations() {
		return new HashSet<BratRelation>(relations.values());
	}
	
	
	public BratEntity getEntityByName(String n){
		return entities.get(n);
	}

	public BratRelation getRelationByName(String n){
		return relations.get(n);
	}
	
	
	public int getIdFromType(String type) { return typeMap.get(type); }

}
