package dk.kaspergsm.stormdeploy.commands;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeoutException;
import org.jclouds.compute.ComputeService;
import org.jclouds.compute.ComputeServiceContext;
import org.jclouds.compute.RunScriptOnNodesException;
import org.jclouds.compute.domain.NodeMetadata;
import org.jclouds.compute.domain.NodeMetadata.Status;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import dk.kaspergsm.stormdeploy.LaunchNodeThread;
import dk.kaspergsm.stormdeploy.Tools;
import dk.kaspergsm.stormdeploy.configurations.NodeConfiguration;
import dk.kaspergsm.stormdeploy.configurations.Storm;
import dk.kaspergsm.stormdeploy.userprovided.Configuration;
import dk.kaspergsm.stormdeploy.userprovided.Credential;

/**
 * Called to deploy a new cluster
 * 
 * @author Kasper Grud Skat Madsen
 */
public class Deploy {
	private static Logger log = LoggerFactory.getLogger(Deploy.class);
	
	/**
	 * Call to deploy a cluster
	 */
	@SuppressWarnings("unchecked")
	public static void deploy(String clustername, Credential credentials, Configuration config, ComputeServiceContext computeContext) {
		
		
		/**
		 * Check no cluster with clustername is currently deployed
		 */
		for (NodeMetadata n : (Set<NodeMetadata>) computeContext.getComputeService().listNodes()) {
			if (n.getStatus() != Status.TERMINATED && n.getGroup() != null &&
					n.getGroup().toLowerCase().equals(clustername.toLowerCase())) {
				
				// Currently running cluster with same name was detected
				log.error("Cluster with name " + clustername + " is already deployed");
				return;
			}
		}
		
		
		/**
		 * Start instances
		 */
		log.info("Deploying cluster: " + clustername);
		HashMap<Integer, NodeMetadata> newNodes = startNodesNow(config, computeContext.getComputeService(), clustername);
		
		
		/**
		 * Attach
		 */
		try {
			log.info("Attaching to cluster");
			
			String uiPublicAddress = "";
			if (getUINode(config, newNodes) != null)
				uiPublicAddress = getUINode(config, newNodes).getPublicAddresses().iterator().next();
			
			Storm.writeStormAttachConfigFiles(
					getNewInstancesPublicIp(config, "ZK", newNodes), 
					getNewInstancesPublicIp(config, "WORKER", newNodes), 
					getNimbusNode(config, newNodes).getPublicAddresses().iterator().next(),
					uiPublicAddress, 
					clustername);
		} catch (IOException ex) {
			log.error("Problem attaching to cluster", ex);
		}
		
		
		/**
		 * Configure all nodes
		 */
		try {
			log.info("Configuring instance(s)");
			Tools.executeOnNodes(
					NodeConfiguration.getCommands(
							clustername,
							credentials,
							config, 
							getNewInstancesPrivateIp(config, "ZK", newNodes), 
							getNewInstancesPrivateIp(config, "DRPC", newNodes), 
							getNimbusNode(config, newNodes).getPrivateAddresses().iterator().next(), 
							getUINode(config, newNodes).getPrivateAddresses().iterator().next()),
					true,
					clustername, 
					computeContext.getComputeService(),
					config.getImageUsername(),
					config.getSSHKeyName());
		} catch (RunScriptOnNodesException ex) {
			log.error("Problem configuring instance(s)", ex);
		} catch (InterruptedException ex) {
			log.error("Problem configuring instance(s)", ex);
		} catch (ExecutionException ex) {
			log.error("Problem configuring instance(s)", ex);
		} catch (TimeoutException ex) {
			log.error("Problem configuring instance(s)", ex);
		}
		
		
		/**
		 * Print final info
		 */
		log.info("User: " + config.getImageUsername());
		log.info("Started:");
		for (NodeMetadata n : newNodes.values())
			log.info("\t" + n.getPublicAddresses().iterator().next() + "\t" + n.getUserMetadata().get("daemons").toString());
		log.info("Storm UI: http://" + getUINode(config, newNodes).getPublicAddresses().iterator().next() + ":8080");
		log.info("Ganglia UI: http://" + getUINode(config, newNodes).getPublicAddresses().iterator().next() + "/ganglia");
		
		/**
		 * Close application now
		 */
		System.exit(0);
	}
	
	private static NodeMetadata getNimbusNode(Configuration config, HashMap<Integer, NodeMetadata> nodes) {
		for (NodeMetadata n : nodes.values()) {
			if (n.getUserMetadata().containsKey("daemons") && n.getUserMetadata().get("daemons").contains("MASTER"))
				return n;
		}
		return null;
	}
	
	private static NodeMetadata getUINode(Configuration config, HashMap<Integer, NodeMetadata> nodes) {
		for (NodeMetadata n : nodes.values()) {
			if (n.getUserMetadata().containsKey("daemons") && n.getUserMetadata().get("daemons").contains("UI"))
				return n;
		}
		return null;
	}
	
	private static ArrayList<String> getNewInstancesPublicIp(Configuration config, String daemon, HashMap<Integer, NodeMetadata> nodes) {
		ArrayList<Integer> nodeIds = new ArrayList<Integer>(nodes.keySet());
		Collections.sort(nodeIds);
		
		ArrayList<String> newNodes = new ArrayList<String>();
		for (int nodeid : nodeIds) {
			NodeMetadata n = nodes.get(nodeid);
			if (n.getUserMetadata().containsKey("daemons") && n.getUserMetadata().get("daemons").contains(daemon))
				newNodes.add(n.getPublicAddresses().iterator().next());
		}
		return newNodes;
	}
	
	private static ArrayList<String> getNewInstancesPrivateIp(Configuration config, String daemon, HashMap<Integer, NodeMetadata> nodes) {
		ArrayList<Integer> nodeIds = new ArrayList<Integer>(nodes.keySet());
		Collections.sort(nodeIds);
		
		ArrayList<String> newNodes = new ArrayList<String>();
		for (int nodeid : nodeIds) {
			NodeMetadata n = nodes.get(nodeid);
			if (n.getUserMetadata().containsKey("daemons") && n.getUserMetadata().get("daemons").contains(daemon))
				newNodes.add(n.getPrivateAddresses().iterator().next());
		}
		return newNodes;
	}
	
	private static HashMap<Integer, NodeMetadata> startNodesNow(Configuration config, ComputeService compute, String clustername) {	

		// To maintain worker threads
		ArrayList<LaunchNodeThread> workerThreads = new ArrayList<LaunchNodeThread>();
				
		/**
		 * Loop each unique set of daemons
		 */
		for (Entry<ArrayList<String>, ArrayList<Integer>> daemonsToNodeIds : config.getDaemonsToNodeIds().entrySet()) {
			
			// Create instanceType -> List[nodeIds]
			HashMap<String, ArrayList<Integer>> instanceTypeToNodeIdsToStart = new HashMap<String, ArrayList<Integer>>();
			for (Integer nodeId : daemonsToNodeIds.getValue()) {
				String curInstanceType = config.getNodeIdToInstanceType().get(nodeId);
				if (!instanceTypeToNodeIdsToStart.containsKey(curInstanceType))
					instanceTypeToNodeIdsToStart.put(curInstanceType, new ArrayList<Integer>());
				instanceTypeToNodeIdsToStart.get(curInstanceType).add(nodeId);
			}
			
			// Iterate all different types of instanceTypes for the nodes to start
			for (String instanceType : instanceTypeToNodeIdsToStart.keySet()) {
				
				// If any of the daemons are ZK, launch one node at a time and write zkid
				if (daemonsToNodeIds.getKey().contains("ZK")) {
					
					// Loop each nodeid to start
					for (int nodeIdToStartWithZK : instanceTypeToNodeIdsToStart.get(instanceType)) {
						log.info("Starting 1 instance of type " + instanceType + " with daemons " + daemonsToNodeIds.getKey().toString());
						workerThreads.add(new LaunchNodeThread(
								compute,
								config,
								instanceType, 
								clustername, 
								new ArrayList<Integer>(Arrays.asList(nodeIdToStartWithZK)),
								daemonsToNodeIds.getKey(), config.getNodeIdToZkId().get(nodeIdToStartWithZK)));
					}
					
				} else {
					log.info("Starting " + instanceTypeToNodeIdsToStart.get(instanceType).size() + " instance(s) of type " + instanceType + " with daemons " + daemonsToNodeIds.getKey().toString());
					workerThreads.add(new LaunchNodeThread(
							compute, 
							config,
							instanceType, 
							clustername, 
							instanceTypeToNodeIdsToStart.get(instanceType), 
							daemonsToNodeIds.getKey(), null));	
				}
			}
		}
		
		// Wait until nodes have started
		boolean allOK = false;
		while (!allOK) {
			allOK = true;
			for (LaunchNodeThread l : workerThreads) {
				if (l.getNewNodes() == null)
					allOK = false;
			}
			
			try {
				Thread.sleep(1000);
			} catch (InterruptedException e) {}
		}
		
		// Collect information about started nodes
		HashMap<Integer, NodeMetadata> ret = new HashMap<Integer, NodeMetadata>();
		for (LaunchNodeThread l : workerThreads) {
			int i = 0;
			for (NodeMetadata n : l.getNewNodes())
				ret.put(l.getNodeIds().get(i++), n);
		}
		return ret;
	}
}
