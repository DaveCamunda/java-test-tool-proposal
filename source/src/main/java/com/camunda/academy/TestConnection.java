package com.camunda.academy;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

/**
 * Camunda 8 SaaS Console (Organization / Administration API) Connection Test Tool
 *
 * This tool tests connectivity to the Camunda 8 SaaS Administration API by:
 *
 * 1. Loading credentials from a properties file passed as the first argument
 * 2. Requesting an OAuth access token from login.cloud.camunda.io
 * 3. Calling https://api.cloud.camunda.io/members to verify API connection
 *
 * Exit codes:
 *   0 = Success
 *   1 = SSL Error
 *   2 = Connection Error
 *   3 = Authentication Error
 *   4 = Other/Unexpected Error
 *
 * Author: Camunda Academy
 */
public class TestConnection {

    public static void main(String[] args) {
    	
        try {

            testConnection(TestConnectionUtils.loadEnvVars(args));

            printSuccess();
            
        } catch (Exception e) {
        	
        	printError(TestConnectionUtils.handleException(e));   
        }
    }

    /* * * * * * * * * * * * * * * * * * * * * *
     * 
     * No need to check the Java version. It will fail during start-up before it can check anyway.
     * If they are < version 17, a java.lang.UnsupportedClassVersionError will be shown and will have some info on the mismatch. 
     * It might be cryptic to a new developer so we can reference it in the documentation.
     *
     * private static void checkJavaVersion() { } 
     * 
     * 
     */

    private static void testConnection(Map<String, String> envVars) throws Exception {
    	
        String clientId     = envVars.get(TestConnectionUtils.CLIENT_ID);
        
        String clientSecret = envVars.get(TestConnectionUtils.CLIENT_SECRET);  

        ObjectMapper mapper = new ObjectMapper();   
        
        String tokenBody = mapper.writeValueAsString(
        		
            Map.of("grant_type",    "client_credentials",
                   "audience",      TestConnectionUtils.AUDIENCE,
                   "client_id",     clientId,
                   "client_secret", clientSecret)
            
        );
        
        HttpClient http = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(15)).build();
       
        /****************** TEST ONE: AUTH TOKEN ********************/
        
        System.out.println("Connecting to Camunda 8 SaaS Console API...");
        System.out.println("Using client ID: " + TestConnectionUtils.maskCredential(clientId));
        System.out.println("Requesting access token from " + TestConnectionUtils.OAUTH_URL + " ...");

        HttpResponse<String> tokenResp = http.send(
        		
                HttpRequest.newBuilder(URI.create(TestConnectionUtils.OAUTH_URL))
                           .timeout(Duration.ofSeconds(30))
                           .header("Content-Type", "application/json")
                           .POST(HttpRequest.BodyPublishers.ofString(tokenBody))
                           .build(),
                HttpResponse.BodyHandlers.ofString()
                
        );

        if (tokenResp.statusCode() == 401 || tokenResp.statusCode() == 403) {
        	
            throw new Exception(TestConnectionUtils.AUTH_ERROR_ID + " Authentication failed (HTTP " 
                              + tokenResp.statusCode()
                              + "). Check " + TestConnectionUtils.CLIENT_ID + " and " + TestConnectionUtils.CLIENT_SECRET
                              + ". Response: " + tokenResp.body());
        }
        
        if (tokenResp.statusCode() != 200) {
        	
            throw new Exception("Token request failed with HTTP " + tokenResp.statusCode() + 
            		            ". Response: " + tokenResp.body());
        }

        JsonNode tokenJson = mapper.readTree(tokenResp.body());
        
        if (!tokenJson.has("access_token")) {
        	
            throw new Exception("Token response did not include access_token. Response: " + tokenResp.body()); 
        }
        
        String accessToken = tokenJson.get("access_token").asText();
        
        System.out.println("Access token received.");

        /****************** TEST TWO: API CONNECTION (ADMIN) ********************/
        
        System.out.println("Testing connection to the API...");
        
        HttpResponse<String> apiResp = http.send(
        		
                HttpRequest.newBuilder(URI.create(TestConnectionUtils.CONSOLE_BASE + "/members"))
                           .timeout(Duration.ofSeconds(30))
                           .header("Authorization", "Bearer " + accessToken)
                           .header("Accept", "application/json")
                           .GET()
                           .build(),
                HttpResponse.BodyHandlers.ofString()
                
        );

        if (apiResp.statusCode() == 401 || apiResp.statusCode() == 403) {
        	
            throw new Exception(TestConnectionUtils.AUTH_ERROR_ID  
            		          + " Authorization failed on Administration API (HTTP " + apiResp.statusCode()
                              + "). Response: " + apiResp.body());
        }
        
        if (apiResp.statusCode() != 200) {
        	
            throw new Exception("Administration API call failed with HTTP " + apiResp.statusCode()
                              + ". Response: " + apiResp.body());
        }
        
        System.out.println("Administration API reachable.");  
        
        /****************** TEST THREE: <REGION>.ZEEBE.CAMUNDA.IO WHITELISTED ********************/
        
        // Verify the regional Zeebe gateway hostname is reachable.
        // We don't need a valid response — any HTTP response (even 404) means
        // the network path works, TLS negotiated, and the host isn't blocked.
        System.out.println("Testing connection to regional Zeebe gateway " + TestConnectionUtils.ZEEBE_REGION_HOST + " ...");

        try {
        	
            HttpResponse<String> zeebeResp = http.send(
            		
                    HttpRequest.newBuilder(URI.create(TestConnectionUtils.ZEEBE_REGION_HOST))
                               .timeout(Duration.ofSeconds(15))
                               .GET()
                               .build(),
                    HttpResponse.BodyHandlers.ofString()
                    
            );

            // Any HTTP status code (including 4xx) means the gateway responded.
            System.out.println("Regional Zeebe gateway reachable.");

        } catch (Exception e) {
        	
            throw new Exception(TestConnectionUtils.ZEEBE_BLOCKED_ID  + " Could not reach regional Zeebe gateway at " + 
                                TestConnectionUtils.ZEEBE_REGION_HOST + ". Underlying error: " + e.getMessage(), e);
        }
    }

    private static void printError(TestConnectionError error) {
    	
        System.out.println("***** CONNECTION FAILED *****");
        
        System.out.println(error.errorMessage());
    	
        System.exit(error.errorCode());    	
    }

    private static void printSuccess() {
    	
        System.out.println("***** CONNECTION SUCCESSFUL *****");
        
        System.exit(TestConnectionUtils.EXIT_SUCCESS);
    }
}