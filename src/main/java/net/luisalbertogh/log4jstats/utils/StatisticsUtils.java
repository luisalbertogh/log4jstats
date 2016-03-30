package net.luisalbertogh.log4jstats.utils;

import org.apache.log4j.Appender;
import org.apache.log4j.Hierarchy;
import org.apache.log4j.Logger;
import org.apache.log4j.PatternLayout;
import org.apache.log4j.xml.DOMConfigurator;

/**
 * This class implements some utilities and tools for the Statistics.
 * 
 * @author lagarcia
 */
public class StatisticsUtils {

    /**
     * Get conversion pattern for the given log4j file and appender name.
     * 
     * @param log4jFile
     * @param loggerName
     * @param appenderName
     * @return String - Conversion pattern
     * @throws Exception
     */
    public static String getConversionPattern(String log4jFile, String loggerName, String appenderName)
            throws Exception {
        /* Use log4j API to load logger conversion patter */
        DOMConfigurator domConfig = new DOMConfigurator();
        Hierarchy loggerRepo = new Hierarchy(Logger.getRootLogger());
        domConfig.doConfigure(log4jFile, loggerRepo);
        Logger logger = loggerRepo.getLogger(loggerName);
        Appender appender = logger.getAppender(appenderName);
        PatternLayout layout = (PatternLayout) appender.getLayout();
        String conversionPattern = layout.getConversionPattern();
        return conversionPattern;
    }

    /**
     * Get the date pattern from the given Log4j conversion pattern.
     * 
     * @param conversionPattern
     * @return String - Java date pattern
     */
    public static String getDatePattern(String conversionPattern) {
        String datePattern = "";

        if (conversionPattern == null) {
            return "";
        }

        /** First and last index */
        int begin = conversionPattern.indexOf("%d{");
        int end = -1;
        if (begin != -1) {
            end = conversionPattern.substring(begin).indexOf("}");
        }

        /** Date pattern */
        if (begin != -1 && end != -1) {
            datePattern = conversionPattern.substring(begin + 3, end);
        } else {
            datePattern = "yyyy-MM-dd HH:mm:ss,sss";
        }

        return datePattern;
    }
}
