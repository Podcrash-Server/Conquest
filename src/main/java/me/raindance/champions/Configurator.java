package me.raindance.champions;

import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Consumer;

/**
 * Just a feeling that the async stuff is uneeded as FileConfiguration just caches it....
 *
 */
public class Configurator {
    private static final int MAX_THREADS = 5;
    private static final ExecutorService executor = Executors.newFixedThreadPool(MAX_THREADS);
    private JavaPlugin plugin;

    private File configFile;
    private FileConfiguration config;
    private String fileName;

    private File getFileFromFolder(File folder, String fileName) {
        if(folder.isDirectory()) {
            for(File file1 : folder.listFiles()) {
                if(fileName.equals(file1.getName())) return file1;
            }
        }
        return null;
    }

    public Configurator(JavaPlugin plugin, String fileName, boolean hasDefaults) {
        this.plugin = plugin;
        this.fileName = fileName + ".yml";
        plugin.getLogger().info("[Configurator] Loading " + this.fileName);
        if((this.configFile = getFileFromFolder(plugin.getDataFolder(), this.fileName)) == null) {
            plugin.getLogger().info("[Configurator] " + this.fileName + " did not exist! Creating!");
            this.configFile = new File(plugin.getDataFolder(), this.fileName);
            if (hasDefaults) {
                plugin.getLogger().info("[Configurator] " + this.fileName + " saving default values!");
                plugin.saveResource(this.fileName, false);
            }
        }
        this.config = YamlConfiguration.loadConfiguration(this.configFile);
        saveConfig();
    }
    public Configurator(JavaPlugin plugin, String fileName) {
        this(plugin, fileName, false);
    }

    public CompletableFuture<Void>  read(String path, Consumer<Object> consumer) {
        return CompletableFuture.supplyAsync(() -> {
            return this.config.get(path);
        }, executor).thenAcceptAsync(consumer);
    }
    public CompletableFuture<Void>  readInt(String path, Consumer<Integer> consumer) {
        return CompletableFuture.supplyAsync(() -> {
            return (int) this.config.get(path);
        }, executor).thenAcceptAsync(consumer);
    }
    public CompletableFuture<Void>  readDouble(String path, Consumer<Double> consumer) {
        return CompletableFuture.supplyAsync(() -> {
            return (double) this.config.get(path);
        }, executor).thenAcceptAsync(consumer);
    }
    public CompletableFuture<Void> readString(String path, Consumer<String> consumer) {
        return CompletableFuture.supplyAsync(() -> {
            return (String) this.config.get(path);
        }, executor).thenAcceptAsync(consumer);
    }

    public void set(String path, Object value) {
        Runnable run = () -> config.set(path, value);
        executor.submit(run);
    }

    public boolean hasPath(String path) {
        return config.isSet(path);
    }

    public void deletePath(String path) {
        Runnable deleteCall = () -> {
            if(config.isSet(path) ) {
                config.set(path, null);
                saveConfig();
                Main.getInstance().getLogger().info("Saved deleting " + path + "!");
            }
        };
        executor.submit(deleteCall);
        Main.getInstance().getLogger().info("Deleting " + path + "!");
    }
    public void saveConfig(){
        synchronized (config) {
            try {
                config.save(configFile);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public FileConfiguration getConfig() {
        return config;
    }

    public void reloadConfig() {
        config = YamlConfiguration.loadConfiguration(configFile);
        Reader defConfigStream = new InputStreamReader(plugin.getResource(fileName), StandardCharsets.UTF_8);
        if (defConfigStream != null) {
            YamlConfiguration defConfig = YamlConfiguration.loadConfiguration(defConfigStream);
            config.setDefaults(defConfig);
        }
    }
}
