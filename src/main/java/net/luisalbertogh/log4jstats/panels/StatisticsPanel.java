package net.luisalbertogh.log4jstats.panels;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.beans.PropertyVetoException;
import java.util.Calendar;
import java.util.Date;
import java.util.Map;
import java.util.Set;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.DefaultComboBoxModel;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.JTabbedPane;
import javax.swing.border.TitledBorder;

import net.luisalbertogh.log4jstats.Log4JStats;
import net.luisalbertogh.log4jstats.chart.StatisticsChart;
import net.luisalbertogh.log4jstats.services.SuperService;
import net.luisalbertogh.log4jstats.utils.AppEventItem;
import net.luisalbertogh.log4jstats.utils.JStatsButton;
import net.luisalbertogh.log4jstats.utils.LogLevels;

import com.michaelbaranov.microba.calendar.DatePicker;

/**
 * Generic abstract chart panel for Statistics.
 * 
 * @author lagarcia
 */
public abstract class StatisticsPanel extends JPanel implements ActionListener {

    /**
     * Serial version UID.
     */
    private static final long serialVersionUID = 1L;

    /**
     * Databases files path.
     */
    protected String dbPath;

    /** Parent frame and main class */
    protected Log4JStats main;

    /** Tabbed panel */
    protected JTabbedPane tabbedPane;

    /** Statistics chart renderer */
    protected StatisticsChart sc;

    /** Toolbar */
    protected JPanel toolbar;

    /** Date filter */
    protected DateFilter dateFilter;

    /** Log levels combo filter */
    protected JComboBox levelsCombo;

    /** App events combo filter */
    protected JComboBox appEventsCombo;

    /** Waiting panel & progress bar */
    protected JPanel waitPanel;
    protected JProgressBar waitBar;

    /** Current operation. */
    protected String operation;

    /**
     * Default constructor
     * 
     * @param main - Main app class reference
     */
    protected StatisticsPanel(Log4JStats main) {
        this.main = main;
        sc = main.getStatisticsChart();
        setLayout(new BorderLayout());
        tabbedPane = new JTabbedPane();

        /** DB file path */
        dbPath = main.getProperties().getProperty("sqlitebasepath") + System.getProperty("file.separator")
                + main.getProperties().getProperty("sqlitebasename");

        /** Toolbar */
        toolbar = new JPanel();
        GridLayout gridLay = new GridLayout(1, 3);
        toolbar.setLayout(gridLay);
        /** Date filter */
        dateFilter = new DateFilter(this);
        dateFilter.setBorder(new TitledBorder(main.getProperties().getProperty("dates")));
        toolbar.add(dateFilter);
        toolbar.add(new JPanel());
        toolbar.add(new JPanel());

        /** Wait panel & progress bar */
        waitBar = new JProgressBar();
        waitBar.setIndeterminate(true);
        waitBar.setPreferredSize(new Dimension(600, 50));
        JPanel barPanel = new JPanel();
        barPanel.add(waitBar);
        barPanel.setBorder(BorderFactory.createEmptyBorder(150, 0, 0, 0));
        waitPanel = new JPanel();
        waitPanel.add(barPanel);

        add(toolbar, BorderLayout.NORTH);
    }

    /**
     * Add combo for levels filter to toolbar.
     */
    protected void addLevelsCombo(String[] data) {
        DefaultComboBoxModel model = new DefaultComboBoxModel();
        model.addElement(main.getProperties().getProperty("comboRemove"));
        for (int i = 0; i < data.length; i++) {
            model.addElement(data[i]);
        }
        levelsCombo = new JComboBox(model);
        levelsCombo.setName("levelsCombo");
        levelsCombo.setPreferredSize(new Dimension(200, 30));
        levelsCombo.addActionListener(this);

        JPanel comboPanel = new JPanel();
        comboPanel.add(levelsCombo);
        comboPanel.setBorder(new TitledBorder(main.getProperties().getProperty("levels")));
        JStatsButton resetBt = new JStatsButton(main.getProperties().getProperty("resetBt"));
        resetBt.setCustomAction("resetLevels");
        resetBt.addActionListener(this);
        comboPanel.add(resetBt);
        toolbar.remove(1);
        toolbar.add(comboPanel);
    }

    /**
     * Add combo for levels filter to toolbar.
     */
    protected void addAppEventsCombo(Map<String, String> data) {
        DefaultComboBoxModel model = new DefaultComboBoxModel();
        model.addElement(main.getProperties().getProperty("comboRemove"));
        Set<String> eventNames = data.keySet();
        for (String eventName : eventNames) {
            AppEventItem item = new AppEventItem(eventName, data.get(eventName));
            model.addElement(item);
        }
        appEventsCombo = new JComboBox(model);
        appEventsCombo.setName("appEventsCombo");
        appEventsCombo.setPreferredSize(new Dimension(200, 30));
        appEventsCombo.addActionListener(this);

        JPanel comboPanel = new JPanel();
        comboPanel.add(appEventsCombo);
        comboPanel.setBorder(new TitledBorder(main.getProperties().getProperty("events")));
        JStatsButton resetBt = new JStatsButton(main.getProperties().getProperty("resetBt"));
        resetBt.setCustomAction("resetEvents");
        resetBt.addActionListener(this);
        comboPanel.add(resetBt);
        toolbar.remove(2);
        toolbar.add(comboPanel);
    }

    /** Update levels filter with the passed data */
    protected void updateLevelsCombo(String[] data) {
        DefaultComboBoxModel model = (DefaultComboBoxModel) levelsCombo.getModel();
        model.removeAllElements();
        model.addElement(main.getProperties().getProperty("comboRemove"));
        for (int i = 0; i < data.length; i++) {
            String ele = data[i];
            model.addElement(ele);
        }
    }

    /** Update levels filter with the passed data */
    protected void updateAppEventsCombo(Map<String, String> data) {
        DefaultComboBoxModel model = (DefaultComboBoxModel) appEventsCombo.getModel();
        model.removeAllElements();
        model.addElement(main.getProperties().getProperty("comboRemove"));
        Set<String> eventNames = data.keySet();
        for (String eventName : eventNames) {
            AppEventItem item = new AppEventItem(eventName, data.get(eventName));
            model.addElement(item);
        }
    }

    /**
     * Update charts and table panels.
     */
    protected void updatePanel() {
    }

    /**
     * Date filter panel
     * 
     * @author lagarcia
     */
    class DateFilter extends JPanel {

        /** Initial and end dates */
        private DatePicker initDate, endDate;

        /** Buttons */
        private JStatsButton submit;

        /**
         * Constructor
         * 
         * @param parent
         */
        public DateFilter(StatisticsPanel parent) {
            super();

            setMaximumSize(new Dimension(200, 100));
            setLayout(new BoxLayout(this, BoxLayout.X_AXIS));
            Calendar now = Calendar.getInstance();

            /** Labels */
            JPanel labelPanel = new JPanel();
            labelPanel.setLayout(new BoxLayout(labelPanel, BoxLayout.Y_AXIS));
            labelPanel.add(new JLabel(main.getProperties().getProperty("from")));
            labelPanel.add(Box.createHorizontalStrut(1));
            labelPanel.add(new JLabel(main.getProperties().getProperty("to")));
            labelPanel.add(Box.createHorizontalStrut(1));

            /** Date pickers */
            JPanel datePickersPanel = new JPanel();
            datePickersPanel.setLayout(new BoxLayout(datePickersPanel, BoxLayout.Y_AXIS));
            initDate = new DatePicker();
            String numDays = main.getProperties().getProperty("defaultintervaltime");
            now.add(Calendar.DAY_OF_MONTH, -Integer.parseInt(numDays));
            try {
                initDate.setDate(now.getTime());
            } catch (PropertyVetoException e) {
                e.printStackTrace();
            }
            datePickersPanel.add(initDate);

            endDate = new DatePicker();
            now = Calendar.getInstance();
            now.add(Calendar.DAY_OF_MONTH, 1);
            try {
                endDate.setDate(now.getTime());
            } catch (PropertyVetoException e) {
                e.printStackTrace();
            }
            datePickersPanel.add(endDate);

            /** Buttons */
            submit = new JStatsButton(main.getProperties().getProperty("submit"));
            submit.setCustomAction("submitDates");
            submit.addActionListener(parent);

            add(labelPanel);
            add(datePickersPanel);
            add(submit);
        }

        /**
         * Get initial date.
         * 
         * @return
         */
        public Date getInitDate() {
            return initDate.getDate();
        }

        /**
         * Get end date.
         * 
         * @return
         */
        public Date getEndDate() {
            return endDate.getDate();
        }
    }

    /**
     * Class that implements a thread to update the tabbed panels in parallel.
     * 
     * @author lagarcia
     */
    class UpdateThread extends Thread {
        @Override
        public void run() {
            updatePanel();
        }
    }

    protected void performFullActions(ActionEvent eArg, SuperService ts) {
        Object source = eArg.getSource();
        /** Buttons */
        if (source instanceof JStatsButton) {
            String action = ((JStatsButton) source).getCustomAction();
            /** Submit new dates */
            if ("submitDates".equals(action)) {
                /* Update panels */
                new UpdateThread().start();
            }
            /** Reset levels filter */
            else if ("resetEvents".equals(action)) {
                /** Init app. events list */
                ts.initAppEvents(main.getChartProperties().getProperty("appevents"));
                this.setOperation("resetEvents");
                /* Update tabbed panels */
                new UpdateThread().start();

                /* Wait until thread has finished */
                try {
                    synchronized (this) {
                        this.wait();
                    }
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                /** Add app events combo */
                Map<String, String> appEventsMap = ts.getAppEvents();
                updateAppEventsCombo(appEventsMap);
            }
            /** Reset levels filter */
            else if ("resetLevels".equals(action)) {
                /** Init log levels */
                ts.initLogLevels();
                this.setOperation("resetLevels");
                /* Update tabbed panels */
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
                levels = ts.getLogLevels().toArray(levels);
                updateLevelsCombo(levels);
            }
        }
        /** JComboBox */
        else if (source instanceof JComboBox) {
            String name = ((JComboBox) source).getName();

            /** Remove app events */
            if ("appEventsCombo".equals(name)) {
                int selectedIndex = ((JComboBox) source).getSelectedIndex();
                if (selectedIndex > 0) {
                    AppEventItem selectedEvent = (AppEventItem) ((JComboBox) source).getSelectedItem();
                    ((DefaultComboBoxModel) ((JComboBox) source).getModel()).removeElementAt(selectedIndex);
                    ts.removeAppEvent(selectedEvent.getValue());
                    /* Update tabbed panels */
                    new UpdateThread().start();
                }
            }
            /** Remove levels */
            if ("levelsCombo".equals(name)) {
                String selectedLevel = (String) ((JComboBox) source).getSelectedItem();
                int selectedIndex = ((JComboBox) source).getSelectedIndex();
                if (selectedIndex > 0) {
                    ((DefaultComboBoxModel) ((JComboBox) source).getModel()).removeElementAt(selectedIndex);
                    LogLevels removedLevel = main.getStatisticsService().getLevel(selectedLevel);
                    ts.setSkipLogLevels(removedLevel);
                    /* Update tabbed panels */
                    new UpdateThread().start();
                }
            }
        }
    }

    /**
     * Set current operation name.
     * 
     * @param operation
     */
    public void setOperation(String operation) {
        this.operation = operation;
    }

    /**
     * Retrieev current operation name.
     * 
     * @return
     */
    public String getOperation() {
        return this.operation;
    }

    /**
     * Return DB files path.
     * 
     * @return
     */
    public String getDbPath() {
        return this.dbPath;
    }
}
