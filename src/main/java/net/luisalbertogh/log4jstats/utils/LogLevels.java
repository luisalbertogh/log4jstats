package net.luisalbertogh.log4jstats.utils;

import java.util.HashSet;
import java.util.Set;

/**
 * Log4j levels.
 * 
 * @author lagarcia
 */
public enum LogLevels {
    /**
     * 
     */
    FATAL("FATAL"), /**
     * 
     */
    ERROR("ERROR"), /**
     * 
     */
    WARN("WARN"), /**
     * 
     */
    INFO("INFO"), /**
     * 
     */
    DEBUG("DEBUG"),
    /**
     * 
     */
    TRACE("TRACE");

    private String level;

    /**
     * Constructor
     * 
     * @param level
     */
    LogLevels(String level) {
        this.level = level;
    }

    /**
     * Get level.
     * 
     * @return
     */
    public String getLevel() {
        return this.level;
    }

    /**
     * Get a list of the available log labels.
     * 
     * @return
     */
    public Set<String> getLogLevels() {
        Set<String> logLevels = new HashSet<String>();
        logLevels.add(FATAL.getLevel());
        logLevels.add(ERROR.getLevel());
        logLevels.add(DEBUG.getLevel());
        logLevels.add(INFO.getLevel());
        logLevels.add(WARN.getLevel());
        logLevels.add(TRACE.getLevel());
        return logLevels;
    }
}
