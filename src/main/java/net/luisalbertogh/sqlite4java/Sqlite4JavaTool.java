/**
 * © 2009-2012 Tex Toll Services, LLC
 */
package net.luisalbertogh.sqlite4java;

import java.io.File;
import java.io.PrintStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.StringTokenizer;
import java.util.TreeMap;

import javax.swing.JTextArea;

import net.luisalbertogh.log4jstats.services.logfiles.TableService;
import net.luisalbertogh.log4jstats.utils.PropertiesLoader;
import net.luisalbertogh.log4jstats.utils.StatisticsUtils;

import com.almworks.sqlite4java.SQLiteConnection;
import com.almworks.sqlite4java.SQLiteException;
import com.almworks.sqlite4java.SQLiteStatement;

/**
 * This class implements some utilities to operate on the SQLite Log4J events database.
 * 
 * @author lagarcia
 */
public class Sqlite4JavaTool {

    /** The app properties */
    private PropertiesLoader properties = new PropertiesLoader();

    /** The chart properties */
    private PropertiesLoader chartProps = new PropertiesLoader();

    /** Application events for the table */
    private Map<String, String> appEventList;

    /** Application events for the table */
    private Map<String, String> appEventList4Charts;

    /** Create table statement. */
    private static final String CREATE_TABLE = "CREATE TABLE logevents(id INTEGER PRIMARY KEY, date DATE NOT NULL, "
            + "datetime TIMESTAMP NOT NULL, level VARCHAR(10) NOT NULL, event VARCHAR(100), log VARCHAR(250), "
            + "source VARCHAR(50), chart CHAR(1))";

    /** Create table for text search. */
    private static final String CREATE_VIRTUAL_TABLE = "CREATE VIRTUAL TABLE logtext using fts4(text)";

    /** Insert new log event. */
    private static final String INSERT_LOG_EVENTS = "INSERT INTO logevents(DATE,DATETIME,LEVEL,EVENT,LOG,SOURCE,CHART) VALUES(?,?,?,?,?,?,?)";

    /** Insert log text into text search table. */
    private static final String INSERT_TEXT = "INSERT INTO logtext(TEXT) VALUES(?)";

    /** Select log events. */
    private static final String SELECT_LOG_EVENTS = "SELECT id FROM logevents WHERE datetime=?";

    /** Maximum number of dates to process. */
    private static final int MAX_NUMBER_DATES = 366;

    /** Do not return more than these events from the service. */
    private static final int MAX_NUMBER_EVENTS = 500;

    /** Input data format message */
    private static final String DATE_FORMAT = "dd-MM-yyyy";

    /** Row counter */
    private long insertedRows = 0;

    /** Output for GUI */
    private JTextArea output;

    /**
     * Cancel process execution.
     */
    private boolean cancel = false;

    /**
     * Select the time division for the DB dumping format.
     * 
     * @author lagarcia
     */
    public static enum TimeDivision {
        /**
         * MONTHLY.
         */
        MONTH("MONTH"), /**
         * WEEKLY.
         */
        WEEK("WEEK");

        private String name;

        TimeDivision(String name) {
            this.name = name;
        }

        /**
         * Return name of time division.
         * 
         * @return
         */
        public String getName() {
            return this.name;
        }
    }

    /**
     * Main.
     * 
     * @param args
     */
    public static void main(String args[]) {

        try {
            if (args.length < 2) {
                System.err.println("Usage: java Sqlite4JavaTool <Init_Date> <End_Date> <Time_Division>");
                System.err.println("\tDate format: " + DATE_FORMAT);
                System.err.println("\tTime division: MONTH (default) | WEEK ");
                System.exit(0);
            }

            /** Date formatter */
            SimpleDateFormat sdf = new SimpleDateFormat(DATE_FORMAT);

            /* From */
            String fromStr = args[0];
            System.out.println("From: " + fromStr);
            Date from = sdf.parse(fromStr);
            /* To */
            String toStr = args[1];
            System.out.println("To: " + toStr);
            Date to = sdf.parse(toStr);

            /* Dump to SQLite */
            TimeDivision division = TimeDivision.MONTH;
            if (args.length >= 3) {
                if ("WEEK".equals(args[2])) {
                    division = TimeDivision.WEEK;
                }
            }
            System.out.println("Time division: " + division.getName());

            new Sqlite4JavaTool().dumpFilesToDB(from, to, division, null, null);
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    /**
     * Start the dumping of the log files to the SQLite DB.
     * 
     * @param from
     * @param to
     * @param division
     * @param logsPath
     * @param dbPath
     */
    public void dumpFilesToDB(Date from, Date to, TimeDivision division, String logsPath, String dbPath) {
        try {
            /** Load properties */
            properties.load("/labels.properties");
            /** Load chart properties */
            chartProps.load("/charts.properties");
            printMessage(System.out, properties.getProperty("propsloaded"));

            /** Init events list */
            initAppEvents(chartProps.getProperty("appeventstable"));
            initAppEvents4Charts(chartProps.getProperty("appevents"));

            /** Init files paths */
            if (logsPath == null) {
                logsPath = properties.getProperty("log4jdir");
            }
            if (dbPath == null) {
                dbPath = properties.getProperty("sqlitebasepath") + System.getProperty("file.separator")
                        + properties.getProperty("sqlitebasename");
            }

            /** Get log4j date pattern */
            String conversionPattern = StatisticsUtils.getConversionPattern(properties.getProperty("log4jfilepath"),
                    properties.getProperty("log4jlogger"), properties.getProperty("log4jappender"));
            String datetimePattern = StatisticsUtils.getDatePattern(conversionPattern);

            /** Date formatters */
            SimpleDateFormat sdf = new SimpleDateFormat(datetimePattern);

            /** Init table service */
            TableService ts = new TableService(Integer.parseInt(properties.getProperty("defaultintervaltime")),
                    MAX_NUMBER_DATES);
            /** Init log levels */
            ts.initLogLevels();
            /** Init app. events list */
            ts.initAppEvents(chartProps.getProperty("appeventstable"));

            /* Init the latest date to the oldest date */
            Date prevNow = null;
            Date now = to;
            Calendar dateCal = Calendar.getInstance();
            String prevFilepath = "";
            SQLiteConnection db = null;
            do {
                /* The previous date is the same than the current date, exit the loop */
                if (prevNow != null && now.equals(prevNow)) {
                    break;
                }

                /* Extract events from log files with max. number of events */
                ts.setDatesInterval(from, now);
                List<Map<String, String>> tableDataset = ts.getEventsData(datetimePattern, logsPath, null, 0,
                        MAX_NUMBER_EVENTS);

                /* Only the last record was found, exit the loop */
                if (tableDataset.size() < 2) {
                    break;
                }

                /* Log message */
                printMessage(System.out, properties.getProperty("insertingrows"));

                /* Iterate through the list */
                for (Map<String, String> rowData : tableDataset) {
                    /* Cancel process */
                    if (cancel) {
                        break;
                    }

                    /* Event date */
                    Date date = sdf.parse(rowData.get("date"));
                    dateCal.setTime(date);

                    /* Datebase file */
                    String filepath = dbPath + "__";
                    switch (division) {
                        case MONTH: {
                            filepath += dateCal.get(Calendar.MONTH);
                            break;
                        }
                        case WEEK: {
                            filepath += dateCal.get(Calendar.WEEK_OF_YEAR);
                            break;
                        }
                    }
                    filepath += "_" + dateCal.get(Calendar.YEAR);

                    boolean createTable = false;
                    /* New DB file */
                    if (!filepath.equalsIgnoreCase(prevFilepath)) {
                        if (db != null) {
                            db.dispose();
                        }
                        /* Get DB connection */
                        db = openDBConnection(filepath);
                        /* Check if it is needed to create the data table first if DB file does not exist */
                        File dbFile = new File(filepath);
                        if (!dbFile.exists() || !areTablesInDB(db, filepath)) {
                            createTable = true;
                        }
                        /* Previous file path */
                        prevFilepath = filepath;
                    }

                    /* Insert the date */
                    insertData(db, rowData, filepath, createTable);
                }

                /* Next date */
                prevNow = now;
                now = sdf.parse(tableDataset.get(tableDataset.size() - 1).get("date"));
            } while (from.before(now) && !cancel);

            /** Close any possible open db connection */
            if (db != null) {
                db.dispose();
            }

            printMessage(System.out, insertedRows + " " + properties.getProperty("rowsinserted"));
            printMessage(System.out, properties.getProperty("processfinished"));
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    /**
     * Start the dumping of the log files to the SQLite DB.
     * 
     * @param from
     * @param to
     * @param division
     * @param dbPath
     */
    public void dumpFilesToDBbyHTTP(Date from, Date to, TimeDivision division, String dbPath) {
        try {
            /** Load properties */
            properties.load("/labels.properties");
            /** Load chart properties */
            chartProps.load("/charts.properties");
            printMessage(System.out, properties.getProperty("propsloaded"));

            /** Init events list */
            initAppEvents(chartProps.getProperty("appeventstable"));
            initAppEvents4Charts(chartProps.getProperty("appevents"));

            /** Init files paths */
            if (dbPath == null) {
                dbPath = properties.getProperty("sqlitebasepath") + System.getProperty("file.separator")
                        + properties.getProperty("sqlitebasename");
            }

            /** Get log4j date pattern */
            String conversionPattern = StatisticsUtils.getConversionPattern(properties.getProperty("log4jfilepath"),
                    properties.getProperty("log4jlogger"), properties.getProperty("log4jappender"));
            String datetimePattern = StatisticsUtils.getDatePattern(conversionPattern);

            /** Date formatters */
            SimpleDateFormat sdf = new SimpleDateFormat(datetimePattern);

            /** Init table service */
            TableService ts = new TableService(Integer.parseInt(properties.getProperty("defaultintervaltime")),
                    MAX_NUMBER_DATES);
            /** Init log levels */
            ts.initLogLevels();
            /** Init app. events list */
            ts.initAppEvents(chartProps.getProperty("appeventstable"));

            /* Init the lowest date limit (now) to the oldest date (Date is in ascending order) */
            Date prevNow = null;
            Date now = from;
            Calendar dateCal = Calendar.getInstance();
            String prevFilepath = "";
            SQLiteConnection db = null;
            do {
                /* The previous date is the same than the current date, exit the loop */
                if (prevNow != null && now.equals(prevNow)) {
                    break;
                }

                /* Extract events from log files with max. number of events */
                ts.setDatesInterval(now, to);
                List<Map<String, String>> tableDataset = ts.getEventsDataByHTTP(properties.getProperty("urlservice"),
                        properties.getProperty("logfilesbasename"), datetimePattern, null, 0, MAX_NUMBER_EVENTS);

                /* Only the last record was found, exit the loop */
                if (tableDataset.size() < 2) {
                    break;
                }

                /* Log message */
                printMessage(System.out, properties.getProperty("insertingrows"));

                /* Iterate through the list */
                for (Map<String, String> rowData : tableDataset) {
                    /* Cancel process */
                    if (cancel) {
                        break;
                    }

                    /* Event date */
                    Date date = sdf.parse(rowData.get("date"));
                    dateCal.setTime(date);

                    /* Datebase file */
                    String filepath = dbPath + "__";
                    switch (division) {
                        case MONTH: {
                            filepath += dateCal.get(Calendar.MONTH);
                            break;
                        }
                        case WEEK: {
                            filepath += dateCal.get(Calendar.WEEK_OF_YEAR);
                            break;
                        }
                    }
                    filepath += "_" + dateCal.get(Calendar.YEAR);

                    boolean createTable = false;
                    /* New DB file */
                    if (!filepath.equalsIgnoreCase(prevFilepath)) {
                        if (db != null) {
                            db.dispose();
                        }
                        /* Get DB connection */
                        db = openDBConnection(filepath);
                        /* Check if it is needed to create the data table first if DB file does not exist */
                        File dbFile = new File(filepath);
                        if (!dbFile.exists() || !areTablesInDB(db, filepath)) {
                            createTable = true;
                        }
                        /* Previous file path */
                        prevFilepath = filepath;
                    }

                    /* Insert the date */
                    insertData(db, rowData, filepath, createTable);
                }

                /* Log message */
                printMessage(System.out, insertedRows + " " + properties.getProperty("rowsinserted"));

                /* Next date (Date is in ascending order) */
                prevNow = now;
                now = sdf.parse(tableDataset.get(tableDataset.size() - 1).get("date"));
            } while (to.after(now) && !cancel);

            /** Close any possible open db connection */
            if (db != null) {
                db.dispose();
            }

            printMessage(System.out, insertedRows + " " + properties.getProperty("rowsinserted"));
            printMessage(System.out, properties.getProperty("processfinished"));
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    /**
     * Insert or update a single log event data into the SQLite DB.
     * 
     * @param db
     * @param tableDataset
     * @param filepath
     * @throws SQLiteException
     */
    private void insertData(SQLiteConnection db, Map<String, String> dataRow, String filepath, boolean createTable) {
        try {
            /* Create log events table if needed */
            if (createTable) {
                createTables(db, filepath);
            }

            /* Insert data table row for log events */
            Map<Integer, String> params = new HashMap<Integer, String>();
            /* Date */
            String datetime = dataRow.get("date");
            params.put(1, datetime.split(" ")[0]);
            /* Timestamp */
            params.put(2, datetime);
            /* Log level */
            params.put(3, dataRow.get("level"));
            /* Log event */
            String logLine = dataRow.get("text");
            Map<String, String> appEvent = getAppEvent(logLine);
            params.put(4, appEvent.get("name"));
            /* Log line */
            params.put(5, logLine);
            /* Source */
            params.put(6, "");
            /* Chart */
            if (isAppEvent4Chart(logLine)) {
                params.put(7, "1");
            } else {
                params.put(6, "0");
            }

            /* Insert */
            SQLiteStatement st = executeStatement(db, INSERT_LOG_EVENTS, params);
            st.step();
            st.dispose();

            /* Insert log text into text search table */
            params = new HashMap<Integer, String>();
            params.put(1, dataRow.get("text"));
            st = executeStatement(db, INSERT_TEXT, params);
            st.step();
            st.dispose();

            /* Increment counter */
            insertedRows++;
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    /**
     * Create new tables when needed.
     * 
     * @param db
     * @param filepath
     * @throws SQLiteException
     */
    private void createTables(SQLiteConnection db, String filepath) throws SQLiteException {
        /* Init counter */
        printMessage(System.out, insertedRows + " " + properties.getProperty("rowsinserted"));
        insertedRows = 0;
        printMessage(System.out, "Inserting data in " + filepath);

        /* Create data table */
        SQLiteStatement st = db.prepare(CREATE_TABLE);
        st.step();
        st.dispose();

        /* Create text search table */
        st = db.prepare(CREATE_VIRTUAL_TABLE);
        st.step();
        st.dispose();
    }

    /**
     * Check if the tables are correctly created into the corresponding database.
     * 
     * @param db
     * @param filepath
     * @return
     */
    private boolean areTablesInDB(SQLiteConnection db, String filepath) {
        boolean ret = true;
        try {
            /* Select log events */
            SQLiteStatement st = executeStatement(db, SELECT_LOG_EVENTS, null);
            st.step();
            st.dispose();
        } catch (SQLiteException sqlex) {
            ret = false;
        }

        return ret;
    }

    /**
     * Init all available application events.
     * 
     * @param appEvents - Comma-separated application events.
     */
    private void initAppEvents(String appEvents) {
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
     * Init all available application events for chart events ONLY.
     * 
     * @param appEvents - Comma-separated application events.
     */
    private void initAppEvents4Charts(String appEvents) {
        this.appEventList4Charts = new TreeMap<String, String>();
        StringTokenizer st = new StringTokenizer(appEvents, ",");
        while (st.hasMoreTokens()) {
            String appEvent = st.nextToken();
            String[] parts = appEvent.split(";");
            String name = parts[0];
            String value = parts[1];
            this.appEventList4Charts.put(name, value);
        }
    }

    /**
     * Get the application event code within the passed log line.
     * 
     * @param logLine
     * @return
     */
    private Map<String, String> getAppEvent(String logLine) {
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
     * Check if application event is available for charting or not.
     * 
     * @param logLine
     * @return
     */
    private boolean isAppEvent4Chart(String logLine) {
        String logLineTmp = logLine.toLowerCase();
        Set<String> names = appEventList4Charts.keySet();
        for (String name : names) {
            String value = appEventList4Charts.get(name);
            if (logLineTmp.indexOf(value.toLowerCase()) != -1) {
                return true;
            }
        }

        return false;
    }

    /**
     * Create DB connection.
     * 
     * @param filepath
     * @return DB connection
     * @throws SQLiteException
     */
    public static SQLiteConnection openDBConnection(String filepath) throws SQLiteException {
        SQLiteConnection db = new SQLiteConnection(new File(filepath));
        db.open(true);
        return db;
    }

    /**
     * Perform SQL query.
     * 
     * @param db
     * @param sqlQuery
     * @param params
     * @return Executed statement with resultset
     * @throws SQLiteException
     */
    public static SQLiteStatement executeStatement(SQLiteConnection db, String sqlQuery, Map<Integer, String> params)
            throws SQLiteException {
        SQLiteStatement st = db.prepare(sqlQuery);
        if (params != null) {
            Set<Integer> keys = params.keySet();
            for (Integer key : keys) {
                st.bind(key.intValue(), params.get(key));
            }
        }

        return st;
    }

    /**
     * Print a message through the output stream.
     * 
     * @param output
     * @param msg
     */
    private void printMessage(PrintStream output, String msg) {
        if (this.output == null) {
            output.println(msg);
        } else {
            this.output.append(msg + "\n");
        }
    }

    /**
     * Get a string array of the possible time division values.
     * 
     * @return
     */
    public static String[] getTimeDivisions() {
        String[] retValues = new String[0];
        TimeDivision[] values = TimeDivision.values();
        List<String> arrayValues = new ArrayList<String>();
        for (int i = 0; i < values.length; i++) {
            String value = values[i].getName();
            arrayValues.add(value);
        }

        retValues = arrayValues.toArray(retValues);
        return retValues;
    }

    /**
     * Set output as text area.
     * 
     * @param textArea
     */
    public void setTextArea(JTextArea textArea) {
        this.output = textArea;
    }

    /**
     * Set cancel flag.
     * 
     * @param cancel
     */
    public void setCancel(boolean cancel) {
        this.cancel = cancel;
    }
}
