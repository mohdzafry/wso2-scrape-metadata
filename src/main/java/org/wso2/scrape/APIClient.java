package org.wso2.scrape;

import org.wso2.scrape.utils.OAuthToken;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;



/**
 * Handles communication with the API, including fetching OAuth tokens and sending data.
 */
public class APIClient {

    private static final Logger LOGGER = Logger.getLogger(APIClient.class.getName());


    /**
     * Sends collected data to the API.
     *
     * @param osDetails            OS details.
     * @param jdkDetails           JDK details.
     * @param wso2ProductDetails   WSO2 product details.
     * @param kubernetesDetails    Kubernetes details.
     * @param clientId             Client ID for OAuth.
     * @param clientSecret         Client secret for OAuth.
     */
    public void sendDataToAPI(DataCollector dataCollector,String apiUrl, String applicationStartTime, Map<String, String> osDetails, Map<String, String> jdkDetails, Map<String, String> wso2ProductDetails, Map<String, String> libraryVersions, Map<String, String> kubernetesDetails, String environmentType, String projectNumber, String hostname, String ipAddress, String clientId, String clientSecret, String tokenUrl) {
        try {
            // Fetch the OAuth token
            String accessToken = OAuthToken.getOAuthToken(tokenUrl, clientId, clientSecret);
            if (accessToken == null) {
                LOGGER.severe("Failed to obtain OAuth token. Exiting.");
                return;
            }

            URI uri = new URI(apiUrl);
            URL url = uri.toURL();
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("POST");
            conn.setRequestProperty("Content-Type", "application/json; utf-8");
            conn.setRequestProperty("Accept", "application/json");
            conn.setRequestProperty("Authorization", "Bearer " + accessToken); // Add OAuth token to headers
            conn.setDoOutput(true);

            // Prepare JSON payload
            String platformVersion = dataCollector.isRunningInKubernetes() ?
                    "K8 - " + osDetails.get("OS Name") + ":" + osDetails.get("OS Version") + ":" + kubernetesDetails.get("Master Version") + ":" + kubernetesDetails.get("Node Pool Version") :
                    "VM - " + osDetails.get("OS Name") + ":" + osDetails.get("OS Version");

            // Construct libraries string dynamically
            StringBuilder librariesBuilder = new StringBuilder();
            for (Map.Entry<String, String> entry : libraryVersions.entrySet()) {
                librariesBuilder.append(entry.getKey()).append(": ").append(entry.getValue()).append("; ");
            }
            String libraries = librariesBuilder.toString().replaceAll("; $", ""); // Remove trailing semicolon

            String jsonPayload = "{" +
                    "\"Platform Version\":\"" + platformVersion + "\"," +
                    "\"JDK version\":\"" + jdkDetails.get("JDK Version") + "\"," +
                    "\"Application Start Time\":\"" + applicationStartTime + "\"," +
                    "\"WSO2 Product\":\"" + wso2ProductDetails.get("WSO2 Product") + ":" + wso2ProductDetails.get("Version") + "\"," +
                    "\"Update Level\":\"" + wso2ProductDetails.get("Update Level") + "\"," +
                    "\"Libraries\":\"" + libraries + "\"," +
                    "\"Environment Type\":\"" + environmentType + "\"," +
                    "\"Project Number\":\"" + projectNumber + "\"," +
                    "\"Hostname\":\"" + hostname + "\"," +
                    "\"IP Address\":\"" + ipAddress + "\"" +
                    "}";

            LOGGER.info("Sending data to API: " + jsonPayload);

            try (DataOutputStream os = new DataOutputStream(conn.getOutputStream())) {
                byte[] input = jsonPayload.getBytes("utf-8");
                os.write(input, 0, input.length);
            }

            int responseCode = conn.getResponseCode();
            if (responseCode == HttpURLConnection.HTTP_OK) {
                LOGGER.info("Data sent to API successfully.");
            } else {
                LOGGER.warning("Failed to send data to API: " + responseCode);
                // Log the response from the API
                try (BufferedReader in = new BufferedReader(new InputStreamReader(conn.getErrorStream()))) {
                    String inputLine;
                    StringBuilder response = new StringBuilder();
                    while ((inputLine = in.readLine()) != null) {
                        response.append(inputLine);
                    }
                    LOGGER.warning("API Response: " + response.toString());
                }
            }
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "An error occurred while sending data to API", e);
        }
    }
}