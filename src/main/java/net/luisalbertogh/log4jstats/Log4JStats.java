package net.luisalbertogh.log4jstats;

import java.awt.Dimension;
import java.awt.image.BufferedImage;

import javax.imageio.ImageIO;
import javax.swing.BorderFactory;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.JTabbedPane;
import javax.swing.UIManager;
import javax.swing.UIManager.LookAndFeelInfo;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import net.luisalbertogh.log4jstats.chart.StatisticsChart;
import net.luisalbertogh.log4jstats.panels.AllEventsPanel;
import net.luisalbertogh.log4jstats.panels.AppEventsPanel;
import net.luisalbertogh.log4jstats.panels.DataTablePanel;
import net.luisalbertogh.log4jstats.panels.LogLevelsPanel;
import net.luisalbertogh.log4jstats.panels.SQLToolPanel;
import net.luisalbertogh.log4jstats.services.logfiles.StatisticsService;
import net.luisalbertogh.log4jstats.services.sqlite.StatisticsSQLService;
import net.luisalbertogh.log4jstats.utils.PropertiesLoader;
import net.luisalbertogh.log4jstats.utils.StatisticsUtils;

/**
 * Log4JStats main class.
 * 
 * @author lagarcia
 */
public class Log4JStats implements ChangeListener {

    /** The app properties */
    private PropertiesLoader properties = new PropertiesLoader();

    /** The chart properties */
    private PropertiesLoader chartProps = new PropertiesLoader();

    /** Datetime pattern from lo4j.xml file */
    private String datePattern;

    /** Date pattern */
    private String dateOnlyPattern;

    /** Statistics services */
    private StatisticsService ss;

    /** Statistics services */
    private StatisticsSQLService ssql;

    /** Statistics chart renderer */
    private StatisticsChart sc;

    /** Main frame */
    private JFrame frame;

    /** Tabbed pane */
    private JTabbedPane tabbedPane;

    /** Waiting panel & progress bar */
    protected JPanel waitPanel;
    protected JProgressBar waitBar;

    /**
     * Render main frame.
     * 
     * @param args
     */
    public static void main(String[] args) {
        new Log4JStats().run();
    }

    /**
     * Create the main frame.
     */
    public void run() {
        try {
            /** Set Nimbus L&F */
            for (LookAndFeelInfo info : UIManager.getInstalledLookAndFeels()) {
                if ("Nimbus".equals(info.getName())) {
                    UIManager.setLookAndFeel(info.getClassName());
                    break;
                }
            }

            /** Wait panel & progress bar */
            waitBar = new JProgressBar();
            waitBar.setIndeterminate(true);
            waitBar.setPreferredSize(new Dimension(600, 50));
            JPanel barPanel = new JPanel();
            barPanel.add(waitBar);
            barPanel.setBorder(BorderFactory.createEmptyBorder(150, 0, 0, 0));
            waitPanel = new JPanel();
            waitPanel.add(barPanel);

            /** Main frame */
            frame = new JFrame("Log4JStats");
            frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            frame.setResizable(true);

            /** Image icon */
            BufferedImage image = null;
            image = ImageIO.read(frame.getClass().getResource("/icons/chart-icon.png"));
            frame.setIconImage(image);

            /** Load generic properties */
            properties.load("/labels.properties");

            /** Load chart properties */
            chartProps.load("/charts.properties");

            /** Init Statistics services and chart renderer */
            ss = new StatisticsService(Integer.parseInt(properties.getProperty("defaultintervaltime")),
                    Integer.parseInt(properties.getProperty("maxnumberdates")));
            ssql = new StatisticsSQLService(Integer.parseInt(properties.getProperty("defaultintervaltime")),
                    Integer.parseInt(properties.getProperty("maxnumberdates")), properties.getProperty("timedivision"));
            sc = new StatisticsChart();

            /** Get log4j date pattern */
            String conversionPattern = StatisticsUtils.getConversionPattern(properties.getProperty("log4jfilepath"),
                    properties.getProperty("log4jlogger"), properties.getProperty("log4jappender"));
            datePattern = StatisticsUtils.getDatePattern(conversionPattern);
            dateOnlyPattern = datePattern.split(" ")[0];

            /** Add a tabbed panel */
            tabbedPane = new JTabbedPane();
            tabbedPane.addChangeListener(this);
            /** First panel */
            JPanel fPanel = new AppEventsPanel(this);
            tabbedPane.add(properties.getProperty("appEventsTab"), fPanel);
            /** Log levels panel tab */
            tabbedPane.add(properties.getProperty("logLevelsTab"), null);
            /** All events panel tab */
            tabbedPane.add(properties.getProperty("allEventsTab"), null);
            /** Data table tab */
            tabbedPane.add(properties.getProperty("dataTableTab"), null);
            /** SQL tools tab */
            tabbedPane.add(properties.getProperty("sqltools"), new SQLToolPanel(this));

            frame.add(tabbedPane);
            frame.setPreferredSize(new Dimension(Integer.parseInt(properties.getProperty("mainwidth")), Integer
                    .parseInt(properties.getProperty("mainheight"))));
            frame.pack();
            /** Set in middle of the screen */
            frame.setLocationRelativeTo(null);
            frame.setVisible(true);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Get a reference to the properties loader.
     * 
     * @return
     */
    public PropertiesLoader getProperties() {
        return properties;
    }

    /**
     * Get a reference to the chart properties loader.
     * 
     * @return
     */
    public PropertiesLoader getChartProperties() {
        return chartProps;
    }

    /**
     * Get date pattern.
     * 
     * @return
     */
    public String getDatePattern() {
        return datePattern;
    }

    /**
     * Get Statistics service reference.
     * 
     * @return
     */
    public StatisticsService getStatisticsService() {
        return ss;
    }

    /**
     * Get a reference to the main frame.
     * 
     * @return
     */
    public JFrame getFrame() {
        return this.frame;
    }

    /**
     * Get Statistics chart renderer reference.
     * 
     * @return
     */
    public StatisticsChart getStatisticsChart() {
        return sc;
    }

    /**
     * Determine whether the tabbed pane changed (for tab selection)
     */
    @Override
    public void stateChanged(ChangeEvent evtArg) {
        Object source = evtArg.getSource();
        if (source instanceof JTabbedPane) {
            final int tabIndex = ((JTabbedPane) source).getSelectedIndex();
            JPanel tabPanel = (JPanel) tabbedPane.getComponentAt(tabIndex);
            if (tabPanel == null) {
                tabbedPane.setComponentAt(tabIndex, waitPanel);
                UpdateTabThread ut = new UpdateTabThread(this);
                ut.setTabIndex(tabIndex);
                ut.start();
            }
        }
    }

    /**
     * Return ssql attribute.
     * 
     * @return ssql - Attribute returned
     */
    public final StatisticsSQLService getStatisticsSQLService() {
        return ssql;
    }

    /**
     * Set attribute ssql.
     * 
     * @param ssqlArg - Set value
     */
    public final void setStatisticsSQLService(StatisticsSQLService ssqlArg) {
        ssql = ssqlArg;
    }

    /**
     * Return dateOnlyPattern attribute.
     * 
     * @return dateOnlyPattern - Attribute returned
     */
    public final String getDateOnlyPattern() {
        return dateOnlyPattern;
    }

    /**
     * Set attribute dateOnlyPattern.
     * 
     * @param dateOnlyPatternArg - Set value
     */
    public final void setDateOnlyPattern(String dateOnlyPatternArg) {
        dateOnlyPattern = dateOnlyPatternArg;
    }

    /**
     * Class that implements a thread to update the tabbed panels in the background.
     * 
     * @author lagarcia
     */
    class UpdateTabThread extends Thread {
        /** Tab index for the main tabbed panel. */
        private int tabIndex;
        /** Reference to the main frame. */
        private Log4JStats mainFrame;

        /**
         * Default constructor.
         * 
         * @param mainFrame - Th reference to the main frame.
         */
        public UpdateTabThread(Log4JStats mainFrame) {
            this.mainFrame = mainFrame;
        }

        @Override
        public void run() {
            switch (tabIndex) {
                case 1: {
                    tabbedPane.setComponentAt(tabIndex, new LogLevelsPanel(mainFrame));
                    break;
                }
                case 2: {
                    tabbedPane.setComponentAt(tabIndex, new AllEventsPanel(mainFrame));
                    break;
                }
                case 3: {
                    tabbedPane.setComponentAt(tabIndex, new DataTablePanel(mainFrame));
                    break;
                }
            }
        }

        /**
         * Set tab index.
         * 
         * @param tabIndex - The tab index
         */
        public void setTabIndex(int tabIndex) {
            this.tabIndex = tabIndex;
        }
    }
}
