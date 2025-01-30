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