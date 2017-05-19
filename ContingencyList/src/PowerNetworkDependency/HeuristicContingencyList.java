package PowerNetworkDependency;

import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class HeuristicContingencyList {
	private HashMap<String, Integer> entityLabeltoIndexMap; 
	private HashMap<String, List<List<String>>> IIRs;
	private HashMap<String, List<Integer>> componentBoundAndValuesReal;
	private HashMap<String, Integer> transFlow;
	private HashMap<String, Integer> transCap;
	private HashMap<String, List<String>> busInputLines;
	private HashMap<String, List<String>> busOutputLines;
	private HashMap<String, List<String>> transLine;
	List<String> failedEntities;
	List<String> initFailedEntities;
	private int K;
	
	HeuristicContingencyList(int KVal, IIRGenerator object) throws FileNotFoundException{
		K = KVal;
		entityLabeltoIndexMap = object.getMaps();
		IIRs = object.getIIRSol();
		transFlow = object.getEntityPowerVal();
		transCap = object.getEntityPowerBound();
		busInputLines = object.getToBus();
		busOutputLines = object.getFromBus();
		transLine = object.getTransLine();
	}
	
	public void computeContingencyList(){
		failedEntities = new ArrayList<String>();
		initFailedEntities = new ArrayList<String>();
		
		int countK = 0;
		while(countK < K){
			countK ++;
			HashMap<String, List<List<String>>> IIRsForIteration = new HashMap<String, List<List<String>>>();
			HashMap<String, List<String>> toBusForIteration = new HashMap<String, List<String>>();
			HashMap<String, List<String>> fromBusForIteration = new HashMap<String, List<String>>();
			HashMap<String, List<Integer>> compBoundForIteration = new HashMap<String, List<Integer>>();
			HashMap<String, Integer> transFlowForIteration = new HashMap<String, Integer>();
			
			List<String> killSetForIteration = new ArrayList<String>();
			int mintermCovNum = 0;
			
			for(String entity: entityLabeltoIndexMap.keySet()){
				if(failedEntities.contains(entity)) continue;
				
				HashMap<String, List<List<String>>> IIRdummy = new HashMap<String, List<List<String>>>();
				HashMap<String, List<String>> toBusDummy = new HashMap<String, List<String>>();
				HashMap<String, List<String>> fromBusDummy = new HashMap<String, List<String>>();
				HashMap<String, List<Integer>> compBoundDummy = new HashMap<String, List<Integer>>();
				HashMap<String, Integer> transFlowDummy = new HashMap<String, Integer>();
				
				List<String> curFailedEntity = new ArrayList<String>(); 
				curFailedEntity.add(entity);
				int curMintermCovNum = 0;
				
				HashMap<String, Integer> excessRequired = new HashMap<String, Integer>();
				
				for(String str: IIRs.keySet()){
					if(!failedEntities.contains(str))
						IIRdummy.put(str, IIRs.get(str));
				}
				
				for(String str: busInputLines.keySet()){
					if(!failedEntities.contains(str)){
						toBusDummy.put(str, busInputLines.get(str));
						fromBusDummy.put(str, busOutputLines.get(str));
					}
				}
				
				for(String str: componentBoundAndValuesReal.keySet()){
					if(!failedEntities.contains(str))
						compBoundDummy.put(str, componentBoundAndValuesReal.get(str));
				}
			
				for(String str: transFlow.keySet()){
					if(!failedEntities.contains(str))
						transFlowDummy.put(str, transFlow.get(str));
				}
				
				int start = 0;
				
				while(start < curFailedEntity.size() || excessRequired.size() != 0){
					
					// Kill entities based on IIRs
					String entityToKill = curFailedEntity.get(start);
					
					List<String> termsToIterate = new ArrayList<String>();
					for(String str: IIRdummy.keySet()) termsToIterate.add(str);
					
					for(String firstTerm: termsToIterate){
						List<List<String>> mintermToAdd = new ArrayList<List<String>>();
						List<String> toBusLines = toBusDummy.get(firstTerm);
						List<String> fromBusLines = fromBusDummy.get(entityToKill);
						for(List<String> minterm: IIRdummy.get(firstTerm)){
							if(minterm.contains(entityToKill)){
								curMintermCovNum ++;
								// Remove transmission line entities
								if(!curFailedEntity.contains(minterm.get(0)) && minterm.size() > 0){
									curFailedEntity.add(minterm.get(0));
									toBusLines.remove(minterm.get(0));
									fromBusLines.remove(minterm.get(0));
									transFlowDummy.remove(minterm.get(0));
								}   
							}
							else mintermToAdd.add(minterm);
						}
						IIRdummy.replace(firstTerm, mintermToAdd);
					}
					
					for(String str: termsToIterate){
						if(IIRdummy.get(str).size() == 0){
							IIRdummy.remove(str);
							compBoundDummy.remove(str);
							if(toBusDummy.containsKey(str)) toBusDummy.remove(str);
							if(fromBusDummy.containsKey(str)){
								for(String line: fromBusDummy.get(str)){
									if(!curFailedEntity.contains(line)){
										toBusDummy.get(transLine.get(line).get(1)).remove(line);
										transFlowDummy.remove(line);
										curFailedEntity.add(line);
									}
								}
							}
							fromBusDummy.remove(str);
							if(!curFailedEntity.contains(str)) curFailedEntity.add(str);
						}
					}
					
					// Kill entities based on flow values	
					
					// Check the requirement of the transmission lines
					for(String str: excessRequired.keySet()){
						int requiredValue = excessRequired.get(str);
						int numLines = toBusDummy.size();
						int countLinesKilled = 0;
						List<String> linesToKill = new ArrayList<String>();
						int minValue = 10000000;
						int flowValue = 0;
						String lineToAllot = new String();
						for(String lines: toBusDummy.get(str)){
							if(transFlowDummy.get(lines) + requiredValue >= transCap.get(lines)){
								countLinesKilled ++;
								transFlowDummy.remove(lines);
								linesToKill.add(lines);
							}
							else if(transCap.get(lines) - transFlowDummy.get(lines) - requiredValue < minValue){
								minValue = transCap.get(lines) - transFlowDummy.get(lines) - requiredValue;
								flowValue = transFlowDummy.get(lines) + requiredValue;
								lineToAllot = new String(lines);
							}
						}
						if(numLines == countLinesKilled){
							for(List<String> minterms: IIRdummy.get(str)){
								if(minterms.size() == 1) continue;
								if(fromBusDummy.get(minterms.get(1)).contains(minterms.get(0)))
									fromBusDummy.get(minterms.get(1)).remove(minterms.get(0));
								    transFlowDummy.remove(minterms.get(0));
								    curFailedEntity.add(minterms.get(0));
							}
							IIRdummy.remove(str);
							toBusDummy.remove(str);
							fromBusDummy.remove(str);
							compBoundDummy.remove(str);
							curFailedEntity.add(str);
						}
						else{
							transFlowDummy.replace(lineToAllot, flowValue);
							List<Integer> bounds = compBoundDummy.get(str);
							bounds.set(4, bounds.get(4) + requiredValue);
							
							// Remove the minterms from the IIRs
							int index = 0;
							List<List<String>> newIIR = new ArrayList<List<String>>();
							for(List<String> minterms: IIRdummy.get(str)){
								if(minterms.get(0).equals(linesToKill.get(index))){
									index ++;
									// Remove the to and from bus values
									toBusDummy.get(str).remove(minterms.get(0));
									fromBusDummy.get(minterms.get(1)).remove(minterms.get(0));
									transFlowDummy.remove(minterms.get(0));
								    curFailedEntity.add(minterms.get(0));
								}
								else{
									index ++;
									newIIR.add(minterms);
								}
							}
							IIRdummy.put(str, newIIR);
						}
					}
					
					excessRequired = new HashMap<String, Integer>();
					
					List<String> keySet = new ArrayList<String>();
					
					for(String str: compBoundDummy.keySet()) keySet.add(str);
					// check the excess requirement for each bus
					for(String busEntity: keySet){
						if(!IIRdummy.containsKey(busEntity)) continue;
						// check for excess requirement
						int loadReq = compBoundDummy.get(busEntity).get(0);
						int genCap = compBoundDummy.get(busEntity).get(0);;
						int curGen = compBoundDummy.get(busEntity).get(2);
						List<Integer> bounds = compBoundDummy.get(busEntity);
						int currentInVal = 0;
						for(String str: toBusDummy.get(busEntity)){
							if(!curFailedEntity.contains(str))
								currentInVal += transFlowDummy.get(str);
						}
					    bounds.set(3, currentInVal);
						
						int curOutVal = 0;
						for(String str: fromBusDummy.get(busEntity)){
							if(!curFailedEntity.contains(str))
								curOutVal += transFlowDummy.get(str);
						}
						
						/* if curInVal is zero increase the generation capacity and check if feasible
						else set the excess capacity for the bus */
						
						// Note: set the component values as well (Joydeep)
						
						if(currentInVal == 0){
							if(curOutVal > curGen - loadReq){
								if(curOutVal + loadReq < genCap)
									curGen = genCap;
								else{
									// make the bus die
									compBoundDummy.remove(busEntity);
									for(String line: toBusDummy.get(busEntity)){
										transFlowDummy.remove(line);
										curFailedEntity.add(line);
									}
									toBusDummy.remove(busEntity);
									for(String line: fromBusDummy.get(busEntity)){
										transFlowDummy.remove(line);
										curFailedEntity.add(line);
									}
									fromBusDummy.remove(busEntity);
									curFailedEntity.add(busEntity);
									IIRdummy.remove(busEntity);
								}
							}
						}
						else if(curOutVal + loadReq - curGen != currentInVal)
							excessRequired.put(busEntity, curOutVal + loadReq - curGen - currentInVal);
					}
					start ++;
				}
				
				if(curFailedEntity.size() > killSetForIteration.size()){
					killSetForIteration = new ArrayList<String>();
					IIRsForIteration = new HashMap<String, List<List<String>>>();
					toBusForIteration = new HashMap<String, List<String>>();
					fromBusForIteration = new HashMap<String, List<String>>();
					compBoundForIteration = new HashMap<String, List<Integer>>();
					transFlowForIteration = new HashMap<String, Integer>();
					
					for(String str: curFailedEntity)
						killSetForIteration.add(str);
					for(String str: IIRdummy.keySet())
						IIRsForIteration.put(str,  IIRdummy.get(str));
					for(String str: toBusDummy.keySet())
						toBusForIteration.put(str, toBusDummy.get(str));
					for(String str: fromBusDummy.keySet())
						fromBusForIteration.put(str, fromBusDummy.get(str));
					for(String str: compBoundDummy.keySet())
						compBoundForIteration.put(str, compBoundDummy.get(str));
					for(String str: transFlowDummy.keySet())
						transFlowForIteration.put(str, transFlowDummy.get(str));
					
					if(initFailedEntities.size() < countK) initFailedEntities.add(entity);
					else initFailedEntities.set(countK - 1, entity);
				}
				
				else if(curFailedEntity.size() == killSetForIteration.size()){
					if(mintermCovNum < curMintermCovNum){
						mintermCovNum = curMintermCovNum;
						
						IIRsForIteration = new HashMap<String, List<List<String>>>();
						toBusForIteration = new HashMap<String, List<String>>();
						fromBusForIteration = new HashMap<String, List<String>>();
						compBoundForIteration = new HashMap<String, List<Integer>>();
						transFlowForIteration = new HashMap<String, Integer>();
						
						for(String str: curFailedEntity)
							killSetForIteration.add(str);
						for(String str: IIRdummy.keySet())
							IIRsForIteration.put(str,  IIRdummy.get(str));
						for(String str: toBusDummy.keySet())
							toBusForIteration.put(str, toBusDummy.get(str));
						for(String str: fromBusDummy.keySet())
							fromBusForIteration.put(str, fromBusDummy.get(str));
						for(String str: compBoundDummy.keySet())
							compBoundForIteration.put(str, compBoundDummy.get(str));
						for(String str: transFlowDummy.keySet())
							transFlowForIteration.put(str, transFlowDummy.get(str));
						
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
			
			componentBoundAndValuesReal = new HashMap<String, List<Integer>>();
			for(String str: compBoundForIteration.keySet()){
				componentBoundAndValuesReal.put(str, compBoundForIteration.get(str));
			}
			
			busInputLines = new HashMap<String, List<String>>();
			for(String str: toBusForIteration.keySet()){
				busInputLines.put(str, toBusForIteration.get(str));
			}
			
			busOutputLines = new HashMap<String, List<String>>();
			for(String str: fromBusForIteration.keySet()){
				busOutputLines.put(str, fromBusForIteration.get(str));
			}
			
			transFlow = new HashMap<String, Integer>();
			for(String str: transFlowForIteration.keySet()){
				transFlow.put(str, transFlowForIteration.get(str));
			}
			
		}
	}
	
	public int getFailedEntitiesCount() { return failedEntities.size();}
	public List<String> getFailedEntities() { return failedEntities;}
	public List<String> getInitFailedEntities() { return initFailedEntities;}
}
