import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;

public class Reservoir {
	
	public static int initialVol = 500;
	public static int maxVol = 900;
	public static int mandatoryVol = 100;
	
	public String name;
	public int currentVol;
	public int timeIndex;
	
	public int[] output;
	
	public Reservoir(String name) {
		this.name = name;
		currentVol = initialVol;
		timeIndex = 0;
		output = new int[96];
	}

	public void loadData() {
		// populate output data
		int index = 0;
        BufferedReader buffReader = null;
        try {
            buffReader = new BufferedReader (new FileReader("./datasets/" + name + ".txt"));
            String line = buffReader.readLine();
            while(line != null){
                output[index] = Integer.parseInt(line);
		    	index++;
                
                line = buffReader.readLine();
            }
        }
        catch (IOException ioe) {
            ioe.printStackTrace();
        }
        finally {
            try {
                buffReader.close();
            }
            catch(IOException ioe1) {
                //Leave It
            }
        }
	}
	
	public void update(int time) {
		currentVol -= output[time];
		timeIndex = time;
	}
	
	public int getTotalDemand() {
		int total = 0;
		for (int i = 0; i < 94; i++) {
			total += output[i];
		}
		return total;
	}
	
	public int getLevelAt(int interval) {
		int level = currentVol;
		for (int i = 0; i < interval && (timeIndex + i) > -1 && (timeIndex + i) < 96; i++) {
			level -= output[timeIndex + i];
		}
		return level;
	}


}
