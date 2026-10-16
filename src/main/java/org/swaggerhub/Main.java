package org.swaggerhub;

import com.google.gson.*;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Scanner;

public class Main {
    public static String logfile = ("migration_log_" + LocalDateTime.now() + ".txt").replace(":", "-");

    public static void main(String[] args) {

        System.out.println("This application can be used to migrate document data\n" +
                "from SwaggerHub instance A (source) to instance B (destination) with Registry API. \n" +
                "It accepts host parameters and admin API keys of both instances. \n" +
                "Optionally, the data can be downloaded to the project directory.\n");

        Scanner input = new Scanner(System.in);
        String hostA, hostB;
        String keyA, keyB;

        do {
            System.out.println("Enter admin API key of instance A (source)");
            keyA = input.nextLine();
            System.out.println("Enter host with protocol of instance A (source)");
            hostA = input.nextLine().toLowerCase();

            System.out.println("Enter admin API key of instance B (destination)");
            keyB = input.nextLine();
            System.out.println("Enter host with protocol of instance B (destination)");
            hostB = input.nextLine().toLowerCase();

            if (!CheckInput(hostA, hostB))
            {
                System.out.println("Bad host input.");
            }
        }while (!CheckInput(hostA, hostB));

        System.out.println("Writing migration log to " + logfile);

        if(!TestConnection(keyA, keyB, hostA, hostB)){
            return;
        }

        SaveMigrationLog("Do you want to save files to disk? (yes/no)");
        String response = input.nextLine().toLowerCase();

        boolean save;

        if (response.equals("yes")) save = true;
        else if (response.equals("no")) save = false;
        else{
            SaveMigrationLog("Unaccepted response.");
            return;
        }
        SaveMigrationLog(response);
        SaveMigrationLog("Instance A: " + hostA);
        SaveMigrationLog("Instance B: " + hostB);

        MigrateDocuments(keyA, keyB, hostA, hostB, save);
        MigrateIntegrations(keyA, keyB, hostA, hostB, save);
        MigrateProjects(keyA, keyB, hostA, hostB, save);

        SaveMigrationLog("\nMigration completed.");
    }

    public static boolean TestConnection(String keyA, String keyB, String hostA, String hostB)
    {
        String urlA = String.format("%s/v1/specs", hostA);
        String urlB = String.format("%s/v1/specs", hostB);

        HttpClient httpClient = HttpClient.newHttpClient();

        HttpRequest httpRequestA = HttpRequest.newBuilder()
                .uri(URI.create(urlA))
                .headers(
                        "Accept", "application/json",
                        "Authorization", keyA
                )
                .GET()
                .build();

        HttpRequest httpRequestB = HttpRequest.newBuilder()
                .uri(URI.create(urlB))
                .headers(
                        "Accept", "application/json",
                        "Authorization", keyB
                )
                .GET()
                .build();

        try {
            SaveMigrationLog("Testing connection to instance A...");
            HttpResponse<String> responseA = httpClient.send(httpRequestA, HttpResponse.BodyHandlers.ofString());
            if (responseA.statusCode() == 200){
                SaveMigrationLog("Connection to instance A successful.");
            }
            else{
                SaveMigrationLog("Connection to instance A failed. Status code: " + responseA.statusCode());
                SaveMigrationLog(responseA.body());
                return false;
            }
            SaveMigrationLog("Testing connection to instance B...");
            HttpResponse<String> responseB = httpClient.send(httpRequestB, HttpResponse.BodyHandlers.ofString());
            if (responseB.statusCode() == 200){
                SaveMigrationLog("Connection to instance B successful.");
            }
            else{
                SaveMigrationLog("Connection to instance B failed. Status code: " + responseB.statusCode());
                SaveMigrationLog(responseB.body());
                return false;
            }

        } catch (IOException | InterruptedException | IllegalArgumentException exception) {
            SaveMigrationLog(exception.getMessage());
        }

        httpClient.close();
        return true;
    }

    public static boolean CheckInput(String hostA, String hostB)
    {

        if (hostA.contains("http://"))
        {
            if (hostA.indexOf("http://") != 0)
            {
                return false;
            }
        }else
        {
            if (hostA.contains("https://"))
            {
                if (hostA.indexOf("https://") != 0)
                {
                    return false;
                }
            }
            return false;
        }

        if (hostB.contains("http://"))
        {
            if (hostB.indexOf("http://") != 0)
            {
                return false;
            }
        }else
        {
            if (hostB.contains("https://"))
            {
                if (hostB.indexOf("https://") != 0)
                {
                    return false;
                }
            }
            return false;
        }
        if (hostA.equals("http://") || hostA.equals("https://"))
        {
            return false;
        }
        if (hostB.equals("http://") || hostB.equals("https://"))
        {
            return false;
        }
        if (hostA.trim().isEmpty() || hostB.trim().isEmpty())
        {
            return false;
        }
        if (hostA.endsWith("/") || hostB.endsWith("/"))
        {
            System.out.println("Host should not end with a slash /.\n");
            return false;
        }
        return true;
    }

    public static void SaveMigrationLog(String message){
        System.out.println(message);
        try(FileWriter fileWriter = new FileWriter(logfile, true)){
            message+= "\n";
            fileWriter.append(message);
            fileWriter.close();
        }
        catch(IOException exception){
            System.out.println("Error writing file: " + exception.getMessage());
        }
    }

    public static void MigrateDocuments (String keyA, String keyB, String hostA, String hostB, boolean save)
    {
        int totalCount = GetSpecs(keyA, hostA, 0, "ANY").totalCount;
        if (totalCount == 0)
        {
            SaveMigrationLog("No documents to migrate");
            return;
        }
        SaveMigrationLog("Copying documents from instance A to instance B...");
        SaveMigrationLog("Total unique documents: " + totalCount);
        HttpClient httpClient = HttpClient.newHttpClient();

        for (int i = 0; i < totalCount; i++){

            Specs spec = GetSpecs (keyA, hostA, i, "ANY");
            int propertiesCount = spec.apis[0].properties.length;
            String x_versions = "";
            for (int j = 0; j < propertiesCount; j++)
            {
                if (spec.apis[0].properties[j].type.equals("X-Versions"))
                {
                    x_versions = spec.apis[0].properties[j].value;
                    break;
                }
            }
            String[] versions = x_versions.split(",");

            for (String s : versions) {

                String[] URL_split = spec.apis[0].properties[0].url.split("/");
                if (s.charAt(0) == '-' || s.charAt(0) == '*'){
                    s = s.substring(1);
                }
                String URL = String.format("%s//%s/%s/%s/%s/%s/%s",
                        URL_split[0],
                        URL_split[2],
                        URL_split[3],
                        URL_split[4],
                        URL_split[5],
                        URL_split[6],
                        s);
                HttpRequest httpGETRequest = HttpRequest.newBuilder()
                        .uri(URI.create(URL))
                        .headers(
                                "Accept", "application/yaml",
                                "Authorization", keyA
                        )
                        .GET()
                        .build();

                HttpResponse<String> getResponse;

                String[] substrings = URL.split("/");
                String documentName = substrings[6];
                String version = substrings[7];
                String organisationName = substrings[5];
                String type = substrings[4];

                String path = String.format("SwaggerHub-Downloads\\%s\\%s\\",
                        organisationName,
                        substrings[4]);

                String filename = String.format("%s-version_%s.yaml",
                        documentName,
                        version);
                String postURL = String.format("%s/v1/%s/%s/%s?isPrivate=true&version=%s",
                        hostB, type, organisationName, documentName, version);

                HttpResponse<String> postResponse;

                try {
                    getResponse = httpClient.send(httpGETRequest, HttpResponse.BodyHandlers.ofString());
                    if (getResponse.statusCode() == 200)
                    {
                        if (save)
                        {
                            SaveDocumentToFile(getResponse.body(), path, filename);
                        }

                        HttpRequest httpPOSTRequest = HttpRequest.newBuilder()
                                .uri(URI.create(postURL))
                                .headers(
                                        "Content-Type", "application/yaml",
                                        "Authorization", keyB
                                )
                                .POST(HttpRequest.BodyPublishers.ofString(getResponse.body()))
                                .build();

                        postResponse = httpClient.send(httpPOSTRequest, HttpResponse.BodyHandlers.ofString());
                        if (postResponse.statusCode() == 200 || postResponse.statusCode() == 201)
                        {
                            SaveMigrationLog("Successfully created " +
                                    documentName +
                                    " ver. " +
                                    version +
                                    " in organization " +
                                    organisationName);
                        }else
                        {
                            SaveMigrationLog("Failed to create " +
                                    documentName +
                                    " ver. " +
                                    version +
                                    " in organization " +
                                    organisationName +
                                    ": " +
                                    postResponse.body());
                        }
                    }
                    else{
                        System.out.println("Something went wrong: " + getResponse.statusCode());
                        System.out.println(getResponse.body());
                        return;
                    }
                } catch (IOException | InterruptedException exception) {
                    System.out.println(exception.getMessage());
                }
            }
        }
        httpClient.close();
        SaveMigrationLog("Finished copying documents.\n");
    }

    public static Specs GetSpecs (String keyA, String hostA, int page, String type){

        String URL = String.format("%s/v1/specs?specType=%s&visibility=ANY&state=ALL&page=%d&limit=1&sort=NAME&order=ASC",
                hostA, type, page);

        HttpClient httpClient = HttpClient.newHttpClient();
        HttpRequest httpRequest = HttpRequest.newBuilder()
                .uri(URI.create(URL))
                .headers(
                        "Accept", "application/json",
                        "Authorization", keyA
                )
                .GET()
                .build();

        HttpResponse<String> response;
        Specs apiResponse = new Specs();

        try{
            response = httpClient.send(httpRequest, HttpResponse.BodyHandlers.ofString());
            Gson gson = new Gson();
            apiResponse = gson.fromJson(response.body(), Specs.class);
            httpClient.close();

        }catch(IOException | InterruptedException exception){
            System.out.println(exception.getMessage());
        }

        return apiResponse;
    }

    public static void SaveDocumentToFile(String response, String path, String filename){
        new File(path).mkdirs();

        try(FileWriter fileWriter = new FileWriter(path + filename)){
            fileWriter.write(response);
            fileWriter.close();
            SaveMigrationLog("File saved to: " + path + filename);
        }
        catch(IOException exception){
            System.out.println("Error writing file: " + exception.getMessage());
        }
    }

    public static void MigrateIntegrations(String keyA, String keyB, String hostA, String hostB, boolean save)
    {
        Specs startSpec = GetSpecs(keyA, hostA, 0, "API");
        int totalCount = startSpec.totalCount;

        SaveMigrationLog("Copying integrations from instance A to instance B...");

        if (totalCount == 0)
        {
            return;
        }
        HttpClient httpClient = HttpClient.newHttpClient();
        for (int i = 0 ; i < totalCount; i ++)
        {
            Specs spec = GetSpecs (keyA, hostA, i, "API");
            int propertiesCount = spec.apis[0].properties.length;
            String x_versions = "";

            for (int j = 0; j < propertiesCount; j++)
            {
                if (spec.apis[0].properties[j].type.equals("X-Versions"))
                {
                    x_versions = spec.apis[0].properties[j].value;
                    break;
                }
            }

            String[] versions = x_versions.split(",");
            for (String s : versions) {

                String[] URL_split = spec.apis[0].properties[0].url.split("/");
                if (s.charAt(0) == '-' || s.charAt(0) == '*') {
                    s = s.substring(1);
                }
                String URL = String.format("%s//%s/%s/%s/%s/%s/%s",
                        URL_split[0],
                        URL_split[2],
                        URL_split[3],
                        URL_split[4],
                        URL_split[5],
                        URL_split[6],
                        s);

                String[] substrings = URL.split("/");
                String documentName = substrings[6];
                String version = substrings[7];
                String organisationName = substrings[5];

                String getURL = String.format("%s/v1/apis/%s/%s/%s/integrations",
                        hostA, organisationName, documentName, version);
                HttpRequest httpGETRequest = HttpRequest.newBuilder()
                        .uri(URI.create(getURL))
                        .headers(
                                "Accept", "application/json",
                                "Authorization", keyA
                        )
                        .GET()
                        .build();

                HttpResponse<String> getResponse;
                HttpResponse<String> postResponse;
                String path = String.format("SwaggerHub-Downloads\\%s\\integrations\\",
                        organisationName);
                String filename = String.format("%s_%s_%s.json",
                        organisationName,
                        documentName,
                        version);
                String postURL = String.format("%s/v1/apis/%s/%s/%s/integrations",
                        hostB, organisationName, documentName, version);
                try {
                    getResponse = httpClient.send(httpGETRequest, HttpResponse.BodyHandlers.ofString());
                    if (getResponse.statusCode() == 200)
                    {
                        if (save)
                        {
                            SaveDocumentToFile(getResponse.body(), path, filename);
                        }

                        JsonObject jsonObject = JsonParser.parseString(getResponse.body()).getAsJsonObject();
                        JsonArray integrations = jsonObject.getAsJsonArray("integrations");

                        for (JsonElement integration : integrations)
                        {
                            HttpRequest httpPOSTRequest = HttpRequest.newBuilder()
                                    .uri(URI.create(postURL))
                                    .headers(
                                            "Content-Type", "application/json",
                                            "Authorization", keyB
                                    )
                                    .POST(HttpRequest.BodyPublishers.ofString(integration.toString()))
                                    .build();

                            postResponse = httpClient.send(httpPOSTRequest, HttpResponse.BodyHandlers.ofString());
                            JsonObject integrationObject = integration.getAsJsonObject();
                            String integrationName = integrationObject.get("name").getAsString();

                            if (postResponse.statusCode() == 200 || postResponse.statusCode() == 201)
                            {
                                SaveMigrationLog("Successfully created integration " +
                                        integrationName +
                                        " for " +
                                        documentName +
                                        " version " +
                                        version +
                                        " in organization " +
                                        organisationName);
                            }else
                            {
                                SaveMigrationLog("Failed to create integration " +
                                        integrationName +
                                        " for " +
                                        documentName +
                                        " version " +
                                        version +
                                        " in organization " +
                                        organisationName +
                                        " :" +
                                        postResponse.body());
                            }
                        }
                    }else
                    {
                        System.out.println("Something went wrong: " + getResponse.statusCode());
                    }

                } catch (IOException | InterruptedException exception)
                {
                    System.out.println(exception.getMessage());
                }
            }
        }
        httpClient.close();
        SaveMigrationLog("Finished copying integrations.\n");
    }
    public static void MigrateProjects(String keyA, String keyB, String hostA, String hostB, boolean save)
    {
        SaveMigrationLog("Copying projects from instance A to instance B...");

        Orgs organizations = GetOrgs(keyA, hostA, 0);
        int totalCount = organizations.totalCount;
        if (totalCount == 0)
        {
            System.out.println("Couldn't find any organizations.");
            return;
        }
        ArrayList<String> organizationNames = new ArrayList<String>();

        for (int i = 0; i < totalCount; i ++)
        {
            Orgs org = GetOrgs(keyA, hostA, i);
            for (int j = 0; j < org.items.length; j++)
            {
                organizationNames.add(org.items[j].name);
            }
        }

        if (organizationNames.isEmpty())
        {
            return;
        }
        Projects projects = GetProjects(keyA, hostA, organizationNames.getFirst(), 1);//this endpoint should be iterated from index 1
        int totalProjectCount = projects.totalCount;
        Gson gson = new Gson();

        HttpClient httpClient = HttpClient.newHttpClient();
        HttpResponse response;
        int page = 1;
        for (int i = 0; i < organizationNames.size(); i ++)
        {
            for (int j = 0; j < totalProjectCount; j ++)
            {
                Projects project = GetProjects(keyA, hostA, organizationNames.get(i), page);
                if (j >= project.projects.length)
                {
                    break;
                }

                ProjectsObject projectsObject = project.projects[j];
                JsonElement projectElement = gson.toJsonTree(projectsObject);
                JsonObject projectJsonObject = (JsonObject) projectElement;
                if (save)
                {
                    String path = String.format("SwaggerHub-Downloads\\%s\\projects\\",
                            organizationNames.get(i));
                    String filename = project.projects[j].name + ".json";
                    SaveDocumentToFile(projectJsonObject.toString(), path, filename);
                }

                String postURL = String.format("%s/v1/projects/%s",
                        hostB, organizationNames.get(i));

                HttpRequest httpPOSTRequest = HttpRequest.newBuilder()
                        .uri(URI.create(postURL))
                        .headers(
                                "Content-Type", "application/json",
                                "Authorization", keyB
                        )
                        .POST(HttpRequest.BodyPublishers.ofString(projectJsonObject.toString()))
                        .build();
                try
                {
                    response = httpClient.send(httpPOSTRequest, HttpResponse.BodyHandlers.ofString());

                    if (response.statusCode() == 200 || response.statusCode() == 201)
                    {
                        SaveMigrationLog("Successfully created project " +
                                project.projects[j].name +
                                " in organization " +
                                organizationNames.get(i));
                    }else
                    {
                        SaveMigrationLog("Failed to create project " +
                                project.projects[j].name +
                                " in organization " +
                                organizationNames.get(i) +
                                " :" +
                                response.body());
                    }
                }catch (IOException | InterruptedException exception)
                {
                    System.out.println(exception.getMessage());
                }
                //if more projects than 100 which is the maximum -> increment page value and reset the loop
                if(j >= 99)
                {
                    page ++;
                    j = 0;
                }
            }
        }
        httpClient.close();
        SaveMigrationLog("Finished copying projects.");
    }

    public static Orgs GetOrgs(String keyA, String hostA, int page)
    {
        String getOrganizationsURL = String.format("%s/api/user-management/v1/orgs?sortBy=NAME&order=ASC&page=%d&pageSize=1",
                hostA, page);

        HttpRequest httpGETRequest = HttpRequest.newBuilder()
                .uri(URI.create(getOrganizationsURL))
                .headers(
                        "Accept", "application/json",
                        "Authorization", keyA
                )
                .GET()
                .build();

        HttpResponse<String> response;
        HttpClient httpClient = HttpClient.newHttpClient();
        Orgs apiResponse = new Orgs();
        try
        {
            response = httpClient.send(httpGETRequest, HttpResponse.BodyHandlers.ofString());
            Gson gson = new Gson();
            apiResponse = gson.fromJson(response.body(), Orgs.class);
            httpClient.close();

        }catch(IOException | InterruptedException exception)
        {
            System.out.println(exception.getMessage());
        }
        return apiResponse;
    }

    public static Projects GetProjects(String keyA, String hostA, String organizationName, int page)
    {
        String getProjectsURL = String.format("%s/v1/projects/%s?nameOnly=false&page=%d&limit=100&order=ASC",
                hostA, organizationName, page);

        HttpRequest httpGETRequest = HttpRequest.newBuilder()
                .uri(URI.create(getProjectsURL))
                .headers(
                        "Accept", "application/json",
                        "Authorization", keyA
                )
                .GET()
                .build();

        HttpResponse<String> response;
        HttpClient httpClient = HttpClient.newHttpClient();
        Projects apiResponse = new Projects();
        try
        {
            response = httpClient.send(httpGETRequest, HttpResponse.BodyHandlers.ofString());
            Gson gson = new Gson();
            apiResponse = gson.fromJson(response.body(), Projects.class);
            httpClient.close();

        }catch(IOException | InterruptedException exception)
        {
            System.out.println(exception.getMessage());
        }
        return apiResponse;
    }
}

