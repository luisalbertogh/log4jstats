package net.luisalbertogh.log4jstats.services.logfiles;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.text.SimpleDateFormat;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.StringTokenizer;
import java.util.TreeMap;
import java.util.TreeSet;

import net.luisalbertogh.log4jstats.interfaces.StatisticsInterface;
import net.luisalbertogh.log4jstats.services.SuperService;
import net.luisalbertogh.log4jstats.utils.LogLevels;

/**
 * Implement the main statistics services to load the main data sets.
 * 
 * @author lagarcia
 */
public class StatisticsService extends SuperService implements StatisticsInterface {

    /** Current app. events */
    private Map<String, String> appEvents;

    /** Current log levels */
    private Set<String> logLevels;

    /**
     * Default constructor
     * 
     * @param defaultNumberDates - Default number of dates between init and end dates
     * @param maxNumberOfDates - Maximum number of dates to process
     */
    @SuppressWarnings("static-access")
    public StatisticsService(int defaultNumberDates, int maxNumberOfDates) {
        super(defaultNumberDates);
        this.maxNumberOfDates = maxNumberOfDates;
        initLogLevels();
    }

    /**
     * Constructor
     * 
     * @param initDate
     * @param endDate
     */
    public StatisticsService(Date initDate, Date endDate) {
        super();
        this.initDate = initDate;
        this.endDate = endDate;
        initLogLevels();
    }

    /**
     * Get the relation between the number of log entries and the date they were raised. It is assumed that the date is
     * always the first token in each line, and that the date is composed of the date and the time, separated by a blank
     * space. Read all the log files contained in the log folder.
     * 
     * @param datePattern
     * @param logsDir
     * @param level - Log4j level to filter in
     * @return Map<String,Integer> - Relation between data as text and number of events.
     * @throws Exception
     */
    @Override
    @SuppressWarnings("unchecked")
    public Map<String, Integer> getNEventsPerDate(String datePattern, String logsDir, LogLevels level) throws Exception {

        Map<String, Integer> dataset = new TreeMap<String, Integer>(new DateComparator(datePattern));

        try {
            /** Log files in date descending order */
            File[] logFiles = initLogFiles(logsDir);

            /** Iterate over the log files */
            for (File logFile : logFiles) {
                /** Read log file */
                BufferedReader bf = new BufferedReader(new FileReader(logFile));
                String line = "";
                while ((line = bf.readLine()) != null) {
                    /** Skip level lines if level is not null */
                    if (level != null) {
                        String levelStr = level.getLevel();
                        if (line.indexOf(levelStr) == -1) {
                            continue;
                        }
                    }

                    String dateStr, timeStr;
                    try {
                        StringTokenizer st = new StringTokenizer(line, " ");
                        /** Assuming that date are always the first two words */
                        dateStr = st.nextToken();
                        timeStr = st.nextToken();
                        /** If this is not true, skip this line */
                        SimpleDateFormat sdf = new SimpleDateFormat(datePattern);
                        Date logDate = sdf.parse(dateStr + " " + timeStr);

                        /** Check dates. If log date is before init date, cotinue to next log line */
                        if (logDate.after(endDate)) {
                            continue;
                        }
                        /** If date is after endDate, finish processing data */
                        else if (logDate.before(initDate)) {
                            if (bf != null) {
                                bf.close();
                            }
                            return dataset;
                        }
                    } catch (Exception e) {
                        /** Skip problematic parsing */
                        continue;
                    }

                    /** Insert or update in dataset */
                    if (dataset.containsKey(dateStr)) {
                        /** Increment count */
                        dataset.put(dateStr, dataset.get(dateStr) + 1);
                    } else {
                        /** Insert for first time */
                        dataset.put(dateStr, new Integer(1));
                    }
                }
                bf.close();
            }
        } catch (Exception e) {
            throw new Exception(e.getMessage());
        }

        return dataset;
    }

    /**
     * Get number of different events per date.
     * 
     * @param datePattern
     * @param logsDir
     * @return Map of different events with map of data/n events.
     * @throws Exception
     */
    @Override
    public Map<String, Map<String, Integer>> getNDiffEventsPerDate(String datePattern, String logsDir) throws Exception {
        Map<String, Map<String, Integer>> collection = new HashMap<String, Map<String, Integer>>();

        try {
            /** Load available levels */
            List<LogLevels> availableLevels = getSkipLogLevels();
            for (LogLevels level : availableLevels) {
                Map<String, Integer> logs = getNEventsPerDate(datePattern, logsDir, level);
                if (logs.size() != 0) {
                    collection.put(level.getLevel(), logs);
                }
            }
        } catch (Exception e) {
            throw new Exception(e.getMessage());
        }

        return collection;
    }

    /**
     * Get the total number of events for the different log levels.
     * 
     * @param dataset - Data with log events grouped by date and by log level.
     * @return
     * @throws Exception
     */
    @Override
    public Map<String, Integer> getTotalEventNumbers(Map<String, Map<String, Integer>> dataset) throws Exception {
        Map<String, Integer> output = new HashMap<String, Integer>();

        try {
            Set<String> levels = dataset.keySet();
            for (String level : levels) {
                int nEvents = 0;
                Set<String> dates = dataset.get(level).keySet();
                for (String date : dates) {
                    int events = dataset.get(level).get(date);
                    nEvents += events;
                }

                output.put(level, nEvents);
            }
        } catch (Exception e) {
            throw new Exception(e.getMessage());
        }

        return output;
    }

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
    @Override
    @SuppressWarnings("unchecked")
    public Map<String, Object> getAppEventsTableData(String datePattern, String logsDir, LogLevels level,
            String appEventFilter) throws Exception {

        /** Wrap dataset and data info */
        Map<String, Object> dataBundle = new HashMap<String, Object>();

        /** Dataset with events and counters - Use LinkedHashMap to sort the values by entering order */
        Map<String, Object> eventCounter = new LinkedHashMap<String, Object>();

        try {
            /** Log files in date descending order */
            File[] logFiles = initLogFiles(logsDir);

            /** Available log levels */
            Set<String> logLevels = new TreeSet<String>();

            /** Available app. events */
            Map<String, String> appEvents = new TreeMap<String, String>();

            /** Iterate over the log files */
            for (File logFile : logFiles) {
                /** Read log file */
                BufferedReader bf = new BufferedReader(new FileReader(logFile));
                String line = "";
                while ((line = bf.readLine()) != null) {
                    /** Skip level lines if level is not null */
                    if (level != null) {
                        String levelStr = level.getLevel();
                        if (line.indexOf(levelStr) == -1) {
                            continue;
                        }
                    }

                    /** Skip log line if app. event filter is not null */
                    if (appEventFilter != null) {
                        if (line.toLowerCase().indexOf(appEventFilter) == -1) {
                            continue;
                        }
                    }

                    String dateStr, timeStr;
                    try {
                        StringTokenizer st = new StringTokenizer(line, " ");
                        /** Assuming that date are always the first two words */
                        dateStr = st.nextToken();
                        timeStr = st.nextToken();
                        /** If this is not true, skip this line */
                        SimpleDateFormat sdf = new SimpleDateFormat(datePattern);
                        Date logDate = sdf.parse(dateStr + " " + timeStr);

                        /** Check dates. If log date is before init date, cotinue to next log line */
                        if (logDate.after(endDate)) {
                            continue;
                        }
                        /** If date is after endDate, finish processing data */
                        else if (logDate.before(initDate)) {
                            if (bf != null) {
                                bf.close();
                            }
                            dataBundle.put("dataset", eventCounter);
                            dataBundle.put("logLevels", logLevels);
                            dataBundle.put("appEvents", appEvents);
                            this.appEvents = appEvents;
                            this.logLevels = logLevels;
                            return dataBundle;
                        }
                    } catch (Exception e) {
                        /** Skip problematic parsing */
                        continue;
                    }

                    Map<String, Object> logLine = new HashMap<String, Object>();
                    logLine.put("date", dateStr);
                    /** Is log level available? */
                    String logLevel = getLevel(line).getLevel();
                    if (!isLogLevelAvailable(logLevel)) {
                        continue;
                    }
                    logLevels.add(logLevel);
                    logLine.put("log", logLevel);
                    Map<String, String> appEvent = getAppEvent(line);
                    /** Avoid NULL or not available events */
                    if (appEvent == null) {
                        continue;
                    }
                    addAppEvent(appEvent, appEvents);
                    logLine.put("eventName", appEvent.get("name"));
                    /** Counter */
                    logLine.put("counter", new Integer(1));

                    /** Event ID */
                    String id = dateStr + "_" + logLevel + "_" + appEvent;

                    /** Add to event counter */
                    if (eventCounter.containsKey(id)) {
                        Map<String, Object> logEvent = (Map<String, Object>) eventCounter.get(id);
                        logEvent.put("counter", (Integer) logEvent.get("counter") + new Integer(1));
                    } else {
                        eventCounter.put(id, logLine);
                    }
                }
                bf.close();
            }

            dataBundle.put("logLevels", logLevels);
            dataBundle.put("appEvents", appEvents);
            dataBundle.put("dataset", eventCounter);
            this.appEvents = appEvents;
            this.logLevels = logLevels;
        } catch (Exception e) {
            throw new Exception(e.getMessage());
        }

        return dataBundle;
    }

    /**
     * This class implements a compartor for dates.
     * 
     * @author lagarcia
     */
    class DateComparator implements Comparator {

        /** Date pattern */
        private String datePattern;
        /** Ordering, default is descendant */
        private String order = "desc";

        public DateComparator(String datePattern) {
            this.datePattern = datePattern.split(" ")[0];
        }

        public DateComparator(String datePattern, String order) {
            this.datePattern = datePattern.split(" ")[0];
            this.order = order;
        }

        @Override
        public int compare(Object id1, Object id2) {
            try {
                SimpleDateFormat sdf = new SimpleDateFormat(datePattern);

                Date o1 = sdf.parse((String) id1);
                Date o2 = sdf.parse((String) id2);

                /** Order in ascendant order */
                if (order.equals("asc")) {
                    if (o1.before(o2)) {
                        return -1;
                    } else if (o1.after(o2)) {
                        return 1;
                    }
                } else {
                    if (o1.before(o2)) {
                        return 1;
                    } else if (o1.after(o2)) {
                        return -1;
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
            return 0;
        }
    }

    /**
     * Set attribute appEventList.
     * 
     * @param appEventListArg - Set value
     */
    public final void setAppEventList(Map<String, String> appEventListArg) {
        appEventList = appEventListArg;
    }

    /**
     * Return initDate attribute.
     * 
     * @return initDate - Attribute returned
     */
    public final Date getInitDate() {
        return initDate;
    }

    /**
     * Return endDate attribute.
     * 
     * @return endDate - Attribute returned
     */
    public final Date getEndDate() {
        return endDate;
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
     * Return logLevels attribute.
     * 
     * @return logLevels - Attribute returned
     */
    @Override
    public final Set<String> getLogLevels() {
        return logLevels;
    }

}
