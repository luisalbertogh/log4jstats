/**
 * © 2009-2012 Tex Toll Services, LLC
 */
package net.luisalbertogh.log4jstats.services.sqlite;

import java.util.Calendar;

import net.luisalbertogh.log4jstats.services.SuperService;
import net.luisalbertogh.sqlite4java.Sqlite4JavaTool.TimeDivision;

/**
 * This abstract class contains the common code for all the services based on SQLite.
 * 
 * @author lagarcia
 */
public abstract class SuperSQLService extends SuperService {
    /** Time division for SQLite querying. */
    protected TimeDivision timedivision;

    /** Number of weeks in a year. */
    protected static final int WEEKS_IN_YEAR = 52;

    /** Number of months in a year. */
    protected static final int MONTHS_IN_YEAR = 12;

    /**
     * Date constructor
     * 
     * @param defaultNumberDates
     */
    public SuperSQLService(int defaultNumberDates) {
        super(defaultNumberDates);
    }

    /**
     * Return the list of DB file paths to query, depending on the dates.
     * 
     * @param dbDirArg
     * @return
     */
    protected String[] getDBFilepaths(String dbDirArg) {
        String[] dbfilepaths = new String[0];

        /* Init counter and limit */
        int counter = 0;
        int limit = 0;
        switch (timedivision) {
            case MONTH: {
                counter = initCal.get(Calendar.MONTH);
                limit = endCal.get(Calendar.MONTH);
                break;
            }
            case WEEK: {
                counter = initCal.get(Calendar.WEEK_OF_YEAR);
                limit = endCal.get(Calendar.WEEK_OF_YEAR);
                break;
            }
        }

        /* Initial year */
        int year = initCal.get(Calendar.YEAR);
        while (counter <= limit) {
            /* New file path */
            String newfilepath = dbDirArg + "__" + counter + "_" + year;
            if (dbfilepaths.length == 0) {
                dbfilepaths = new String[1];
                dbfilepaths[0] = newfilepath;
            } else {
                String[] tmp = new String[dbfilepaths.length];
                System.arraycopy(dbfilepaths, 0, tmp, 0, dbfilepaths.length);
                dbfilepaths = new String[dbfilepaths.length + 1];
                System.arraycopy(tmp, 0, dbfilepaths, 0, tmp.length);
                dbfilepaths[dbfilepaths.length - 1] = newfilepath;
            }

            /* Increment counter */
            counter++;
            /* If year finishes, restart counter and increment year */
            if (counter > MONTHS_IN_YEAR && timedivision == TimeDivision.MONTH) {
                counter = 0;
                year++;
            } else if (counter > WEEKS_IN_YEAR && timedivision == TimeDivision.WEEK) {
                counter = 1;
                year++;
            }
        }

        return dbfilepaths;
    }
}
