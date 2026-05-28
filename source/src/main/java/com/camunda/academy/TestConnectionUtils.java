package com.camunda.academy;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

public class TestConnectionUtils {
	
	public static final String AUTH_ERROR_ID = "[AUTH]";
	public static final String ZEEBE_BLOCKED_ID = "[ZEEBE_BLOCKED]";
	
	public static final int EXIT_SUCCESS = 0;
    public static final int EXIT_SSL_ERROR = 1;
    public static final int EXIT_CONNECTION_ERROR = 2;
    public static final int EXIT_AUTH_ERROR = 3;
    public static final int EXIT_OTHER_ERROR = 4;

    public static final String OAUTH_URL    = "https://login.cloud.camunda.io/oauth/token";
    public static final String AUDIENCE     = "api.cloud.camunda.io";
    public static final String CONSOLE_BASE = "https://api.cloud.camunda.io";
    public static final String ZEEBE_REGION_HOST = "https://bru-2.zeebe.camunda.io/";
	
	public static final String CLIENT_ID = "CAMUNDA_CONSOLE_CLIENT_ID";
	public static final String CLIENT_SECRET = "CAMUNDA_CONSOLE_CLIENT_SECRET";
	
    public static Map<String, String> loadEnvVars(String[] args) throws IOException {
    	
        System.out.println("Loading environment variables...");
    	
        if (args == null || args.length == 0 || args[0].isBlank()) {
        	
            throw new IOException("No env file specified.\n" + usage());    
        }

        Path envPath = Paths.get(args[0]);
        
        if (!Files.exists(envPath)) {
        	
            throw new IOException("Env file not found: " + envPath.toAbsolutePath() + "\n" + usage());       
        }

        System.out.println("Using env file: " + envPath.toAbsolutePath());

        Properties props = new Properties();
        
        try (BufferedReader reader = new BufferedReader(new FileReader(envPath.toFile()))) {
        	
            props.load(reader);
        }

        Map<String, String> envVars = new HashMap<>();
        
        for (String key : props.stringPropertyNames()) {
        	
            envVars.put(key, props.getProperty(key));
        }

        String[] requiredVars = { CLIENT_ID, CLIENT_SECRET };

        for (String var : requiredVars) {
        	
            if (!envVars.containsKey(var) || envVars.get(var).isBlank()) {
            	
                throw new IOException("Missing required variable in " + envPath.getFileName() + ": " + var);
            }
        }

        return envVars;  
    }
    
    public static TestConnectionError handleException(Exception e) {
    	
        String errorMsg = e.getMessage() != null ? e.getMessage() : "";
        
        String className = e.getClass().getName();
        
        TestConnectionError error;

        if (errorMsg.startsWith(AUTH_ERROR_ID)) {
            
            error = new TestConnectionError(EXIT_AUTH_ERROR, "Authentication error: " + errorMsg);
            
        } else if (className.contains("SSL") || errorMsg.contains("certificate")) {
        	  	
            String message = "SSL error: " + errorMsg;
            
            if (looksLikeTlsInterception(e)) { message += tlsHelp(); }
            
            error = new TestConnectionError(EXIT_SSL_ERROR, message);
      
        } else if (errorMsg.startsWith(ZEEBE_BLOCKED_ID)) {
            
            error = new TestConnectionError(EXIT_CONNECTION_ERROR, "Regional Zeebe gateway unreachable: " + errorMsg + "\n" + zeebeBlockedHelp());

        } else if (className.contains("Connect") || className.contains("UnknownHost") || className.contains("Timeout")) {
            
            error = new TestConnectionError(EXIT_CONNECTION_ERROR, "Connection error: " + errorMsg);
            
        } else {
        	
            String message = "Unexpected error: " + errorMsg;
            
            if (e.getCause() != null) { message += "\nCaused by: " + e.getCause().getMessage(); }
            
            error = new TestConnectionError(EXIT_OTHER_ERROR, message);
        }
        
        return error;
    }
    
    public static String maskCredential(String credential) {
    	
        if (credential == null || credential.length() < 8) {
        	
            return "***";   
        }
        
        return credential.substring(0, 4) + "****" + credential.substring(credential.length() - 4);
    }
    
    private static boolean looksLikeTlsInterception(Exception e) {
    	
        for (Throwable t = e; t != null; t = t.getCause()) {
        	
            String cn = t.getClass().getName();
            
            if (cn.contains("CertPath") || cn.contains("Validator")) {
            	
                return true;
            }
        }
        
        return false;
    }
    
    private static String tlsHelp() {
    	
    	return 
    			
        "\nThis error pattern usually indicates corporate TLS interception." +
        "\nYour network's security tool (Zscaler, Netskope, Palo Alto, etc.)" +
        "\nis likely intercepting HTTPS traffic and re-signing it with a" +
        "\ncertificate Java doesn't trust.\n" +
        "\nPossible fixes:" +
        "\n  - Ask IT to allowlist *.camunda.io (bypass interception)" +
        "\n  - Or have IT install the corporate CA cert into your JVM's" +
        "\n    trust store (cacerts)" +
        "\n  - Or try from a personal network/hotspot to confirm the cause";
        
    }
    
    private static String usage() {
    	
        return 
        	
        "\nUsage: java -jar testConnection.jar <path-to-env-file>\n" +
        "\nThe env file must be a properties file containing:\n" +
        "  " + CLIENT_ID + "=<your client id>\n" +
        "  " + CLIENT_SECRET + "=<your client secret>\n" +
        "\nExample: java -jar testConnection.jar ./envVarsExtended.txt";
        
    }
    
    private static String zeebeBlockedHelp() {

        return

        "\nAll other Camunda endpoints worked, but the regional Zeebe gateway is not reachable." +
        "\nDuring training, your job worker will connect to this hostname, so this needs to work." +
        "\n" +
        "\nThe most common cause is a corporate firewall that allows" +
        "\n  api.cloud.camunda.io   and   login.cloud.camunda.io" +
        "\nbut blocks" +
        "\n  *.zeebe.camunda.io" +
        "\n" +
        "\nPossible fixes:" +
        "\n  - Ask IT to allowlist *.zeebe.camunda.io" +
        "\n  - Or try from a personal network/hotspot to confirm the cause";
    }
}
