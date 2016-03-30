/**
 * © 2009-2012 Tex Toll Services, LLC
 */
package net.luisalbertogh.log4jstats.utils;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.StringTokenizer;

import javax.swing.filechooser.FileFilter;

/**
 * This class implements the file filter for export data options.
 * 
 * @author lagarcia
 */
public class ExportFilter extends FileFilter {

    /** Accepted file extensions */
    public enum Extensions {
        /**
         * Comma Separated Value.
         */
        CSV("csv"), /**
         * HTML.
         */
        HTML("html");

        private String extension;

        Extensions(String extension) {
            this.extension = extension;
        }

        /**
         * Return extension.
         * 
         * @return
         */
        public String getExtension() {
            return this.extension;
        }
    };

    /**
     * @see javax.swing.filechooser.FileFilter#accept(java.io.File)
     */
    @Override
    public boolean accept(File fileArg) {
        if (fileArg.isDirectory()) {
            return true;
        }

        String extension = getExtension(fileArg);
        if (extension != null) {
            if (isValidExtension(extension)) {
                return true;
            } else {
                return false;
            }
        }

        return false;
    }

    /**
     * Get file extension.
     * 
     * @param fileArg
     * @return
     */
    public static String getExtension(File fileArg) {
        if (fileArg != null) {
            String fileName = fileArg.getName();
            int lastIndex = fileName.lastIndexOf(".");
            return fileName.substring(lastIndex + 1).toLowerCase();
        }

        return null;
    }

    /**
     * Retrieve a list of export options.
     * 
     * @param optionsStr
     * @return
     */
    public static String[] getExportOptions(String optionsStr) {
        String[] options = new String[0];
        List<String> optionsArray = new ArrayList<String>();
        StringTokenizer st = new StringTokenizer(optionsStr, ",");
        while (st.hasMoreTokens()) {
            String option = st.nextToken();
            optionsArray.add(option);
        }

        if (!optionsArray.isEmpty()) {
            options = optionsArray.toArray(options);
        }

        return options;
    }

    /**
     * File extension is valid.
     * 
     * @param option
     * @return True or false
     */
    public static boolean isValidExtension(String option) {
        Extensions[] extensions = Extensions.values();
        for (int i = 0; i < extensions.length; i++) {
            if (extensions[i].getExtension().equalsIgnoreCase(option)) {
                return true;
            }
        }

        return false;
    }

    /**
     * @see javax.swing.filechooser.FileFilter#getDescription()
     */
    @Override
    public String getDescription() {
        return "Filter for export files.";
    }

}
