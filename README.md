# cii-service
## Getting started
## Deploying to Azure
### Build 
To make a azure-deployable uber-jar: 
 mvn clean package -Dquarkus.package.type=uber-jar -D%prod.quarkus.http.port=80
### Deploy to Azure web app
mvn azure-webapp:deploy


