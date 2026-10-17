## Description
This is a Java console application that can be used to copy data between two SwaggerHub/Studio on-premise instances using Registry API/User Management API endpoints. The application was tested on SwaggerHub on-premise versions 2.10.1 and 2.15.2 - migration from 2.10.1 to 2.15.2 and from 2.15.2 to 2.10.1. 

### Known limitations
- The application requires both instances to be up and running
- The application is using synchronous web requests and the data transfer can take some time
- Some integrations, such as Webhook, may fail to copy because the GET request returns incomplete data, which in turn is rejected by the server when we do a POST request
- The application is storing data in class objects, which means that if the structure of JSON response changes, the application will need to be reworked
- Some values are hardcoded, such as path, filenames, page size query parameters in web requests for simplicity
- Since there are no supported endpoints for recreating organisations they have to be recreated manually before running the application
- All APIs are downloaded as `unresolved`
- All copied documents by default have `private` visibility

### Execution steps

1. Input parameters
2. Check for correctness of the provided host values
3. Connection tests for both instances
4. Question whether save files to disk
5. Copy documents
6. Copy integrations
7. Copy projects

### Input parameters
- Admin API key of instance A (source)
- Host of instance A (source)
- Admin API key of instance B (destination)
- Host of instance B (destination)
- Option to save files to the project directory

### What data is copied
- APIs, domains, templates
- Projects
- Integrations

### Output
The application prints logs to a text file named `migration_log_DATE.txt` and user inputs without confidential API keys.
Sample output from a successful migration:
```
Testing connection to instance A...
Connection to instance A successful.
Testing connection to instance B...
Connection to instance B successful.
Do you want to save files to disk? (yes/no)
yes
Instance A: http://10.200.30.110.nip.io
Instance B: http://10.200.30.217.nip.io
Copying documents from instance A to instance B...
Total unique documents: 5
File saved to: SwaggerHub-Downloads\SmartBear3\apis\APISB3-version_1.0.0.yaml
Successfully created APISB3 ver. 1.0.0 in organization SmartBear3
File saved to: SwaggerHub-Downloads\SmartBear3\apis\APISB3-version_2.0.yaml
Successfully created APISB3 ver. 2.0 in organization SmartBear3
File saved to: SwaggerHub-Downloads\SmartBear3\apis\APISB3-version_version-new.yaml
Successfully created APISB3 ver. version-new in organization SmartBear3
File saved to: SwaggerHub-Downloads\SmartBear\domains\domain-version_1.0.0.yaml
Successfully created domain ver. 1.0.0 in organization SmartBear
File saved to: SwaggerHub-Downloads\SmartBear\apis\IOT-version_1.0.0.yaml
Successfully created IOT ver. 1.0.0 in organization SmartBear
File saved to: SwaggerHub-Downloads\SmartBear\templates\IOT-T-version_1.0.0.yaml
Successfully created IOT-T ver. 1.0.0 in organization SmartBear
File saved to: SwaggerHub-Downloads\SmartBear2\apis\IOTSB2-version_1.0.0.yaml
Successfully created IOTSB2 ver. 1.0.0 in organization SmartBear2
File saved to: SwaggerHub-Downloads\SmartBear2\apis\IOTSB2-version_1.0.0-ver2.yaml
Successfully created IOTSB2 ver. 1.0.0-ver2 in organization SmartBear2
Finished copying documents.

Copying integrations from instance A to instance B...
File saved to: SwaggerHub-Downloads\SmartBear3\integrations\SmartBear3_APISB3_1.0.0.json
Successfully created integration API Auto Mocking for APISB3 version 1.0.0 in organization SmartBear3
File saved to: SwaggerHub-Downloads\SmartBear3\integrations\SmartBear3_APISB3_2.0.json
Successfully created integration API Auto Mocking for APISB3 version 2.0 in organization SmartBear3
File saved to: SwaggerHub-Downloads\SmartBear3\integrations\SmartBear3_APISB3_version-new.json
Successfully created integration API Auto Mocking for APISB3 version version-new in organization SmartBear3
File saved to: SwaggerHub-Downloads\SmartBear\integrations\SmartBear_IOT_1.0.0.json
Successfully created integration API Auto Mocking for IOT version 1.0.0 in organization SmartBear
File saved to: SwaggerHub-Downloads\SmartBear2\integrations\SmartBear2_IOTSB2_1.0.0.json
Successfully created integration API Auto Mocking for IOTSB2 version 1.0.0 in organization SmartBear2
File saved to: SwaggerHub-Downloads\SmartBear2\integrations\SmartBear2_IOTSB2_1.0.0-ver2.json
Successfully created integration API Auto Mocking for IOTSB2 version 1.0.0-ver2 in organization SmartBear2
Finished copying integrations.

Copying projects from instance A to instance B...
File saved to: SwaggerHub-Downloads\SmartBear\projects\Project2SB.json
Successfully created project Project2SB in organization SmartBear
File saved to: SwaggerHub-Downloads\SmartBear\projects\ProjectSB.json
Successfully created project ProjectSB in organization SmartBear
File saved to: SwaggerHub-Downloads\SmartBear2\projects\ProjectSB2.json
Successfully created project ProjectSB2 in organization SmartBear2
File saved to: SwaggerHub-Downloads\SmartBear3\projects\ProjectSB3.json
Successfully created project ProjectSB3 in organization SmartBear3
Finished copying projects.

Migration completed.
```
