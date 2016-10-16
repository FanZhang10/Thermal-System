import java.util.Scanner;


public class SystemMain {

	public static void main(String[] args) {
		// TODO Auto-generated method stub
		System.out.println("The following results are calculated during the processing, simulation at botton of console, please select 1 or 2 to display ");
		SystemManager manager = new SystemManager();
		manager.init();
		manager.setMinimumGenerators();
		manager.schedule();

		Scanner userInput = new Scanner( System.in );
		String input = "";
		
		System.out.println("\n"+"Please Select Display :");
		System.out.println("  1. Print Schedule Sheet");
		System.out.println("  2. Run System Simulator");

		input = userInput.next( );
		int mode = Integer.parseInt(input);
		
		if (mode == 1) {
			manager.printScheduleSheet();
		}
		else if (mode == 2) {
			manager.simulate();
		}
		else {
			manager.printScheduleSheet();
		}
		
		userInput.close();
	}

}
