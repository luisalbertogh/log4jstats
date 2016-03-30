package net.luisalbertogh.log4jstats.services.logfiles;

import java.io.File;
import java.io.RandomAccessFile;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.StringTokenizer;
import java.util.TreeMap;
import java.util.TreeSet;

import net.luisalbertogh.log4jstats.services.SuperService;
import net.luisalbertogh.log4jstats.utils.LogLevels;

/**
 * This class implements the service for the log events table function.
 * 
 * @author lagarcia
 */
public class TableService extends SuperService {

    /** Current log levels */
    private Set<String> logLevels;

    /** Current app. events */
    private Map<String, String> appEvents;

    /** Keyword to filter in */
    private String keyword;

    /**
     * Default constructor
     * 
     * @param defaultNumberDates - Default number of dates between init and end dates
     * @param maxNumberOfDates - Maximum number of dates to process
     */
    @SuppressWarnings("static-access")
    public TableService(int defaultNumberDates, int maxNumberOfDates) {
        super(defaultNumberDates);
        this.maxNumberOfDates = maxNumberOfDates;
    }

    /**
     * Read a file line, given a file pointer, until it finds the end of line char.
     * 
     * @param raf
     * @param filePointer
     * @return
     * @throws Exception
     */
    private String readFileLine(RandomAccessFile raf, long filePointer) throws Exception {
        StringBuilder line = new StringBuilder();

        byte readByte;
        do {
            readByte = raf.readByte();
            filePointer--;
            if (filePointer > -1) {
                raf.seek(filePointer);
            }

            /** Use the right encoding */
            line.append(new String(new byte[] {readByte }, "ISO-8859-1"));
            /** End of line */
            if (((char) readByte) == '\n' || ((char) readByte) == '\r') {
                break;
            }
        } while (filePointer > -1);

        return line.reverse().toString();
    }

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
            int end) throws Exception {
        List<Map<String, String>> dataset = new ArrayList<Map<String, String>>();

        /** Log counter - First row in table start in 1 */
        int counter = 0;

        /** Random access file */
        RandomAccessFile raf = null;

        try {
            /** Log files */
            File[] logFiles = initLogFiles(logsDir);

            /** Available log levels */
            logLevels = new TreeSet<String>();

            /** Available app. events */
            appEvents = new TreeMap<String, String>();

            /** Iterate over the log files. Each files are already ordered by date */
            for (File logFile : logFiles) {
                /** Let us read the file from last line to first line, so events will appear in descending order */
                raf = new RandomAccessFile(logFile, "r");
                /** Exclude EOF char */
                long fileSize = logFile.length() - 1;
                /** File pointer set to read first char */
                long filePointer = fileSize - 1;
                if (filePointer < 0) {
                    continue;
                }
                raf.seek(filePointer);

                /** Read log file line */
                String line = "";
                while (filePointer > -1 && (line = readFileLine(raf, filePointer)) != null) {
                    /** Recalculate file pointer position to read next line in next iteration */
                    filePointer -= line.length();

                    /** Remove end of line chars at the beginning of the line, if exists */
                    if (line.charAt(0) == '\n' || line.charAt(0) == '\r') {
                        line = line.substring(1);
                    }

                    /** Skip level lines if level is not null */
                    if (level != null) {
                        String levelStr = level.getLevel();
                        if (line.indexOf(levelStr) == -1) {
                            continue;
                        }
                    }

                    /** Avoid non-log event lines */
                    String fullDateStr = "";
                    try {
                        StringTokenizer st = new StringTokenizer(line, " ");
                        /** Assuming that date are always the first two words */
                        String dateStr = st.nextToken();
                        String timeStr = st.nextToken();
                        fullDateStr = dateStr + " " + timeStr;
                        /** If this is not true, skip this line */
                        SimpleDateFormat sdf = new SimpleDateFormat(datePattern);
                        Date logDate = sdf.parse(fullDateStr);

                        /** Check dates. If log date is after end date, cotinue to next log line */
                        if (logDate.after(endDate)) {
                            continue;
                        }
                        /** If date is before initDate, finish processing data */
                        else if (logDate.before(initDate)) {
                            if (raf != null) {
                                raf.close();
                            }

                            return dataset;
                        }

                    } catch (Exception e) {
                        /** Skip problematic parsing */
                        continue;
                    }

                    /** Is log level available? */
                    String logLevel = getLevel(line).getLevel();
                    if (!isLogLevelAvailable(logLevel)) {
                        continue;
                    }

                    /** Avoid NULL or not available events */
                    Map<String, String> appEvent = getAppEvent(line);
                    if (appEvent == null) {
                        continue;
                    }

                    /** Check keyword, if exists */
                    if (keyword != null && !"".equals(keyword)) {
                        if (line.toLowerCase().indexOf(keyword) == -1) {
                            continue;
                        }
                    }

                    /** Increment counter first */
                    counter++;

                    /** Check that log line is within start and end indeces */
                    if (start != -1 && counter < start) {
                        continue;
                    }
                    /** Do not add any more events */
                    else if (end != -1 && counter > end) {
                        break;
                    }

                    Map<String, String> logLine = new HashMap<String, String>();
                    logLine.put("date", fullDateStr);
                    logLine.put("level", logLevel);
                    logLine.put("text", line);

                    /** Add data to data set */
                    logLevels.add(logLevel);
                    addAppEvent(appEvent, appEvents);
                    dataset.add(logLine);
                }

                /** Close stream */
                raf.close();

                /** Finish adding events to the final dataset */
                if (counter >= end) {
                    break;
                }
            }
        } catch (Exception e) {
            throw e;
        } finally {
            if (raf != null) {
                raf.close();
            }
        }

        return dataset;
    }

    /**
     * Get the data of the logging system for table rendering.
     * 
     * @param url
     * @param baseName - Log files base name on server
     * @param datePattern
     * @param level
     * @param start - int First index of returned subset
     * @param end - int Last index of returned subset
     * @return Dataset with events data.
     * @throws Exception
     */
    public List<Map<String, String>> getEventsDataByHTTP(String url, String baseName, String datePattern,
            LogLevels level, int start, int end) throws Exception {
        List<Map<String, String>> dataset = new ArrayList<Map<String, String>>();

        /** Log counter - First row in table start in 1 */
        int counter = 0;

        try {
            /** Log files content */
            List<String[]> logFiles = getLogFilesByHTTP(url, baseName);

            /** Available log levels */
            logLevels = new TreeSet<String>();

            /** Available app. events */
            appEvents = new TreeMap<String, String>();

            /** Iterate over the log files contents. Each files are already ordered by date */
            for (String[] logFile : logFiles) {
                for (int i = 0; i < logFile.length; i++) {
                    /** Read log file line */
                    String line = logFile[i];
                    /** Remove end of line chars at the beginning of the line, if exists */
                    if (line.length() != 0 && (line.charAt(0) == '\n' || line.charAt(0) == '\r')) {
                        line = line.substring(1);
                    }

                    /** Skip level lines if level is not null */
                    if (level != null) {
                        String levelStr = level.getLevel();
                        if (line.indexOf(levelStr) == -1) {
                            continue;
                        }
                    }

                    /** Avoid non-log event lines */
                    String fullDateStr = "";
                    try {
                        StringTokenizer st = new StringTokenizer(line, " ");
                        /** Assuming that date are always the first two words */
                        String dateStr = st.nextToken();
                        String timeStr = st.nextToken();
                        fullDateStr = dateStr + " " + timeStr;
                        /** If this is not true, skip this line */
                        SimpleDateFormat sdf = new SimpleDateFormat(datePattern);
                        Date logDate = sdf.parse(fullDateStr);

                        /** Check dates. If log date is after end date, return dataset */
                        if (logDate.after(endDate)) {
                            return dataset;
                        }
                        /** If date is before initDate, continue till initial date */
                        else if (logDate.before(initDate)) {
                            continue;
                        }

                    } catch (Exception e) {
                        /** Skip problematic parsing */
                        continue;
                    }

                    /** Is log level available? */
                    String logLevel = getLevel(line).getLevel();
                    if (!isLogLevelAvailable(logLevel)) {
                        continue;
                    }

                    /** Avoid NULL or not available events */
                    Map<String, String> appEvent = getAppEvent(line);
                    if (appEvent == null) {
                        continue;
                    }

                    /** Check keyword, if exists */
                    if (keyword != null && !"".equals(keyword)) {
                        if (line.toLowerCase().indexOf(keyword) == -1) {
                            continue;
                        }
                    }

                    /** Increment counter first */
                    counter++;

                    /** Check that log line is within start and end indeces */
                    if (start != -1 && counter < start) {
                        continue;
                    }
                    /** Do not add any more events */
                    else if (end != -1 && counter > end) {
                        break;
                    }

                    Map<String, String> logLine = new HashMap<String, String>();
                    logLine.put("date", fullDateStr);
                    logLine.put("level", logLevel);
                    logLine.put("text", line);

                    /** Add data to data set */
                    logLevels.add(logLevel);
                    addAppEvent(appEvent, appEvents);
                    dataset.add(logLine);
                }

                /** Finish adding events to the final dataset */
                if (counter >= end) {
                    break;
                }
            }
        } catch (Exception e) {
            throw e;
        }

        return dataset;
    }

    /**
     * Export the dataset to a CSV format.
     * 
     * @param dataset
     * @param chartDetails
     * @return
     */
    public String getEventsCSV(List<Map<String, String>> dataset) {
        StringBuilder sb = new StringBuilder();

        /** Headers */
        sb.append("Date, Level, Text\n");

        /** Add table rows */
        for (Map<String, String> event : dataset) {
            /* Table row */
            String row = event.get("date") + "," + event.get("level") + "," + event.get("text");

            /* Add row */
            sb.append(row + "\n");
        }

        return sb.toString();
    }

    /**
     * Export the dataset to a HTML table.
     * 
     * @param dataset
     * @param chartDetails
     * @return
     */
    public String getEventsHTML(List<Map<String, String>> dataset) {
        StringBuilder sb = new StringBuilder();

        /** Headers */
        sb.append("<table border='1' width='100%'>");
        sb.append("<tr><th>Date</th><th>Level</th><th>Text</th></tr>");
        sb.append("<tbody>");

        /** Add table rows */
        for (Map<String, String> event : dataset) {
            /* Table row */
            String row = "<tr><td>" + event.get("date") + "</td><td>" + event.get("level") + "</td><td>"
                    + event.get("text") + "</td></tr>";

            /* Add row */
            sb.append(row);
        }

        sb.append("</tbody>");
        sb.append("</table>");

        return sb.toString();
    }

    /**
     * Return logLevels attribute.
     * 
     * @return logLevels - Attribute returned
     */
    @Override
    public final Set<String> getLogLevels() {
        return logLevels;
    }

    /**
     * Set attribute logLevels.
     * 
     * @param logLevelsArg - Set value
     */
    public final void setLogLevels(Set<String> logLevelsArg) {
        logLevels = logLevelsArg;
    }

    /**
     * Return appEvents attribute.
     * 
     * @return appEvents - Attribute returned
     */
    @Override
    public final Map<String, String> getAppEvents() {
        return appEvents;
    }

    /**
     * Set attribute appEvents.
     * 
     * @param appEventsArg - Set value
     */
    public final void setAppEvents(Map<String, String> appEventsArg) {
        appEvents = appEventsArg;
    }

    /**
     * Return keyword attribute.
     * 
     * @return keyword - Attribute returned
     */
    public final String getKeyword() {
        return keyword;
    }

    /**
     * Set attribute keyword.
     * 
     * @param keywordArg - Set value
     */
    public final void setKeyword(String keywordArg) {
        if (keywordArg != null) {
            keyword = keywordArg.trim().toLowerCase();
        }
    }
}
