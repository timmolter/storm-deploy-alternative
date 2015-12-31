package dk.kaspergsm.stormdeploy.image;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Timer;
import java.util.TimerTask;

/**
 * Is used to monitor a process, and restart as necessary.
 * 
 * Can be executed by:
 * java -cp storm-deploy-alternative.jar dk.kaspergsm.stormdeploy.image.ProcessMonitor 
 * 
 * @author Kasper Grud Skat Madsen
 */
public class ProcessMonitor {
	private static long _daemonStartTime = 300000; // 5 minutes in milliseconds
	private static long _startDaemonTs;
	private static String[] _toExec;
	private static String _process;
	private static Timer t;
	
	public static void main(String[] args) {
		// Expected args
		// 1. Process id to check
		// 2. Executable string
		if (args.length <= 1) {
			System.err.println("Wrong number of arguments given. Please provide process id and executable string");
			return;
		}
		
		// Parse
		_process = args[0].replaceAll("\"", "");
		_toExec = new String[args.length - 1];
		for (int i = 1; i < args.length; i++)
			_toExec[i-1] = args[i].replaceAll("\"", "");

		// Schedule work
		t = new Timer();
		t.scheduleAtFixedRate(new TimerTask() {
			@Override
			public void run() {
				try {
					if (!isRunning()) {
						Runtime.getRuntime().exec(_toExec);
						_startDaemonTs = System.currentTimeMillis();
					}
				} catch (Exception ex) {
					System.err.println(ex.toString());
				}
			}
		}, 1000, 5000);
	}
	
	private static boolean isRunning() throws IOException {
		Runtime rt = Runtime.getRuntime();
		Process p = rt.exec(new String[]{"ps","aux"});
        BufferedReader i = new BufferedReader(new InputStreamReader(p.getInputStream()));
        
        // read the output from the command
        String s = null;
        while ((s = i.readLine()) != null) {
        	if (s.contains(_process) && !s.contains("storm-deploy-alternative.jar")) // filter the monitoring process
        		return true;
        }
        
        // Only if more than _initialStartup has passed, return false
        // It is imperative the daemons gets enough time to start!
        long passedTimeSinceDaemonLaunch = System.currentTimeMillis() - _startDaemonTs;
        if (passedTimeSinceDaemonLaunch >= _daemonStartTime)
        	return false;
        return true;
	}
}