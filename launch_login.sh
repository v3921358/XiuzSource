#!/bin/sh
export CLASSPATH=".:dist/odinms.jar:mina-core.jar:slf4j-api.jar:slf4j-jdk14.jar:mysql-connector-java-bin.jar"
java -Dnet.sf.odinms.recvops=recvops.properties \
-Dnet.sf.odinms.sendops=sendops.properties \
-Dnet.sf.odinms.wzpath=. \
-Dnet.sf.odinms.login.config=login.properties \
-Djavax.net.ssl.keyStore=login.keystore \
-Djavax.net.ssl.keyStorePassword=loginkeystorepassword \
-Djavax.net.ssl.trustStore=login.truststore \
-Djavax.net.ssl.trustStorePassword=logintruststorepassword \
net.sf.odinms.net.login.LoginServer