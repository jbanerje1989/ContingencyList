package PowerNetworkDependency;

import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

public class HeuristicContingencyList {
	private HashMap<String, Integer> entityLabeltoIndexMap; 
	private HashMap<String, List<List<String>>> IIRs;
	
	List<String> failedEntities;
	List<String> initFailedEntities;
	private int K;
	
	HeuristicContingencyList(int KVal, IIRGenerator object) throws FileNotFoundException{
		K = KVal;
		entityLabeltoIndexMap = object.getMaps();
		IIRs = object.getIIRSol();
	}
	
	public void computeContingencyList(){
		failedEntities = new ArrayList<String>();
		initFailedEntities = new ArrayList<String>();
		
		int countK = 0;
		while(countK < K){
			countK ++;
			int curFailedEntitySize = 0;
			int mintermCovNum = 0;
			String entityToKill = new String();
			
			for(String entity: entityLabeltoIndexMap.keySet()){
				if(failedEntities.contains(entity)) continue;
				
				List<Integer> result = computeCascade(entity);
				
				if(result.get(0) > curFailedEntitySize){
					curFailedEntitySize = result.get(0);
					mintermCovNum = result.get(1);
					entityToKill = new String();
					entityToKill = entity;
				}
				else if(result.get(0) == curFailedEntitySize){
					if(result.get(1) > mintermCovNum){
						mintermCovNum = result.get(1);
						entityToKill = new String();
						entityToKill = entity;
					}
				}
			}
			
			initFailedEntities.add(entityToKill);
			IIRs = computeCascadeIIR(entityToKill, failedEntities);
		}
	}
	
	private List<Integer> computeCascade(String entity){
		HashMap<String, List<List<String>>> IIRdummy = new HashMap<String, List<List<String>>>();
		
		List<String> curFailedEntity = new ArrayList<String>(); 
		curFailedEntity.add(entity);
		int curMintermCovNum = 0;
		
		for(String str: IIRs.keySet()){
			if(!failedEntities.contains(str))
				IIRdummy.put(str, IIRs.get(str));
		}
		
		int start = 0;
		
		while(start < curFailedEntity.size()){
			
			// Kill entities based on IIRs
			String entityToKill = curFailedEntity.get(start);
			
			List<String> termsToIterate = new ArrayList<String>();
			for(String str: IIRdummy.keySet()) termsToIterate.add(str);
			
			for(String firstTerm: termsToIterate){
				List<List<String>> mintermToAdd = new ArrayList<List<String>>();
				for(List<String> minterm: IIRdummy.get(firstTerm)){
					if(minterm.contains(entityToKill)){
						curMintermCovNum ++;
						// Remove transmission line entities
						if(!curFailedEntity.contains(minterm.get(0)) && minterm.size() > 0){
							curFailedEntity.add(minterm.get(0));
						}   
					}
					else mintermToAdd.add(minterm);
				}
				IIRdummy.replace(firstTerm, mintermToAdd);
			}
			
			for(String str: termsToIterate){
				if(IIRdummy.get(str).size() == 0){
					IIRdummy.remove(str);
					if(!curFailedEntity.contains(str)) curFailedEntity.add(str);
				}
			}
	
			start ++;
		}
		return Arrays.asList(curFailedEntity.size(), curMintermCovNum);
	}
	
	private HashMap<String, List<List<String>>> computeCascadeIIR(String entity,
			List<String> failedEntities){
		HashMap<String, List<List<String>>> IIRdummy = new HashMap<String, List<List<String>>>();
		
		List<String> curFailedEntity = new ArrayList<String>(); 
		curFailedEntity.add(entity);
		
		for(String str: IIRs.keySet()){
			if(!failedEntities.contains(str))
				IIRdummy.put(str, IIRs.get(str));
		}
		
		int start = 0;
		
		while(start < curFailedEntity.size()){
			
			// Kill entities based on IIRs
			String entityToKill = curFailedEntity.get(start);
			
			List<String> termsToIterate = new ArrayList<String>();
			for(String str: IIRdummy.keySet()) termsToIterate.add(str);
			
			for(String firstTerm: termsToIterate){
				List<List<String>> mintermToAdd = new ArrayList<List<String>>();
				for(List<String> minterm: IIRdummy.get(firstTerm)){
					if(minterm.contains(entityToKill)){
						// Remove transmission line entities
						if(!curFailedEntity.contains(minterm.get(0)) && minterm.size() > 0){
							curFailedEntity.add(minterm.get(0));
						}   
					}
					else mintermToAdd.add(minterm);
				}
				IIRdummy.replace(firstTerm, mintermToAdd);
			}
			
			for(String str: termsToIterate){
				if(IIRdummy.get(str).size() == 0){
					IIRdummy.remove(str);
					if(!curFailedEntity.contains(str)) curFailedEntity.add(str);
				}
			}
	
			start ++;
		}
		
		for(String str: curFailedEntity)
			failedEntities.add(str);
		
		return IIRdummy;
	}
	
	public int getFailedEntitiesCount() { return failedEntities.size();}
	public List<String> getFailedEntities() { return failedEntities;}
	public List<String> getInitFailedEntities() { return initFailedEntities;}
}
