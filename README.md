Fast tool to deploy [Storm](https://github.com/apache/incubator-storm) on [Amazon EC2](http://aws.amazon.com/ec2/), written entirely in Java.

_Please don't hesitate to contact me. Your feedback will help to further improve this tool._

## Features
+ Runs Storm and Zookeeper daemons under supervision (automatically restarted in case of failure)
+ Only fetch and compile what is needed (can deploy on prepared images in a few minutes)
+ Supports executing user-defined commands both pre-config and post-config
+ Automatically sets up s3cmd, making it easy to get/put files on [Amazon S3](http://aws.amazon.com/s3/)
+ Automatically sets up [Ganglia](http://ganglia.sourceforge.net/), making it easy to monitor performance
+ Automatically sets up [Amazon EC2 AMI Tools](http://docs.aws.amazon.com/AWSEC2/latest/CommandLineReference/ami-tools.html) on new nodes
+ Supports Zookeeper versions: _3.4.5_ & _3.4.6_
+ Supports Storm versions: _0.8.2_ & _0.9.0.1_ & _0.9.2_ & _0.9.3_ & _0.9.4_ & _0.9.5_

## Configuration
This tool, requires two configurationfiles: `conf/credential.yaml` and `conf/configuration.yaml`. Put your credentials into the file `conf/credential.yaml`. It's required that you have generated an SSH key-pair in `~/.ssh` with an empty pass phrase.

Below is an example of a single cluster configuration, for `conf/configuration.yaml`

```
mycluster:
    - m1.medium {ZK, WORKER, MASTER, UI}
    - m1.medium {WORKER}
    - storm-version "0.9.5"
    - zk-version "3.4.6"
    - image "eu-west-1/ami-97344ae0" 	#official Ubuntu 14.04 LTS AMI
    - region "eu-west-1"
    - remote-exec-preconfig {cd ~, echo hey > hey.txt}
    - remote-exec-postconfig {}
    - ssh-key-name "mySSHKeyName"           # Optional. defaults to "id_rsa"
```
+ MASTER is the Storm Nimbus daemon
+ WORKER is the Storm Supervisor daemon
+ UI is the Storm and Ganglia User-Interface
+ LOGVIEWER is the Storm Logviewer daemon
+ DRPC is the Storm DRPC daemon
+ ZK is the [Zookeeper](http://zookeeper.apache.org) daemon

_Please ensure the image resides in the same region as specified._

## Usage

### Deploy
Execute `java -jar storm-deploy-alternative.jar deploy CLUSTER_NAME`

After successful deployment, a small file is written to $HOME/.storm/, which allows you to interact with the cluster directly from the bin/storm script. For details on how to use the bin/storm script, please refer to the [Storm wiki](https://github.com/nathanmarz/storm/wiki).

### Kill
Execute `java -jar storm-deploy-alternative.jar kill CLUSTER_NAME`

Kills all nodes belonging in the cluster with name CLUSTER_NAME.

### Attach
Execute `java -jar storm-deploy-alternative.jar attach CLUSTER_NAME`

Attaches the `bin/storm` script to a cluster with name CLUSTER_NAME.

### Scaling
Execute `java -jar storm-deploy-alternative.jar scaleout CLUSTER_NAME #NumInstances INSTANCE_TYPE`

Adds new worker instances to an already running cluster. For example, you could execute `java -jar storm-deploy-alternative.jar scaleout test 2 m1.medium`, to add two new instances of the type m1.medium to the cluster called test. When completed, you can see the new nodes in the Storm UI.

## FAQ
+ I am seeing the error: `net.schmizz.sshj.userauth.UserAuthException: publickey auth failed`. This error means the software could not connect to the newly launched instances using SSH (for configuring them). There can be multiple reasons why this error happens. Please ensure you have ~/.ssh/id_rsa and ~/.ssh/id_rsa.pub and that both files are _valid_. Furthermore, please go to AWS EC2 interface -> Key Pairs, and delete the jclouds#CLUSTER_NAME keypair. If deploying the same cluster, using multiple machines, please ensure the same keypair exists on all machines. In case problems persist, please try generating a new keypair by executing `ssh-keygen -t rsa`, then delete old keypair from AWS EC2 interface and retry deployment.
+ I am seeing the warning: `cipher strengths apparently limited by JCE policy`. You can improve your security by installing [Java Cryptography Extension (JCE) Unlimited Strength Jurisdiction Policy Files](http://www.oracle.com/technetwork/java/javase/downloads/index.html).
+ I am seeing the error: `the trustAnchors parameter must be non-empty`. This error usually means the Java CA certificates are broken. To fix first execute `sudo dpkg --purge --force-depends ca-certificates-java` then `sudo apt-get install ca-certificates-java`.

## Limitations
Currently, only deploying to Ubuntu AMIs on Amazon EC2 is supported.
