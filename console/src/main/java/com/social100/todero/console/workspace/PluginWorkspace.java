package com.social100.todero.console.workspace;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Properties;
import java.util.Set;

public class PluginWorkspace {
    private String pluginName;
    private Workspace workspace;

    // Directories for this plugin within the workspace
    private File pluginDir;
    private File propertiesDir;
    private File filesDir;

    // File used to store properties
    private File propertiesFile;
    private Properties properties = new Properties();

    /**
     * Constructs a Plugin instance within a given Workspace.
     * It sets up the directories for properties and files.
     *
     * @param pluginName the plugin identifier
     * @param workspace  the workspace that owns this plugin
     */
    public PluginWorkspace(String pluginName, Workspace workspace) {
        this.pluginName = pluginName;
        this.workspace = workspace;

        // Create a directory for this plugin under workspace/plugins
        this.pluginDir = new File(workspace.getPluginsDir(), pluginName);
        this.propertiesDir = new File(pluginDir, "properties");
        this.filesDir = new File(pluginDir, "files");

        // Create directories if they do not exist.
        if (!pluginDir.exists()) {
            pluginDir.mkdirs();
        }
        if (!propertiesDir.exists()) {
            propertiesDir.mkdirs();
        }
        if (!filesDir.exists()) {
            filesDir.mkdirs();
        }

        // Define the properties file inside the properties directory.
        this.propertiesFile = new File(propertiesDir, "plugin.properties");

        // Load existing properties if available.
        loadProperties();
    }

    ///////////// Properties Management /////////////

    /**
     * Adds or updates a property.
     *
     * @param key   the property key
     * @param value the property value
     */
    public void addProperty(String key, String value) {
        properties.setProperty(key, value);
        saveProperties();
    }

    /**
     * Removes a property by key.
     *
     * @param key the property key to remove
     */
    public void removeProperty(String key) {
        properties.remove(key);
        saveProperties();
    }

    /**
     * Retrieves the value of a property.
     *
     * @param key the property key
     * @return the property value or null if not found
     */
    public String getProperty(String key) {
        return properties.getProperty(key);
    }

    /**
     * Lists all the property keys.
     *
     * @return a Set of all property keys
     */
    public Set<String> listProperties() {
        return properties.stringPropertyNames();
    }

    // Loads properties from the properties file.
    private void loadProperties() {
        if (propertiesFile.exists()) {
            try (FileInputStream fis = new FileInputStream(propertiesFile)) {
                properties.load(fis);
            } catch (IOException e) {
                System.err.println("Failed to load properties for plugin " + pluginName);
                e.printStackTrace();
            }
        }
    }

    // Saves properties to the properties file.
    private void saveProperties() {
        try (FileOutputStream fos = new FileOutputStream(propertiesFile)) {
            properties.store(fos, "Properties for plugin: " + pluginName);
        } catch (IOException e) {
            System.err.println("Failed to save properties for plugin " + pluginName);
            e.printStackTrace();
        }
    }

    ///////////// File Management /////////////

    /**
     * Adds a file to the plugin's file storage area.
     * This method copies the source file into the plugin's files directory.
     *
     * @param sourceFile the file to add
     * @throws IOException if an I/O error occurs during copying
     */
    public void addFile(File sourceFile) throws IOException {
        File destFile = new File(filesDir, sourceFile.getName());
        Files.copy(sourceFile.toPath(), destFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
    }

    /**
     * Deletes a file from the plugin's file storage.
     *
     * @param filename the name of the file to delete
     * @return true if deletion was successful; false otherwise
     */
    public boolean deleteFile(String filename) {
        File fileToDelete = new File(filesDir, filename);
        return fileToDelete.delete();
    }

    /**
     * Retrieves a file from the plugin's storage.
     *
     * @param filename the name of the file to retrieve
     * @return the File object if it exists; null otherwise
     */
    public File getFile(String filename) {
        File file = new File(filesDir, filename);
        return file.exists() ? file : null;
    }

    /**
     * Lists all files stored for this plugin.
     *
     * @return a List of Files in the plugin's file storage
     */
    public List<File> listFiles() {
        File[] files = filesDir.listFiles();
        return files != null ? Arrays.asList(files) : Collections.emptyList();
    }
}