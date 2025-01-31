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
import org.wso2.scrape.utils.DecryptionUtil;
import java.io.BufferedReader;
import java.io.FileReader;
import java.util.*;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Map;
import java.util.Properties;
import java.util.logging.Logger;

/**
 * Manages the scraping process, including loading properties, collecting data, and sending it to the API.
 */
public class ScrapeManager {

    private static final Logger LOGGER = Logger.getLogger(ScrapeManager.class.getName());
    private final String carbonHome;
    private final Properties properties;

    public ScrapeManager(String carbonHome) throws IOException {
        this.carbonHome = carbonHome;
        this.properties = loadProperties();
    }

    /**
     * Executes the scraping process.
     *
     * @throws Exception if an error occurs during execution.
     */
    public void execute(String[] args) throws Exception {
        LOGGER.info("Starting the Java application...");

        // Initialize components
        //DecryptionUtil decryptionUtil = new DecryptionUtil(carbonHome, properties);
        DataCollector dataCollector = new DataCollector(carbonHome);
        APIClient apiClient = new APIClient();

        // Decrypt client.id and client.secret using the decryption
        String clientId = decryption(carbonHome, properties.getProperty("client.id"));
        String clientSecret = decryption(carbonHome, properties.getProperty("client.secret"));

        String apiUrl = properties.getProperty("api.url");
        String tokenUrl = properties.getProperty("token.url");
        String environmentType = properties.getProperty("environment.type"); // Remove default value
        String projectNumber = properties.getProperty("project.number");


        String productPath = args.length > 1 ? args[1] : Paths.get(System.getProperty("user.dir")).toString();
        // Collect data
        Map<String, String> osDetails = dataCollector.getOSDetails();
        Map<String, String> jdkDetails = dataCollector.getJDKDetails();
        Map<String, String> wso2ProductDetails = dataCollector.getWSO2ProductDetails(productPath);
        Map<String, String> libraryVersions = dataCollector.getLibraryVersions();
        Map<String, String> kubernetesDetails = dataCollector.getKubernetesDetails();

        // Get hostname and IP address
        String hostname = dataCollector.getHostname();
        String ipAddress = dataCollector.getIpAddress();

        // Capture application start time
        String applicationStartTime = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));

        // Send data to API
        apiClient.sendDataToAPI(dataCollector,apiUrl, applicationStartTime, osDetails, jdkDetails, wso2ProductDetails, libraryVersions, kubernetesDetails, environmentType, projectNumber, hostname, ipAddress, clientId, clientSecret, tokenUrl);

        LOGGER.info("Java application execution completed.");
    }

    /**
     * Loads properties from the configuration file.
     *
     * @return Properties object containing configuration values.
     * @throws IOException if the properties file cannot be read.
     *
     */
    private Properties loadProperties() throws IOException {
        Properties properties = new Properties();
        try (FileInputStream input = new FileInputStream(carbonHome + "/scrape/scrape_config.properties")) {
            properties.load(input);
        }
        return properties;
    }

    private static String decryption(String carbonHome, String encryptedValue) throws Exception {
        // Path to the keystore
        String keystorePath = carbonHome + "/repository/resources/security/wso2carbon.jks";

        // Read the keystore alias from the properties file
        Properties properties = new Properties();
        try (FileInputStream input = new FileInputStream(carbonHome + "/scrape/scrape_config.properties")) {
            properties.load(input);
        }
        String keystoreAlias = properties.getProperty("keystore.alias", "wso2carbon");

        // Read the keystore password from the password-tmp file
        String keystorePassword = readKeystorePassword(carbonHome + "/password-tmp");

        // Decrypt using the DecryptionUtil class
        String decryptedValue = DecryptionUtil.decrypt(encryptedValue, keystorePath, keystoreAlias, keystorePassword);


        return decryptedValue;
    }

    private static String readKeystorePassword(String passwordFilePath) throws IOException {
        try (BufferedReader reader = new BufferedReader(new FileReader(passwordFilePath))) {
            return reader.readLine().trim(); // Read the first line and trim any whitespace
        }
    }
}