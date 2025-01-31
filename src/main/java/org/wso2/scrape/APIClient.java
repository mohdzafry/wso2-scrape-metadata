/*
 * Copyright (c) 2023-2024, WSO2 LLC. (http://www.wso2.com).
 *
 * WSO2 LLC. licenses this file to you under the Apache License,
 * Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

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
     * @param dataCollector        Instance of {@link DataCollector} to determine the environment.
     * @param apiUrl               The API endpoint URL where data is sent.
     * @param applicationStartTime The timestamp when the application started.
     * @param osDetails            A map containing OS details such as name and version.
     * @param jdkDetails           A map containing JDK details, including the version.
     * @param wso2ProductDetails   A map containing WSO2 product details, including version and update level.
     * @param libraryVersions      A map of library names and their corresponding versions.
     * @param kubernetesDetails    A map containing Kubernetes details such as master and node pool versions.
     * @param environmentType      The type of environment (e.g., Dev, QA, Production).
     * @param projectNumber        The project number associated with the deployment.
     * @param hostname             The hostname of the server or instance.
     * @param ipAddress            The IP address of the server or instance.
     * @param clientId             The client ID used for OAuth authentication.
     * @param clientSecret         The client secret used for OAuth authentication.
     * @param tokenUrl             The URL to obtain the OAuth token.
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