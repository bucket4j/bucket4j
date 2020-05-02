mvn clean install -P coverage

mvn sonar:sonar -Dsonar.projectKey=com.github.vladimir-bukhtoyarov:bucket4j -Dsonar.organization=vladimir-bukhtoyarov-github -Dsonar.host.url=https://sonarcloud.io -Dsonar.login=$MY_SECURITY_TOKEN

# browse results at https://sonarcloud.io/dashboard?id=com.github.vladimir-bukhtoyarov%3Abucket4j