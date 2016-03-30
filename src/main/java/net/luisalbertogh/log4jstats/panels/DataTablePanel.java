/**
 * © 2009-2012 Tex Toll Services, LLC
 */
package net.luisalbertogh.log4jstats.panels;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;

import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.border.TitledBorder;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.table.DefaultTableModel;

import net.luisalbertogh.log4jstats.Log4JStats;
import net.luisalbertogh.log4jstats.services.sqlite.TableSQLService;
import net.luisalbertogh.log4jstats.utils.ColorCellRenderer;
import net.luisalbertogh.log4jstats.utils.DateCellRenderer;
import net.luisalbertogh.log4jstats.utils.ExportFilter;
import net.luisalbertogh.log4jstats.utils.ExportFilter.Extensions;
import net.luisalbertogh.log4jstats.utils.LogColors;
import net.luisalbertogh.log4jstats.utils.STableModel;

/**
 * This class implements the panel that contains the data table with the log information.
 * 
 * @author lagarcia
 */
public class DataTablePanel extends StatisticsPanel {

    /**
     * Serial Version UID.
     */
    private static final long serialVersionUID = 3725152691802528542L;

    /** Reference to the main frame. */
    private Log4JStats mainArg;

    /** Table services. */
    private TableSQLService ts;

    /** Table dataset. */
    private List<Map<String, String>> tableDataset;

    /** Swing table. */
    private JTable dataTable;

    /** Table model. */
    private DefaultTableModel tableModel;

    /** Table indices. */
    private JLabel labelIndices;
    private int start, end;

    /** Table gap. */
    private int gap;

    /** Table indices fast moving gap. */
    private static final int FAST = 2;

    /** Custom indices drop down. */
    private NavCombo customIndex;

    /** Search input box. */
    private JTextField searchTxt;

    /** File chooser */
    private final JFileChooser fc = new JFileChooser();

    /**
     * Default constructor.
     * 
     * @param mainArg - A reference to the main frame
     */
    public DataTablePanel(Log4JStats mainArg) {
        super(mainArg);
        this.mainArg = mainArg;

        try {
            /** Init table indices */
            gap = Integer.parseInt(main.getProperties().getProperty("tablegap"));
            start = 1;
            end = start + gap;

            /** Init table service */
            ts = new TableSQLService(Integer.parseInt(main.getProperties().getProperty("defaultintervaltime")),
                    Integer.parseInt(main.getProperties().getProperty("maxnumberdates")), null);
            /** Init log levels */
            ts.initLogLevels();
            /** Init app. events list */
            ts.initAppEvents(main.getChartProperties().getProperty("appeventstable"));

            /** Create dataset with new dates (true) */
            createDataset(true);

            /** Add app events combo */
            Map<String, String> appEventsMap = ts.getAppEvents();
            addAppEventsCombo(appEventsMap);

            /** Add levels combo to toolbar */
            String[] levels = new String[0];
            levels = ts.getLogLevels().toArray(levels);
            addLevelsCombo(levels);

            /** Draw the data table */
            drawDataTable();
        } catch (Exception ex) {
            ex.printStackTrace();
        }

        /** Add tabbed panel */
        add(tabbedPane, BorderLayout.CENTER);
    }

    /**
     * Generate the dataset to plot.
     */
    private void createDataset(boolean newDates) {
        try {
            /** Get dataset */
            if (newDates) {
                ts.setDatesInterval(super.dateFilter.getInitDate(), super.dateFilter.getEndDate());
            }
            tableDataset = ts.getEventsData(main.getDatePattern(), getDbPath(), null, start, end);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Generate a data table
     */
    private void drawDataTable() {
        try {
            /* New table model */
            tableModel = new STableModel();

            /** Columns */
            tableModel.addColumn(main.getChartProperties().getProperty("datatable_col01"));
            tableModel.addColumn(main.getChartProperties().getProperty("datatable_col02"));
            tableModel.addColumn(main.getChartProperties().getProperty("datatable_col04"));
            tableModel.addColumn(main.getChartProperties().getProperty("datatable_col03"));

            /** Update data table */
            updateTable();

            dataTable = new JTable(tableModel);
            dataTable.setFillsViewportHeight(true);
            dataTable.setAutoCreateRowSorter(true);

            /* Set date cell renderer and cell sizes */
            dataTable.getColumn(dataTable.getColumnName(0))
                    .setCellRenderer(new DateCellRenderer(main.getDatePattern()));
            dataTable.getColumn(dataTable.getColumnName(1)).setCellRenderer(new ColorCellRenderer());

            dataTable.getColumn(dataTable.getColumnName(0)).setMaxWidth(300);
            dataTable.getColumn(dataTable.getColumnName(0)).setMinWidth(200);
            dataTable.getColumn(dataTable.getColumnName(1)).setMaxWidth(100);
            dataTable.getColumn(dataTable.getColumnName(2)).setMaxWidth(100);

            /** List selection listener */
            dataTable.getSelectionModel().addListSelectionListener(new DTListSelectionListener());

            JScrollPane scrollPane = new JScrollPane(dataTable);
            JPanel gridPanel = new JPanel();
            gridPanel.setLayout(new BorderLayout());
            gridPanel.add(scrollPane, BorderLayout.CENTER);

            /* Navigation buttons */
            JPanel navPanel = getNavPanel();
            /* Additional options panel */
            JPanel addOptionsPanel = getAddOptionsPanel();

            /* Operations panel */
            JSplitPane operationsPanel = new JSplitPane(JSplitPane.VERTICAL_SPLIT, addOptionsPanel, navPanel);
            operationsPanel.setPreferredSize(new Dimension(0, 100));
            operationsPanel.setOneTouchExpandable(true);
            operationsPanel.setDividerLocation(0);
            gridPanel.add(operationsPanel, BorderLayout.SOUTH);
            tabbedPane.add(gridPanel, main.getChartProperties().getProperty("datatable_tab"));

        } catch (Exception pe) {
            pe.printStackTrace();
        }
    }

    /**
     * Update data table.
     */
    private void updateTable() throws ParseException {
        /* Date formatter */
        SimpleDateFormat sdf = new SimpleDateFormat(main.getDatePattern());

        /* Table model */
        if (dataTable != null) {
            tableModel = (DefaultTableModel) dataTable.getModel();
        }

        /* Remove former rows */
        final int rowCount = tableModel.getRowCount();
        for (int i = 0; i < rowCount; i++) {
            /* When removing a row, indeces are changed, so always remove first row */
            tableModel.removeRow(0);
        }

        /** Fill in data matrix */
        for (Map<String, String> row : tableDataset) {
            Object[] rowData = new Object[4];
            rowData[0] = sdf.parse(row.get("date"));
            rowData[1] = row.get("level");
            rowData[2] = row.get("event");
            rowData[3] = row.get("text");
            tableModel.addRow(rowData);
        }

        /* Update label indices */
        if (labelIndices != null) {
            labelIndices.setText(start + " - " + end);
        }
    }

    /**
     * Create the panel with the table navigation buttons.
     * 
     * @return
     */
    private JPanel getNavPanel() throws Exception {
        JPanel controlPanel = new JPanel();
        controlPanel.setLayout(new BoxLayout(controlPanel, BoxLayout.X_AXIS));

        /* Navigation buttons */
        JPanel navPanel = new JPanel();
        navPanel.setBorder(new TitledBorder(main.getProperties().getProperty("navigation")));
        NavButton initBt = new NavButton(main.getProperties().getProperty("init"), "init");
        initBt.setToolTipText(main.getProperties().getProperty("initTip"));
        initBt.addActionListener(this);
        NavButton fprevBt = new NavButton("<<", "fbackward");
        fprevBt.setToolTipText(main.getProperties().getProperty("fbackTip"));
        fprevBt.addActionListener(this);
        NavButton prevBt = new NavButton("<", "backward");
        prevBt.setToolTipText(main.getProperties().getProperty("backTip"));
        prevBt.addActionListener(this);
        NavButton nextBt = new NavButton(">", "forward");
        nextBt.setToolTipText(main.getProperties().getProperty("forwTip"));
        nextBt.addActionListener(this);
        NavButton fnextBt = new NavButton(">>", "fforward");
        fnextBt.setToolTipText(main.getProperties().getProperty("fforwTip"));
        fnextBt.addActionListener(this);
        NavButton endBt = new NavButton(main.getProperties().getProperty("end"), "end");
        endBt.setToolTipText(main.getProperties().getProperty("endTip"));
        endBt.addActionListener(this);

        navPanel.add(Box.createHorizontalStrut(10));
        navPanel.add(initBt);
        navPanel.add(Box.createHorizontalStrut(5));
        navPanel.add(fprevBt);
        navPanel.add(prevBt);
        navPanel.add(nextBt);
        navPanel.add(fnextBt);
        navPanel.add(Box.createHorizontalStrut(5));
        navPanel.add(endBt);
        navPanel.add(Box.createHorizontalStrut(10));

        /* Number of rows */
        int totalRowNumber = ts.getRowCounter();
        JPanel rowNumberPanel = new JPanel();
        rowNumberPanel.setBorder(new TitledBorder(main.getProperties().getProperty("rows")));
        labelIndices = new JLabel(start + " - " + end);
        rowNumberPanel.add(labelIndices);

        /* Custom indices */
        JPanel customPanel = new JPanel();
        customPanel.setBorder(new TitledBorder(main.getProperties().getProperty("custom")));
        customIndex = new NavCombo(getComboModel(totalRowNumber, main.getProperties().getProperty("tablegap")));
        customIndex.addActionListener(this);
        customPanel.add(customIndex);

        controlPanel.add(rowNumberPanel);
        controlPanel.add(navPanel);
        controlPanel.add(customPanel);

        return controlPanel;
    }

    /**
     * Create additional options panel for table grid.
     * 
     * @return
     * @throws Exception
     */
    private JPanel getAddOptionsPanel() throws Exception {
        JPanel optionsPanel = new JPanel();
        optionsPanel.setLayout(new GridLayout(1, 2));

        /* Search box */
        JPanel searchPanel = new JPanel();
        searchPanel.setBorder(new TitledBorder(main.getProperties().getProperty("search")));
        searchTxt = new JTextField(10);
        String filter = ts.getKeyword();
        if (filter != null && !"".equals(filter)) {
            searchTxt.setText(filter);
            searchTxt.setBackground(LogColors.GREEN.getColor());
        }
        searchPanel.add(searchTxt);
        NavButton searchBt = new NavButton(main.getProperties().getProperty("search"), "search");
        searchBt.addActionListener(this);
        searchPanel.add(searchBt);
        NavButton cleanBt = new NavButton(main.getProperties().getProperty("clean"), "clean");
        cleanBt.addActionListener(this);
        searchPanel.add(cleanBt);

        /* Export options */
        JPanel exportPanel = new JPanel();
        exportPanel.setBorder(new TitledBorder(main.getProperties().getProperty("export")));
        ExportCombo exportCombo = new ExportCombo(ExportFilter.getExportOptions(main.getProperties().getProperty(
                "exportoptions")));
        exportCombo.setPreferredSize(new Dimension(200, 30));
        exportCombo.addActionListener(this);
        exportPanel.add(exportCombo);

        optionsPanel.add(searchPanel);
        optionsPanel.add(exportPanel);

        return optionsPanel;
    }

    /**
     * Update charts and table panels.
     */
    @Override
    protected void updatePanel() {
        /** Remove previous charts */
        tabbedPane.removeAll();
        tabbedPane.add(waitPanel);

        /** Create new dataset with new dates and plot new charts and table */
        createDataset(true);
        if (tableDataset != null) {
            drawDataTable();

            /** Update app events combo */
            Map<String, String> appEventsMap = ts.getAppEvents();
            if (appEventsMap != null) {
                updateAppEventsCombo(appEventsMap);
            } else {
                addAppEventsCombo(appEventsMap);
            }

            /** Update levels combo to toolbar */
            String[] levels = new String[0];
            levels = ts.getLogLevels().toArray(levels);
            if (levelsCombo != null) {
                updateLevelsCombo(levels);
            } else {
                addLevelsCombo(levels);
            }
        }

        /** Remove wait panel */
        tabbedPane.remove(0);

        /** Notify if needed */
        String curOper = this.getOperation();
        if ("resetEvents".equals(curOper) || "resetLevels".equals(curOper)) {
            synchronized (this) {
                this.notifyAll();
            }
        }
    }

    /**
     * Calculate new values for table indeces.
     * 
     * @return boolean - False if invalid indeces were calculated
     */
    private boolean calculateTableIndeces(String subaction, int customIni, int customEnd) {
        int startTmp = start, endTmp = end;

        /** Table forward */
        if ("forward".equals(subaction)) {
            startTmp = endTmp + 1;
            endTmp = startTmp + gap;
        }
        /** Table backward */
        else if ("backward".equals(subaction)) {
            endTmp = startTmp - 1;
            startTmp = endTmp - gap;
        }
        /** Fast forward */
        else if ("fforward".equals(subaction)) {
            startTmp += (FAST * gap) + FAST;
            endTmp = startTmp + gap;
        }
        /** Fast backward */
        else if ("fbackward".equals(subaction)) {
            endTmp -= (FAST * gap) + FAST;
            startTmp = endTmp - gap;
        }
        /** Init */
        else if ("init".equals(subaction)) {
            startTmp = 1;
            endTmp = startTmp + gap;
        }
        /** End */
        else if ("end".equals(subaction)) {
            endTmp = ts.getRowCounter();
            startTmp = endTmp - gap;
        }
        /** Custom */
        else if ("custom".equals(subaction)) {
            startTmp = customIni;
            endTmp = customEnd;
        }

        /** Validations */
        if (startTmp < 0) {
            return false;
        }

        start = startTmp;
        end = endTmp;

        return true;
    }

    /**
     * @see java.awt.event.ActionListener#actionPerformed(java.awt.event.ActionEvent)
     */
    @Override
    public void actionPerformed(ActionEvent eArg) {
        try {
            Object source = eArg.getSource();
            /* Navigation drop-down */
            if (source instanceof NavCombo) {
                /* Custom indices */
                String customIndeces = (String) customIndex.getSelectedItem();
                int cInit = Integer.parseInt(customIndeces.split(" - ")[0]);
                int cEnd = Integer.parseInt(customIndeces.split(" - ")[1]);
                calculateTableIndeces("custom", cInit, cEnd);

                /* New dataset */
                createDataset(false);
                /* Update table */
                updateTable();
            }
            /* Export drop-down */
            else if (source instanceof ExportCombo) {
                /* Subaction */
                String option = (String) ((ExportCombo) source).getSelectedItem();
                if (!ExportFilter.isValidExtension(option)) {
                    return;
                }
                /* Open file chooser */
                fc.addChoosableFileFilter(new ExportFilter());
                int returnVal = fc.showOpenDialog(this);
                File file = null;
                if (returnVal == JFileChooser.APPROVE_OPTION) {
                    file = fc.getSelectedFile();
                    /* Export to csv */
                    String dump = null;
                    if (Extensions.CSV.getExtension().equals(option)) {
                        dump = ts.getEventsCSV(tableDataset);
                        if (!Extensions.CSV.getExtension().equals(ExportFilter.getExtension(file))) {
                            file = new File(file.getAbsolutePath() + "." + Extensions.CSV.getExtension());
                        }
                    }
                    /* Export to html */
                    else if (Extensions.HTML.getExtension().equals(option)) {
                        dump = ts.getEventsHTML(tableDataset);
                        if (!Extensions.HTML.getExtension().equals(ExportFilter.getExtension(file))) {
                            file = new File(file.getAbsolutePath() + "." + Extensions.HTML.getExtension());
                        }
                    }

                    /* Save to file */
                    if (dump != null) {
                        try {
                            Writer w = new FileWriter(file);
                            w.write(dump);
                            w.flush();
                            w.close();
                        } catch (IOException e) {
                            JOptionPane.showMessageDialog(main.getFrame(),
                                    main.getProperties().getProperty("exportfailed"), "Error",
                                    JOptionPane.ERROR_MESSAGE);
                        }
                    }

                    /* Info message */
                    JOptionPane.showMessageDialog(main.getFrame(), main.getProperties().getProperty("exportsucced"));
                }
            }
            /* Navigation button */
            else if (source instanceof NavButton) {
                /* Deselect any table row */
                dataTable.clearSelection();

                /* Subaction */
                String subaction = ((NavButton) source).getSubAction();

                /* Filter text */
                if ("search".equals(subaction)) {
                    String filter = searchTxt.getText();
                    if (filter != null && !"".equals(filter)) {
                        searchTxt.setBackground(LogColors.GREEN.getColor());
                    } else {
                        searchTxt.setBackground(Color.WHITE);
                    }
                    ts.setKeyword(filter);
                    /* Restart indices */
                    subaction = "init";
                }
                /* Reset filter */
                else if ("clean".equals(subaction)) {
                    ts.setKeyword("");
                    searchTxt.setText("");
                    searchTxt.setBackground(Color.WHITE);
                    /* Restart indices */
                    subaction = "init";
                }

                /* Calculate table indices automatically */
                calculateTableIndeces(subaction, 0, 0);

                /* New dataset */
                createDataset(false);
                /* Update table */
                updateTable();
            }
            /* Other buttons */
            else {
                calculateTableIndeces("init", 0, 0);
                performFullActions(eArg, ts);
            }
        } catch (ParseException pe) {
            pe.printStackTrace();
        }
    }

    /**
     * Data table list selection listener.
     * 
     * @author lagarcia
     */
    class DTListSelectionListener implements ListSelectionListener {

        /**
         * @see ListSelectionListener#valueChanged(ListSelectionEvent).
         */
        @Override
        public void valueChanged(ListSelectionEvent evtArg) {
            /* Avoid firing the events twice */
            if (!evtArg.getValueIsAdjusting()) {
                int viewRow = dataTable.getSelectedRow();
                if (viewRow > -1) {
                    int modelRow = dataTable.convertRowIndexToModel(viewRow);
                    Date date = (Date) tableModel.getValueAt(modelRow, 0);
                    String level = (String) tableModel.getValueAt(modelRow, 1);
                    String event = (String) tableModel.getValueAt(modelRow, 2);
                    String text = (String) tableModel.getValueAt(modelRow, 3);
                    Object[] data = new Object[4];
                    data[0] = date;
                    data[1] = level;
                    data[2] = event;
                    data[3] = text;

                    /* Open dialog box */
                    JDialog dialog = new JDialog();
                    dialog.setTitle(mainArg.getProperties().getProperty("logEvent"));
                    dialog.setContentPane(new SelectionPanel(mainArg, data, dialog));
                    dialog.setResizable(false);
                    dialog.setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
                    dialog.pack();
                    dialog.setVisible(true);
                }
            }
        }
    }

    /**
     * Get combo model for custom indeces in table grid.
     * 
     * @param totalRowNumber
     * @param tablegap
     * @throws Exception
     * @return
     */
    private String[] getComboModel(int totalRowNumber, String tablegap) throws Exception {
        List<String> comboElements = new ArrayList<String>();
        int tgInt = Integer.parseInt(tablegap);
        int start = 1;
        int end = start + tgInt;
        do {
            comboElements.add(start + " - " + end);
            start = end + 1;
            end = start + tgInt;
        } while (end < totalRowNumber);

        if (start < totalRowNumber) {
            /* Add last interval */
            comboElements.add(start + " - " + end);
        }

        String[] comboElementsStr = new String[0];
        comboElementsStr = comboElements.toArray(comboElementsStr);

        return comboElementsStr;
    }

    /**
     * It implements a navigation table button.
     * 
     * @author lagarcia
     */
    class NavButton extends JButton {
        /** The representing action. */
        private String action;

        /**
         * Constructor
         * 
         * @param text
         * @param actionArg
         */
        public NavButton(String text, String actionArg) {
            super(text);
            this.action = actionArg;
        }

        /**
         * Retrieve the subaction representing this button.
         * 
         * @return Subaction
         */
        public String getSubAction() {
            return this.action;
        }
    }

    /**
     * Export button.
     * 
     * @author lagarcia
     */
    class ExportCombo extends JComboBox {
        /**
         * Constructor.
         * 
         * @param options
         */
        public ExportCombo(String[] options) {
            super(options);
        }
    }

    /**
     * Implementation of the combo box for navigation purposes.
     * 
     * @author lagarcia
     */
    class NavCombo extends JComboBox {
        public NavCombo(String[] options) {
            super(options);
        }
    }
}
