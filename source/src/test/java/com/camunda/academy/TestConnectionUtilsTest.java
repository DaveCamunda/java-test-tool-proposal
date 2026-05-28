package com.camunda.academy;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.api.TestInfo;

import java.io.IOException;
import java.net.ConnectException;
import java.net.UnknownHostException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.cert.CertPathValidatorException;
import java.util.Map;

import javax.net.ssl.SSLHandshakeException;

import static org.junit.jupiter.api.Assertions.*;

class TestConnectionUtilsTest {

    // ---------- loadEnvVars ----------

    @Test
    void loadEnvVars_noArgs_throws() {
    	
        IOException e = assertThrows(IOException.class, () -> TestConnectionUtils.loadEnvVars(new String[]{}));
        
        assertTrue(e.getMessage().contains("No env file specified"));
        
        assertTrue(e.getMessage().contains("Usage:"));
    }

    @Test
    void loadEnvVars_nullArgs_throws() {
    	
        IOException e = assertThrows(IOException.class, () -> TestConnectionUtils.loadEnvVars(null));
        
        assertTrue(e.getMessage().contains("No env file specified"));
    }

    @Test
    void loadEnvVars_blankArg_throws() {
    	
        IOException e = assertThrows(IOException.class, () -> TestConnectionUtils.loadEnvVars(new String[]{"   "}));
        
        assertTrue(e.getMessage().contains("No env file specified"));
    }

    @Test
    void loadEnvVars_fileNotFound_throws() {
    	
        IOException e = assertThrows(IOException.class, () -> TestConnectionUtils.loadEnvVars(new String[]{"/nonexistent/path/to/file.txt"}));
        
        assertTrue(e.getMessage().contains("Env file not found"));
    }

    @Test
    void loadEnvVars_validFile_returnsCredentials(@TempDir Path tempDir) throws IOException {
    	
        Path envFile = tempDir.resolve("env.properties");
        
        Files.writeString(envFile,
                          "CAMUNDA_CONSOLE_CLIENT_ID=abc123\n" +
                          "CAMUNDA_CONSOLE_CLIENT_SECRET=secret456\n");

        Map<String, String> vars = TestConnectionUtils.loadEnvVars(new String[]{envFile.toString()});

        assertEquals("abc123", vars.get(TestConnectionUtils.CLIENT_ID));
        
        assertEquals("secret456", vars.get(TestConnectionUtils.CLIENT_SECRET));
    }

    @Test
    void loadEnvVars_missingClientSecret_throws(@TempDir Path tempDir) throws IOException {
    	
        Path envFile = tempDir.resolve("env.properties");
        
        Files.writeString(envFile, "CAMUNDA_CONSOLE_CLIENT_ID=abc123\n");

        IOException e = assertThrows(IOException.class, () -> TestConnectionUtils.loadEnvVars(new String[]{envFile.toString()}));
        
        assertTrue(e.getMessage().contains(TestConnectionUtils.CLIENT_SECRET));
    }

    @Test
    void loadEnvVars_blankValue_throws(@TempDir Path tempDir) throws IOException {
    	
        Path envFile = tempDir.resolve("env.properties");
        
        Files.writeString(envFile,
                          "CAMUNDA_CONSOLE_CLIENT_ID=\n" +
                          "CAMUNDA_CONSOLE_CLIENT_SECRET=secret456\n");

        IOException e = assertThrows(IOException.class, () -> TestConnectionUtils.loadEnvVars(new String[]{envFile.toString()}));
        
        assertTrue(e.getMessage().contains(TestConnectionUtils.CLIENT_ID));
    }

    // ---------- handleException ----------

    @Test
    void handleException_authTag_returnsAuthError(TestInfo info) {

        Exception e = new Exception("[AUTH] something failed");

        TestConnectionError err = TestConnectionUtils.handleException(e);

        printResult(info, err);

        assertEquals(TestConnectionUtils.EXIT_AUTH_ERROR, err.errorCode());

        assertTrue(err.errorMessage().startsWith("Authentication error:"));
    }

    @Test
    void handleException_zeebeBlockedTag_returnsConnectionErrorWithHelp(TestInfo info) {

        Exception e = new Exception("[ZEEBE_BLOCKED] gateway unreachable");

        TestConnectionError err = TestConnectionUtils.handleException(e);

        printResult(info, err);

        assertEquals(TestConnectionUtils.EXIT_CONNECTION_ERROR, err.errorCode());

        assertTrue(err.errorMessage().contains("Regional Zeebe gateway unreachable"));

        assertTrue(err.errorMessage().contains("*.zeebe.camunda.io"), "should include the helpful hint about firewall rules");
    }

    @Test
    void handleException_sslException_returnsSslError(TestInfo info) {

        Exception e = new SSLHandshakeException("handshake failed");

        TestConnectionError err = TestConnectionUtils.handleException(e);

        printResult(info, err);

        assertEquals(TestConnectionUtils.EXIT_SSL_ERROR, err.errorCode());

        assertTrue(err.errorMessage().startsWith("SSL error:"));
    }

    @Test
    void handleException_tlsInterception_includesHelpfulHint(TestInfo info) {

        CertPathValidatorException cause = new CertPathValidatorException("unable to find valid certification path");

        SSLHandshakeException e = new SSLHandshakeException("handshake failed");

        e.initCause(cause);

        TestConnectionError err = TestConnectionUtils.handleException(e);

        printResult(info, err);

        assertEquals(TestConnectionUtils.EXIT_SSL_ERROR, err.errorCode());

        assertTrue(err.errorMessage().contains("corporate TLS interception"), "should include the TLS interception hint when cert path validation fails");
    }

    @Test
    void handleException_connectException_returnsConnectionError(TestInfo info) {

        Exception e = new ConnectException("Connection refused");

        TestConnectionError err = TestConnectionUtils.handleException(e);

        printResult(info, err);

        assertEquals(TestConnectionUtils.EXIT_CONNECTION_ERROR, err.errorCode());

        assertTrue(err.errorMessage().startsWith("Connection error:"));
    }

    @Test
    void handleException_unknownHost_returnsConnectionError(TestInfo info) {

        Exception e = new UnknownHostException("api.cloud.camunda.io");

        TestConnectionError err = TestConnectionUtils.handleException(e);

        printResult(info, err);

        assertEquals(TestConnectionUtils.EXIT_CONNECTION_ERROR, err.errorCode());

        assertTrue(err.errorMessage().startsWith("Connection error:"));
    }

    @Test
    void handleException_unknownException_returnsOtherError(TestInfo info) {

        Exception e = new RuntimeException("something weird happened");

        TestConnectionError err = TestConnectionUtils.handleException(e);

        printResult(info, err);

        assertEquals(TestConnectionUtils.EXIT_OTHER_ERROR, err.errorCode());

        assertTrue(err.errorMessage().startsWith("Unexpected error:"));
    }

    @Test
    void handleException_withCause_includesCauseMessage(TestInfo info) {

        Exception cause = new IllegalStateException("root cause here");

        Exception e = new RuntimeException("wrapper message", cause);

        TestConnectionError err = TestConnectionUtils.handleException(e);

        printResult(info, err);

        assertEquals(TestConnectionUtils.EXIT_OTHER_ERROR, err.errorCode());

        assertTrue(err.errorMessage().contains("root cause here"));
    }
    
    private static void printResult(TestInfo info, TestConnectionError err) {

        System.out.println();
        System.out.println("=== " + info.getDisplayName() + " ===");
        System.out.println("Exit code: " + err.errorCode());
        System.out.println(err.errorMessage());
        System.out.println();
    }
}