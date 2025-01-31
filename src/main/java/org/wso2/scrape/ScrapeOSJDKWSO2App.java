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