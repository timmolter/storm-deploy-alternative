## Storm-Deploy-Alternative Fork

Fast tool to deploy [Storm](https://github.com/apache/storm) on [Amazon EC2](http://aws.amazon.com/ec2/), written entirely in Java.

Note that this is a heavily-modified fork of [storm-deploy-alternative](https://github.com/KasperMadsen/storm-deploy-alternative). The main difference is that this fork removes a lot of unnecessary code and dependencies, allows the user to choose the download location of of `storm`, `zookeeper`, and `storm-deploy-alternative-cluster` jar. This fork also splits the code base into two modules, `storm-deploy-alternative-local` and `storm-deploy-alternative-cloud`, in order to separate the logic and keep the necessary cloud jar size minimal. 

Full explanation of useage can be found at the following link: <http://knowm.org/how-to-deploy-an-apache-storm-cluster-to-the-amazon-elastic-compute-cloud-ec2/>.

## Features

+ Runs Storm and Zookeeper daemons under supervision (automatically restarted in case of failure)
+ Only fetch and compile what is needed (can deploy on prepared images in a few minutes)
+ Supports executing user-defined commands both pre-config and post-config
+ Automatically sets up [Ganglia](http://ganglia.sourceforge.net/), making it easy to monitor performance
+ Automatically sets up [Amazon EC2 AMI Tools](http://docs.aws.amazon.com/AWSEC2/latest/CommandLineReference/ami-tools.html) on new nodes

## Configuration

This tool, requires two configuration files: `conf/credential.yaml` and `conf/configuration.yaml`. Put your credentials into the file `conf/credential.yaml`. It's required that you have generated an SSH key-pair on your local machine in `~/.ssh` with an empty pass phrase.

Below is an example of a single cluster configuration, for `conf/configuration.yaml`

```
#
# Amazon EC2 example cluster configuration
#
mycluster:
    - image "us-west-2/ami-c94856a8"        # Ubuntu 14.04 LTS AMI
    - region "us-west-2"                    # Region
    - remote-exec-preconfig {}
    - remote-exec-postconfig {}
    - ssh-key-name "ec2"     # Optional. defaults to "id_rsa"
    - storm-deploy-alternative-cloud-jar-url "https://s3-us-west-2.amazonaws.com/your-bucket/storm-deploy-alternative-cloud.jar"
    - storm-tar-gz-url "http://mirror.yannic-bonenberger.com/apache/storm/apache-storm-0.9.3/apache-storm-0.9.3.tar.gz"
    - zk-tar-gz-url "http://apache.lauf-forum.at/zookeeper/zookeeper-3.4.7/zookeeper-3.4.7.tar.gz"
    - memory-monitor "false"
    - t2.micro {ZK, WORKER, MASTER, UI, LOGVIEWER}      # Request service
    - t2.micro {WORKER}      # Request service
    - t2.micro {WORKER}      # Request service
```

+ MASTER is the Storm Nimbus daemon
+ WORKER is the Storm Supervisor daemon
+ UI is the Storm and Ganglia User-Interface
+ LOGVIEWER is the Storm Logviewer daemon
+ DRPC is the Storm DRPC daemon
+ ZK is the [Zookeeper](http://zookeeper.apache.org) daemon

Ensure the image resides in the same region as specified. Choose a mirror download URL for [storm](https://storm.apache.org/downloads.html) and [zookeeper](https://www.apache.org/dyn/closer.cgi/zookeeper/) or put the files in your own S3 bucket and use those URLs.

Below is an example of a single cluster configuration, for `conf/credential.yaml`

```
##
## Amazon AWS Credentials
##
ec2-identity: "GDYTFC59KU6JKHJG"
ec2-credential: "YIO7jgjhg987qKgRfFJuke958mmGwrPsgsd"
```

## Usage

After cloning the repo via `git`, build the project with `Maven` with the command: `mvn clean package`. Two jars will be produced: `storm-deploy-alternative-local/target/storm-deploy-alternative-local.jar` and `storm-deploy-alternative-cloud/target/storm-deploy-alternative-cloud.jar`. You need to upload `storm-deploy-alternative-cloud/target/storm-deploy-alternative-cloud.jar` to some location on the web accessible by your cluster instances via `wget`. You own S3 bucket would be a logical location. Update the `configuration.yaml` entry, `storm-deploy-alternative-cloud-jar-url`, accordingly.


### Deploy

Execute `java -jar storm-deploy-alternative-local/target/storm-deploy-alternative-local.jar deploy CLUSTER_NAME`

Deploys all nodes belonging in the cluster with name CLUSTER_NAME.

### Kill

Execute `java -jar storm-deploy-alternative-local/target/storm-deploy-alternative-local.jar kill CLUSTER_NAME`

Kills all nodes belonging in the cluster with name CLUSTER_NAME.

## Limitations

Only deploying to Ubuntu AMIs on Amazon EC2 is supported.
