package org.wso2.scrape;

import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Main class to execute the scraping process.
 */
public class ScrapeOSJDKWSO2App {

    private static final Logger LOGGER = Logger.getLogger(ScrapeOSJDKWSO2App.class.getName());

    public static void main(String[] args) {
        try {
            if (args.length < 1) {
                throw new RuntimeException("CARBON_HOME directory must be passed as an argument.");
            }

            String carbonHome = args[0];
            ScrapeManager scrapeManager = new ScrapeManager(carbonHome);
            scrapeManager.execute(args);
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "An error occurred during execution", e);
        }
    }
}