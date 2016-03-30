package net.luisalbertogh.log4jstats.panels;

import java.awt.BorderLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.table.DefaultTableModel;

import net.luisalbertogh.log4jstats.Log4JStats;
import net.luisalbertogh.log4jstats.services.sqlite.StatisticsSQLService;
import net.luisalbertogh.log4jstats.utils.JStatsButton;
import net.luisalbertogh.log4jstats.utils.STableModel;

import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;

/**
 * Draw a panel with information about the whole set of registered log entries.
 * 
 * @author lagarcia
 */
public class AllEventsPanel extends StatisticsPanel {

    /**
     * Serial version UID
     */
    private static final long serialVersionUID = 1L;

    /** Dataset */
    private Map<String, Integer> dataset;

    /**
     * Default constructor
     * 
     * @param main - Reference to main Log4JStats class
     */
    public AllEventsPanel(Log4JStats main) {
        super(main);
        try {
            /** 1. Get data */
            createDataset();
            /** 2. Plot as time-serie */
            drawTimeSeriePanel();
            /** 3. Plot as bar chart */
            drawBarSeriePanel();
            /** 4. Draw the data table */
            drawDataTable();
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
            dataset = ssql.getNEventsPerDate(main.getDateOnlyPattern(), getDbPath(), null);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /** Render time-serie panel content */
    private void drawTimeSeriePanel() {
        try {
            Map<String, String> chartDetails = new HashMap<String, String>();
            chartDetails.put("serieTitle", main.getChartProperties().getProperty("allevents_serieTitle"));
            chartDetails.put("datePattern", main.getDatePattern().split(" ")[0]);
            chartDetails.put("xLabel", main.getChartProperties().getProperty("allevents_xLabel"));
            chartDetails.put("yLabel", main.getChartProperties().getProperty("allevents_yLabel"));
            chartDetails.put("chartTitle", main.getChartProperties().getProperty("allevents_chartTitle"));
            JFreeChart chart = sc.getSingleTimeserie(dataset, chartDetails);
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
            chartDetails.put("serieTitle", main.getChartProperties().getProperty("allevents_serieTitle"));
            chartDetails.put("datePattern", main.getDatePattern().split(" ")[0]);
            chartDetails.put("xLabel", main.getChartProperties().getProperty("allevents_xLabel"));
            chartDetails.put("yLabel", main.getChartProperties().getProperty("allevents_yLabel"));
            chartDetails.put("chartTitle", main.getChartProperties().getProperty("allevents_chartTitle"));
            JFreeChart chart = sc.getTotalsBarChart(dataset, chartDetails);
            ChartPanel cPanel = new ChartPanel(chart);
            tabbedPane.add("Bar-serie", cPanel);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Generate a data table
     */
    private void drawDataTable() {
        try {
            /* Date formatter */
            SimpleDateFormat sdf = new SimpleDateFormat(main.getDatePattern().split(" ")[0]);

            /* Table model */
            DefaultTableModel tableModel = new STableModel();

            /** Columns */
            tableModel.addColumn(main.getChartProperties().getProperty("allevents_col01"));
            tableModel.addColumn(main.getChartProperties().getProperty("allevents_col02"));

            /** Fill in data matrix */
            Set<String> keys = dataset.keySet();
            for (String key : keys) {
                Object[] rowData = new Object[2];
                rowData[0] = sdf.parse(key);
                rowData[1] = dataset.get(key);
                tableModel.addRow(rowData);
            }

            JTable dataTable = new JTable(tableModel);
            dataTable.setFillsViewportHeight(true);
            dataTable.setAutoCreateRowSorter(true);
            JScrollPane scrollPane = new JScrollPane(dataTable);
            tabbedPane.add("Data", scrollPane);
        } catch (ParseException pe) {
            pe.printStackTrace();
        }
    }

    /** @see ActionListener#actionPerformed */
    @Override
    public void actionPerformed(ActionEvent eArg) {
        Object source = eArg.getSource();
        if (source instanceof JStatsButton) {
            String action = ((JStatsButton) source).getCustomAction();
            /** Submit new dates */
            if ("submitDates".equals(action)) {
                /** Remove previous charts */
                tabbedPane.removeAll();
                tabbedPane.add(waitPanel);
                /** Create new dataset with new dates and plot new charts and table */
                createDataset();
                if (dataset != null) {
                    drawTimeSeriePanel();
                    drawBarSeriePanel();
                    drawDataTable();
                }
                /** Remove wait panel */
                tabbedPane.remove(0);
            }
        }
    }
}
