package config;

import android.content.Context;
import com.esotericsoftware.yamlbeans.YamlReader;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.List;
import android.content.res.AssetManager;


public class YamlConfig {
    public static final String CONFIG_FILE_NAME = "config.yaml";
    public static YamlConfig config = null;
    public List<WorldConfig> worlds;
    public ServerConfig server;

    public static YamlConfig loadConfig(Context context) {
        try {
            AssetManager assetManager = context.getAssets();
            InputStream inputStream = assetManager.open("config.yaml");

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
