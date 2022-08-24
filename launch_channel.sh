#!/bin/sh
export CLASSPATH=".:dist/odinms.jar:mina-core.jar:slf4j-api.jar:slf4j-jdk14.jar:mysql-connector-java-bin.jar"
java -Dnet.sf.odinms.recvops=recvops.properties \
-Dnet.sf.odinms.sendops=sendops.properties \
-Dnet.sf.odinms.wzpath=. \
-Dnet.sf.odinms.channel.config=channel.properties \
-Djavax.net.ssl.keyStore=channel.keystore \
-Djavax.net.ssl.keyStorePassword=channelkeystorepass \
-Djavax.net.ssl.trustStore=channel.truststore \
-Djavax.net.ssl.trustStorePassword=channeltruststorepass \
net.sf.odinms.net.channel.ChannelServer