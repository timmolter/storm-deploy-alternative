package dk.kaspergsm.stormdeploy.image;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Arrays;
import java.util.Timer;
import java.util.TimerTask;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Is used to monitor a process, and restart as necessary.
 * 
 * Can be executed by:
 * java -cp storm-deploy-alternative-cloud.jar dk.kaspergsm.stormdeploy.image.ProcessMonitor 
 * 
 * @author Kasper Grud Skat Madsen
 */
public class ProcessMonitor {
  
  private final static Logger logger = LoggerFactory.getLogger(ProcessMonitor.class);

	private static final long _daemonStartTime = 300000; // 5 minutes in milliseconds
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
		logger.debug("_process= "+ _process);
		_toExec = new String[args.length - 1];
    for (int i = 1; i < args.length; i++){
			_toExec[i-1] = args[i].replaceAll("\"", "");
    }
    logger.debug("_toExec= "+ Arrays.toString(_toExec));

		// Schedule work
		t = new Timer();
		t.scheduleAtFixedRate(new TimerTask() {
			@Override
			public void run() {
				try {
					if (!isRunning()) {
					  System.out.println("Executing...");
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
          logger.trace("   s: " + s);
        	if (s.contains(_process) && !s.contains("storm-deploy-alternative-cloud.jar")){ // filter the monitoring process
        	  logger.trace("   true");
        		return true;
        	}
        }
        
        // Only if more than _initialStartup has passed, return false
        // It is imperative the daemons gets enough time to start!
        long passedTimeSinceDaemonLaunch = System.currentTimeMillis() - _startDaemonTs;
        if (passedTimeSinceDaemonLaunch >= _daemonStartTime){
          logger.trace("   false");
        	return false;
        }
        logger.trace("   true");
        return true;
	}
}