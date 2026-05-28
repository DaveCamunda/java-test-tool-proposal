# Camunda 8 SaaS Connection Test Tool - Java

## Requirements

- **Java 17 or higher** (check with `java -version`)
- **Internet connection** (The tool will attempt to connect to Camunda)
- **envVarsExtended.txt** — cluster credentials file. Might also be named envVars.txt or something similar. You should have received this along with the jar file.

## Usage

**Run it:**
```
java -jar testConnection.jar PATH/TO/CREDENTIALS_FILE.txt
```

**Or from behind a proxy:**
```
java -Dhttps.proxyHost=proxy.example.com -Dhttps.proxyPort=8080 -jar testConnection.jar PATH/TO/CREDENTIALS_FILE.txt

# With authentication:
java -Dhttps.proxyHost=proxy.example.com -Dhttps.proxyPort=8080 \
     -Dhttps.proxyUser=username -Dhttps.proxyPassword=password \ 
     -jar testConnection.jar PATH/TO/CREDENTIALS_FILE.txt
```

If you see "\*\*\*\*\* CONNECTION SUCCESSFUL \*\*\*\*\*", you're good to go! If not, see below.

## Exit Codes

| Code | Meaning |
|------|---------|
| 0 | Success |
| 1 | SSL Error |
| 2 | Connection Error |
| 3 | Authentication Error |
| 4 | Other Error |

## Troubleshooting

**"Error: LinkageError occurred while loading main class com.camunda.academy..." AND/OR "...java.lang.UnsupportedClassVersionError:..."**
- You're probably not on Java version 17 or higher
- Upgrade Java from [adoptium.net](https://adoptium.net/)

**"No env file specified."**
Make sure to include the credentials file when you run it. For example:

```
 -jar testConnection.jar /home/trainee/envVars.txt
```

**"Connection error: ..."**
- Check internet/proxy settings
- If behind a proxy, make sure you include that in the execution of the jar file. See 'Or from behind a proxy' instructions above.
- If it persists, email us or contact your training manager

**"Authentication error: ..."**
Verify credentials in your credentials file.

**"SSL error: ..."**
- If you got something like: "This error pattern usually indicates corporate TLS interception...", this is a security issue with your company's network. You'll need to either ask IT to allowlist *.camunda.io or have IT install the corporate CA cert into your JVM's trust store (cacerts).
- If you didn't receive that message, check your VPN (if you have one) or any privacy/adblocker apps running on your system. Also, it could be that your JVM is on 'restricted mode' and you'll need to talk to IT about that.

**"Regional Zeebe gateway unreachable: ..."**
- Probably calls to api.cloud.camunda.io are allowed but are blocked for *.zeebe.camunda.io.
- Ask IT to whitelist *.zeebe.camunda.io
- Check without a VPN and off of the company network to confirm

**"Unexpected error: ..."**
Email us or contact your training manager
