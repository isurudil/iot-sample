package hms.sdp.samples.iot.util;


import org.apache.log4j.Logger;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

public class PropertyLoader {

    public static final Properties properties = new Properties();
    private static Logger logger = Logger.getLogger(PropertyLoader.class);

    public static String getProperty(String key) {
        return properties.getProperty(key);
    }

    public static boolean getBoolean(String key) {
        return Boolean.parseBoolean(properties.getProperty(key));
    }

    public static int getInt(String key) {
        return Integer.parseInt(properties.getProperty(key));
    }

    public static long getLong(String key) {
        return Long.parseLong(properties.getProperty(key));
    }

    static {
        InputStream inputStream = PropertyLoader.class.getClassLoader().getResourceAsStream("app.properties");
        try {
            properties.load(inputStream);
        } catch (IOException e) {
            logger.error("Error while loading properties", e);
        }
    }
}
