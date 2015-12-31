package dk.kaspergsm.stormdeploy.image;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.management.ManagementFactory;
import java.lang.management.MemoryMXBean;
import java.util.ArrayList;
import java.util.Arrays;
import javax.management.remote.JMXConnector;
import javax.management.remote.JMXConnectorFactory;
import javax.management.remote.JMXServiceURL;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.sun.tools.attach.VirtualMachine;

/**
 * Continuously monitors free memory on node
 * if freeMemory < 10% on node, do garbage collection on all JVM.
 * 	This is needed to ensure all java processes give back their unused memory when needed.
 * 
 * can be executed by:
 * java -cp "storm-deploy-alternative.jar:/usr/lib/jvm/java-7-openjdk-amd64/lib/tools.jar" dk.kaspergsm.stormdeploy.image.MemoryMonitor
 * 
 * @author Kasper Grud Skat Madsen
 */
class MemoryMonitor {
	private static Logger log = LoggerFactory.getLogger(MemoryMonitor.class);
	
	public static void main(String[] args) throws IOException {
		log.info("Initialized MemoryMonitor");
		log.info("Software for helping Java proceses share memory");
		log.info("it works by invoking garbage collection on all Java processes as needed");
		
		while (true) {
			try {
				// Get memory information from node
				Long freeMem = getFreeMemoryNode();
				Long totalMem = getTotalMemoryNode();
				if (freeMem != null && totalMem != null) {
					// Calculate unused memory in %
					double freeMemory = ((double)freeMem / (double)totalMem) * 100;
					if (freeMemory < 10) {
						log.info("Detected system has less than 10% free memory");
						GCExternalProcesses(); // invoke gc on all java processes
						sleep(10);
					} else if (freeMemory < 20) {
						log.info("Detected system has less than 20% free memory");
						GCExternalProcesses(); // invoke gc on all java processes
						sleep(30);
					} else if (freeMemory < 40) {
						log.info("Detected system has less than 40% free memory");
						GCExternalProcesses(); // invoke gc on all java processes
						sleep(60);
					}
				}
				sleep(5);
			} catch (Exception ex) {
				log.error("Problem", ex);
			}
		}
	}
	
	private static void sleep(int seconds) {
		try {
			Thread.sleep(1000*seconds);	
		} catch (InterruptedException ie) {}	
	}
	
	private static Long getTotalMemoryNode() {
		Long memTotal = 0l;
		try {
			ProcessBuilder pb = new ProcessBuilder(Arrays.asList("cat", "/proc/meminfo"));
			Process process = pb.start();
			final InputStream is = process.getInputStream();
            BufferedReader reader = new BufferedReader(new InputStreamReader(is));
            String line;
            while ((line = reader.readLine()) != null) {
            	if (line.startsWith("MemTotal:")) {
            		String noStart = line.substring(line.indexOf("MemTotal:") + 9).trim();
            		String noEnd = noStart.substring(0, noStart.indexOf(" ")).trim();
            		memTotal = Long.valueOf(noEnd) * 1024; // convert to bytes 
            	}
            }
            is.close();
    		process.waitFor();
        } catch (Exception ex) {
        	log.error("Problem", ex);
            return null;
        }
		return memTotal;
	}
	
	private static Long getFreeMemoryNode() {
		long memFree = 0, memCached = 0, memBuffer = 0;
		try {
			ProcessBuilder pb = new ProcessBuilder(Arrays.asList("cat", "/proc/meminfo"));
			Process process = pb.start();
			final InputStream is = process.getInputStream();
            BufferedReader reader = new BufferedReader(new InputStreamReader(is));
            String line;
            while ((line = reader.readLine()) != null) {
            	if (line.startsWith("MemFree:")) {
            		String noStart = line.substring(line.indexOf("MemFree:") + 8).trim();
            		String noEnd = noStart.substring(0, noStart.indexOf(" ")).trim();
            		memFree = Long.valueOf(noEnd);
            	}
            		
            	if (line.startsWith("Buffers:")) {
            		String noStart = line.substring(line.indexOf("Buffers:") + 8).trim();
            		String noEnd = noStart.substring(0, noStart.indexOf(" ")).trim();
            		memBuffer = Long.valueOf(noEnd);
            	}
            		
            	if (line.startsWith("Cached:")) {
            		String noStart = line.substring(line.indexOf("Cached:") + 7).trim();
            		String noEnd = noStart.substring(0, noStart.indexOf(" ")).trim();
            		memCached = Long.valueOf(noEnd);
            	}
            		
            }
            is.close();
    		process.waitFor();
        } catch (Exception ex) {
        	log.error("Problem", ex);
            return null;
        }
		
		return (memFree + memCached + memBuffer) * 1024; // convert to bytes
	}
	
	
	private static ArrayList<String>  getAllJavaProcesses() {
		ArrayList<String> javaProcesses = new ArrayList<String>();
		try {
			ProcessBuilder pb = new ProcessBuilder(Arrays.asList("jps"));
			Process process = pb.start();
			final InputStream is = process.getInputStream();
            BufferedReader reader = new BufferedReader(new InputStreamReader(is));
            String line;
            while ((line = reader.readLine()) != null)
            	javaProcesses.add(line.substring(0, line.indexOf(" ")).trim());
            is.close();
    		process.waitFor();
        } catch (Exception ex) {
        	log.error("Problem", ex);
        }
		return javaProcesses;
	}
	
	private static void GCExternalProcesses() {
		for (String jvmPid : getAllJavaProcesses()) {
			log.info("Requested process ( pid " + jvmPid + " ) to garbage collect");
			GCExternalProcess(jvmPid);
		}
	}
	
	private static void GCExternalProcess(String pid) {
		System.out.println("Asked process with pid " + pid + " to do GC");
		
		VirtualMachine vm = null;
		JMXConnector connector = null;

		try {
			// Attach to JVM process with pid
			vm = VirtualMachine.attach(pid);

			// Load management agent
			vm.loadAgent(vm.getSystemProperties().getProperty("java.home") + "/lib/management-agent.jar");
			
			// Connect using JMX
			JMXServiceURL url = new JMXServiceURL(vm.getAgentProperties().getProperty("com.sun.management.jmxremote.localConnectorAddress"));
			connector = JMXConnectorFactory.newJMXConnector(url, null);
			connector.connect();
			
			// Do GC
			ManagementFactory.newPlatformMXBeanProxy(connector.getMBeanServerConnection(), ManagementFactory.MEMORY_MXBEAN_NAME, MemoryMXBean.class).gc();			
		} catch (Exception ex) {
			log.error("Problem", ex);
		}

		// Detach from JVM process
		try {
			if (connector != null)
				connector.close();
			if (vm != null)
				vm.detach();
		} catch (Exception ex) {
			log.error("Problem", ex);
		}
	}
}
