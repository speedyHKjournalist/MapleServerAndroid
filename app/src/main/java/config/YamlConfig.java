package config;

import android.content.Context;
import com.esotericsoftware.yamlbeans.YamlReader;

import java.io.*;
import java.util.List;


public class YamlConfig {
    public static final String CONFIG_FILE_NAME = "config.yaml";
    public static YamlConfig config = null;
    public List<WorldConfig> worlds;
    public ServerConfig server;

    public static YamlConfig loadConfig(Context context) {
        try {
            File configFile = new File(context.getFilesDir(), "config.yaml");
            FileInputStream inputStream = new FileInputStream(configFile);
            YamlReader reader = new YamlReader(new InputStreamReader(inputStream))/*, CharsetConstants.CHARSET))*/;
            YamlConfig config = reader.read(YamlConfig.class);
            reader.close();
            return config;
        } catch (FileNotFoundException e) {
            throw new RuntimeException("Could not read config file " + YamlConfig.CONFIG_FILE_NAME + ": " + e.getMessage());
        } catch (IOException e) {
            throw new RuntimeException("Could not successfully parse config file " + YamlConfig.CONFIG_FILE_NAME + ": " + e.getMessage());
        }
    }
}
