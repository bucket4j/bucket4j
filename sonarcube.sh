mvn clean install -P coverage

mvn org.sonarsource.scanner.maven:sonar-maven-plugin:3.2:sonar \
-Dsonar.host.url=https://sonarqube.com \
-Dsonar.login=$MY_SONAR_LOGIN \
-Dsonar.organization=vladimir-bukhtoyarov-github