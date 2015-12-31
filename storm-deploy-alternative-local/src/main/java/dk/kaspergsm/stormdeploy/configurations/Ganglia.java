package dk.kaspergsm.stormdeploy.configurations;

import static org.jclouds.scriptbuilder.domain.Statements.exec;
import java.util.ArrayList;
import java.util.List;
import org.jclouds.scriptbuilder.domain.Statement;
import dk.kaspergsm.stormdeploy.Tools;

/**
 * Contains all methods to install Ganglia
 * 
 * @author Kasper Grud Skat Madsen
 */
public class Ganglia {

	/**
	 * Install Ganglia
	 */
	public static List<Statement> install() {
		ArrayList<Statement> st = new ArrayList<Statement>();
		
		// Install monitoring base
		st.add(exec("apt-get install -y ganglia-monitor gmetad rrdtool librrds-perl librrd-dev"));
		
		// Install webinterface only on one node
		st.add(Tools.execOnUI("apt-get install -q -y ganglia-webfrontend"));
		
		// Ensure daemons have not been started
		st.add(exec("/etc/init.d/ganglia-monitor stop"));
		st.add(exec("/etc/init.d/gmetad stop"));
		
		return st;
	}

	/**
	 * Configure Ganglia
	 */
	public static List<Statement> configure(String clustername, String uiHostname) {
		ArrayList<Statement> st = new ArrayList<Statement>();
		
		// Strip top of configurationfile
		st.add(exec("sed \'1,/Each metrics module that is referenced/d\' /etc/ganglia/gmond.conf > /etc/ganglia/stripped_gmond.conf"));
		
		// Write configuration
		st.add(exec("echo \"" + getConfiguration(clustername, uiHostname) + "\" > /etc/ganglia/gmond.conf"));
		
		// Append stripped_gmond.conf
		st.add(exec("cat /etc/ganglia/stripped_gmond.conf >> /etc/ganglia/gmond.conf"));
		
		// In case node is containing UI, it should be deaf = no!
		st.add(Tools.execOnUI("sed \"s/deaf = yes/deaf = no/\" -i \"/etc/ganglia/gmond.conf\""));
		
		// In case node is containing UI, it should create /ganglia alias for apache2 server
		st.add(Tools.execOnUI("cp /etc/ganglia-webfrontend/apache.conf /etc/apache2/sites-enabled/"));
		
		// In case node is containing UI, it should add cluster as datasource
		st.add(Tools.execOnUI("echo data_source \"" + clustername + "\" localhost >> /etc/ganglia/gmetad.conf"));
		
		// In case node is containing UI, it should modify auto_system to disabled. This allows events to be added externally to Ganglia
		st.add(Tools.execOnUI("sed \"s/$conf\\['auth_system'\\] = 'readonly'/$conf\\['auth_system'\\] = 'disabled'/\" -i \"/usr/share/ganglia-webfrontend/conf_default.php\""));
		
		// In case node is containing UI, it should make events.json writable by apache webserver
		st.add(Tools.execOnUI("chmod 777 /var/lib/ganglia-web/conf/events.json"));
		
		return st;
	}
	
	/**
	 * Start daemons
	 */
	public static List<Statement> start() {
		ArrayList<Statement> st = new ArrayList<Statement>();
		
		// In case node is containing UI, it should enable module_rewrite for apache2
		st.add(Tools.execOnUI("a2enmod rewrite"));
		
		// In case node is containing UI, it should restart apache2 webserver
		st.add(Tools.execOnUI("/etc/init.d/apache2 restart"));
		
		st.add(exec("/etc/init.d/ganglia-monitor restart"));
		st.add(exec("/etc/init.d/gmetad restart"));
		return st;
	}
	
	private static String getConfiguration(String clustername, String uiHostname) {
		return 
		"globals {" + "\n" +
		"  daemonize = yes" + "\n" +
		"  setuid = yes" + "\n" +
		"  user = ganglia" + "\n" +
		"  debug_level = 0" + "\n" +
		"  max_udp_msg_len = 1472" + "\n" +
		"  mute = no" + "\n" +
		"  deaf = yes" + "\n" +
		"  allow_extra_data = yes" + "\n" +
		"  host_dmax = 86400 /* Remove host from UI after it hasn't report for a day */" + "\n" +
		"  cleanup_threshold = 300 /*secs */" + "\n" +
		"  gexec = no" + "\n" +
		"  send_metadata_interval = 30 /*secs */" + "\n" +
		"}" + "\n" +
		"" + "\n" +
		"cluster {" + "\n" +
		"  name = \\\"" + clustername + "\\\"" + "\n" +
		"  owner = \\\"unspecified\\\"" + "\n" +
		"  latlong = \\\"unspecified\\\"" + "\n" +
		"  url = \\\"unspecified\\\"" + "\n" +
		"}" + "\n" +
		"" + "\n" +
		"host {" + "\n" +
		"  location = \\\"unspecified\\\"" + "\n" +
		"}" + "\n" +
		"" + "\n" +
		"udp_send_channel {" + "\n" +
		"  host = " + uiHostname + "\n" +
		"  port = 8649" + "\n" +
		"  ttl = 1" + "\n" +
		"}" + "\n" +
		"" + "\n" +
		"udp_recv_channel {" + "\n" + 
		"  port = 8649" + "\n" +
		"}" + "\n" +
		"" + "\n" +
		"tcp_accept_channel {" + "\n" +
		"  port = 8649" + "\n" +
		"}" + "\n" +
		"/* Each metrics module that is referenced by gmond must be specified and\n"; // Removed when stripping with sed
	}
}
