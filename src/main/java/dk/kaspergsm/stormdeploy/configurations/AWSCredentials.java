package dk.kaspergsm.stormdeploy.configurations;

import static org.jclouds.scriptbuilder.domain.Statements.exec;

import java.util.ArrayList;
import java.util.Collection;

import org.jclouds.scriptbuilder.domain.Statement;

public class AWSCredentials {

	// This is lame.  Should really use IAM roles instead!!
	public static Collection<Statement> configure(String region, String key, String secret) {
		ArrayList<Statement> st = new ArrayList<Statement>();
		
		// Add AWS credentials to env so AWS API can create credentials.
		st.add(exec("echo \"export AWS_ACCESS_KEY_ID=" + key + "\" >> ~/.bashrc"));
		st.add(exec("echo \"export AWS_SECRET_ACCESS_KEY=" + secret + "\" >> ~/.bashrc"));
		st.add(exec("echo \"export AWS_DEFAULT_REGION=" + region + "\" >> ~/.bashrc"));

		// Read changes into current environment
		st.add(exec("source ~/.bashrc"));
		
		// ok, the above doesn't work when using storm.  Probably because storm doesn't get the env??
		// let's try creating a file too and see if that works.
		st.add(exec("cd ~"));
		st.add(exec("mkdir ~/.aws"));
		st.add(exec("touch ~/.aws/credentials"));
		st.add(exec("echo \"[default]\" >> ~/.aws/credentials"));
		st.add(exec("echo \"aws_access_key_id=" + key + "\" >> ~/.aws/credentials"));
		st.add(exec("echo \"aws_secret_access_key=" + secret + "\" >> ~/.aws/credentials"));

		return st;
	}

}
