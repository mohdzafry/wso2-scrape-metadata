package org.wso2.scrape;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Fetches library versions dynamically based on the Linux distribution.
 */
public class LibraryVersionFetcher {

    private static final Logger LOGGER = Logger.getLogger(LibraryVersionFetcher.class.getName());
    private static final Map<String, String> libraryVersionCache = new HashMap<>(); // Cache for library versions

    /**
     * Fetches the versions of libraries specified in the configuration file.
     *
     * @param carbonHome The CARBON_HOME directory.
     * @return A map of library names and their versions.
     */
    public static Map<String, String> getLibraryVersions(String carbonHome) {
        // Return cached versions if available
        if (!libraryVersionCache.isEmpty()) {
            return libraryVersionCache;
        }

        // Load the configuration file
        Properties properties = new Properties();
        try (FileInputStream input = new FileInputStream(carbonHome + "/scrape/scrape_config.properties")) {
            properties.load(input);
        } catch (IOException e) {
            LOGGER.log(Level.WARNING, "Failed to load libraries configuration", e);
            return libraryVersionCache; // Return empty cache if configuration fails
        }

        // Get the list of libraries from the configuration
        String libraries = properties.getProperty("libraries", "");
        Set<String> librarySet = new HashSet<>(Arrays.asList(libraries.split(",")));

        // Fetch all installed packages in a single command
        try {
            String command;
            if (isUbuntuOrDebian()) {
                command = "dpkg-query -l"; // Fetch all installed packages on Debian/Ubuntu
            } else if (isCentOSOrRHEL()) {
                command = "rpm -qa"; // Fetch all installed packages on CentOS/RHEL
            } else if (isArchLinux()) {
                command = "pacman -Q"; // Fetch all installed packages on Arch Linux
            } else {
                LOGGER.warning("Unsupported Linux distribution for library version check.");
                return libraryVersionCache;
            }

            Process process = Runtime.getRuntime().exec(command);
            BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
            String line;

            while ((line = reader.readLine()) != null) {
                String[] parts = line.split("\\s+");
                String packageName, version;

                if (isUbuntuOrDebian()) {
                    // Example line: "ii  openssl  1.1.1f-1ubuntu2.16  amd64  Secure Sockets Layer toolkit"
                    if (line.startsWith("ii")) {
                        packageName = parts[1];
                        version = parts[2];
                    } else {
                        continue; // Skip non-installed packages
                    }
                } else if (isCentOSOrRHEL()) {
                    // Example line: "openssl-1.1.1k-2.el8.x86_64"
                    packageName = parts[0].substring(0, parts[0].lastIndexOf("-"));
                    version = parts[0].substring(parts[0].lastIndexOf("-") + 1);
                } else if (isArchLinux()) {
                    // Example line: "openssl 1.1.1.k-1"
                    packageName = parts[0];
                    version = parts[1];
                } else {
                    continue; // Skip unsupported distributions
                }

                // Check if the package is in the list of libraries to query
                if (librarySet.contains(packageName)) {
                    libraryVersionCache.put(packageName, version);
                }
            }
        } catch (IOException e) {
            LOGGER.log(Level.WARNING, "Failed to get library versions", e);
        }

        return libraryVersionCache;
    }

    /**
     * Checks if the Linux distribution is Ubuntu or Debian.
     *
     * @return true if the distribution is Ubuntu or Debian, false otherwise.
     */
    private static boolean isUbuntuOrDebian() {
        try {
            Process process = Runtime.getRuntime().exec("cat /etc/os-release");
            BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
            String line = reader.readLine();
            return line != null && (line.contains("Ubuntu") || line.contains("Debian"));
        } catch (IOException e) {
            LOGGER.log(Level.WARNING, "Failed to check Linux distribution", e);
        }
        return false;
    }

    /**
     * Checks if the Linux distribution is CentOS or RHEL.
     *
     * @return true if the distribution is CentOS or RHEL, false otherwise.
     */
    private static boolean isCentOSOrRHEL() {
        try {
            Process process = Runtime.getRuntime().exec("cat /etc/os-release");
            BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
            String line = reader.readLine();
            return line != null && (line.contains("CentOS") || line.contains("Red Hat"));
        } catch (IOException e) {
            LOGGER.log(Level.WARNING, "Failed to check Linux distribution", e);
        }
        return false;
    }

    /**
     * Checks if the Linux distribution is Arch Linux.
     *
     * @return true if the distribution is Arch Linux, false otherwise.
     */
    private static boolean isArchLinux() {
        try {
            Process process = Runtime.getRuntime().exec("cat /etc/os-release");
            BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
            String line;
            while ((line = reader.readLine()) != null) {
                if (line.contains("NAME=\"Arch Linux\"")) {
                    return true;
                }
            }
        } catch (IOException e) {
            LOGGER.log(Level.WARNING, "Failed to check Linux distribution", e);
        }
        return false;
    }
}
