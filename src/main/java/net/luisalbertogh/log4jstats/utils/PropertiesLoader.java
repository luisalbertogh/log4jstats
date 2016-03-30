package net.luisalbertogh.log4jstats.utils;

import java.util.Properties;

/**
 * Load and return properties values.
 * 
 * @author lagarcia
 */
public class PropertiesLoader {

    /** Properties */
    private Properties props;

    /**
     * Load the properties with the resource name as argument.
     * 
     * @param resource
     */
    public void load(String resource) {
        try {
            props = new Properties();
            props.load(this.getClass().getResourceAsStream(resource));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Get property value.
     * 
     * @param name
     * @return
     */
    public String getProperty(String name) {
        return props.getProperty(name);
    }
}
