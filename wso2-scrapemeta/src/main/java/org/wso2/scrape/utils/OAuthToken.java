package org.wso2.scrape.utils;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.logging.Level;
import java.util.logging.Logger;

public class OAuthToken {

    private static final Logger LOGGER = Logger.getLogger(OAuthToken.class.getName());

    public static String getOAuthToken(String tokenUrl, String clientId, String clientSecret) {
        try {
            URL url = new URL(tokenUrl);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("POST");
            conn.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
            conn.setDoOutput(true);

            // Prepare the request body
            String requestBody = "grant_type=client_credentials&client_id=" + clientId + "&client_secret=" + clientSecret;

            try (DataOutputStream os = new DataOutputStream(conn.getOutputStream())) {
                byte[] input = requestBody.getBytes("utf-8");
                os.write(input, 0, input.length);
            }

            // Read the response
            int responseCode = conn.getResponseCode();

            if (responseCode == HttpURLConnection.HTTP_OK) {
                try (BufferedReader in = new BufferedReader(new InputStreamReader(conn.getInputStream()))) {
                    StringBuilder response = new StringBuilder();
                    String inputLine;
                    while ((inputLine = in.readLine()) != null) {
                        response.append(inputLine);
                    }
                    // Parse the JSON response to extract the access token
                    String jsonResponse = response.toString();
                    return JsonUtil.getValueFromJson(jsonResponse, "access_token");
                }
            } else {
                LOGGER.warning("Failed to fetch OAuth token: " + responseCode);
                try (BufferedReader in = new BufferedReader(new InputStreamReader(conn.getErrorStream()))) {
                    StringBuilder response = new StringBuilder();
                    String inputLine;
                    while ((inputLine = in.readLine()) != null) {
                        response.append(inputLine);
                    }
                    LOGGER.warning("OAuth Error Response: " + response.toString());
                }
            }
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "An error occurred while fetching OAuth token", e);
        }
        return null;
    }
}
