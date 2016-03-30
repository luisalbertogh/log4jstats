package net.luisalbertogh.log4jstats.panels;

import java.awt.BorderLayout;
import java.awt.event.ActionEvent;
import java.text.SimpleDateFormat;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.table.DefaultTableModel;

import net.luisalbertogh.log4jstats.Log4JStats;
import net.luisalbertogh.log4jstats.services.sqlite.StatisticsSQLService;
import net.luisalbertogh.log4jstats.utils.STableModel;

import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;

/**
 * This class implements the application events panel.
 * 
 * @author lagarcia
 */
public class AppEventsPanel extends StatisticsPanel implements ChangeListener {

    /**
     * Serial version UID.
     */
    private static final long serialVersionUID = 1L;

    /** The dataset for events & levels */
    private Map<String, Object> dataset;

    /** Levels dataset */
    private Map<String, Map<String, Integer>> levelsDataset;

    /** Statistcis service */
    private StatisticsSQLService ssql;

    /**
     * Constructor
     * 
     * @param mainArg
     */
    public AppEventsPanel(Log4JStats mainArg) {
        super(mainArg);

        /** Statistics service reference */
        ssql = main.getStatisticsSQLService();

        /** Init app events */
        ssql.initAppEvents(main.getChartProperties().getProperty("appevents"));

        /** Create dataset */
        try {
            createDataset();

            /** Add app events combo */
            Map<String, String> appEventsMap = (Map<String, String>) dataset.get("appEvents");
            addAppEventsCombo(appEventsMap);

            /** Add levels combo to toolbar */
            String[] levels = new String[0];
            levels = levelsDataset.keySet().toArray(levels);
            addLevelsCombo(levels);

            /** Draw charts */
            drawTimeSeriePanel();
            drawBarSeriePanel();
            drawAppEventsPiePanel();
            drawAppEventsComboPanel();
            drawAppEventsTable();
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        /** Add tabbed panel change listener */
        tabbedPane.addChangeListener(this);

        /** Add tabbed panel */
        add(tabbedPane, BorderLayout.CENTER);
    }

    /**
     * Generate the dataset to plot.
     */
    private void createDataset() {
        try {
            /** Set service */
            ssql.setDatesInterval(super.dateFilter.getInitDate(), super.dateFilter.getEndDate());

            dataset = ssql.getAppEventsTableData(main.getDateOnlyPattern(), getDbPath(), null,
                    ssql.getAppEventsListAsString());
            levelsDataset = ssql.getNDiffEventsPerDate(main.getDateOnlyPattern(), getDbPath());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /** Render time-serie panel content */
    private void drawTimeSeriePanel() {
        try {
            Map<String, String> chartDetails = new HashMap<String, String>();
            chartDetails.put("datePattern", main.getDatePattern().split(" ")[0]);
            chartDetails.put("xLabel", main.getChartProperties().getProperty("appevents_xLabel"));
            chartDetails.put("yLabel", main.getChartProperties().getProperty("appevents_yLabel"));
            chartDetails.put("chartTitle", main.getChartProperties().getProperty("appevents_chartTitle"));
            JFreeChart chart = sc.getAppEventsTimeserie(dataset, chartDetails);
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
            chartDetails.put("xLabel", main.getChartProperties().getProperty("appevents_xLabel"));
            chartDetails.put("yLabel", main.getChartProperties().getProperty("appevents_yLabel"));
            chartDetails.put("chartTitle", main.getChartProperties().getProperty("appevents_chartTitle"));
            JFreeChart chart = sc.getAppEventsBarChart(dataset, chartDetails);

            ChartPanel cPanel = new ChartPanel(chart);
            tabbedPane.add("Bar-serie", cPanel);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /** Render pie-chart panel content */
    private void drawAppEventsPiePanel() {
        try {
            Map<String, String> chartDetails = new HashMap<String, String>();
            chartDetails.put("datePattern", main.getDatePattern().split(" ")[0]);
            chartDetails.put("xLabel", main.getChartProperties().getProperty("appevents_xLabel"));
            chartDetails.put("yLabel", main.getChartProperties().getProperty("appevents_yLabel"));
            chartDetails.put("chartTitle", main.getChartProperties().getProperty("appevents_chartTitle"));
            JFreeChart chart = sc.getAppEventsPieChart(dataset, chartDetails);
            ChartPanel cPanel = new ChartPanel(chart);
            tabbedPane.add("Pie-chart", cPanel);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /** Render combo-chart panel content */
    private void drawAppEventsComboPanel() {
        try {
            Map<String, String> chartDetails = new HashMap<String, String>();
            chartDetails.put("datePattern", main.getDatePattern().split(" ")[0]);
            chartDetails.put("xLabel", main.getChartProperties().getProperty("appevents_xLabel"));
            chartDetails.put("yLabel", main.getChartProperties().getProperty("appevents_yLabel"));
            chartDetails.put("chartTitle", main.getChartProperties().getProperty("appevents_chartTitle"));
            chartDetails.put("plotOrientation", "VERTICAL");

            JFreeChart chartBar = sc.getAppEventsComboChart(dataset, levelsDataset, chartDetails);
            ChartPanel cPanelBar = new ChartPanel(chartBar);
            tabbedPane.add(main.getProperties().getProperty("totallevelsTab"), cPanelBar);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /** Draw data table with events grouped by date and log level type */
    private void drawAppEventsTable() {
        try {
            /* Date formatter */
            SimpleDateFormat sdf = new SimpleDateFormat(main.getDatePattern().split(" ")[0]);

            /* Columns */
            DefaultTableModel tableModel = new STableModel();
            tableModel.addColumn(main.getChartProperties().getProperty("appevents_col01"));
            tableModel.addColumn(main.getChartProperties().getProperty("appevents_col02"));
            tableModel.addColumn(main.getChartProperties().getProperty("appevents_col03"));
            tableModel.addColumn(main.getChartProperties().getProperty("appevents_col04"));

            /* Fill in data matrix */
            Map<String, Object> eventData = (Map<String, Object>) dataset.get("dataset");
            Set<String> keys = eventData.keySet();
            for (String key : keys) {
                Map<String, Object> dataRow = (Map<String, Object>) eventData.get(key);
                Object[] rowData = new Object[4];
                rowData[0] = sdf.parse((String) dataRow.get("date"));
                rowData[1] = dataRow.get("log");
                rowData[2] = dataRow.get("eventName");
                rowData[3] = dataRow.get("counter");
                tableModel.addRow(rowData);
            }

            /* Add table */
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
            drawTimeSeriePanel();
            drawBarSeriePanel();
            drawAppEventsPiePanel();
            drawAppEventsComboPanel();
            drawAppEventsTable();

            /** Update app events combo */
            Map<String, String> appEventsMap = ssql.getAppEvents();
            if (appEventsCombo != null) {
                updateAppEventsCombo(appEventsMap);
            } else {
                addAppEventsCombo(appEventsMap);
            }

            /** Update levels combo to toolbar */
            String[] levels = new String[0];
            levels = levelsDataset.keySet().toArray(levels);
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
     * @see java.awt.event.ActionListener#actionPerformed(java.awt.event.ActionEvent)
     */
    @Override
    public void actionPerformed(ActionEvent eArg) {
        performFullActions(eArg, ssql);
    }

    /**
     * @see javax.swing.event.ChangeListener#stateChanged(javax.swing.event.ChangeEvent)
     */
    @Override
    public void stateChanged(ChangeEvent arg0Arg) {
    }
}
