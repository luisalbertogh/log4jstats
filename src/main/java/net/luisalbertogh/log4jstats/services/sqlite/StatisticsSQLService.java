/**
 * © 2009-2012 Tex Toll Services, LLC
 */
package net.luisalbertogh.log4jstats.services.sqlite;

import java.text.SimpleDateFormat;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

import net.luisalbertogh.log4jstats.interfaces.StatisticsInterface;
import net.luisalbertogh.log4jstats.utils.LogLevels;
import net.luisalbertogh.sqlite4java.Sqlite4JavaTool;
import net.luisalbertogh.sqlite4java.Sqlite4JavaTool.TimeDivision;

import com.almworks.sqlite4java.SQLiteConnection;
import com.almworks.sqlite4java.SQLiteStatement;

/**
 * This class implements the Statistics service using the SQLite DB.
 * 
 * @author lagarcia
 */
public class StatisticsSQLService extends SuperSQLService implements StatisticsInterface {
    /** Select number of events per date. */
    private static final String SELECT_EVENTS_PER_DATE = "SELECT count(*) as num, date FROM logevents ";

    private static final String SELECT_APP_EVENTS = "SELECT count(*) as num, date, level, event FROM logevents ";

    /** Current app. events */
    private Map<String, String> appEvents;

    /** Current log levels */
    private Set<String> logLevels;

    /**
     * Default constructor
     * 
     * @param defaultNumberDates - Default number of dates between init and end dates
     * @param maxNumberOfDates - Maximum number of dates to process
     * @param timedivision - Time division (MONTH or WEEK)
     */
    public StatisticsSQLService(int defaultNumberDates, int maxNumberOfDates, String timedivision) {
        super(defaultNumberDates);
        this.maxNumberOfDates = maxNumberOfDates;
        if (TimeDivision.MONTH.getName().equals(timedivision)) {
            this.timedivision = TimeDivision.MONTH;
        } else {
            this.timedivision = TimeDivision.WEEK;
        }
        initLogLevels();
    }

    /**
     * @see net.luisalbertogh.log4jstats.interfaces.StatisticsInterface#getNEventsPerDate(java.lang.String,
     *      java.lang.String, net.luisalbertogh.log4jstats.utils.LogLevels)
     */
    @Override
    public Map<String, Integer> getNEventsPerDate(String datePatternArg, String dbDirArg, LogLevels levelArg)
            throws Exception {
        return getNEventsPerDate(datePatternArg, dbDirArg, levelArg, true);
    }

    /**
     * Retrieve chart data grouped by events and ordered by date.
     * 
     * @param datePatternArg
     * @param dbDirArg
     * @param levelArg
     * @param showDataChart
     * @return
     * @throws Exception
     */
    public final Map<String, Integer> getNEventsPerDate(String datePatternArg, String dbDirArg, LogLevels levelArg,
            boolean showDataChart) throws Exception {

        Map<String, Integer> dataset = new TreeMap<String, Integer>();
        SQLiteConnection db = null;
        try {
            /* Simple date format */
            SimpleDateFormat sdf = new SimpleDateFormat(datePatternArg);

            /* Get the list of DB files to query */
            String[] dbfilepaths = getDBFilepaths(dbDirArg);

            /* Iterate over the file dbs */
            int cont = 0;
            String sqlQuery = SELECT_EVENTS_PER_DATE;
            for (String dbfilepath : dbfilepaths) {
                db = Sqlite4JavaTool.openDBConnection(dbfilepath);

                /* Set where clause for each corresponding database */
                if (dbfilepaths.length == 1) {
                    /* Only one file is accessed */
                    sqlQuery += "WHERE date >= '" + sdf.format(initDate) + "' AND date < '" + sdf.format(endDate) + "'";
                }
                /* Several DB files must be accesed */
                else {
                    if (cont == 0) {
                        sqlQuery += "WHERE date >= '" + sdf.format(initDate) + "'";
                    } else if (cont == dbfilepaths.length - 1) {
                        sqlQuery += "WHERE date < '" + sdf.format(endDate) + "'";
                    }
                }

                /* Log levels filter */
                if (levelArg != null) {
                    if (sqlQuery.indexOf("WHERE") != -1) {
                        sqlQuery += " AND level='" + levelArg + "'";
                    } else {
                        sqlQuery += " WHERE level='" + levelArg + "'";
                    }
                } else {
                    if (sqlQuery.indexOf("WHERE") != -1) {
                        sqlQuery += " AND level in(" + getSkipLogLevelsCommaSeparated() + ")";
                    } else {
                        sqlQuery += " WHERE level in(" + getSkipLogLevelsCommaSeparated() + ")";
                    }
                }

                /* Available for CHARTS only */
                if (showDataChart) {
                    if (sqlQuery.indexOf("WHERE") != -1) {
                        sqlQuery += " AND chart='1'";
                    } else {
                        sqlQuery += " WHERE chart='1'";
                    }
                }

                sqlQuery += " GROUP BY date";

                SQLiteStatement st = Sqlite4JavaTool.executeStatement(db, sqlQuery, null);
                while (st.step()) {
                    int count = st.columnInt(0);
                    String date = st.columnString(1);
                    dataset.put(date, count);
                }
                st.dispose();
                db.dispose();

                /* Increase counter */
                cont++;
                sqlQuery = SELECT_EVENTS_PER_DATE;
            }
        } catch (Exception ex) {
            if (db != null) {
                db.dispose();
            }

            throw ex;
        }

        return dataset;
    }

    /**
     * @see StatisticsInterface#getNDiffEventsPerDate(String, String).
     */
    @Override
    public final Map<String, Map<String, Integer>> getNDiffEventsPerDate(String datePatternArg, String dbDirArg)
            throws Exception {
        Map<String, Map<String, Integer>> collection = new HashMap<String, Map<String, Integer>>();

        try {
            /** Load available levels */
            List<LogLevels> availableLevels = getSkipLogLevels();
            for (LogLevels level : availableLevels) {
                Map<String, Integer> logs = getNEventsPerDate(datePatternArg, dbDirArg, level, false);
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
     * @see StatisticsInterface#getTotalEventNumbers(Map).
     */
    @Override
    public final Map<String, Integer> getTotalEventNumbers(Map<String, Map<String, Integer>> datasetArg)
            throws Exception {
        Map<String, Integer> output = new HashMap<String, Integer>();

        try {
            Set<String> levels = datasetArg.keySet();
            for (String level : levels) {
                int nEvents = 0;
                Set<String> dates = datasetArg.get(level).keySet();
                for (String date : dates) {
                    int events = datasetArg.get(level).get(date);
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
     * @see StatisticsInterface#getAppEventsTableData(String, String, LogLevels, String).
     */
    @Override
    public final Map<String, Object> getAppEventsTableData(String datePatternArg, String dbDirArg, LogLevels levelArg,
            String appEventFilterArg) throws Exception {

        /** Wrap dataset and data info */
        Map<String, Object> dataBundle = new HashMap<String, Object>();
        /** Dataset with events and counters - Use LinkedHashMap to sort the values by entering order */
        Map<String, Object> eventCounter = new LinkedHashMap<String, Object>();
        /** Available log levels */
        Set<String> logLevels = new TreeSet<String>();
        /** Available app. events */
        Map<String, String> appEvents = new TreeMap<String, String>();
        /* SQLite DB connection */
        SQLiteConnection db = null;

        try {
            /* Simple date format */
            SimpleDateFormat sdf = new SimpleDateFormat(datePatternArg);

            /* Get the list of DB files to query */
            String[] dbfilepaths = getDBFilepaths(dbDirArg);

            /* Iterate through the list of files */
            String sqlQuery = SELECT_APP_EVENTS;
            int cont = 0;
            for (String dbfilepath : dbfilepaths) {
                /* DB connection */
                db = Sqlite4JavaTool.openDBConnection(dbfilepath);

                /* Set where clause for each corresponding database */
                if (dbfilepaths.length == 1) {
                    /* Only one file is accessed */
                    sqlQuery += "WHERE date >= '" + sdf.format(initDate) + "' AND date < '" + sdf.format(endDate) + "'";
                }
                /* Several DB files must be accesed */
                else {
                    if (cont == 0) {
                        sqlQuery += "WHERE date >= '" + sdf.format(initDate) + "'";
                    } else if (cont == dbfilepaths.length - 1) {
                        sqlQuery += "WHERE date < '" + sdf.format(endDate) + "'";
                    }
                }

                /* Log levels filter */
                if (sqlQuery.indexOf("WHERE") != -1) {
                    sqlQuery += " AND level in(" + getSkipLogLevelsCommaSeparated() + ")";
                } else {
                    sqlQuery += " WHERE level in(" + getSkipLogLevelsCommaSeparated() + ")";
                }

                /* App events filter */
                if (appEventFilterArg != null) {
                    if (sqlQuery.indexOf("WHERE") != -1) {
                        sqlQuery += " AND event in(" + appEventFilterArg + ")";
                    } else {
                        sqlQuery += " WHERE event in(" + appEventFilterArg + ")";
                    }
                }

                /* Available for CHARTS */
                if (sqlQuery.indexOf("WHERE") != -1) {
                    sqlQuery += " AND chart='1'";
                } else {
                    sqlQuery += " WHERE chart='1'";
                }

                sqlQuery += " GROUP BY date, level, event";

                SQLiteStatement st = Sqlite4JavaTool.executeStatement(db, sqlQuery, null);
                while (st.step()) {
                    Map<String, Object> logLine = new HashMap<String, Object>();
                    logLine.put("counter", new Integer(st.columnInt(0)));
                    logLine.put("date", st.columnString(1));
                    logLine.put("log", st.columnString(2));
                    logLine.put("eventName", st.columnString(3));
                    eventCounter.put(logLine.get("date") + "_" + logLine.get("log") + "_" + logLine.get("eventName"),
                            logLine);

                    /* Add event name */
                    Map<String, String> newEvent = new HashMap<String, String>();
                    newEvent.put("name", (String) logLine.get("eventName"));
                    newEvent.put("value", (String) logLine.get("eventName"));
                    addAppEvent(newEvent, appEvents);

                    /* Add log level */
                    if (!logLevels.contains((String) logLine.get("log"))) {
                        logLevels.add((String) logLine.get("log"));
                    }
                }
                st.dispose();
                db.dispose();

                /* Increase counter */
                cont++;
                sqlQuery = SELECT_APP_EVENTS;
            }
        } catch (Exception ex) {
            if (db != null) {
                db.dispose();
            }

            throw ex;
        }

        dataBundle.put("dataset", eventCounter);
        dataBundle.put("logLevels", logLevels);
        dataBundle.put("appEvents", appEvents);
        /* Update current list of events and log levels */
        this.appEvents = appEvents;
        this.logLevels = logLevels;

        return dataBundle;
    }

    /**
     * Return timedivision attribute.
     * 
     * @return timedivision - Attribute returned
     */
    public final TimeDivision getTimedivision() {
        return timedivision;
    }

    /**
     * Set attribute timedivision.
     * 
     * @param timedivisionArg - Set value
     */
    public final void setTimedivision(TimeDivision timedivisionArg) {
        timedivision = timedivisionArg;
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
