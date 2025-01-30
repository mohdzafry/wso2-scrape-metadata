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

        // Determine the Linux distribution
        LinuxDistributionChecker.Distribution distribution = LinuxDistributionChecker.getLinuxDistribution();

        // Fetch all installed packages based on the distribution
        try {
            String command;
            switch (distribution) {
                case UBUNTU:
                case DEBIAN:
                    command = "dpkg-query -l"; // Fetch all installed packages on Debian/Ubuntu
                    break;
                case CENTOS:
                case RHEL:
                    command = "rpm -qa"; // Fetch all installed packages on CentOS/RHEL
                    break;
                case ARCH:
                    command = "pacman -Q"; // Fetch all installed packages on Arch Linux
                    break;
                default:
                    LOGGER.warning("Unsupported Linux distribution for library version check.");
                    return libraryVersionCache;
            }

            Process process = Runtime.getRuntime().exec(command);
            BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
            String line;

            while ((line = reader.readLine()) != null) {
                String[] parts = line.split("\\s+");
                String packageName, version;

                switch (distribution) {
                    case UBUNTU:
                    case DEBIAN:
                        // Example line: "ii  openssl  1.1.1f-1ubuntu2.16  amd64  Secure Sockets Layer toolkit"
                        if (line.startsWith("ii")) {
                            packageName = parts[1];
                            version = parts[2];
                        } else {
                            continue; // Skip non-installed packages
                        }
                        break;
                    case CENTOS:
                    case RHEL:
                        // Example line: "openssl-1.1.1k-2.el8.x86_64"
                        packageName = parts[0].substring(0, parts[0].lastIndexOf("-"));
                        version = parts[0].substring(parts[0].lastIndexOf("-") + 1);
                        break;
                    case ARCH:
                        // Example line: "openssl 1.1.1.k-1"
                        packageName = parts[0];
                        version = parts[1];
                        break;
                    default:
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
}