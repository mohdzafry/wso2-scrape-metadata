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
import org.wso2.scrape.utils.JsonUtil;
import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.InetAddress;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Collects system and product details such as OS, JDK, WSO2 product, and Kubernetes details.
 */
public class DataCollector {

    private static final Logger LOGGER = Logger.getLogger(DataCollector.class.getName());
    private final String carbonHome;

    public DataCollector(String carbonHome) {
        this.carbonHome = carbonHome;
    }

    /**
     * Collects OS details.
     *
     * @return A map containing OS details.
     */
    public Map<String, String> getOSDetails() {
        Map<String, String> osDetails = new HashMap<>();
        osDetails.put("OS Name", System.getProperty("os.name"));

        try {
            String[] command = {"uname", "-a"};
            Process process = Runtime.getRuntime().exec(command);
            BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
            String line = reader.readLine();
            if (line != null) {
                // Remove the date and unnecessary details
                String cleanedLine = line.replaceAll("\\w{3} \\w{3} \\d{1,2} \\d{2}:\\d{2}:\\d{2} \\w{3} \\d{4}", "").trim();
                osDetails.put("OS Version", cleanedLine);
            }
        } catch (IOException e) {
            LOGGER.log(Level.WARNING, "Failed to get OS Version", e);
            osDetails.put("OS Version", "Not Found");
        }

        osDetails.put("Machine Type", System.getProperty("os.arch"));
        return osDetails;
    }

    /**
     * Collects JDK details.
     *
     * @return A map containing JDK details.
     */
    public Map<String, String> getJDKDetails() {
        Map<String, String> jdkDetails = new HashMap<>();
        try {
            String jdkVersion = getJDKVersionOfRunningProcess();
            jdkDetails.put("JDK Version", jdkVersion);
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "Failed to get JDK Details", e);
            jdkDetails.put("JDK Version", "Not Found");
        }
        return jdkDetails;
    }

    public static String getJDKVersionOfRunningProcess() {
        try {
            // Command and its arguments as an array
            String[] command = {
                    "sh",
                    "-c",
                    "ps -ef | grep -i 'wso2' | grep '[j]ava' | awk '{for(i=1;i<=NF;i++) if($i ~ /java/) print $i}' | head -n 1"
            };

            Process process = Runtime.getRuntime().exec(command);
            BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
            String line = reader.readLine();

            // Check if the line is not null and matches the expected format
            if (line != null && line.contains("java")) {
                return getJDKVersionFromJavaCommand(line);
            }
        } catch (IOException e) {
            LOGGER.log(Level.WARNING, "Failed to get JDK Version of Running Process", e);
        }
        return "Not Found";
    }

    public static String getJDKVersionFromJavaCommand(String javaCommand) {
        try {
            String[] command = {
                    "sh",
                    "-c",
                    javaCommand + " -version 2>&1"
            };

            Process process = Runtime.getRuntime().exec(command);
            BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
            String line;
            while ((line = reader.readLine()) != null) {
                if (line.contains("version")) {
                    return line.split("\"")[1];
                }
            }
        } catch (IOException e) {
            LOGGER.log(Level.WARNING, "Failed to get JDK Version from Java Command", e);
        }
        return "Not Found";
    }

    /**
     * Collects WSO2 product details.
     *
     * @return A map containing WSO2 product details.
     */
    public Map<String, String> getWSO2ProductDetails(String productPath) {
        Map<String, String> wso2ProductDetails = new HashMap<>();

        String configPath = productPath + "/updates/config.json";

        try (BufferedReader reader = new BufferedReader(new FileReader(configPath))) {
            StringBuilder jsonContent = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                jsonContent.append(line);
            }
            String jsonString = jsonContent.toString();

            // Extract product name
            String productName = JsonUtil.getValueFromJson(jsonString, "name");
            wso2ProductDetails.put("WSO2 Product", productName);

            // Extract product pattern
            String productVersion = JsonUtil.getValueFromJson(jsonString, "version");
            wso2ProductDetails.put("Version", productVersion);

            // Extract update level
            String updateLevel = JsonUtil.getValueFromJson(jsonString, "update-level");
            wso2ProductDetails.put("Update Level", updateLevel);
        } catch (IOException e) {
            LOGGER.log(Level.WARNING, "Failed to get WSO2 Product Details", e);
            wso2ProductDetails.put("WSO2 Product", "Not Found");
            wso2ProductDetails.put("Update Level", "Not Found");
        }
        return wso2ProductDetails;
    }

    public Map<String, String> getLibraryVersions() {
        return LibraryVersionFetcher.getLibraryVersions(carbonHome);
    }

    public String getHostname() {
        try {
            return InetAddress.getLocalHost().getHostName();
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "Failed to get hostname", e);
            return "Unknown";
        }
    }

    public String getIpAddress() {
        try {
            return InetAddress.getLocalHost().getHostAddress();
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "Failed to get IP address", e);
            return "Unknown";
        }
    }


    /**
     * Collects Kubernetes details.
     *
     * @return A map containing Kubernetes details.
     */
    public Map<String, String> getKubernetesDetails() {
        Map<String, String> kubernetesDetails = new HashMap<>();
        if (isRunningInKubernetes()) {
            try {
                // Get Kubernetes master version
                String masterVersion = getKubernetesMasterVersion();
                if (masterVersion != null) {
                    kubernetesDetails.put("Master Version", masterVersion);
                }

                // Get Kubernetes node pool version
                String nodePoolVersion = getKubernetesNodePoolVersion();
                if (nodePoolVersion != null) {
                    kubernetesDetails.put("Node Pool Version", nodePoolVersion);
                }
            } catch (IOException e) {
                LOGGER.log(Level.WARNING, "Failed to get Kubernetes Details", e);
            }
        }
        return kubernetesDetails;
    }

    public String getKubernetesMasterVersion() throws IOException {
        String kubernetesServiceHost = System.getenv("KUBERNETES_SERVICE_HOST");
        String kubernetesServicePort = System.getenv("KUBERNETES_SERVICE_PORT");
        String apiUrl = "https://" + kubernetesServiceHost + ":" + kubernetesServicePort + "/version";

        String[] command = {
                "curl", "-k", "-H", "Authorization: Bearer " + getKubernetesToken(), apiUrl
        };

        Process process = Runtime.getRuntime().exec(command);
        BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
        String line;
        StringBuilder response = new StringBuilder();

        while ((line = reader.readLine()) != null) {
            response.append(line);
        }

        String jsonResponse = response.toString();
        return JsonUtil.getValueFromJson(jsonResponse, "gitVersion");
    }

    public String getKubernetesNodePoolVersion() throws IOException {
        String kubernetesServiceHost = System.getenv("KUBERNETES_SERVICE_HOST");
        String kubernetesServicePort = System.getenv("KUBERNETES_SERVICE_PORT");
        String apiUrl = "https://" + kubernetesServiceHost + ":" + kubernetesServicePort + "/api/v1/nodes";

        String[] command = {
                "curl", "-k", "-H", "Authorization: Bearer " + getKubernetesToken(), apiUrl
        };

        Process process = Runtime.getRuntime().exec(command);
        BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
        String line;
        StringBuilder response = new StringBuilder();

        while ((line = reader.readLine()) != null) {
            response.append(line);
        }

        String jsonResponse = response.toString();
        return JsonUtil.getValueFromJson(jsonResponse, "kubeletVersion");
    }

    public String getKubernetesToken() throws IOException {
        String tokenPath = "/var/run/secrets/kubernetes.io/serviceaccount/token";
        try (BufferedReader reader = new BufferedReader(new FileReader(tokenPath))) {
            return reader.readLine();
        }
    }

    public boolean isRunningInKubernetes() {
        return System.getenv("KUBERNETES_SERVICE_HOST") != null;
    }
}
