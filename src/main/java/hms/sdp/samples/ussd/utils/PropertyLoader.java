package hms.sdp.samples.ussd.utils;

import java.io.IOException;
import java.io.InputStream;
import java.lang.IllegalStateException;
import java.lang.String;
import java.util.MissingResourceException;
import java.util.Properties;
import java.util.logging.Logger;

/**
 * Loading menu contents form a property file.
 * Singleton object.
 * @author emil
 */
public class PropertyLoader {
	
	private final static Logger LOGGER = Logger.getLogger(PropertyLoader.class.getName());
    
    private final static String PROPERTY_NAME="ussdmenu.properties";
	 
	private static PropertyLoader instance;
	
	private Properties properties = new Properties();
	
	private PropertyLoader(){
        loadProperties();
	}

    /**
     * Only one instance
     * @return
     */
	public synchronized static PropertyLoader getInstance(){
		if(instance==null){
			instance=new PropertyLoader();
		}
		return instance;
	}

    /**
     * load property file from resource location
     */
	private void loadProperties(){
		try {
            InputStream in = this.getClass().getClassLoader().getResourceAsStream(PROPERTY_NAME);
			properties.load(in);
		} catch (IOException e) {
			LOGGER.info(PROPERTY_NAME+" unable to load.");
			throw new IllegalStateException(e.toString());
		}
	}

    /**
     * Return string value for given key
     * @param key - String
     * @return
     */
	public String getText(String key){
		final String value=properties.getProperty(key);
		if(value==null||value.isEmpty())
			throw new MissingResourceException("Expected value null or empty", PROPERTY_NAME,key);
		else
			return value;
	}

}