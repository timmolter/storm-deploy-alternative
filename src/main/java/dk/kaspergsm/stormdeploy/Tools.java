package dk.kaspergsm.stormdeploy;

import static com.google.common.base.Charsets.UTF_8;
import static org.jclouds.scriptbuilder.domain.Statements.exec;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import org.jclouds.Constants;
import org.jclouds.ContextBuilder;
import org.jclouds.apis.ApiMetadata;
import org.jclouds.apis.Apis;
import org.jclouds.compute.ComputeService;
import org.jclouds.compute.ComputeServiceContext;
import org.jclouds.compute.RunScriptOnNodesException;
import org.jclouds.compute.config.ComputeServiceProperties;
import org.jclouds.compute.options.RunScriptOptions;
import org.jclouds.compute.predicates.NodePredicates;
import org.jclouds.domain.LoginCredentials;
import org.jclouds.enterprise.config.EnterpriseConfigurationModule;
import org.jclouds.logging.slf4j.config.SLF4JLoggingModule;
import org.jclouds.providers.ProviderMetadata;
import org.jclouds.providers.Providers;
import org.jclouds.scriptbuilder.domain.Statement;
import org.jclouds.scriptbuilder.domain.StatementList;
import org.jclouds.sshj.config.SshjSshClientModule;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.yaml.snakeyaml.Yaml;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Iterables;
import com.google.common.collect.Maps;
import com.google.common.io.Files;
import com.google.inject.Module;

import dk.kaspergsm.stormdeploy.userprovided.Configuration;
import dk.kaspergsm.stormdeploy.userprovided.Credential;

public class Tools {
	private static final String _workDir = System.getProperty("user.dir").endsWith("/") ? System.getProperty("user.dir") : System.getProperty("user.dir") + "/";
	private static final String _homeDir = System.getProperty("user.home").endsWith("/") ? System.getProperty("user.home") : System.getProperty("user.home") + "/";
	private static final Map<String, ProviderMetadata> _appProviders = Maps.uniqueIndex(Providers.viewableAs(ComputeServiceContext.class), Providers.idFunction());
	private static final Map<String, ApiMetadata> _allApis = Maps.uniqueIndex(Apis.viewableAs(ComputeServiceContext.class), Apis.idFunction());
	private static final Set<String> _allProviders = ImmutableSet.copyOf(Iterables.concat(_appProviders.keySet(), _allApis.keySet()));
	private static Logger log = LoggerFactory.getLogger(Tools.class);

	public static Set<String> getAllProviders() {
		return _allProviders;
	}
	
	/**
	 * Get login credentials (contains private ssh key)
	 */
	public static LoginCredentials getPrivateKeyCredentials(String username, String sshKeyName) {
		try {			
			return LoginCredentials.builder()
					.user(username)
					.authenticateSudo(false)
					.privateKey(Files.toString(new File(System.getProperty("user.home")  + File.separator + ".ssh"  + File.separator + sshKeyName), UTF_8).trim())
					.build();
		} catch (Exception ex) {
			log.error("Error reading ssh keys", ex);
			System.exit(0);
			return null;
		}
	}
	
	/**
	 * Get public key (raw)
	 * @param sshKeyName 
	 */
	public static String getPublicKey(String sshKeyName) {
		try {
			return Files.toString(new File(System.getProperty("user.home")  + File.separator + ".ssh" + File.separator + sshKeyName + ".pub"), UTF_8).trim();
		} catch (IOException ex) {
			log.error("Error reading ssh keys", ex);
			System.exit(0);
			return null;
		}
	}

	/**
	 * Initialize JClouds
	 */
	public static ComputeServiceContext initComputeServiceContext(Configuration conf, Credential cred) {
		Properties properties = new Properties();
		
		// Max time a script can take to execute
		properties.setProperty(ComputeServiceProperties.TIMEOUT_SCRIPT_COMPLETE, String.valueOf(TimeUnit.MILLISECONDS.convert(60, TimeUnit.MINUTES)));
		properties.setProperty(ComputeServiceProperties.TIMEOUT_PORT_OPEN, String.valueOf(TimeUnit.MILLISECONDS.convert(60, TimeUnit.MINUTES)));
		properties.setProperty(ComputeServiceProperties.TIMEOUT_NODE_RUNNING, String.valueOf(TimeUnit.MILLISECONDS.convert(60, TimeUnit.MINUTES)));

		properties.setProperty(Constants.PROPERTY_CONNECTION_TIMEOUT, String.valueOf(TimeUnit.MILLISECONDS.convert(60, TimeUnit.MINUTES)));
		properties.setProperty(Constants.PROPERTY_REQUEST_TIMEOUT, String.valueOf(TimeUnit.MILLISECONDS.convert(60, TimeUnit.MINUTES)));
		properties.setProperty(Constants.PROPERTY_MAX_CONNECTIONS_PER_HOST, "5");
		properties.setProperty(Constants.PROPERTY_MAX_CONNECTIONS_PER_CONTEXT, "20");
		properties.setProperty(Constants.PROPERTY_MAX_CONNECTION_REUSE, "10");
		properties.setProperty(Constants.PROPERTY_MAX_RETRIES, "999999");
		
		// inject ssh implementation
		Iterable<Module> modules = ImmutableSet.<Module> of(new SshjSshClientModule(), new SLF4JLoggingModule(), new EnterpriseConfigurationModule());
		return ContextBuilder.newBuilder("aws-ec2").credentials(cred.get_ec2_identity(), cred.get_ec2_credential()).modules(modules).overrides(properties).buildView(ComputeServiceContext.class);
	}
	
	/**
	 * Run set of queued commands now
	 */
	public static void executeOnNodes(List<Statement> commands, boolean runAsRoot, String clustername, ComputeService compute, String username, String sshKeyName) throws RunScriptOnNodesException, InterruptedException, ExecutionException, TimeoutException {
		compute.runScriptOnNodesMatching(
				NodePredicates.runningInGroup(clustername),
				new StatementList(commands),
				new RunScriptOptions()
					.nameTask("Setup")
				 	.overrideLoginCredentials(Tools.getPrivateKeyCredentials(username, sshKeyName))
				 	.wrapInInitScript(true)
				 	.overrideLoginUser(username)
				 	.blockOnComplete(true)
				 	.runAsRoot(runAsRoot));
	}
	
	public static String getWorkDir() {
		return _workDir;
	}
	
	public static String getHomeDir() {
		return _homeDir;
	}
	
	/**
	 * Get ports to open
	 * 	22 = SSH, 6627 = Thrift, 8080 = UI, 80 = GANGLIA UI, 8000 = Logviewer, 3772 = DRPC
	 */
	public static int[] getPortsToOpen() {
            return new int[]{22, 6627, 8080, 80, 8000, 3772};
	}
	
	@SuppressWarnings("unchecked")
	public static HashMap<String, Object> readYamlConf(File f) {
		// Create parser
		Yaml yaml = new Yaml();
		
		// Read file
		String fileContent = readFile(f.getAbsolutePath());
		
		// Parse and return
		return (HashMap<String, Object>) yaml.load(fileContent);
	}
	
	private static String readFile(String filePath) {
		StringBuffer fileData = new StringBuffer(1000);
		try {
			BufferedReader reader = new BufferedReader(new FileReader(filePath));
			char[] buf = new char[1024];
			int numRead = 0;
			while ((numRead = reader.read(buf)) != -1) {
				String readData = String.valueOf(buf, 0, numRead);
				fileData.append(readData);
				buf = new char[1024];
			}
			reader.close();
		} catch (Exception ex) {
			System.out.println(ex.toString());
		}
		return fileData.toString();
	}

	
	/**
	 * Run set of custom commands
	 */
	public static List<Statement> runCustomCommands(List<String> commands) {
		ArrayList<Statement> st = new ArrayList<Statement>();
		for (String command : commands)
			st.add(exec(command));
		return st;
	}

	/**
	 * Used to read local file, and echo into remote file
	 */
	public static List<Statement> echoFile(String localPath, String remotePath) {
		ArrayList<Statement> st = new ArrayList<Statement>();
		for (String l : readFile(localPath).split("\n"))
			st.add(exec("echo '" + l + "' >> " + remotePath));
		return st;
	}
	
	public static List<Statement> download(String localPath, String remotePath, boolean extract, boolean delete) {
		return download(localPath, remotePath, extract, delete, null);
	}
	
	/**
	 * Download, extract, remove and rename if necessary
	 * RemotePath should always be downloadable by wget
	 */
	public static List<Statement> download(String localPath, String remotePath, boolean extract, boolean delete, String finalName) {
		ArrayList<Statement> st = new ArrayList<Statement>();
		st.add(exec("cd " + localPath));
		
		// Extract filename
		String filename = remotePath.substring(remotePath.lastIndexOf("/") + 1);
		
		// Download file
		st.add(exec("wget -N " + remotePath));
		
		// Extract file
		if (extract) {
			st.add(exec("tar -zxf " + filename));
		}
		
		// Delete file
		if (delete) {
			st.add(exec("rm " + filename));
		}
		
		if (finalName != null) {
			 int index = filename.lastIndexOf(".tar");
			 if (index > 0) {
				 filename = filename.substring(0, filename.lastIndexOf(".tar"));
			 }
			 String testAndMove = "[ ! -e " + finalName + " ] && mv " + filename + " " + finalName;
			 st.add(exec(testAndMove));
		}
		return st;
	}
	
	/**
	 * @param cond	must contain all between []; of bash if,then
	 * @param exec	to execute, if cond==true.
	 */
	public static String conditionalExec(String cond, String exec) {
		if (exec.endsWith(";"))
			exec = exec.substring(0, exec.lastIndexOf(";"));
		return "if [ " + cond + " ]; then " + exec + "; fi";
	}
	
	public static Statement execOnUI(String cmd) {
		return exec("case $(head -n 1 ~/daemons) in *UI*) " + cmd + " ;; esac");
	}
}
