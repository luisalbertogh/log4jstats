/**
 * © 2009-2012 Tex Toll Services, LLC
 */
package net.luisalbertogh.log4jstats.interfaces;

import java.util.Map;

import net.luisalbertogh.log4jstats.utils.LogLevels;

/**
 * This interface bundles the main methods for the statistics service.
 * 
 * @author lagarcia
 */
public interface StatisticsInterface {

    /**
     * Get the relation between the number of log entries and the date they were raised.
     * 
     * @param datePattern
     * @param logsDir
     * @param level - Log4j level to filter in
     * @return Map<String,Integer> - Relation between data as text and number of events.
     * @throws Exception
     */
    public Map<String, Integer> getNEventsPerDate(String datePattern, String logsDir, LogLevels level) throws Exception;

    /**
     * Get number of different events per date.
     * 
     * @param datePattern
     * @param logsDir
     * @return Map of different events with map of data/n events.
     * @throws Exception
     */
    public Map<String, Map<String, Integer>> getNDiffEventsPerDate(String datePattern, String logsDir) throws Exception;

    /**
     * Get the total number of events for the different log levels.
     * 
     * @param dataset - Data with log events grouped by date and by log level.
     * @return
     * @throws Exception
     */
    public Map<String, Integer> getTotalEventNumbers(Map<String, Map<String, Integer>> dataset) throws Exception;

    /**
     * Retrieve a dataset with the details of the application event logs, ready to be used for plotting or rendering.
     * 
     * @param datePattern
     * @param logsDir
     * @param level
     * @param appEventFilter
     * @return
     * @throws Exception
     */
    public Map<String, Object> getAppEventsTableData(String datePattern, String logsDir, LogLevels level,
            String appEventFilter) throws Exception;
}
