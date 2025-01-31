package org.wso2.scrape.utils;
import java.util.logging.Logger;


/**
 * Utility class for handling JSON operations such as parsing and extracting values.
 */
public class JsonUtil {

    private static final Logger LOGGER = Logger.getLogger(JsonUtil.class.getName());

    /**
     * Extracts the value associated with a given key from a JSON string.
     *
     * @param jsonString The JSON string to parse.
     * @param key        The key whose value needs to be extracted.
     * @return The value associated with the key, or "Not Found" if the key is not present.
     */
    public static String getValueFromJson(String jsonString, String key) {
        String value = "Not Found";
        int keyIndex = jsonString.indexOf("\"" + key + "\"");
        if (keyIndex != -1) {
            int valueStartIndex = jsonString.indexOf(":", keyIndex) + 1;
            int valueEndIndex = jsonString.indexOf(",", valueStartIndex);
            if (valueEndIndex == -1) {
                valueEndIndex = jsonString.indexOf("}", valueStartIndex);
            }
            value = jsonString.substring(valueStartIndex, valueEndIndex).trim().replace("\"", "").replace(",", "").replace("}", "");
        }
        return value;
    }
}