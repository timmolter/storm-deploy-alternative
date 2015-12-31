package dk.kaspergsm.stormdeploy.configurations;

import static org.jclouds.scriptbuilder.domain.Statements.exec;

import java.util.ArrayList;
import java.util.List;

import org.jclouds.scriptbuilder.domain.Statement;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Contains all methods to configure system tools on nodes.
 * 	If tools already exists, this takes almost no time!
 * 
 * @author Kasper Grud Skat Madsen
 */
public class SystemTools {
	public enum PACKAGE_MANAGER {APT};	
	private static Logger log = LoggerFactory.getLogger(SystemTools.class);
	
	public static List<Statement> init(PACKAGE_MANAGER pm) {
		ArrayList<Statement> st = new ArrayList<Statement>();
		if (pm == PACKAGE_MANAGER.APT) {
			
			// Enable multiverse
			st.add(exec("sed -i \"/^# deb.*multiverse/ s/^# //\" /etc/apt/sources.list"));
			
			// Hide interactive installation prompts
			st.add(exec("export DEBIAN_FRONTEND=noninteractive"));
			
			// Init apt
			st.add(exec("apt-get update -y"));
			//st.add(exec("apt-get upgrade -y")); - user can add to remote-exec-preconfig if needed
			
			// Install JDK (OpenJDK 7)
			st.add(exec("apt-get install -y openjdk-7-jdk"));
			st.add(exec("export JAVA_HOME=$(dirname $(dirname $(find `ls -d /usr/lib/jvm/* | sort -k1 -r` -name 'javac' | head -1)))"));
			st.add(exec("update-alternatives --set java $JAVA_HOME/jre/bin/java"));
			
			// Install ant
			st.add(exec("apt-get install -y ant"));
			
			// Install git
			st.add(exec("apt-get install -y git"));
			
			// Install build-tools
			st.add(exec("apt-get install -y build-essential"));
			st.add(exec("apt-get install -y uuid-dev"));
			st.add(exec("apt-get install -y pkg-config"));
			st.add(exec("apt-get install -y libtool"));
			st.add(exec("apt-get install -y automake1.10"));
			st.add(exec("apt-get install -y unzip"));
			st.add(exec("update-alternatives --set automake /usr/bin/automake-1.10"));
			
		} else {
			log.error("PACKAGE MANAGER not supported: " + pm.toString());
		}
		
		return st;
	}
}
