package dk.kaspergsm.stormdeploy.userprovided;

import java.io.File;
import java.util.HashMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import dk.kaspergsm.stormdeploy.Tools;

/**
 * Used to maintain credentials
 * 
 * @author Kasper Grud Skat Madsen
 */
public class Credential {
  
	private static Logger logger = LoggerFactory.getLogger(Credential.class);
	private String _identityEC2 = null, _credentialEC2 = null;
		
	public Credential(File f) {
		HashMap<String, Object> credentials = Tools.readYamlConf(f);
		if (credentials == null || credentials.size() == 0) {
			logger.error("No credentials found. Please ensure credentials.yaml exists");
			System.exit(0);
		}
		
		// Parse ec2 credentials
		if (credentials.containsKey("ec2-identity"))
			_identityEC2 = (String)credentials.get("ec2-identity");
		if (credentials.containsKey("ec2-credential"))
			_credentialEC2 = (String)credentials.get("ec2-credential");
		if ((_identityEC2 == null && _credentialEC2 != null) || (_identityEC2 != null && _credentialEC2 == null)) {
			logger.error("Incomplete credentials for Amazon EC2");
			System.exit(0);
		}		

	}
	
	public String get_ec2_identity() {
		return _identityEC2;
	}
	
	public String get_ec2_credential() {
		return _credentialEC2;
	}
}