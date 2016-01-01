package dk.kaspergsm.stormdeploy.configurations;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import org.jclouds.scriptbuilder.domain.Statement;
import dk.kaspergsm.stormdeploy.Tools;
import dk.kaspergsm.stormdeploy.configurations.SystemTools.PACKAGE_MANAGER;
import dk.kaspergsm.stormdeploy.userprovided.Configuration;
import dk.kaspergsm.stormdeploy.userprovided.Credential;

/**
 * @author Kasper Grud Skat Madsen
 */
public class NodeConfiguration {
	
	public static List<Statement> getCommands(String clustername, Credential credentials, Configuration config, List<String> zookeeperHostnames, List<String> drpcHostnames, String nimbusHostname, String uiHostname) {
	  
		List<Statement> commands = new ArrayList<Statement>();
		
		// Install system tools, Java, etc.
		commands.addAll(SystemTools.init(PACKAGE_MANAGER.APT));

		// Configure IAM credentials
		// FIXME: this is lame.  Want to use an IAM role for the machines
		// but jclouds doesn't support IAM yet.  Can probably make it works
		// using: https://github.com/jclouds/jclouds-labs-aws/blob/jclouds-labs-aws-1.8.1/iam/src/test/java/org/jclouds/iam/features/RolePolicyApiLiveTest.java
		// but there are no docs yet and I've wasted too much time messing with already.
		commands.addAll(AWSCredentials.configure(config.getDeploymentLocation(), credentials.get_ec2_identity(), credentials.get_ec2_credential()));
		
		// Download and configure storm-deploy-alternative (before anything with supervision is started)
    commands.addAll(StormDeployAlternative.download(config.getSDACloudJarLocation(), config.getImageUsername()));
		commands.addAll(StormDeployAlternative.writeConfigurationFilesToRemote(Tools.getWorkDir() + "conf" + File.separator + "configuration.yaml", Tools.getWorkDir() + "conf" + File.separator + "credential.yaml"));
		commands.addAll(StormDeployAlternative.writeLocalSSHKeysToRemote(config.getSSHKeyName()));
		
		// Download Storm
		commands.addAll(Storm.download(config.getStormRemoteLocation()));
		
		// Download Zookeeper
		commands.addAll(Zookeeper.download(config.getZookeeperRemoteLocation()));
		
		// Download Ganglia
		commands.addAll(Ganglia.install());
		
		// Execute custom code, if user provided (pre config)
		if (config.getRemoteExecPreConfig().size() > 0)
			commands.addAll(Tools.runCustomCommands(config.getRemoteExecPreConfig()));
		
		// Configure Zookeeper (update configuration files)
		commands.addAll(Zookeeper.configure(zookeeperHostnames));
		
		// Configure Storm (update configuration files)
		commands.addAll(Storm.configure(nimbusHostname, zookeeperHostnames, drpcHostnames, config.getImageUsername()));
		
		// Configure Ganglia
		commands.addAll(Ganglia.configure(clustername, uiHostname));
				
		// Execute custom code, if user provided (post config)
		if (config.getRemoteExecPostConfig().size() > 0)
			commands.addAll(Tools.runCustomCommands(config.getRemoteExecPostConfig()));
		
		
		/**
		 * Start daemons (only on correct nodes, and under supervision)
		 */
		commands.addAll(Zookeeper.startDaemonSupervision(config.getImageUsername()));
		commands.addAll(Storm.startNimbusDaemonSupervision(config.getImageUsername()));
		commands.addAll(Storm.startSupervisorDaemonSupervision(config.getImageUsername()));
		commands.addAll(Storm.startUIDaemonSupervision(config.getImageUsername()));
		commands.addAll(Storm.startDRPCDaemonSupervision(config.getImageUsername()));
		commands.addAll(Storm.startLogViewerDaemonSupervision(config.getImageUsername()));
		commands.addAll(Ganglia.start());
		
		/**
		 * Start memory manager (to help share resources among Java processes)
		 * 	requires StormDeployAlternative is installed remotely
		 *  and user has specified he wants it running
		 */
		if (config.executeMemoryMonitor()){
			commands.addAll(StormDeployAlternative.runMemoryMonitor(config.getImageUsername()));
		}
		
		// Return commands
		return commands;
	}
}
