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
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Utility class to determine the Linux distribution.
 */
public class LinuxDistributionChecker {

    private static final Logger LOGGER = Logger.getLogger(LinuxDistributionChecker.class.getName());

    /**
     * Enum representing supported Linux distributions.
     */
    public enum Distribution {
        UBUNTU,
        DEBIAN,
        CENTOS,
        RHEL,
        ARCH,
        UNKNOWN
    }

    /**
     * Determines the Linux distribution by reading /etc/os-release.
     *
     * @return The detected Linux distribution.
     */
    public static Distribution getLinuxDistribution() {
        try {
            Process process = Runtime.getRuntime().exec("cat /etc/os-release");
            BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
            String line;

            while ((line = reader.readLine()) != null) {
                if (line.startsWith("NAME=")) {
                    if (line.contains("Ubuntu")) {
                        return Distribution.UBUNTU;
                    } else if (line.contains("Debian")) {
                        return Distribution.DEBIAN;
                    } else if (line.contains("CentOS")) {
                        return Distribution.CENTOS;
                    } else if (line.contains("Red Hat")) {
                        return Distribution.RHEL;
                    } else if (line.contains("Arch Linux")) {
                        return Distribution.ARCH;
                    }
                }
            }
        } catch (IOException e) {
            LOGGER.log(Level.WARNING, "Failed to check Linux distribution", e);
        }

        return Distribution.UNKNOWN;
    }
}