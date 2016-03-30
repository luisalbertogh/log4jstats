/**
 * © 2009-2012 Tex Toll Services, LLC
 */
package net.luisalbertogh.log4jstats.utils;

import java.text.SimpleDateFormat;

import javax.swing.table.DefaultTableCellRenderer;

/**
 * This class implements the custom date cell renderer.
 * 
 * @author lagarcia
 */
public class DateCellRenderer extends DefaultTableCellRenderer {

    /** Date formatter. */
    SimpleDateFormat formatter;

    /**
     * Default constructor.
     * 
     * @param dateFormat
     */
    public DateCellRenderer(String dateFormat) {
        super();
        formatter = new SimpleDateFormat(dateFormat);
    }

    /**
     * @see javax.swing.table.DefaultTableCellRenderer#setValue(java.lang.Object)
     */
    @Override
    public void setValue(Object value) {
        if (value != null) {
            setText(formatter.format(value));
        } else {
            setText("");
        }
    }
}
