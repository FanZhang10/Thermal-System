import java.util.ArrayList;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;


public class SystemManager {
	
	public static int CounterUint = 15; // 15 minutes
	
	public ArrayList<Generator> generators;
	public ArrayList<Reservoir> reservoirs;
	public int numOfGenerators;

	public SystemManager() {
		generators = new ArrayList<Generator>();
		reservoirs = new ArrayList<Reservoir>();
		
		numOfGenerators = 0;
	}
	
	public void init() {
		// load Reservoir : Hospital
		Reservoir resOne = new Reservoir("hospital");
		resOne.loadData();
		reservoirs.add(resOne);
		
		// load Reservoir : School
		Reservoir resTwo = new Reservoir("school");
		resTwo.loadData();
		reservoirs.add(resTwo);
		
		// load Reservoir : Edifice
		Reservoir resThree = new Reservoir("edifice");
		resThree.loadData();
		reservoirs.add(resThree);
	}
	
	public void setMinimumGenerators() {
		int totalDemand = 0;
		for (int i = 0; i < 3; i++) {
			Reservoir res = reservoirs.get(i);
			totalDemand += res.getTotalDemand();
		}
		
		int numOfHeating = Math.round(totalDemand / Generator.HotWaterVolume);
		int numOfGereratorHeating = (24 * 60 / Generator.getCycleTime());
		numOfGenerators = Math.round(numOfHeating / numOfGereratorHeating) + 1;
		
		for (int j = 0; j < numOfGenerators; j++) {
			Generator gen = new Generator();
			gen.setReservoir(reservoirs);
			generators.add(gen);
		}
	}
	
	public void setGeneratorState(int index, int state) {
		generators.get(index).setState(state);
	}
	
	public void rescheduleGenerators_1(int timeIndex, int[][] distributedVol) {
		boolean enableReset = false;
		for (int j = 0; j < numOfGenerators; j++) {
			Generator gen = generators.get(j);
			enableReset = gen.setSchedule(timeIndex, distributedVol);
			
			if (enableReset) {
				break;
			}
		}
		
		// Note : TODO for the small ratio of capacity of generator (< 250) over quarter consuming  , 
		//             here need add multiple generator one time
		if (!enableReset) {
			Generator gen = new Generator();
			enableReset = gen.setSchedule(timeIndex, distributedVol);
			gen.setReservoir(reservoirs);
			generators.add(gen);
			numOfGenerators++;
		}
	}
	
	public void rescheduleGenerators(int timeIndex, int[][] distributedVol) {
		boolean enableReset = false;
		
		// Calculate Generator number ...
		int numOne = 0;
		int numThree = 0;
		for (int i = 0; i < 3; i++) {
			if (Generator.HotWaterVolume >= distributedVol[i][0]) {
				int nOne = (Generator.HotWaterVolume - distributedVol[i][0]) / Generator.HotWaterVolume + 1;
				if (nOne > 0) {
					numOne = Math.max(numOne, nOne);
				}
			}
			
			if (Generator.HotWaterVolume * 3 >= distributedVol[i][1]) {
				int nThree = (Generator.HotWaterVolume * 3 - distributedVol[i][1]) / (Generator.HotWaterVolume * 3) + 1;
				if (nThree > 0) {
					numThree = Math.max(numThree, nThree);
				}
			}
		}
		
		int numGen = Math.max(numOne, numThree);
		System.out.println(" =========== " + numGen);
		
		// Calculate distribution
		// use idle generator
		for (int j = 0; j < numOfGenerators && (numGen > 0); j++) {
			Generator gen = generators.get(j);
			enableReset = gen.setSchedule(timeIndex, distributedVol);
			
			if (enableReset) {
				numGen--;
			}
		}
		
		// if not enough available gen, start new gen ...
		while (numGen > 0) {
			Generator gen = new Generator();
			enableReset = gen.setSchedule(timeIndex, distributedVol);
			gen.setReservoir(reservoirs);
			generators.add(gen);
			numOfGenerators++;
			numGen--;
		}
	}
	
	public void schedule() {
		for (int i = 0; i < 96; i++) {
			
			boolean needReschedule = false;
			int[][] distributedVol = new int[3][2];
			for (int resIndex = 0; resIndex < 3; resIndex++) {
				Reservoir res = reservoirs.get(resIndex);
				int nextVol = res.getLevelAt(1);
				int next3Vol = res.getLevelAt(3);
				
				for (int j = 0; j < numOfGenerators; j++) {
					Generator gen = generators.get(j);
					nextVol += gen.getOutputAt(resIndex, 1);
					next3Vol += gen.getOutputAt(resIndex, 3);
				}
				
				distributedVol[resIndex][0] = nextVol;
				distributedVol[resIndex][1] = next3Vol;
				if (nextVol < Reservoir.mandatoryVol) {
					needReschedule = true;
				}
			}
			
			if (needReschedule) {
				this.rescheduleGenerators(i, distributedVol);
			}

			for (int resIndex = 0; resIndex < 3; resIndex++) {
				Reservoir res = reservoirs.get(resIndex);
				res.update(i);
			}

			for (int j = 0; j < numOfGenerators; j++) {
				Generator gen = generators.get(j);
				gen.update(i);
			}
			
			///// Debugging .....
			System.out.println("---- " + i + "  ----");
			for (int k = 0; k < 3; k++) {
				Reservoir res = reservoirs.get(k);
				System.out.println("" + k + " : " + res.currentVol);
			}
		}
	}

	public void printScheduleSheet() {
		for (int resIndex = 0; resIndex < 3; resIndex++) {
			Reservoir res = reservoirs.get(resIndex);
			res.currentVol = Reservoir.initialVol;
			res.timeIndex = 0;
		}
		
		for (int j = 0; j < numOfGenerators; j++) {
			Generator gen = generators.get(j);
			gen.id = j + 1;
			gen.state = 0;
			gen.timeIndex = 0;
		}
		
		for (int count = 0; count < 96; count++) {
			//System.out.println(new Date());
        	int hours = count / 4;
        	int quarter = count % 4;
        	System.out.println("--------- "
        			+ (hours < 10 ? ("0" + hours) : (hours)) 
        			+ ":" 
        			+ (quarter == 0 ? "00" : quarter * 15)
        			+ " ---------");
          
			for (int resIndex = 0; resIndex < 3; resIndex++) {
				Reservoir res = reservoirs.get(resIndex);
				res.update(count);
			}

			for (int j = 0; j < numOfGenerators; j++) {
				Generator gen = generators.get(j);
				gen.updateSimulator(count);
			}

			int numOutputGen = 0;
			int[] distributedVolume = {0, 0, 0};
			for (int j = 0; j < numOfGenerators; j++) {
				Generator gen = generators.get(j);
				if (gen.isInOutput(count)) {
					numOutputGen++;
					
					for (int i = 0; i < 3; i++) {
						distributedVolume[i] += gen.getInOutputVolume(count, i);
					}
				}
			}
			
			if (numOutputGen > 0) {
				System.out.println("");
				System.out.println("  " + numOutputGen + " Generator(s) Output ...");
			}
			
			System.out.println("");
			for (int k = 0; k < 3; k++) {
				Reservoir res = reservoirs.get(k);
				System.out.println(("  Res [" + res.name + "]             ").substring(0, 22)
							+ "output : "
							+ ("" + res.output[count] + "               ").substring(0, 10)
							+ "input : "
							+ ("" + distributedVolume[k] + "               ").substring(0, 10)
							+ "level : "
							+ res.currentVol);
			}
			
			System.out.println("");
			
			for (int j = 0; j < numOfGenerators; j++) {
				Generator gen = generators.get(j);
				String status = "";
				switch(gen.schedule[count]) {
				case 0 :
					status = "idle";
					break;
				case 1 :
					status = "heating";
					break;
				case 2 :
					status = "emptying";
					break;
				case 3 :
					status = "cooling";
					break;
				case 4 :
					status = "filling";
					break;
				default :
					status = "idle";
				}
				System.out.println("  Gen [" + gen.id + "] status : " + status);
			}
			
			System.out.println("");
		}
	}

	public void simulate() {
		
		for (int resIndex = 0; resIndex < 3; resIndex++) {
			Reservoir res = reservoirs.get(resIndex);
			res.currentVol = Reservoir.initialVol;
			res.timeIndex = 0;
		}
		
		for (int j = 0; j < numOfGenerators; j++) {
			Generator gen = generators.get(j);
			gen.id = j + 1;
			gen.state = 0;
			gen.timeIndex = 0;
		}
		
	    final ScheduledExecutorService service = Executors.newSingleThreadScheduledExecutor();
	    service.schedule(new Runnable()
		      {
			    	private int count = 0;
			        public void run()
			        {
			        	//System.out.println(new Date());
			        	int hours = count / 4;
			        	int quarter = count % 4;
			        	System.out.println("--------- "
			        			+ (hours < 10 ? ("0" + hours) : (hours)) 
			        			+ ":" 
			        			+ (quarter == 0 ? "00" : quarter * 15)
			        			+ " ---------");
			          
						for (int resIndex = 0; resIndex < 3; resIndex++) {
							Reservoir res = reservoirs.get(resIndex);
							res.update(count);
						}
						
						for (int j = 0; j < numOfGenerators; j++) {
							Generator gen = generators.get(j);
							gen.updateSimulator(count);
						}

						int numOutputGen = 0;
						int[] distributedVolume = {0, 0, 0};
						for (int j = 0; j < numOfGenerators; j++) {
							Generator gen = generators.get(j);
							if (gen.isInOutput(count)) {
								numOutputGen++;
								
								for (int i = 0; i < 3; i++) {
									distributedVolume[i] += gen.getInOutputVolume(count, i);
								}
							}
						}
						
						if (numOutputGen > 0) {
							System.out.println("");
							System.out.println("  " + numOutputGen + " Generators Output ...");
						}
						
						System.out.println("");
						for (int k = 0; k < 3; k++) {
							Reservoir res = reservoirs.get(k);
							System.out.println(("  Res [" + res.name + "]             ").substring(0, 22)
										+ "output : "
										+ ("" + res.output[count] + "               ").substring(0, 10)
										+ "input : "
										+ ("" + distributedVolume[k] + "               ").substring(0, 10)
										+ "level : "
										+ res.currentVol);
						}
						
						System.out.println("");
						
						for (int j = 0; j < numOfGenerators; j++) {
							Generator gen = generators.get(j);
							String status = "";
							switch(gen.schedule[count]) {
							case 0 :
								status = "idle";
								break;
							case 1 :
								status = "heating";
								break;
							case 2 :
								status = "emptying";
								break;
							case 3 :
								status = "cooling";
								break;
							case 4 :
								status = "filling";
								break;
							default :
								status = "idle";
							}
							System.out.println("  Gen [" + gen.id + "] status : " + status);
						}
						
			          // Increment progress value
			          count++;
		
			          // Check progress value
			          if (count < 96)
			        	  service.schedule(this, 2000, TimeUnit.MILLISECONDS);
			        }
		      }, 
		      1, TimeUnit.SECONDS);
	}
	
}
