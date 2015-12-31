package dk.kaspergsm.stormdeploy.configurations;

import static org.jclouds.scriptbuilder.domain.Statements.exec;
import java.util.ArrayList;
import java.util.List;
import org.jclouds.scriptbuilder.domain.Statement;
import dk.kaspergsm.stormdeploy.Tools;

/**
 * Contains all methods to configure Zookeeper on nodes
 * 
 * @author Kasper Grud Skat Madsen
 */
public class Zookeeper {

	public static List<Statement> download(String zookeeperRemoteLocation) {
		return Tools.download("~/", zookeeperRemoteLocation, true, true, "zookeeper");
	}

	public static List<Statement> configure(List<String> zkNodesHostnames) {
		ArrayList<Statement> st = new ArrayList<Statement>();
		StringBuilder sb = new StringBuilder();
		for (int i = 1; i <= zkNodesHostnames.size(); i++) {
			sb.append("server.");
			sb.append(i);
			sb.append("=");
			sb.append(zkNodesHostnames.get(i-1));
			sb.append(":2888:3888");
			if (i != zkNodesHostnames.size())
				sb.append("\\n"); // escaped newline
		}
		st.add(exec("cd ~/zookeeper/conf/"));
		st.add(exec("[ ! -e zoo.cfg ] && cp zoo_sample.cfg zoo.cfg && echo -e \"# the zookeeper ensemble\nserver.x\" >> zoo.cfg"));
		st.add(exec("sed \"s|dataDir=.*|dataDir=/tmp/zktmp|\" -i \"zoo.cfg\""));	// set dataDir to /tmp/zktmp
		st.add(exec("sed \"s/server.*/server.x/\" -i \"zoo.cfg\""));				// convert each serverline to server.x
		st.add(exec("sed '$!N; /^\\(.*\\)\\n\\1$/!P; D' -i \"zoo.cfg\""));			// delete duplicate lines => one server.x
		st.add(exec("sed \"s/server.x/" + sb.toString() + "/\" -i \"zoo.cfg\""));	// replace server.x with new lines
		return st;
	}
	
	public static List<Statement> writeZKMyIds(String username, Integer zkid) {
		ArrayList<Statement> st = new ArrayList<Statement>();
		st.add(exec("mkdir -p /tmp/zktmp"));												// ensure folders exist
		st.add(exec("chown " + username + " /tmp/zktmp"));
		st.add(exec("echo " + zkid + " > /tmp/zktmp/myid"));								// write myid
		return st;
	}
	
	/**
	 * Uses Monitor to restart daemon, if it stops
	 */
	public static List<Statement> startDaemonSupervision(String username) {
		ArrayList<Statement> st = new ArrayList<Statement>();
		st.add(exec("cd ~"));
		st.add(exec("su -c 'case $(head -n 1 ~/daemons) in *ZK*) java -cp \"sda/storm-deploy-alternative.jar\" dk.kaspergsm.stormdeploy.image.ProcessMonitor org.apache.zookeeper.server zookeeper/bin/zkServer.sh start ;; esac &' - " + username));
		return st;
	}
}
