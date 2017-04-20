package PowerNetworkDependency;

import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class HeuristicContingencyList {
	private HashMap<String, Integer> entityLabeltoIndexMap; 
	private HashMap<String, List<List<String>>> IIRs;
	private HashMap<String, List<Double>> componentBoundAndValuesReal;
	List<String> failedEntities;
	List<String> initFailedEntities;
	private int K;
	
	HeuristicContingencyList(int KVal, IIRGenerator object) throws FileNotFoundException{
		K = KVal;
		entityLabeltoIndexMap = object.getMaps();
		IIRs = object.getIIRSol();
		componentBoundAndValuesReal = object.getComponentBoundAndValuesReal();
	}
	
	public void computeContingencyList(){
		failedEntities = new ArrayList<String>();
		initFailedEntities = new ArrayList<String>();
		int countK = 0;
		while(countK < K){
			countK ++;
			HashMap<String, List<List<String>>> IIRsForIteration = new HashMap<String, List<List<String>>>();
			List<String> killSetForIteration = new ArrayList<String>();
			int mintermCovNum = 0;
			for(String entity: entityLabeltoIndexMap.keySet()){
				if(failedEntities.contains(entity)) continue;
				
				HashMap<String, List<List<String>>> IIRdummy = new HashMap<String, List<List<String>>>();
				List<String> curFailedEntity = new ArrayList<String>(); 
				curFailedEntity.add(entity);
				int curMintermCovNum = 0;
				
				for(String str: IIRs.keySet())
					if(!failedEntities.contains(str))
						IIRdummy.put(str, IIRs.get(str));
				int start = 0;
				while(start < curFailedEntity.size()){
					String entityToKill = curFailedEntity.get(start);
					List<String> termsToIterate = new ArrayList<String>();
					for(String str: IIRdummy.keySet()) termsToIterate.add(str);
					for(String firstTerm: termsToIterate){
						List<List<String>> mintermToAdd = new ArrayList<List<String>>();
						for(List<String> minterm: IIRdummy.get(firstTerm)){
							if(minterm.contains(entityToKill)) curMintermCovNum ++;
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
				if(curFailedEntity.size() > killSetForIteration.size()){
					killSetForIteration = new ArrayList<String>();
					IIRsForIteration = new HashMap<String, List<List<String>>>();
					for(String str: curFailedEntity)
						killSetForIteration.add(str);
					for(String str: IIRdummy.keySet())
						IIRsForIteration.put(str,  IIRdummy.get(str));
					if(initFailedEntities.size() < countK) initFailedEntities.add(entity);
					else initFailedEntities.set(countK - 1, entity);
				}
				
				else if(curFailedEntity.size() == killSetForIteration.size()){
					if(mintermCovNum < curMintermCovNum){
						mintermCovNum = curMintermCovNum;
						killSetForIteration = new ArrayList<String>();
						IIRsForIteration = new HashMap<String, List<List<String>>>();
						for(String str: curFailedEntity)
							killSetForIteration.add(str);
						for(String str: IIRdummy.keySet())
							IIRsForIteration.put(str,  IIRdummy.get(str));
						if(initFailedEntities.size() < countK) initFailedEntities.add(entity);
						else initFailedEntities.set(countK - 1, entity);
					}
				}
				
			}
			
			for(String str: killSetForIteration)
				failedEntities.add(str);

			IIRs = new HashMap<String, List<List<String>>>();
			for(String str: IIRsForIteration.keySet()){
				IIRs.put(str, IIRsForIteration.get(str));
			}
			
		}
	}
	
	public int getFailedEntitiesCount() { return failedEntities.size();}
	public List<String> getFailedEntities() { return failedEntities;}
	public List<String> getInitFailedEntities() { return initFailedEntities;}
}
