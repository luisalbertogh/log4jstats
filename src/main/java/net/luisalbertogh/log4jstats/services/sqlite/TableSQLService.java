/**
 * © 2009-2012 Tex Toll Services, LLC
 */
package net.luisalbertogh.log4jstats.services.sqlite;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

import net.luisalbertogh.log4jstats.interfaces.TableInterface;
import net.luisalbertogh.log4jstats.utils.LogLevels;
import net.luisalbertogh.sqlite4java.Sqlite4JavaTool;
import net.luisalbertogh.sqlite4java.Sqlite4JavaTool.TimeDivision;

import com.almworks.sqlite4java.SQLiteConnection;
import com.almworks.sqlite4java.SQLiteStatement;

/**
 * This class implements the service for the log events table function based on SQLite.
 * 
 * @author lagarcia
 */
public class TableSQLService extends SuperSQLService implements TableInterface {
    /** Current log levels */
    private Set<String> logLevels;

    /** Current app. events */
    private Map<String, String> appEvents;

    /** Keyword to filter in */
    private String keyword;

    /** Row counter */
    private int rowCounter;

    /** SQL query for log events selection */
    private static final String SELECT_LOG_EVENTS = "SELECT datetime, level, event, log FROM logevents ";

    /** SQL query for text search */
    private static final String SELECT_TEXT = "SELECT docid FROM logtext WHERE text MATCH ";

    /**
     * Default constructor
     * 
     * @param defaultNumberDates - Default number of dates between init and end dates
     * @param maxNumberOfDates - Maximum number of dates to process
     * @param timedivision - Time division
     */
    public TableSQLService(int defaultNumberDates, int maxNumberOfDates, String timedivision) {
        super(defaultNumberDates);
        this.maxNumberOfDates = maxNumberOfDates;
        if (TimeDivision.WEEK.getName().equals(timedivision)) {
            this.timedivision = TimeDivision.WEEK;
        } else {
            this.timedivision = TimeDivision.MONTH;
        }
    }

    /**
     * Get the data of the logging system for table rendering.
     * 
     * @param datePattern
     * @param dbDirArg
     * @param level
     * @param start - int First index of returned subset
     * @param end - int Last index of returned subset
     * @return Dataset with events data.
     * @throws Exception
     */
    @Override
    public List<Map<String, String>> getEventsData(String datePattern, String dbDirArg, LogLevels level, int start,
            int end) throws Exception {
        List<Map<String, String>> dataset = new ArrayList<Map<String, String>>();
        SQLiteConnection db = null;
        try {
            /* Simple date format */
            SimpleDateFormat sdf = new SimpleDateFormat(datePattern);

            /* Get the list of DB files to query */
            String[] dbfilepaths = getDBFilepaths(dbDirArg);

            /** Init available log levels */
            logLevels = new TreeSet<String>();

            /** Init available app. events */
            appEvents = new TreeMap<String, String>();

            /* Iterate through the databases */
            /* DB file counter */
            int cont = 0;
            /* Init row counter */
            rowCounter = 1;
            String sqlQuery = SELECT_LOG_EVENTS;
            for (String dbfilepath : dbfilepaths) {
                db = Sqlite4JavaTool.openDBConnection(dbfilepath);

                /* Set where clause for each corresponding database */
                if (dbfilepaths.length == 1) {
                    /* Only one file is accessed */
                    sqlQuery += "WHERE datetime >= '" + sdf.format(initDate) + "' AND datetime < '"
                            + sdf.format(endDate) + "'";
                }
                /* Several DB files must be accesed */
                else {
                    if (cont == 0) {
                        sqlQuery += "WHERE datetime >= '" + sdf.format(initDate) + "'";
                    } else if (cont == dbfilepaths.length - 1) {
                        sqlQuery += "WHERE datetime < '" + sdf.format(endDate) + "'";
                    }
                }

                /* Log levels filter */
                if (sqlQuery.indexOf("WHERE") != -1) {
                    sqlQuery += " AND level in(" + getSkipLogLevelsCommaSeparated() + ")";
                } else {
                    sqlQuery += " WHERE level in(" + getSkipLogLevelsCommaSeparated() + ")";
                }

                /* App events filter */
                if (sqlQuery.indexOf("WHERE") != -1) {
                    sqlQuery += " AND event in(" + getAppEventsListAsString() + ")";
                } else {
                    sqlQuery += " WHERE event in(" + getAppEventsListAsString() + ")";
                }

                /* Keyword search */
                if (keyword != null && !"".equals(keyword)) {
                    if (sqlQuery.indexOf("WHERE") != -1) {
                        sqlQuery += " AND id in(" + SELECT_TEXT + "'" + keyword + "')";
                    } else {
                        sqlQuery += " WHERE id in(" + SELECT_TEXT + "'" + keyword + "')";
                    }
                }

                /* Perform query and store retrieved data */
                SQLiteStatement st = Sqlite4JavaTool.executeStatement(db, sqlQuery, null);
                /* While there is data */
                while (st.step()) {
                    Map<String, String> logLine = new HashMap<String, String>();
                    logLine.put("date", st.columnString(0));
                    logLine.put("level", st.columnString(1));
                    logLine.put("event", st.columnString(2));
                    logLine.put("text", st.columnString(3));

                    /* Add event name */
                    Map<String, String> newEvent = new HashMap<String, String>();
                    newEvent.put("name", (String) logLine.get("event"));
                    newEvent.put("value", (String) logLine.get("event"));
                    addAppEvent(newEvent, appEvents);

                    /* Add log level */
                    if (!logLevels.contains((String) logLine.get("level"))) {
                        logLevels.add((String) logLine.get("level"));
                    }

                    /* Add only row between table indices */
                    if (rowCounter >= start && rowCounter <= end) {
                        dataset.add(logLine);
                    }

                    /* Increment row counter */
                    rowCounter++;
                }
                st.dispose();
                db.dispose();

                /* Increase counter */
                cont++;
                sqlQuery = SELECT_LOG_EVENTS;
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

    /**
     * @see net.luisalbertogh.log4jstats.interfaces.TableInterface#getEventsCSV(java.util.List)
     */
    @Override
    public String getEventsCSV(List<Map<String, String>> dataset) {
        StringBuilder sb = new StringBuilder();

        /** Headers */
        sb.append("Date, Level, Event, Text\n");

        /** Add table rows */
        for (Map<String, String> event : dataset) {
            /* Table row */
            String row = event.get("date") + "," + event.get("level") + "," + event.get("event") + ","
                    + event.get("text");

            /* Add row */
            sb.append(row + "\n");
        }

        return sb.toString();
    }

    /**
     * @see net.luisalbertogh.log4jstats.interfaces.TableInterface#getEventsHTML(java.util.List)
     */
    @Override
    public String getEventsHTML(List<Map<String, String>> dataset) {
        StringBuilder sb = new StringBuilder();

        /** Headers */
        sb.append("<html><body>");
        sb.append("<table border='1' width='100%'>");
        sb.append("<tr><th>Date</th><th>Level</th><th>Event</th><th>Text</th></tr>");
        sb.append("<tbody>");

        /** Add table rows */
        for (Map<String, String> event : dataset) {
            /* Table row */
            String row = "<tr><td>" + event.get("date") + "</td><td>" + event.get("level") + "</td><td>"
                    + event.get("event") + "</td><td>" + event.get("text") + "</td></tr>";

            /* Add row */
            sb.append(row);
        }

        sb.append("</tbody>");
        sb.append("</table>");
        sb.append("</body></html>");

        return sb.toString();
    }

    /**
     * Get the row counter value.
     * 
     * @return The number of rows matching the search criteria
     */
    public int getRowCounter() {
        return this.rowCounter;
    }
}
