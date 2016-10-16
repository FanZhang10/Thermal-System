import java.util.ArrayList;


public class Generator {

	public static int HotWaterVolume = 300; // TRY : 400 ; 300; 250-ok; 200 - failed on method 1
	
	public static int HeatingTime = 45;
	public static int EmptyingTime = 15;
	public static int CoolingTime = 15;
	public static int FillingWaterTime = 15;
	
	// 0 - idle
	// 1 - heating
	// 2 - emptying
	// 3 - cooling
	// 4 - filling 
	// Cycle = idle -> heating -> emptying -> cooling -> filling -> idle
	public int id;
	public int state;
	public int timeIndex;
	
	public int[] schedule;
	public int[][] scheduleOutput;
	
	public ArrayList<Reservoir> reservoirs;
	
	public Generator() {
		state = 0;
		schedule = new int[96];
		scheduleOutput = new int[96][3];
	}
	
	public static int getCycleTime() {
		return (HeatingTime + EmptyingTime + CoolingTime + FillingWaterTime);
	}
	
	public void setReservoir(ArrayList<Reservoir> reservoirs) {
		this.reservoirs = reservoirs;
	}
	
	public void setState(int state) {
		this.state = state;
	}
	
	public void update(int time) {
		if (time == 0) {
			return;
		}
		
		if (schedule[time-1] == 0) {
			schedule[time] = 0;
		}
		else {
			if (schedule[time - 1] == 1) {
				if (schedule[time - 1] == 1 && schedule[time - 2] == 1 && schedule[time - 3] == 1) {
					schedule[time] = 2;
					// distribute ..
					for (int i = 0; i < 3; i++) {
						reservoirs.get(i).currentVol += scheduleOutput[time][i];
						System.out.println("    Distribute >>> " + scheduleOutput[time][i]);
					}
				}
				else {
					schedule[time] = 1;
				}
			}
			else if (schedule[time - 1] == 2) {
				schedule[time] = 3;
			}
			else if (schedule[time - 1] == 3) {
				schedule[time] = 4;
			}
			else if (schedule[time - 1] == 4) {
				schedule[time] = 0;
			}
		}
	}
	
	public void updateSimulator(int time) {
		if (time == 0) {
			return;
		}
		
		if (schedule[time] == 2 && schedule[time - 1] == 1) {
			// distribute ..
			//System.out.print("  Gen [" + this.id + "] >> ");

			for (int i = 0; i < 3; i++) {
				reservoirs.get(i).currentVol += scheduleOutput[time][i];
				//System.out.print("Res " + i + " : " + scheduleOutput[time][i] + " | ");
			}
			//System.out.println("");
		}
	}
	
	public boolean isInOutput (int time) {
		if (time == 0) {
			return false;
		}
		return (schedule[time] == 2 && schedule[time - 1] == 1);
	}
	
	public int getInOutputVolume (int time, int resId) {
		if (time == 0) {
			return 0;
		}
		
		if (schedule[time] == 2 && schedule[time - 1] == 1) {
			return scheduleOutput[time][resId];
		}
		
		return 0;
	}
	
	public int getOutputAt(int resIndex, int interval) {
		int level = 0;
		for (int i = 0; i < interval; i++) {
			level += scheduleOutput[timeIndex + i][resIndex];
		}
		return level;
	}
	
	public boolean setSchedule(int timeIndex, int[][] distributedVol) {
		int pastCounter = Math.round((Generator.HeatingTime + Generator.EmptyingTime) / SystemManager.CounterUint);
		boolean isIdle = true;
		for (int i = 0; i < pastCounter; i++) {
			if((timeIndex - i) > -1 && schedule[timeIndex - i] != 0) {
				isIdle = false;
			}
		}
		
		if (isIdle) {
			while (pastCounter != 0 && (timeIndex - pastCounter) > -1) {
				schedule[timeIndex - pastCounter] = 1;
				pastCounter--;
			}
			
			schedule[timeIndex] = 2;
			this.distributeOutput(timeIndex, distributedVol);
		}
		
		return isIdle;
	}
	
	public void distributeOutput(int timeIndex, int[][] distributedVol) {
		int i = 0;
		float rate[] = {0.0f, 0.0f, 0.0f};
		boolean hasDirtributed = false;
		for (; i < 3; i++) {
			if (distributedVol[i][1] < (Reservoir.mandatoryVol * 3)) {
				float w = ((float)((Reservoir.mandatoryVol * 3) - distributedVol[i][1])) / (float)(Reservoir.mandatoryVol * 3);
				rate[i] += 0.6 * w;
				if (i == 0) {
					rate[i + 1] += 0.2f * w;
					rate[i + 2] += 0.2f * w;
				}
				else if (i == 1) {
					rate[i + 1] += 0.2f * w;
					rate[i - 1] += 0.2f * w;
				}
				else if (i == 2) {
					rate[i - 1] += 0.2f * w;
					rate[i - 2] += 0.2f * w;
				}
				hasDirtributed = true;
			}
		}

		for (i = 0; i < 3; i++) {
			if (distributedVol[i][0] < Reservoir.mandatoryVol) {
				float w = ((float)(Reservoir.mandatoryVol - distributedVol[i][0])) / (float)Reservoir.mandatoryVol;
				rate[i] += 0.8f * w;
				if (i == 0) {
					rate[i + 1] += 0.1f * w;
					rate[i + 2] += 0.1f * w;
				}
				else if (i == 1) {
					rate[i + 1] += 0.1f * w;
					rate[i - 1] += 0.1f * w;
				}
				else if (i == 2) {
					rate[i - 1] += 0.1f * w;
					rate[i - 2] += 0.1f * w;
				}
				hasDirtributed = true;
			}
		}

		if (hasDirtributed) {
			float sum = rate[0] + rate[1] + rate[2];
			for (i = 0; i < 3; i++) {
				rate[i] /= sum;
				scheduleOutput[timeIndex][i] = (int)Math.round(rate[i] * HotWaterVolume);
			}
		}

		if (!hasDirtributed) {
			for (i = 0; i < 3; i++) {
				scheduleOutput[timeIndex][i] = (int)Math.round(HotWaterVolume / 3.0);
			}
		}
	}
	
	
	// Test- 1 : no good ...
	public void distributeOutput_2(int timeIndex, int[][] distributedVol) {
		int i = 0;
		float rate[] = {0.0f, 0.0f, 0.0f};
		boolean hasDirtributed = false;
		for (; i < 3; i++) {
			if (distributedVol[i][0] < Reservoir.mandatoryVol && distributedVol[i][1] < (Reservoir.mandatoryVol * 3)) {
				rate[i] += 0.8;
				if (i == 0) {
					rate[i + 1] += 0.1f;
					rate[i + 2] += 0.1f;
				}
				else if (i == 1) {
					rate[i + 1] += 0.1f;
					rate[i - 1] += 0.1f;
				}
				else if (i == 2) {
					rate[i - 1] += 0.1f;
					rate[i - 2] += 0.1f;
				}
				hasDirtributed = true;
			}
		}

		if (!hasDirtributed) {
			i = 0;
			for (; i < 3; i++) {
				if (distributedVol[i][0] < Reservoir.mandatoryVol) {
					rate[i] += 0.6f;
					if (i == 0) {
						rate[i + 1] += 0.2f;
						rate[i + 2] += 0.2f;
					}
					else if (i == 1) {
						rate[i + 1] += 0.2f;
						rate[i - 1] += 0.2f;
					}
					else if (i == 2) {
						rate[i - 1] += 0.2f;
						rate[i - 2] += 0.2f;
					}
					hasDirtributed = true;
				}
			}
		}

		if (hasDirtributed) {
			float sum = rate[0] + rate[1] + rate[2];
			for (i = 0; i < 3; i++) {
				rate[i] /= sum;
				scheduleOutput[timeIndex][i] = (int)Math.round(rate[i] * HotWaterVolume);
			}
		}

		if (!hasDirtributed) {
			for (i = 0; i < 3; i++) {
				scheduleOutput[timeIndex][i] = (int)Math.round(HotWaterVolume / 3.0);
			}
		}
	}
	
}
