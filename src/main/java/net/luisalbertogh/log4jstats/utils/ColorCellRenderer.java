/**
 * © 2009-2012 Tex Toll Services, LLC
 */
package net.luisalbertogh.log4jstats.utils;

import java.awt.Component;

import javax.swing.JTable;
import javax.swing.table.DefaultTableCellRenderer;

/**
 * This is a custom implementation of a cell renderer.
 * 
 * @author lagarcia
 */
public class ColorCellRenderer extends DefaultTableCellRenderer {

    /**
     * @see javax.swing.table.TableCellRenderer#getTableCellRendererComponent(javax.swing.JTable, java.lang.Object,
     *      boolean, boolean, int, int)
     */
    @Override
    public Component getTableCellRendererComponent(JTable tableArg, Object valueArg, boolean isSelectedArg,
            boolean hasFocusArg, int rowArg, int colArg) {
        Component comp = super.getTableCellRendererComponent(tableArg, valueArg, isSelectedArg, hasFocusArg, rowArg,
                colArg);
        /* Level column */
        String level = (String) tableArg.getModel().getValueAt(rowArg, 1);
        /* Log values */
        if (LogLevels.INFO.getLevel().equalsIgnoreCase(level)) {
            comp.setBackground(LogColors.GREEN.getColor());
        } else if (LogLevels.DEBUG.getLevel().equalsIgnoreCase(level)) {
            comp.setBackground(LogColors.YELLOW.getColor());
        } else if (LogLevels.ERROR.getLevel().equalsIgnoreCase(level)) {
            comp.setBackground(LogColors.RED.getColor());
        }

        return comp;
    }
}
