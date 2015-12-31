package dk.kaspergsm.stormdeploy.configurations;

import static org.jclouds.scriptbuilder.domain.Statements.exec;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.jclouds.scriptbuilder.domain.Statement;

import dk.kaspergsm.stormdeploy.Tools;


/**
 * Contains all methods to configure Storm on nodes
 * 
 * @author Kasper Grud Skat Madsen
 */
public class Storm {

	public static List<Statement> download(String stormRemoteLocation) {
        return Tools.download("~/", stormRemoteLocation, true, true, "storm");
	}
	
	/**
	 * Write storm/conf/storm.yaml (basic settings only)
	 */
	public static List<Statement> configure(String hostname, List<String> zkNodesHostname, List<String> drpcHostname, String userName) {
		ArrayList<Statement> st = new ArrayList<Statement>();
		st.add(exec("cd ~/storm/conf/"));
		st.add(exec("touch storm.yaml"));
		
		// Add nimbus.host
		st.add(exec("echo nimbus.host: \"" + hostname + "\" >> storm.yaml"));
		
		// Add storm.zookeeper.servers
		st.add(exec("echo storm.zookeeper.servers: >> storm.yaml"));
		for (int i = 1; i <= zkNodesHostname.size(); i++)
			st.add(exec("echo - \"" + zkNodesHostname.get(i-1) + "\" >> storm.yaml"));

		// Add drpc.servers
		if (drpcHostname.size() > 0) {
			st.add(exec("echo drpc.servers: >> storm.yaml"));
			for (int i = 1; i <= drpcHostname.size(); i++)
				st.add(exec("echo - \"" + drpcHostname.get(i-1) + "\" >> storm.yaml"));
		}

		// Add supervisor metadata
		st.add(exec("echo supervisor.scheduler.meta: >> storm.yaml"));
		st.add(exec("instancetype=$(cat ~/.instance-type)"));
		st.add(exec("echo \"  instancetype: \\\"$instancetype\\\"\" >> storm.yaml"));
		
		// Change owner of storm directory
		st.add(exec("chown -R " + userName + ":" + userName + " ~/storm"));
		
		// Add storm to execution PATH
		st.add(exec("echo \"export PATH=\\\"\\$HOME/storm/bin:\\$PATH\\\"\" >> ~/.bashrc"));
                
		return st;
	}
	
	/**
	 * Uses Monitor to restart daemon, if it stops
	 */
	public static List<Statement> startNimbusDaemonSupervision(String username) {
		ArrayList<Statement> st = new ArrayList<Statement>();
		st.add(exec("cd ~"));
		st.add(exec("su -c 'case $(head -n 1 ~/daemons) in *MASTER*) java -cp \"sda/storm-deploy-alternative.jar\" dk.kaspergsm.stormdeploy.image.ProcessMonitor backtype.storm.daemon.nimbus storm/bin/storm nimbus ;; esac &' - " + username));
		return st;
	}
	
	/**
	 * Uses Monitor to restart daemon, if it stops
	 */
	public static List<Statement> startSupervisorDaemonSupervision(String username) {
		ArrayList<Statement> st = new ArrayList<Statement>();
		st.add(exec("cd ~"));
		st.add(exec("su -c 'case $(head -n 1 ~/daemons) in *WORKER*) java -cp \"sda/storm-deploy-alternative.jar\" dk.kaspergsm.stormdeploy.image.ProcessMonitor backtype.storm.daemon.supervisor storm/bin/storm supervisor ;; esac &' - " + username));
		return st;
	}
	
	/**
	 * Uses Monitor to restart daemon, if it stops
	 */
	public static List<Statement> startUIDaemonSupervision(String username) {
		ArrayList<Statement> st = new ArrayList<Statement>();
		st.add(exec("cd ~"));
		st.add(exec("su -c 'case $(head -n 1 ~/daemons) in *UI*) java -cp \"sda/storm-deploy-alternative.jar\" dk.kaspergsm.stormdeploy.image.ProcessMonitor backtype.storm.ui.core storm/bin/storm ui ;; esac &' - " + username));
		return st;
	}
	
	/**
	 * Uses Monitor to restart daemon, if it stops
	 */
	public static List<Statement> startDRPCDaemonSupervision(String username) {
		ArrayList<Statement> st = new ArrayList<Statement>();
		st.add(exec("cd ~"));
		st.add(exec("su -c 'case $(head -n 1 ~/daemons) in *DRPC*) java -cp \"sda/storm-deploy-alternative.jar\" dk.kaspergsm.stormdeploy.image.ProcessMonitor backtype.storm.daemon.drpc storm/bin/storm drpc ;; esac &' - " + username));
		return st;
	}
	
    /**
     * Uses Monitor to restart daemon, if it stops
     */
	public static List<Statement> startLogViewerDaemonSupervision(String username) {
		ArrayList<Statement> st = new ArrayList<Statement>();
		st.add(exec("cd ~"));
		st.add(exec("su -c 'case $(head -n 1 ~/daemons) in *LOGVIEWER*) java -cp \"sda/storm-deploy-alternative.jar\" dk.kaspergsm.stormdeploy.image.ProcessMonitor backtype.storm.daemon.logviewer storm/bin/storm logviewer ;; esac &' - " + username));
		return st;
	}
	
	/**
	 * Used to write config files to $HOME/.storm/
	 * these are needed for the storm script to know where to submit topologies etc.
	 */
	public static void writeStormAttachConfigFiles(List<String> zookeeperNodesHostname, List<String> supervisorNodesHostname, String nimbusHost, String uiHost, String clustername) throws IOException {
		String userHome = Tools.getHomeDir();
		new File(userHome + ".storm").mkdirs();
		
		// Write $HOME/.storm/storm.yaml
		FileWriter stormYaml = new FileWriter(userHome + ".storm/storm.yaml", false);
		stormYaml.append("storm.zookeeper.servers:\n");
		for (String zookeeperNode : zookeeperNodesHostname) {
			stormYaml.append("    - \"");
			stormYaml.append(zookeeperNode);
			stormYaml.append("\"\n");
		}
		stormYaml.append("nimbus.host: \"");
		stormYaml.append(nimbusHost);
		stormYaml.append("\"\n");
		stormYaml.append("ui.host: \"");
		stormYaml.append(uiHost);
		stormYaml.append("\"\n");
		stormYaml.append("cluster: \"");
		stormYaml.append(clustername);
		stormYaml.append("\"\n");
		
		stormYaml.flush();
		stormYaml.close();
		
		// Write $HOME/.storm/supervisor.yaml
		FileWriter supervisorYaml = new FileWriter(userHome + ".storm/supervisor.yaml", false);
		supervisorYaml.append("storm.supervisor.servers:\n");
		for (String supervisorNode : supervisorNodesHostname) {
			supervisorYaml.append("    - \"");
			supervisorYaml.append(supervisorNode);
			supervisorYaml.append("\"\n");
		}
		supervisorYaml.flush();
		supervisorYaml.close();
	}
}
