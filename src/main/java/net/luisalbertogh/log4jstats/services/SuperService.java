package net.luisalbertogh.log4jstats.services;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.io.StringReader;
import java.net.URLConnection;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Comparator;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.StringTokenizer;
import java.util.TreeMap;

import net.luisalbertogh.log4jstats.utils.HTTPClient;
import net.luisalbertogh.log4jstats.utils.LogLevels;

import org.jdom.Document;
import org.jdom.Element;
import org.jdom.input.SAXBuilder;

/**
 * This abstract class contains the common code for all the services.
 * 
 * @author lagarcia
 */
public abstract class SuperService {

    /** Log levels */
    protected List<LogLevels> skipLogLevels;

    /** Application events for the table */
    protected Map<String, String> appEventList;

    /** A list of invalid words to remove from log lines */
    protected List<String> invalidWords;

    /** Init date for the data to process */
    protected Date initDate;
    protected Calendar initCal;
    /** End date for the data to process */
    protected Date endDate;
    protected Calendar endCal;
    /** Maximum number of dates */
    protected int maxNumberOfDates;

    /**
     * Default constructor
     */
    public SuperService() {
    }

    /**
     * Date constructor
     * 
     * @param defaultNumberDates
     */
    public SuperService(int defaultNumberDates) {
        Calendar now = GregorianCalendar.getInstance();
        now.add(Calendar.DAY_OF_MONTH, 1);
        /** So events of today are visible */
        this.endDate = now.getTime();
        now.add(Calendar.DAY_OF_MONTH, -defaultNumberDates);
        this.initDate = now.getTime();
    }

    /**
     * Init all available log levels.
     */
    public void initLogLevels() {
        this.skipLogLevels = new ArrayList<LogLevels>();
        this.skipLogLevels.add(LogLevels.FATAL);
        this.skipLogLevels.add(LogLevels.ERROR);
        this.skipLogLevels.add(LogLevels.DEBUG);
        this.skipLogLevels.add(LogLevels.INFO);
        this.skipLogLevels.add(LogLevels.WARN);
        this.skipLogLevels.add(LogLevels.TRACE);
    }

    /**
     * Init all available application events.
     * 
     * @param appEvents - Comma-separated application events.
     */
    public void initAppEvents(String appEvents) {
        this.appEventList = new TreeMap<String, String>();
        StringTokenizer st = new StringTokenizer(appEvents, ",");
        while (st.hasMoreTokens()) {
            String appEvent = st.nextToken();
            String[] parts = appEvent.split(";");
            String name = parts[0];
            String value = parts[1];
            this.appEventList.put(name, value);
        }
    }

    /**
     * Remove the passed event name from the available application events.
     * 
     * @param value
     */
    public void removeAppEvent(String value) {
        String keyToRemove = "";
        Set<String> names = this.appEventList.keySet();
        for (String name : names) {
            String curVal = this.appEventList.get(name);
            if (curVal.equalsIgnoreCase(value) || name.equals(value)) {
                keyToRemove = name;
                break;
            }
        }

        this.appEventList.remove(keyToRemove);
    }

    /**
     * Set the init and end date for the date interval
     * 
     * @param initDate
     * @param endDate
     * @return True if new dates were set, otherwise false
     */
    @SuppressWarnings("static-access")
    public boolean setDatesInterval(Date initDate, Date endDate) {
        /** Add maxNumberOfDates to init date */
        Calendar newDate = GregorianCalendar.getInstance();
        newDate.setTimeInMillis(initDate.getTime());
        newDate.add(Calendar.DAY_OF_MONTH, this.maxNumberOfDates);

        Date d = newDate.getTime();

        /** If resulting date is after end date, then is correct, otherwise there are too many dates */
        if (d.after(endDate)) {
            this.initDate = initDate;
            this.initCal = Calendar.getInstance();
            this.initCal.setTime(initDate);
            this.endDate = endDate;
            this.endCal = Calendar.getInstance();
            this.endCal.setTime(endDate);
            return true;
        }

        return false;
    }

    /**
     * Get log level for current line.
     * 
     * @param line
     * @return Log level
     */
    public LogLevels getLevel(String line) {
        if (line.indexOf(LogLevels.FATAL.getLevel()) != -1) {
            return LogLevels.FATAL;
        }
        if (line.indexOf(LogLevels.ERROR.getLevel()) != -1) {
            return LogLevels.ERROR;
        }
        if (line.indexOf(LogLevels.DEBUG.getLevel()) != -1) {
            return LogLevels.DEBUG;
        }
        if (line.indexOf(LogLevels.INFO.getLevel()) != -1) {
            return LogLevels.INFO;
        }
        if (line.indexOf(LogLevels.WARN.getLevel()) != -1) {
            return LogLevels.WARN;
        }
        if (line.indexOf(LogLevels.TRACE.getLevel()) != -1) {
            return LogLevels.TRACE;
        }

        /* Default */
        return LogLevels.INFO;
    }

    /**
     * Get log files from log dir and sort them in date descending order.
     * 
     * @param logsDir
     * @return File[]
     * @throws Exception
     */
    @SuppressWarnings({"unchecked", "rawtypes" })
    protected File[] initLogFiles(String logsDir) throws Exception {
        File dir = new File(logsDir);

        if (!dir.isDirectory()) {
            return null;
        }
        File files[] = dir.listFiles();
        Arrays.sort(files, new Comparator() {
            @Override
            public int compare(final Object o1, final Object o2) {
                return new Long(((File) o2).lastModified()).compareTo(new Long(((File) o1).lastModified()));
            }
        });

        return files;
    }

    /**
     * Get log files from the server via HTTP.
     * 
     * @param url - The main URL
     * @return
     * @throws Exception
     */
    protected List<String[]> getLogFilesByHTTP(String url, String baseName) throws Exception {
        List<String[]> logFiles = new ArrayList<String[]>();
        Map<String, String> params = new HashMap<String, String>();
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
        while (initCal.before(endCal)) {
            params.put("logFile", baseName + sdf.format(initCal.getTime()));
            URLConnection connection = HTTPClient.createHTTPSconnection(url, params);
            /* Reading response */
            StringBuilder sb = new StringBuilder();
            BufferedReader in = new BufferedReader(new InputStreamReader(connection.getInputStream()));
            String responseString;
            /* Do not keep first line */
            boolean saveLine = false;
            while ((responseString = in.readLine()) != null) {
                /* Replace break of line with full tag */
                if (responseString.indexOf("<br/>") != -1) {
                    responseString = responseString.replaceAll("<br/>", "EOL");
                }

                /* Save only the body */
                if (responseString.indexOf("body") != -1) {
                    if (!saveLine) {
                        saveLine = true;
                    } else {
                        sb.append(responseString);
                        break;
                    }
                }
                if (saveLine) {
                    sb.append(responseString);
                }
            }
            in.close();

            /* Parsing response */
            SAXBuilder builder = new SAXBuilder();
            builder.setValidation(false);
            Document htmlDoc = builder.build(new StringReader(sb.toString()));
            Element root = htmlDoc.getRootElement();
            Element table = root.getChild("table");
            List<Element> rows = table.getChildren("tr");
            Element content = rows.get(2);
            String logs = content.getChild("td").getText();
            String[] logsArray = logs.split("EOL");
            logFiles.add(logsArray);
            /* Next day */
            initCal.add(Calendar.DATE, 1);
        }

        return logFiles;
    }

    /**
     * Is the passed log level available?
     * 
     * @param level
     * @return
     */
    public boolean isLogLevelAvailable(String level) {
        List<LogLevels> availableLevels = getSkipLogLevels();
        for (LogLevels logLevel : availableLevels) {
            if (logLevel.getLevel().equals(level)) {
                return true;
            }
        }

        return false;
    }

    /**
     * Get the application event code within the passed log line.
     * 
     * @param logLine
     * @return
     */
    public Map<String, String> getAppEvent(String logLine) {
        Map<String, String> appEvent = new HashMap<String, String>();
        String logLineTmp = logLine.toLowerCase();
        Set<String> names = appEventList.keySet();
        for (String name : names) {
            String value = appEventList.get(name);
            if (logLineTmp.indexOf(value.toLowerCase()) != -1) {
                appEvent.put("name", name);
                appEvent.put("value", value);
                return appEvent;
            }
        }

        return null;
    }

    /**
     * Add a new application event to the given event map.
     * 
     * @param newEvent
     * @param eventsMap
     */
    public void addAppEvent(Map<String, String> newEvent, Map<String, String> eventsMap) {
        if (!eventsMap.containsKey(newEvent.get("name"))) {
            eventsMap.put(newEvent.get("name"), newEvent.get("value"));
        }
    }

    /**
     * Return skipLogLevels attribute.
     * 
     * @return skipLogLevels - Attribute returned
     */
    public final List<LogLevels> getSkipLogLevels() {
        return skipLogLevels;
    }

    /**
     * Get available log levels as string descriptions.
     * 
     * @return
     */
    public List<String> getSkipLogLevelsAsStrings() {
        List<String> levelsStr = new ArrayList<String>();
        for (LogLevels level : this.skipLogLevels) {
            levelsStr.add(level.getLevel());
        }

        return levelsStr;
    }

    /**
     * Get available log levels as string descriptions.
     * 
     * @return
     */
    public String getSkipLogLevelsCommaSeparated() {
        String ret = "";
        for (LogLevels level : this.skipLogLevels) {
            if (ret.length() != 0) {
                ret += ",";
            }
            ret += "'" + level.getLevel() + "'";
        }

        return ret;
    }

    /**
     * Set attribute skipLogLevels.
     * 
     * @param skipLogLevelsArg - Set value
     */
    public final void setSkipLogLevels(LogLevels skipLogLevelsArg) {
        this.skipLogLevels.remove(skipLogLevelsArg);
    }

    /**
     * Return appEventList attribute.
     * 
     * @return appEventList - Attribute returned
     */
    public final Map<String, String> getAppEventList() {
        return appEventList;
    }

    /**
     * Return appEvents attribute.
     * 
     * @return appEvents - Attribute returned
     */
    public Map<String, String> getAppEvents() {
        return null;
    }

    /**
     * Return logLevels attribute.
     * 
     * @return logLevels - Attribute returned
     */
    public Set<String> getLogLevels() {
        return null;
    }

    /**
     * Return the current available application events as string.
     * 
     * @return
     */
    public final String getAppEventsListAsString() {
        String events = "";
        for (String key : appEventList.keySet()) {
            if (events.equals("")) {
                events += "'" + key + "'";
            } else {
                events += ",'" + key + "'";
            }
        }

        return events;
    }
}
