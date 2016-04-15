#!/usr/bin/env bash
mvn clean install -Dmaven.test.skip=true && scp generic-api/target/generic-plugin-api-4.0.0-RC8.jar generic-cli/target/generic-plugin-cli-4.0.0-RC8.jar generic-impl/target/generic-plugin-impl-4.0.0-RC8.jar generic-rest/target/generic-plugin-rest-4.0.0-RC8.jar root@management4.critical-factor.com:/root/ermek/subutai-4.0.0-RC8/deploy
