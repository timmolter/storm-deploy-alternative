package dk.kaspergsm.stormdeploy.configurations;

import static org.jclouds.scriptbuilder.domain.Statements.exec;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import org.jclouds.scriptbuilder.domain.Statement;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import dk.kaspergsm.stormdeploy.configurations.SystemTools.PACKAGE_MANAGER;

/**
 * All logic to configure s3cmd on nodes
 * 
 * @author Kasper Grud Skat Madsen
 */
public class S3CMD {
	private static Random r = new Random();
	private static Logger log = LoggerFactory.getLogger(S3CMD.class);
	
	public static List<Statement> install(PACKAGE_MANAGER pm) {
		ArrayList<Statement> st = new ArrayList<Statement>();
		if (pm == PACKAGE_MANAGER.APT) {
			st.add(exec("wget -O- -q http://s3tools.org/repo/deb-all/stable/s3tools.key | apt-key add -"));
			st.add(exec("wget -O/etc/apt/sources.list.d/s3tools.list http://s3tools.org/repo/deb-all/stable/s3tools.list"));
			st.add(exec("apt-get update && apt-get install s3cmd"));
			return st;
		} else {
			log.error("PACKAGE MANAGER not supported: " + pm.toString());
		}
		return st;
	}
	
	/**
	 * Returns commands to configure credentials
	 * 
	 * Unfortunately there is no way to automatically create the .s3cfg file.
	 * The approach taken here, is to write a default version.
	 */
	public static List<Statement> configure(String identity, String credential) {
		ArrayList<Statement> st = new ArrayList<Statement>();
		st.add(exec("cd ~"));
		st.add(exec("rm .s3cfg"));
		st.add(exec("touch .s3cfg"));
		st.add(exec("echo \"" + "[default]" + "\" >> .s3cfg"));
		st.add(exec("echo \"" + "access_key = " + identity + "\" >> .s3cfg"));
		st.add(exec("echo \"" + "bucket_location = US" + "\" >> .s3cfg"));
		st.add(exec("echo \"" + "cloudfront_host = cloudfront.amazonaws.com" + "\" >> .s3cfg"));
		st.add(exec("echo \"" + "cloudfront_resource = /2010-07-15/distribution" + "\" >> .s3cfg"));
		st.add(exec("echo \"" + "default_mime_type = binary/octet-stream" + "\" >> .s3cfg"));
		st.add(exec("echo \"" + "delete_removed = False" + "\" >> .s3cfg"));
		st.add(exec("echo \"" + "dry_run = False" + "\" >> .s3cfg"));
		st.add(exec("echo \"" + "encoding = UTF-8" + "\" >> .s3cfg"));
		st.add(exec("echo \"" + "encrypt = False" + "\" >> .s3cfg"));
		st.add(exec("echo \"" + "follow_symlinks = False" + "\" >> .s3cfg"));
		st.add(exec("echo \"" + "force = False" + "\" >> .s3cfg"));
		st.add(exec("echo \"" + "get_continue = False" + "\" >> .s3cfg"));
		st.add(exec("echo \"" + "gpg_command = /usr/bin/gpg" + "\" >> .s3cfg"));
		st.add(exec("echo \"" + "gpg_decrypt = %(gpg_command)s -d --verbose --no-use-agent --batch --yes --passphrase-fd %(passphrase_fd)s -o %(output_file)s %(input_file)s" + "\" >> .s3cfg"));
		st.add(exec("echo \"" + "gpg_encrypt = %(gpg_command)s -c --verbose --no-use-agent --batch --yes --passphrase-fd %(passphrase_fd)s -o %(output_file)s %(input_file)s" + "\" >> .s3cfg"));
		st.add(exec("echo \"" + "gpg_passphrase = " + r.nextInt(99999) + "\" >> .s3cfg"));
		st.add(exec("echo \"" + "guess_mime_type = True" + "\" >> .s3cfg"));
		st.add(exec("echo \"" + "host_base = s3.amazonaws.com" + "\" >> .s3cfg"));
		st.add(exec("echo \"" + "host_bucket = %(bucket)s.s3.amazonaws.com" + "\" >> .s3cfg"));
		st.add(exec("echo \"" + "human_readable_sizes = False" + "\" >> .s3cfg"));
		st.add(exec("echo \"" + "list_md5 = False" + "\" >> .s3cfg"));
		st.add(exec("echo \"" + "log_target_prefix = " + "\" >> .s3cfg"));
		st.add(exec("echo \"" + "preserve_attrs = True" + "\" >> .s3cfg"));
		st.add(exec("echo \"" + "progress_meter = True" + "\" >> .s3cfg"));
		st.add(exec("echo \"" + "proxy_host = " + "\" >> .s3cfg"));
		st.add(exec("echo \"" + "proxy_port = 0" + "\" >> .s3cfg"));
		st.add(exec("echo \"" + "recursive = False" + "\" >> .s3cfg"));
		st.add(exec("echo \"" + "recv_chunk = 4096" + "\" >> .s3cfg"));
		st.add(exec("echo \"" + "reduced_redundancy = False" + "\" >> .s3cfg"));
		st.add(exec("echo \"" + "secret_key = " + credential + "\" >> .s3cfg"));
		st.add(exec("echo \"" + "send_chunk = 4096" + "\" >> .s3cfg"));
		st.add(exec("echo \"" + "simpledb_host = sdb.amazonaws.com" + "\" >> .s3cfg"));
		st.add(exec("echo \"" + "skip_existing = False" + "\" >> .s3cfg"));
		st.add(exec("echo \"" + "socket_timeout = 10" + "\" >> .s3cfg"));
		st.add(exec("echo \"" + "urlencoding_mode = normal" + "\" >> .s3cfg"));
		st.add(exec("echo \"" + "use_https = True" + "\" >> .s3cfg"));
		st.add(exec("echo \"" + "verbosity = WARNING" + "\" >> .s3cfg"));
		return st;
	}
}