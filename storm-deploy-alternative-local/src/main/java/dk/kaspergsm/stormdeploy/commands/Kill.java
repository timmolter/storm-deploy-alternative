package dk.kaspergsm.stormdeploy.commands;

import java.util.Set;
import org.jclouds.compute.ComputeService;
import org.jclouds.compute.domain.NodeMetadata;
import org.jclouds.compute.domain.NodeMetadata.Status;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Kill {
	private static Logger log = LoggerFactory.getLogger(Kill.class);

	@SuppressWarnings("unchecked")
	public static void kill(String clustername, ComputeService computeService) {
		
		int nodesToKill = 0;
		for (NodeMetadata n : (Set<NodeMetadata>) computeService.listNodes()) {
			if (n.getStatus() != Status.TERMINATED &&
					n.getGroup() != null &&
					n.getGroup().toLowerCase().equals(clustername.toLowerCase()) &&
					n.getUserMetadata().containsKey("daemons")) {

				// Destroy now
				computeService.destroyNode(n.getId());
				nodesToKill++;
			}
		}
		
		log.info("Terminated " + nodesToKill + " nodes");
	}
}
