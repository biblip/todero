package com.social100.todero.console.base;

import com.social100.todero.common.channels.EventChannel;
import com.social100.todero.common.command.CommandContext;
import com.social100.todero.common.model.plugin.Command;
import com.social100.todero.common.model.plugin.Component;
import com.social100.todero.common.model.plugin.Plugin;
import com.social100.todero.common.model.plugin.PluginInterface;
import lombok.Getter;
import org.reflections.Reflections;
import org.reflections.scanners.Scanners;
import org.reflections.util.ConfigurationBuilder;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Properties;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantReadWriteLock;

@Getter
public class PluginContext {
    private final Map<String, Plugin> plugins = new ConcurrentHashMap<>();
    private final ReentrantReadWriteLock lock = new ReentrantReadWriteLock();

    public PluginContext(Path pluginDir, File pluginJar, EventChannel.EventListener eventListener) throws Exception {
        initializePlugin(pluginDir, pluginJar, eventListener);
    }

    public void initializePlugin(Path pluginDir, File pluginJar, EventChannel.EventListener eventListener) throws Exception {
        // 1) metadata
        Properties meta = loadPluginMetadata(pluginJar);
        String name = meta.getProperty("name");
        if (name == null || name.isEmpty()) {
            throw new IllegalStateException("Plugin descriptor must define a non-empty 'name'");
        }

        lock.writeLock().lock();
        try {
            if (plugins.containsKey(name)) {
                throw new IllegalStateException("Plugin with name '" + name + "' is already loaded");
            }

            // 2) build plugin loader, but force certain packages to parent-first
            URL jarUrl = pluginJar.toURI().toURL();
            //URL[] urls = new URL[]{ jarUrl };
            List<URL> urls = new ArrayList<>();
            urls.add(jarUrl);

            Path libDir = pluginDir.resolve("lib");
            if (Files.isDirectory(libDir)) {
                try (DirectoryStream<Path> stream = Files.newDirectoryStream(libDir, "*.jar")) {
                    for (Path jar : stream) {
                        urls.add(jar.toUri().toURL());
                    }
                }
            }

            // these packages will *always* be loaded by the parent (app) loader:
            String[] parentFirst = new String[]{
                "com.social100.todero.common"
            };
            ChildFirstURLClassLoader pluginLoader = new ChildFirstURLClassLoader(
                urls.toArray(new URL[0]),
                getClass().getClassLoader(),
                parentFirst
            );

            // 3) load the interface (will actually come from the parent)
            @SuppressWarnings("unchecked")
            Class<EventChannel.EventListener> listenerIface =
                (Class<EventChannel.EventListener>) pluginLoader
                    .loadClass("com.social100.todero.common.channels.EventChannel$EventListener");

            // 4) scan for implementations of PluginInterface
            Reflections reflections = new Reflections(new ConfigurationBuilder()
                .addUrls(jarUrl)
                .addClassLoaders(pluginLoader)
                .addScanners(Scanners.TypesAnnotated, Scanners.SubTypes)
                .setExpandSuperTypes(false)
            );

            Set<Class<? extends PluginInterface>> pluginClasses = reflections
                .getSubTypesOf(PluginInterface.class);
            reflections.getStore().clear();

            for (Class<? extends PluginInterface> pluginClass : pluginClasses) {
                if (pluginClass.isInterface()) continue;

                // 5) lookup the ctor that takes the plugin‐loader’s EventListener
                Constructor<? extends PluginInterface> ctor =
                    pluginClass.getDeclaredConstructor(listenerIface);
                ctor.setAccessible(true);

                // 7) instantiate
                PluginInterface instance = ctor.newInstance(eventListener);

                // 8) register it
                Component comp = instance.getComponent();
                String desc = (comp != null && comp.getDescription() != null)
                    ? comp.getDescription() : "";
                Map<String, Map<String, Command>> commands = (comp != null)
                    ? comp.getCommands() : new HashMap<>();

                Plugin plugin = Plugin.builder()
                    .file(pluginJar)
                    .classLoader(pluginLoader)
                    .pluginClass(pluginClass)
                    .pluginInstance(instance)
                    .component(Component.builder()
                        .name(name)
                        .description(desc)
                        .commands(commands)
                        .build())
                    .build();

                plugins.put(name, plugin);
            }
        } finally {
            lock.writeLock().unlock();
        }
    }

    public boolean unloadPlugin(String pluginName) {
        lock.writeLock().lock();
        try {
            Plugin plugin = plugins.remove(pluginName);
            if (plugin == null) return false;

            // Invoke optional shutdown hook
            try {
                Method shutdown = plugin.getPluginInstance().getClass().getMethod("shutdown");
                shutdown.invoke(plugin.getPluginInstance());
            } catch (NoSuchMethodException ignored) {
            } catch (Exception e) {
                e.printStackTrace();
            }

            // Remove BouncyCastle provider if registered by plugin
            /*if (Security.getProvider(BouncyCastleProvider.PROVIDER_NAME) != null) {
                Security.removeProvider(BouncyCastleProvider.PROVIDER_NAME);
            }*/

            // Close class loader to release file handles
            try {
                ((URLClassLoader) plugin.getClassLoader()).close();
            } catch (IOException e) {
                e.printStackTrace();
            }

            return true;
        } finally {
            lock.writeLock().unlock();
        }
    }

    public boolean reloadPlugin(String pluginName, EventChannel.EventListener listener) {
        lock.writeLock().lock();
        try {
            Plugin plugin = plugins.get(pluginName);
            if (plugin == null) return false;
            File jar = plugin.getFile();
            // Unload
            if (!unloadPlugin(pluginName)) return false;
            // Re-initialize
            initializePlugin(jar.getParentFile().toPath(), jar, listener);
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        } finally {
            lock.writeLock().unlock();
        }
    }

    private Properties loadPluginMetadata(File jarFile) throws IOException {
        URL jarUrl = jarFile.toURI().toURL();

        // Create a ClassLoader that can see inside that JAR
        try (URLClassLoader metaLoader = new URLClassLoader(new URL[]{ jarUrl })) {
            // Load plugin.properties from the root of the JAR
            try (InputStream in = metaLoader.getResourceAsStream("plugin.properties")) {
                if (in == null) {
                    throw new IOException("plugin.properties not found in " + jarFile.getName());
                }
                Properties props = new Properties();
                props.load(in);
                return props;
            }
        }
    }

    // Cleanup method to properly close the plugin class loader when no longer needed
    public void cleanup() {
        /*
        try {
            plugin.getClassLoader().close();
            pluginExecutor.shutdown();
        } catch (Exception e) {
            e.printStackTrace();
        }
         */
    }

    public Object execute(String pluginName, String command, CommandContext context) {
        Optional<Plugin> selectedPlugin = plugins
                .values()
                .stream()
                .filter(p -> isPluginAndHasCommand(pluginName, command, p))
                .findFirst();
        if (selectedPlugin.isEmpty()) {
            return "Command Not Found";
        }
        return selectedPlugin.get().getPluginInstance().execute(pluginName, command, context);
    }

    private static boolean isPluginAndHasCommand(String pluginName, String command, Plugin p) {
        return true;
        //return pluginName.equals(Optional.ofNullable(p.getPluginInstance().name()).orElse(""))
        //        && Optional.ofNullable(p.getPluginInstance().hasCommand(command)).orElse(false);
    }
}
