package com.social100.todero.common.config;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import io.github.cdimascio.dotenv.Dotenv;
import org.apache.commons.text.StringSubstitutor;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.CodeSource;
import java.security.ProtectionDomain;
import java.util.HashMap;
import java.util.Map;

public class Utils {

  private Utils() {}

  public static AppConfig loadAppConfig(@Nullable String[] args) {
    String configFilePath = null;

    // Check for the "--config" parameter
    if (args != null) {
      for (int i = 0; i < args.length; i++) {
        if ("--config".equals(args[i]) && (i + 1) < args.length) {
          configFilePath = args[i + 1];
          break;
        }
      }
    }

    // If no config file specified, look in the main application directory
    if (configFilePath == null) {
      try {
        Path appRootDir = getApplicationRootDir();
        configFilePath = appRootDir.resolve("config.yaml").toString();
      } catch (Exception e) {
        System.err.println("Error determining application root directory: " + e.getMessage());
        System.exit(1);
      }
    }

    // Load AppConfig defaults
    AppConfig defaultValues = new AppConfig();
    defaultValues.setApp(new Config());
    defaultValues.getApp().setAi_server(new ServerConfig());
    defaultValues.getApp().getAi_server().setPort(ServerType.AI.getPort());
    defaultValues.getApp().getAi_server().setHost("127.0.0.1");
    defaultValues.getApp().setAia_server(new ServerConfig());
    defaultValues.getApp().getAia_server().setPort(ServerType.AIA.getPort());
    defaultValues.getApp().getAia_server().setHost("127.0.0.1");

    // Check if config file exists
    File configFile = new File(configFilePath);
    if (!configFile.exists()) {
      System.err.println("Configuration file not found: " + configFilePath);
      return defaultValues;
    }

    // Load YAML
    try {
      // Load .env file (automatically reads from the current working directory)
      Dotenv dotenv = Dotenv.load();

      // Merge .env and system environment variables
      Map<String, String> variables = new HashMap<>(dotenv.entries().stream()
          .collect(HashMap::new,
              (m, e) -> m.put(e.getKey(), e.getValue()),
              HashMap::putAll));
      variables.putAll(System.getenv()); // system vars take precedence

      // Read YAML as text
      String yamlContent = Files.readString(configFile.toPath());

      // Replace ${VAR} placeholders
      StringSubstitutor substitutor = new StringSubstitutor(variables);
      String resolvedYaml = substitutor.replace(yamlContent);

      // Parse YAML into object
      ObjectMapper yamlMapper = new ObjectMapper(new YAMLFactory());
      yamlMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
      yamlMapper.configure(DeserializationFeature.FAIL_ON_INVALID_SUBTYPE, false);

      return yamlMapper.readValue(resolvedYaml, AppConfig.class);
    } catch (IOException e) {
      System.err.println("Error reading configuration file: " + e.getMessage());
    }

    return defaultValues;
  }

  public static Path getApplicationRootDir() throws Exception {
    // 1) If launched with `java -jar app.jar`, this will end with .jar
    Path viaSunCmd = tryFromSunJavaCommand();
    if (viaSunCmd != null) return viaSunCmd;

    // 2) Try the TCCL base (works for classes dirs and many fat-jar setups)
    //Path viaTccl = tryFromTcclBase();
    //if (viaTccl != null) return viaTccl;

    // 3) Walk the classpath, but skip our own utility’s location
    //Path self = locationOf(Utils.class);
    //Path viaClasspath = tryFromClasspathExcluding(self);
    //if (viaClasspath != null) return viaClasspath;

    // 4) Last resort: working directory
    return Paths.get(System.getProperty("user.dir")).toAbsolutePath().normalize();
  }

  // ---------- helpers ----------

  private static Path tryFromSunJavaCommand() {
    String cmd = System.getProperty("sun.java.command", "");
    if (cmd.endsWith(".jar")) {
      Path jar = Paths.get(cmd);
      if (!jar.isAbsolute()) {
        jar = Paths.get(System.getProperty("user.dir")).resolve(cmd);
      }
      jar = jar.normalize();
      if (Files.exists(jar)) return jar.getParent();
    }
    return null;
  }

  private static Path tryFromTcclBase() throws Exception {
    ClassLoader cl = Thread.currentThread().getContextClassLoader();
    if (cl == null) return null;

    URL base = cl.getResource("");
    if (base == null) return null;

    String protocol = base.getProtocol();
    if ("file".equalsIgnoreCase(protocol)) {
      Path p = Paths.get(base.toURI()).toAbsolutePath().normalize();
      // classes dir -> its path; if it's a file (unlikely), use its parent
      return Files.isRegularFile(p) ? p.getParent() : p;
    }
    if ("jar".equalsIgnoreCase(protocol)) {
      // jar:file:/path/app.jar!/BOOT-INF/classes!/   -> strip to app.jar
      Path jar = jarUrlToJarPath(base);
      if (jar != null) return Files.isRegularFile(jar) ? jar.getParent() : jar;
    }
    return null;
  }

  private static Path tryFromClasspathExcluding(Path exclude) {
    String cp = System.getProperty("java.class.path", "");
    if (cp.isEmpty()) return null;

    String[] parts = cp.split(File.pathSeparator);
    for (String part : parts) {
      if (part == null || part.isEmpty()) continue;
      Path p = Paths.get(part).toAbsolutePath().normalize();
      if (exclude != null && sameFileSafe(p, exclude)) continue;

      if (Files.exists(p)) {
        // If it's a jar/file, use parent; if dir, use the dir itself
        return Files.isRegularFile(p) ? p.getParent() : p;
      }
    }
    return null;
  }

  private static Path locationOf(Class<?> cls) throws RuntimeException {
    try {
      ProtectionDomain pd = cls.getProtectionDomain();
      if (pd == null) return null;
      CodeSource cs = pd.getCodeSource();
      if (cs == null) return null;
      URL url = cs.getLocation();
      if (url == null) return null;

      String protocol = url.getProtocol();
      if ("file".equalsIgnoreCase(protocol)) {
        return Paths.get(url.toURI()).toAbsolutePath().normalize();
      }
      if ("jar".equalsIgnoreCase(protocol)) {
        Path jar = jarUrlToJarPath(url);
        return jar == null ? null : jar.toAbsolutePath().normalize();
      }
      return null;
    } catch (Exception e) {
      return null;
    }
  }

  private static Path jarUrlToJarPath(URL jarUrl) {
    try {
      // jarUrl.getFile() -> "file:/path/app.jar!/BOOT-INF/classes!/"
      String spec = jarUrl.getFile();
      int bang = spec.indexOf("!/");
      if (bang >= 0) spec = spec.substring(0, bang); // keep up to app.jar
      // spec now "file:/path/app.jar"
      URI fileUri = URI.create(spec);
      return Paths.get(fileUri).normalize();
    } catch (Exception e) {
      return null;
    }
  }

  private static boolean sameFileSafe(Path a, Path b) {
    try {
      return a != null && b != null && Files.exists(a) && Files.exists(b) && Files.isSameFile(a, b);
    } catch (Exception ignored) {
      // fallback to string equality if isSameFile fails
      return a != null && b != null && a.normalize().toString().equals(b.normalize().toString());
    }
  }
}
