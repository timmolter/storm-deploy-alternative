package dk.kaspergsm.stormdeploy.configurations;

import static org.jclouds.scriptbuilder.domain.Statements.exec;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import org.jclouds.scriptbuilder.domain.Statement;
import dk.kaspergsm.stormdeploy.Tools;

/**
 * Contains all methods to configure StormDeployAlternative on remote node
 * 
 * @author Kasper Grud Skat Madsen
 */
public class StormDeployAlternative {

	public static List<Statement> download() {
		return Tools.download("~", "https://s3-eu-west-1.amazonaws.com/storm-deploy-alternative/sda.tar.gz", true, true);
	}
	
	/**
	 * Run memoryMonitor.
	 * 	Requires tools.jar from active jvm is on path. Is automatically searched and found if it exists in /usr/lib/jvm
	 */
	public static List<Statement> runMemoryMonitor(String username) {
		ArrayList<Statement> st = new ArrayList<Statement>();
		st.add(exec("su -c 'java -cp \"/home/"+username+"/sda/storm-deploy-alternative.jar:$( find `ls -d /usr/lib/jvm/* | sort -k1 -r` -name tools.jar | head -1 )\" dk.kaspergsm.stormdeploy.image.MemoryMonitor &' - " + username));
		return st;
	}
	
	public static List<Statement> writeConfigurationFiles(String localConfigurationFile, String localCredentialFile) {	
		ArrayList<Statement> st = new ArrayList<Statement>();
		st.add(exec("mkdir ~/sda/conf"));
		st.addAll(Tools.echoFile(localConfigurationFile, "~/sda/conf/configuration.yaml"));
		st.addAll(Tools.echoFile(localCredentialFile, "~/sda/conf/credential.yaml"));
		return st;
	}
	
	public static List<Statement> writeLocalSSHKeys(String sshKeyName) {
		ArrayList<Statement> st = new ArrayList<Statement>();
		st.add(exec("mkdir ~/.ssh/"));
		st.addAll(Tools.echoFile(Tools.getHomeDir() + ".ssh" + File.separator + sshKeyName, "~/.ssh/id_rsa"));
		st.addAll(Tools.echoFile(Tools.getHomeDir() + ".ssh" + File.separator + sshKeyName + ".pub", "~/.ssh/id_rsa.pub"));
		return st;
	}
}