mvn clean org.jacoco:jacoco-maven-plugin:prepare-agent package org.sonarsource.scanner.maven:sonar-maven-plugin:3.2:sonar \
-Dsonar.host.url=https://sonarqube.com -Dsonar.login=$my.sonar.login \
-Dsonar.organization=vladimir-bukhtoyarov-github -e -X \
-Dsonar.dynamicAnalysis=reuseReports
-P sonar