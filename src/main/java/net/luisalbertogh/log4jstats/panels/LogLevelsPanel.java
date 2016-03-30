package net.luisalbertogh.log4jstats.panels;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.event.ActionEvent;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

import javax.swing.DefaultComboBoxModel;
import javax.swing.JComboBox;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.JTable;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.table.DefaultTableModel;

import net.luisalbertogh.log4jstats.Log4JStats;
import net.luisalbertogh.log4jstats.services.sqlite.StatisticsSQLService;
import net.luisalbertogh.log4jstats.utils.JStatsButton;
import net.luisalbertogh.log4jstats.utils.LogLevels;
import net.luisalbertogh.log4jstats.utils.STableModel;

import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;

/**
 * This class implements the log levels panel.
 * 
 * @author lagarcia
 */
public class LogLevelsPanel extends StatisticsPanel implements ChangeListener {

    /**
     * 
     */
    private static final long serialVersionUID = 1L;

    /** Main dataset */
    private Map<String, Map<String, Integer>> dataset;
    /** Levels dataset */
    private Map<String, Integer> levelsDataset;

    /** Sub tab panel for total levels events */
    private JTabbedPane totalTabs = new JTabbedPane();

    /**
     * Constructor
     * 
     * @param main - Reference to main Log4JStats class
     */
    public LogLevelsPanel(Log4JStats main) {
        super(main);

        try {
            createDataset();

            /** Add levels combo to toolbar */
            String[] levels = new String[0];
            levels = dataset.keySet().toArray(levels);
            addLevelsCombo(levels);

            drawTimeSeriePanel();
            drawBarSeriePanel();
            drawDataTable();

            /** Add other charts tab */
            tabbedPane.addTab(main.getProperties().getProperty("totallevelsTab"), null);
            tabbedPane.addChangeListener(this);
        } catch (Exception ex) {
            ex.printStackTrace();
        }

        add(tabbedPane, BorderLayout.CENTER);
    }

    /**
     * Generate the dataset to plot.
     */
    private void createDataset() {
        try {
            StatisticsSQLService ssql = main.getStatisticsSQLService();

            /** Set dates */
            ssql.setDatesInterval(super.dateFilter.getInitDate(), super.dateFilter.getEndDate());

            dataset = ssql.getNDiffEventsPerDate(main.getDateOnlyPattern(), getDbPath());
            levelsDataset = ssql.getTotalEventNumbers(dataset);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /** Render time-serie panel content */
    private void drawTimeSeriePanel() {
        try {
            Map<String, String> chartDetails = new HashMap<String, String>();
            chartDetails.put("datePattern", main.getDatePattern().split(" ")[0]);
            chartDetails.put("xLabel", main.getChartProperties().getProperty("loglevels_xLabel"));
            chartDetails.put("yLabel", main.getChartProperties().getProperty("loglevels_yLabel"));
            chartDetails.put("chartTitle", main.getChartProperties().getProperty("loglevels_chartTitle"));
            JFreeChart chart = sc.getLevelsTimeserie(dataset, chartDetails);
            ChartPanel cPanel = new ChartPanel(chart);
            tabbedPane.add("Time-serie", cPanel);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /** Render bar-serie panel content */
    private void drawBarSeriePanel() {
        try {
            Map<String, String> chartDetails = new HashMap<String, String>();
            chartDetails.put("datePattern", main.getDatePattern().split(" ")[0]);
            chartDetails.put("xLabel", main.getChartProperties().getProperty("loglevels_xLabel"));
            chartDetails.put("yLabel", main.getChartProperties().getProperty("loglevels_yLabel"));
            chartDetails.put("chartTitle", main.getChartProperties().getProperty("loglevels_chartTitle"));
            JFreeChart chart = sc.getLevelsBarChart(dataset, chartDetails);
            ChartPanel cPanel = new ChartPanel(chart);
            tabbedPane.add("Bar-serie", cPanel);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /** Render bar-serie panel content */
    private void drawAllLevelsBarSeriePanel() {
        try {
            Map<String, String> chartDetails = new HashMap<String, String>();
            chartDetails.put("datePattern", main.getDatePattern().split(" ")[0]);
            chartDetails.put("xLabel", main.getChartProperties().getProperty("loglevels_xLabel"));
            chartDetails.put("yLabel", main.getChartProperties().getProperty("loglevels_yLabel"));
            chartDetails.put("chartTitle", main.getChartProperties().getProperty("loglevels_chartTitle"));
            JFreeChart chart = sc.getDiffTotalsBarChart(levelsDataset, chartDetails);
            ChartPanel cPanel = new ChartPanel(chart);
            totalTabs.add("Bar-serie", cPanel);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /** Render bar-serie panel content */
    private void drawAllLevelsPiePanel() {
        try {
            Map<String, String> chartDetails = new HashMap<String, String>();
            chartDetails.put("datePattern", main.getDatePattern().split(" ")[0]);
            chartDetails.put("xLabel", main.getChartProperties().getProperty("loglevels_xLabel"));
            chartDetails.put("yLabel", main.getChartProperties().getProperty("loglevels_yLabel"));
            chartDetails.put("chartTitle", main.getChartProperties().getProperty("loglevels_chartTitle"));
            JFreeChart chart = sc.getDiffTotalsPieChart(levelsDataset, chartDetails);
            ChartPanel cPanel = new ChartPanel(chart);
            totalTabs.add("Pie-chart", cPanel);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Generate a data table
     */
    private void drawDataTable() {
        try {
            /** Reorganized the dataset to group data by date. */
            SimpleDateFormat sdf = new SimpleDateFormat(main.getDatePattern().split(" ")[0]);
            Set<String> levels = dataset.keySet();
            Map<Date, Map<String, Integer>> dataTableMap = new TreeMap<Date, Map<String, Integer>>();
            for (String level : levels) {
                Set<String> dates = dataset.get(level).keySet();
                for (String date : dates) {
                    Date d = sdf.parse(date);
                    Map<String, Integer> levelData = null;
                    if (dataTableMap.containsKey(d)) {
                        levelData = dataTableMap.get(d);
                    } else {
                        levelData = new HashMap<String, Integer>();
                    }

                    levelData.put(level, dataset.get(level).get(date));
                    dataTableMap.put(d, levelData);
                }
            }

            /* Table model */
            DefaultTableModel tableModel = new STableModel();

            /** Columns (Date + log levels) */
            tableModel.addColumn(main.getChartProperties().getProperty("loglevels_col01"));
            Set<String> keys = dataset.keySet();
            for (String key : keys) {
                tableModel.addColumn(key);
            }

            /** Fill in data matrix */
            Set<Date> dates = dataTableMap.keySet();
            int col = 1;
            for (Date d : dates) {
                Object[] rowData = new Object[dataset.size() + 1];
                rowData[0] = d;
                Map<String, Integer> dLevelsMap = dataTableMap.get(d);
                for (String dLevel : levels) {
                    Integer value = dLevelsMap.get(dLevel);
                    if (value == null) {
                        rowData[col++] = new Integer(0);
                    } else {
                        rowData[col++] = value;
                    }
                }
                col = 1;
                tableModel.addRow(rowData);
            }

            JTable dataTable = new JTable(tableModel);
            dataTable.setFillsViewportHeight(true);
            dataTable.setAutoCreateRowSorter(true);
            JScrollPane scrollPane = new JScrollPane(dataTable);
            tabbedPane.add("Data", scrollPane);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Generate a data table
     */
    private void drawAllLevelsTable() {
        /* Table model */
        DefaultTableModel tableModel = new STableModel();

        /** Columns */
        tableModel.addColumn(main.getChartProperties().getProperty("loglevels_col02"));
        tableModel.addColumn(main.getChartProperties().getProperty("loglevels_col03"));

        /** Fill in data matrix */
        Set<String> keys = levelsDataset.keySet();
        for (String key : keys) {
            Object[] rowData = new Object[2];
            rowData[0] = key;
            rowData[1] = levelsDataset.get(key);
            tableModel.addRow(rowData);
        }

        JTable dataTable = new JTable(tableModel);
        dataTable.setFillsViewportHeight(true);
        dataTable.setAutoCreateRowSorter(true);
        JScrollPane scrollPane = new JScrollPane(dataTable);
        totalTabs.add("Data", scrollPane);
    }

    /**
     * @see java.awt.event.ActionListener#actionPerformed(java.awt.event.ActionEvent)
     */
    @Override
    public void actionPerformed(ActionEvent eArg) {
        Object source = eArg.getSource();
        /** Buttons */
        if (source instanceof JStatsButton) {
            String action = ((JStatsButton) source).getCustomAction();
            /** Submit new dates */
            if ("submitDates".equals(action)) {
                new UpdateThread().start();
            }
            /** Reset levels filter */
            else if ("resetLevels".equals(action)) {
                /** Include all levels */
                main.getStatisticsSQLService().initLogLevels();
                this.setOperation("resetLevels");
                new UpdateThread().start();
                /* Wait until thread has finished */
                try {
                    synchronized (this) {
                        this.wait();
                    }
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                /** Add levels combo to toolbar */
                String[] levels = new String[0];
                levels = dataset.keySet().toArray(levels);
                updateLevelsCombo(levels);
            }
        }
        /** JComboBox */
        else if (source instanceof JComboBox) {
            String selectedLevel = (String) ((JComboBox) source).getSelectedItem();
            int selectedIndex = ((JComboBox) source).getSelectedIndex();
            if (selectedIndex > 0) {
                ((DefaultComboBoxModel) ((JComboBox) source).getModel()).removeElementAt(selectedIndex);
                LogLevels removedLevel = main.getStatisticsSQLService().getLevel(selectedLevel);
                main.getStatisticsSQLService().setSkipLogLevels(removedLevel);
                new UpdateThread().start();
            }
        }
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
        createDataset();

        if (dataset != null) {
            /** Update levels combo */
            String[] levels = new String[0];
            levels = dataset.keySet().toArray(levels);
            if (levelsCombo != null) {
                updateLevelsCombo(levels);
            } else {
                addLevelsCombo(levels);
            }

            /** Redraw panels */
            drawTimeSeriePanel();
            drawBarSeriePanel();
            drawDataTable();
            tabbedPane.addTab(main.getProperties().getProperty("totallevelsTab"), null);

            /** Sub tab */
            if (totalTabs.getComponentCount() != 0) {
                totalTabs.removeAll();
            }
            drawAllLevelsBarSeriePanel();
            drawAllLevelsPiePanel();
            drawAllLevelsTable();
            tabbedPane.setComponentAt(tabbedPane.getTabCount() - 1, totalTabs);
        }

        /** Remove wait panel */
        tabbedPane.remove(0);

        /** Notify if needed */
        String curOper = this.getOperation();
        if ("resetLevels".equals(curOper)) {
            synchronized (this) {
                this.notifyAll();
            }
        }
    }

    /**
     * @see javax.swing.event.ChangeListener#stateChanged(javax.swing.event.ChangeEvent)
     */
    @Override
    public void stateChanged(ChangeEvent evtArg) {
        Object source = evtArg.getSource();
        if (source instanceof JTabbedPane) {
            int tabIndex = ((JTabbedPane) source).getSelectedIndex();
            if (tabIndex > 0) {
                Component tabPanel = tabbedPane.getComponentAt(tabIndex);
                if (tabPanel == null) {
                    drawAllLevelsBarSeriePanel();
                    drawAllLevelsPiePanel();
                    drawAllLevelsTable();
                    tabbedPane.setComponentAt(tabIndex, totalTabs);
                }
            }
        }
    }
}
