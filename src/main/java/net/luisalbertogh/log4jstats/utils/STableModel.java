/**
 * © 2009-2012 Tex Toll Services, LLC
 */
package net.luisalbertogh.log4jstats.utils;

import javax.swing.table.DefaultTableModel;

/**
 * This class implements a simple table model.
 * 
 * @author lagarcia
 */
public class STableModel extends DefaultTableModel {
    /**
     * @see DefaultTableModel#getColumnClass(int).
     */
    @Override
    public Class getColumnClass(int c) {
        return getValueAt(0, c).getClass();
    }
}
