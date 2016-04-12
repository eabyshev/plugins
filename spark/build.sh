#!/usr/bin/env bash
mvn clean install && scp spark-api/target/spark-plugin-api-4.0.0-RC8-SNAPSHOT.jar spark-cli/target/spark-plugin-cli-4.0.0-RC8-SNAPSHOT.jar spark-impl/target/spark-plugin-impl-4.0.0-RC8-SNAPSHOT.jar spark-rest/target/spark-plugin-rest-4.0.0-RC8-SNAPSHOT.jar root@management4.critical-factor.com:/root/kudayar/subutai-4.0.0-RC8-SNAPSHOT/deploy
