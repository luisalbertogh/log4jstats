/**
 * © 2009-2012 Tex Toll Services, LLC
 */
package net.luisalbertogh.log4jstats.interfaces;

import java.util.List;
import java.util.Map;

import net.luisalbertogh.log4jstats.utils.LogLevels;

/**
 * This interface bundles the main methods for the statistics service.
 * 
 * @author lagarcia
 */
public interface TableInterface {
    /**
     * Get the data of the logging system for table rendering.
     * 
     * @param datePattern
     * @param logsDir
     * @param level
     * @param start - int First index of returned subset
     * @param end - int Last index of returned subset
     * @return Dataset with events data.
     * @throws Exception
     */
    public List<Map<String, String>> getEventsData(String datePattern, String logsDir, LogLevels level, int start,
            int end) throws Exception;

    /**
     * Export the dataset to a CSV format.
     * 
     * @param dataset
     * @param chartDetails
     * @return
     */
    public String getEventsCSV(List<Map<String, String>> dataset);

    /**
     * Export the dataset to a HTML table.
     * 
     * @param dataset
     * @param chartDetails
     * @return
     */
    public String getEventsHTML(List<Map<String, String>> dataset);
}
