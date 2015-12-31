package dk.kaspergsm.stormdeploy.configurations;

import static org.jclouds.scriptbuilder.domain.Statements.exec;
import java.util.ArrayList;
import java.util.List;
import org.jclouds.scriptbuilder.domain.Statement;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import dk.kaspergsm.stormdeploy.Tools;
import dk.kaspergsm.stormdeploy.configurations.SystemTools.PACKAGE_MANAGER;

/**
 * All logic to configure ec2-tools on nodes
 * 
 * @author Kasper Grud Skat Madsen
 */
public class EC2Tools {
	private static Logger log = LoggerFactory.getLogger(EC2Tools.class);
	
	public static List<Statement> install(PACKAGE_MANAGER pm) {
		ArrayList<Statement> st = new ArrayList<Statement>();
		if (pm == PACKAGE_MANAGER.APT) {
			st.add(exec("apt-get install ec2-api-tools"));
			return st;
		} else {
			log.error("PACKAGE MANAGER not supported: " + pm.toString());
		}
		return st;
	}
	
	/**
	 * Returns commands to configure credentials
	 */
	public static List<Statement> configure(String certPath, String privPath, String region, String jobname) {
		ArrayList<Statement> st = new ArrayList<Statement>();
		
		// Write credentials
		st.add(exec("mkdir ~/.ec2"));
		st.addAll(Tools.echoFile(certPath, "~/.ec2/cert.pem"));
		st.addAll(Tools.echoFile(privPath, "~/.ec2/priv.pem"));
		
		// Write configuration to bashrc (for logging in)
		st.add(exec("echo \"export EC2_KEYPAIR=jclouds#" + jobname + "\" >> ~/.bashrc")); // Export name of keypair to use
		st.add(exec("echo \"export EC2_URL=https://ec2." + region + ".amazonaws.com\" >> ~/.bashrc")); // Export region url
		st.add(exec("echo \"export EC2_PRIVATE_KEY=~/.ec2/priv.pem\" >> ~/.bashrc")); // Export location of x509 credentials
		st.add(exec("echo \"export EC2_CERT=~/.ec2/cert.pem\" >> ~/.bashrc")); // Export location of x509 credentials
		
		// Write configuration to profile (for wider shell support)
		st.add(exec("echo \"export EC2_KEYPAIR=jclouds#" + jobname + "\" >> ~/.profile")); // Export name of keypair to use
		st.add(exec("echo \"export EC2_URL=https://ec2." + region + ".amazonaws.com\" >> ~/.profile")); // Export region url
		st.add(exec("echo \"export EC2_PRIVATE_KEY=~/.ec2/priv.pem\" >> ~/.profile")); // Export location of x509 credentials
		st.add(exec("echo \"export EC2_CERT=~/.ec2/cert.pem\" >> ~/.profile")); // Export location of x509 credentials
		
		// Read changes into current environment
		st.add(exec("source ~/.bashrc"));
		
		return st;
	}
}
