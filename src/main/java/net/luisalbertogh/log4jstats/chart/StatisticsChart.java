package net.luisalbertogh.log4jstats.chart;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.jfree.chart.ChartFactory;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.labels.StandardPieSectionLabelGenerator;
import org.jfree.chart.plot.PiePlot;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.renderer.category.LineAndShapeRenderer;
import org.jfree.chart.renderer.xy.XYItemRenderer;
import org.jfree.chart.renderer.xy.XYLineAndShapeRenderer;
import org.jfree.data.category.DefaultCategoryDataset;
import org.jfree.data.general.DefaultPieDataset;
import org.jfree.data.time.Day;
import org.jfree.data.time.TimeSeries;
import org.jfree.data.time.TimeSeriesCollection;

/**
 * This class implements the functionality for the chart generation.
 * 
 * @author lagarcia
 */
public class StatisticsChart {

    /**
     * Get a single time series with the given dataset.
     * 
     * @param dataset
     * @param chartDetails
     * @return JFreeChart
     * @throws Exception
     */
    public JFreeChart getSingleTimeserie(Map<String, Integer> dataset, Map<String, String> chartDetails)
            throws Exception {
        /** Create time series dataset */
        TimeSeries s = new TimeSeries(chartDetails.get("serieTitle"));

        /** Date format */
        SimpleDateFormat sdf = new SimpleDateFormat(chartDetails.get("datePattern"));

        /** Populate the serie */
        Set<String> dates = dataset.keySet();
        for (String date : dates) {
            Date d = sdf.parse(date);
            s.add(new Day(d), dataset.get(date));
        }

        TimeSeriesCollection collection = new TimeSeriesCollection();
        collection.addSeries(s);

        /** Create chart */
        JFreeChart chart = ChartFactory.createTimeSeriesChart(chartDetails.get("chartTitle"), // title
                chartDetails.get("xLabel"), // x-axis label
                chartDetails.get("yLabel"), // y-axis label
                collection, // data
                true, // create legend?
                true, // generate tooltips?
                false // generate URLs?
                );

        /** Customization */
        XYPlot plot = (XYPlot) chart.getPlot();
        XYItemRenderer r = plot.getRenderer();
        if (r instanceof XYLineAndShapeRenderer) {
            XYLineAndShapeRenderer renderer = (XYLineAndShapeRenderer) r;
            renderer.setSeriesShapesVisible(0, true);
            renderer.setSeriesShapesFilled(0, true);
        }

        return chart;
    }

    /**
     * Get different levels time series with the given dataset.
     * 
     * @param dataset
     * @param chartDetails
     * @return JFreeChart
     * @throws Exception
     */
    public JFreeChart getLevelsTimeserie(Map<String, Map<String, Integer>> dataset, Map<String, String> chartDetails)
            throws Exception {
        /** Iterate over the collection of datasets */
        TimeSeriesCollection collection = new TimeSeriesCollection();
        int numSeries = dataset.size();
        Set<String> levels = dataset.keySet();
        for (String level : levels) {
            /** Create time series dataset */
            TimeSeries s = new TimeSeries(level.toUpperCase());

            /** Date format */
            SimpleDateFormat sdf = new SimpleDateFormat(chartDetails.get("datePattern"));

            /** Populate the serie */
            Map<String, Integer> levelDataset = dataset.get(level);
            Set<String> dates = levelDataset.keySet();
            for (String date : dates) {
                Date d = sdf.parse(date);
                s.add(new Day(d), levelDataset.get(date));
            }

            collection.addSeries(s);
        }

        /** Create chart */
        JFreeChart chart = ChartFactory.createTimeSeriesChart(chartDetails.get("chartTitle"), // title
                chartDetails.get("xLabel"), // x-axis label
                chartDetails.get("yLabel"), // y-axis label
                collection, // data
                true, // create legend?
                true, // generate tooltips?
                false // generate URLs?
                );

        /** Customization */
        XYPlot plot = (XYPlot) chart.getPlot();
        XYItemRenderer r = plot.getRenderer();
        if (r instanceof XYLineAndShapeRenderer) {
            XYLineAndShapeRenderer renderer = (XYLineAndShapeRenderer) r;
            for (int i = 0; i < numSeries; i++) {
                renderer.setSeriesShapesVisible(i, true);
                renderer.setSeriesShapesFilled(i, true);
            }
        }

        return chart;
    }

    /**
     * Get total log levels bar chart separated by dates.
     * 
     * @param dataset
     * @param chartDetails
     * @return
     * @throws Exception
     */
    public JFreeChart getTotalsBarChart(Map<String, Integer> dataset, Map<String, String> chartDetails)
            throws Exception {

        /** Dataset */
        DefaultCategoryDataset categories = new DefaultCategoryDataset();

        /** Iterate over the data */
        /** Populate the serie */
        SimpleDateFormat sdfPlot = new SimpleDateFormat("dd-MMM");
        SimpleDateFormat sdf = new SimpleDateFormat(chartDetails.get("datePattern"));
        String[] dates = new String[0];
        dates = dataset.keySet().toArray(dates);
        for (int i = dates.length - 1; i >= 0; i--) {
            String dateStr = dates[i];
            Date d = sdf.parse(dateStr);
            categories.addValue(dataset.get(dateStr), chartDetails.get("serieTitle"), sdfPlot.format(d));
        }

        /** Create chart */
        JFreeChart chart = ChartFactory.createBarChart(chartDetails.get("chartTitle"), // title
                chartDetails.get("xLabel"), // x-axis label
                chartDetails.get("yLabel"), // y-axis label
                categories, // data
                PlotOrientation.VERTICAL, true, // create legend?
                true, // generate tooltips?
                false // generate URLs?
                );

        return chart;
    }

    /**
     * Get total log levels bar chart separated by log level.
     * 
     * @param dataset
     * @param chartDetails
     * @return
     * @throws Exception
     */
    public JFreeChart getDiffTotalsBarChart(Map<String, Integer> dataset, Map<String, String> chartDetails)
            throws Exception {

        /** Dataset */
        DefaultCategoryDataset categories = new DefaultCategoryDataset();

        /** Iterate over the data */
        /** Populate the serie */
        Set<String> levels = dataset.keySet();
        for (String level : levels) {
            categories.addValue(dataset.get(level), level, level);
        }

        /** Create chart */
        JFreeChart chart = ChartFactory.createBarChart(chartDetails.get("chartTitle"), // title
                chartDetails.get("xLabel"), // x-axis label
                chartDetails.get("yLabel"), // y-axis label
                categories, // data
                PlotOrientation.VERTICAL, true, // create legend?
                true, // generate tooltips?
                false // generate URLs?
                );

        return chart;
    }

    /**
     * Get different log levels bar chart separated by dates.
     * 
     * @param dataset
     * @param chartDetails
     * @return
     * @throws Exception
     */
    public JFreeChart getLevelsBarChart(Map<String, Map<String, Integer>> dataset, Map<String, String> chartDetails)
            throws Exception {

        /** Dataset */
        DefaultCategoryDataset categories = new DefaultCategoryDataset();

        /** Iterate over the data */
        Set<String> levels = dataset.keySet();
        for (String level : levels) {
            /** Populate the serie */
            Map<String, Integer> levelDataset = dataset.get(level);
            Set<String> dates = levelDataset.keySet();
            for (String date : dates) {
                categories.addValue(levelDataset.get(date), level, date);
            }
        }

        /** Create chart */
        JFreeChart chart = ChartFactory.createBarChart(chartDetails.get("chartTitle"), // title
                chartDetails.get("xLabel"), // x-axis label
                chartDetails.get("yLabel"), // y-axis label
                categories, // data
                PlotOrientation.HORIZONTAL, true, // create legend?
                true, // generate tooltips?
                false // generate URLs?
                );

        return chart;
    }

    /**
     * Get total log levels pie chart grouped by log level.
     * 
     * @param dataset
     * @param chartDetails
     * @return
     * @throws Exception
     */
    public JFreeChart getDiffTotalsPieChart(Map<String, Integer> dataset, Map<String, String> chartDetails)
            throws Exception {

        /** Dataset */
        DefaultPieDataset categories = new DefaultPieDataset();

        /** Iterate over the data */
        /** Populate the serie */
        Set<String> levels = dataset.keySet();
        for (String level : levels) {
            categories.setValue(level, dataset.get(level));
        }

        /** Create chart */
        JFreeChart chart = ChartFactory.createPieChart(chartDetails.get("chartTitle"), // title
                categories, // data
                true, // create legend?
                true, // generate tooltips?
                false // generate URLs?
                );

        /** Customize section labels */
        StandardPieSectionLabelGenerator labelGenerator = new StandardPieSectionLabelGenerator("{2}");
        PiePlot plot = (PiePlot) chart.getPlot();
        plot.setLabelGenerator(labelGenerator);

        return chart;
    }

    /**
     * Get different levels time series with the given dataset.
     * 
     * @param dataset
     * @param chartDetails
     * @return JFreeChart
     * @throws Exception
     */
    @SuppressWarnings("unchecked")
    public JFreeChart getAppEventsTimeserie(Map<String, Object> dataset, Map<String, String> chartDetails)
            throws Exception {

        /** Date transformation */
        SimpleDateFormat sdf = new SimpleDateFormat(chartDetails.get("datePattern"));

        /** Dataset with data */
        Map<String, Object> eventData = (Map<String, Object>) dataset.get("dataset");

        /** Map with time series for the events */
        Map<String, TimeSeries> timeSeries = new HashMap<String, TimeSeries>();

        /** Iterate over the app. events */
        Set<String> ids = eventData.keySet();
        for (String id : ids) {
            Map<String, Object> eventMap = (Map<String, Object>) eventData.get(id);
            String eventName = (String) eventMap.get("eventName");
            TimeSeries currentSerie = null;
            /** Get or create serie if not existing yet */
            if ((currentSerie = timeSeries.get(eventName)) == null) {
                currentSerie = new TimeSeries(eventName);
            }
            /** Date */
            String dateStr = (String) (eventMap).get("date");
            currentSerie.addOrUpdate(new Day(sdf.parse(dateStr)), (Integer) eventMap.get("counter"));

            /** Put new serie */
            if (timeSeries.get(eventName) == null) {
                timeSeries.put(eventName, currentSerie);
            }
        }

        /** Add series to collection */
        TimeSeriesCollection collection = new TimeSeriesCollection();
        Set<String> keys = timeSeries.keySet();
        for (String key : keys) {
            collection.addSeries(timeSeries.get(key));
        }

        /** Create chart */
        JFreeChart chart = ChartFactory.createTimeSeriesChart(chartDetails.get("chartTitle"), // title
                chartDetails.get("xLabel"), // x-axis label
                chartDetails.get("yLabel"), // y-axis label
                collection, // data
                true, // create legend?
                true, // generate tooltips?
                false // generate URLs?
                );

        /** Customization */
        int numSeries = timeSeries.size();
        XYPlot plot = (XYPlot) chart.getPlot();
        XYItemRenderer r = plot.getRenderer();
        if (r instanceof XYLineAndShapeRenderer) {
            XYLineAndShapeRenderer renderer = (XYLineAndShapeRenderer) r;
            for (int i = 0; i < numSeries; i++) {
                renderer.setSeriesShapesVisible(i, true);
                renderer.setSeriesShapesFilled(i, true);
            }
        }

        return chart;
    }

    /**
     * Get different application events bar chart separated by dates.
     * 
     * @param dataset
     * @param chartDetails
     * @return
     * @throws Exception
     */
    public JFreeChart getAppEventsBarChart(Map<String, Object> dataset, Map<String, String> chartDetails)
            throws Exception {

        /** Dataset with data */
        Map<String, Object> eventData = (Map<String, Object>) dataset.get("dataset");

        /** Dataset */
        DefaultCategoryDataset categories = new DefaultCategoryDataset();

        /** Iterate over the data */
        Set<String> ids = eventData.keySet();
        for (String id : ids) {
            /** Populate the serie */
            Map<String, Object> eventMap = (Map<String, Object>) eventData.get(id);
            categories.addValue((Integer) eventMap.get("counter"), (String) eventMap.get("eventName"),
                    (String) eventMap.get("date"));
        }

        /** Plot orientation */
        PlotOrientation plotOrientation = PlotOrientation.HORIZONTAL;
        String plotOrientationStr = chartDetails.get("plotOrientation");
        if (plotOrientationStr != null && plotOrientationStr.equals("VERTICAL")) {
            plotOrientation = PlotOrientation.VERTICAL;
        }

        /** Create chart */
        JFreeChart chart = ChartFactory.createBarChart(chartDetails.get("chartTitle"), // title
                chartDetails.get("xLabel"), // x-axis label
                chartDetails.get("yLabel"), // y-axis label
                categories, // data
                plotOrientation, true, // create legend?
                true, // generate tooltips?
                false // generate URLs?
                );

        return chart;
    }

    /**
     * Get total app events pie chart grouped by log level.
     * 
     * @param dataset
     * @param chartDetails
     * @return
     * @throws Exception
     */
    public JFreeChart getAppEventsPieChart(Map<String, Object> dataset, Map<String, String> chartDetails)
            throws Exception {

        /** Dataset with data */
        Map<String, Object> eventData = (Map<String, Object>) dataset.get("dataset");

        /** Dataset */
        DefaultPieDataset categories = new DefaultPieDataset();

        /** Iterate over the data */
        Set<String> ids = eventData.keySet();
        for (String id : ids) {
            /** Populate the serie */
            Map<String, Object> eventMap = (Map<String, Object>) eventData.get(id);
            categories.setValue((String) eventMap.get("eventName"), (Integer) eventMap.get("counter"));
        }

        /** Create chart */
        JFreeChart chart = ChartFactory.createPieChart(chartDetails.get("chartTitle"), // title
                categories, // data
                true, // create legend?
                true, // generate tooltips?
                false // generate URLs?
                );

        /** Customize section labels */
        StandardPieSectionLabelGenerator labelGenerator = new StandardPieSectionLabelGenerator("{2}");
        PiePlot plot = (PiePlot) chart.getPlot();
        plot.setLabelGenerator(labelGenerator);

        return chart;
    }

    /**
     * Get combo chart with app events and log levels combined.
     * 
     * @param dataset
     * @param levelsDataset
     * @param chartDetails
     * @return
     * @throws Exception
     */
    public JFreeChart getAppEventsComboChart(Map<String, Object> dataset,
            Map<String, Map<String, Integer>> levelsDataset, Map<String, String> chartDetails) throws Exception {

        /** Dataset with data */
        Map<String, Object> eventData = (Map<String, Object>) dataset.get("dataset");

        /** Dataset */
        DefaultCategoryDataset categories = new DefaultCategoryDataset();

        /** Iterate over the data */
        Set<String> ids = eventData.keySet();
        for (String id : ids) {
            /** Populate the serie */
            Map<String, Object> eventMap = (Map<String, Object>) eventData.get(id);
            categories.addValue((Integer) eventMap.get("counter"), (String) eventMap.get("eventName"),
                    (String) eventMap.get("date"));
        }

        /* Levels dataset */
        /** Iterate over the collection of datasets */
        DefaultCategoryDataset levelsCategories = new DefaultCategoryDataset();

        /** Iterate over the data */
        Set<String> levels = levelsDataset.keySet();
        for (String level : levels) {
            /** Populate the serie */
            Map<String, Integer> levelDataset = levelsDataset.get(level);
            Set<String> dates = levelDataset.keySet();
            for (String date : dates) {
                levelsCategories.addValue(levelDataset.get(date), level, date);
            }
        }

        /** Create chart */
        JFreeChart chart = ChartFactory.createBarChart(chartDetails.get("chartTitle"), // title
                chartDetails.get("xLabel"), // x-axis label
                chartDetails.get("yLabel"), // y-axis label
                categories, // data
                PlotOrientation.VERTICAL, true, // create legend?
                true, // generate tooltips?
                false // generate URLs?
                );

        /* Additional dataset (levels) */
        chart.getCategoryPlot().setDataset(1, levelsCategories);

        /* Renderer for the levels dataset */
        LineAndShapeRenderer renderer = new LineAndShapeRenderer();
        renderer.setSeriesShapesVisible(0, true);
        renderer.setSeriesShapesFilled(0, true);
        chart.getCategoryPlot().setRenderer(1, renderer);

        return chart;
    }
}
