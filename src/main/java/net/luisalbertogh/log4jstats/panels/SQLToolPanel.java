/**
 * © 2009-2012 Tex Toll Services, LLC
 */
package net.luisalbertogh.log4jstats.panels;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.io.File;
import java.util.Date;

import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JComboBox;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.border.TitledBorder;

import net.luisalbertogh.log4jstats.Log4JStats;
import net.luisalbertogh.log4jstats.utils.JStatsButton;
import net.luisalbertogh.sqlite4java.Sqlite4JavaTool;
import net.luisalbertogh.sqlite4java.Sqlite4JavaTool.TimeDivision;

/**
 * This class implements a panel for the SQL tools.
 * 
 * @author lagarcia
 */
public class SQLToolPanel extends StatisticsPanel {

    /**
     * File chooser.
     */
    private JFileChooser fc = new JFileChooser();

    /**
     * Dump databases panel.
     */
    private JPanel dumpDBPanel = new JPanel();

    /**
     * Combos.
     */
    private JComboBox timeDivision;

    /**
     * Text fields.
     */
    private JTextField logpath, dbpath;

    /**
     * Output area.
     */
    private JTextArea output;

    /** Progress bar */
    private JProgressBar progressBar;

    /**
     * Process launcher.
     */
    private Launcher launcher;

    /**
     * Launcher operations.
     * 
     * @author lagarcia
     */
    public enum Operation {
        /**
         * FILE TO DB.
         */
        FILE, /**
         * HTTP SERVICE TO DB.
         */
        HTTP;
    }

    /**
     * Constructor.
     * 
     * @param mainArg
     */
    public SQLToolPanel(Log4JStats mainArg) {
        super(mainArg);

        /* Add options panel */
        createDumpDBPanel();

        /* Add operation buttons */
        JPanel operations = new JPanel();
        operations.setBorder(new TitledBorder(main.getProperties().getProperty("sqltools")));
        operations.setLayout(new BoxLayout(operations, BoxLayout.Y_AXIS));

        JStatsButton dumpDB = new JStatsButton(main.getProperties().getProperty("dumpdb"));
        dumpDB.setCustomAction("dumpdb");
        dumpDB.addActionListener(this);
        operations.add(dumpDB);

        toolbar.remove(1);
        toolbar.remove(1);
        /* Add operations panel */
        toolbar.add(operations);
        /* Add dump databases panel */
        JPanel centerPanel = new JPanel();
        centerPanel.setLayout(new BorderLayout());
        centerPanel.add(dumpDBPanel, BorderLayout.NORTH);
        /* Output area */
        output = new JTextArea();
        output.setEditable(false);
        output.setBackground(Color.GRAY);
        output.setForeground(Color.GREEN);
        output.setText(">_");
        JScrollPane scrollArea = new JScrollPane(output);
        centerPanel.add(scrollArea, BorderLayout.CENTER);

        /* Progress bar */
        progressBar = new JProgressBar();
        centerPanel.add(progressBar, BorderLayout.SOUTH);

        add(centerPanel);
    }

    /**
     * Create the dump database panel.
     */
    private void createDumpDBPanel() {
        dumpDBPanel.setBorder(new TitledBorder(main.getProperties().getProperty("dumpdb")));
        dumpDBPanel.setLayout(new BoxLayout(dumpDBPanel, BoxLayout.X_AXIS));
        dumpDBPanel.setVisible(false);

        /* Labels */
        JPanel labels = new JPanel();
        labels.setLayout(new BoxLayout(labels, BoxLayout.Y_AXIS));
        labels.add(new JLabel(main.getProperties().getProperty("logfiles")));
        labels.add(Box.createHorizontalStrut(1));
        labels.add(new JLabel(main.getProperties().getProperty("dbfiles")));
        labels.add(Box.createHorizontalStrut(1));
        labels.add(new JLabel(main.getProperties().getProperty("timedivisionlabel")));

        /* Fields */
        JPanel fields = new JPanel();
        fields.setLayout(new BoxLayout(fields, BoxLayout.Y_AXIS));
        logpath = new JTextField(50);
        logpath.setMaximumSize(new Dimension(300, 50));
        dbpath = new JTextField(50);
        dbpath.setMaximumSize(new Dimension(300, 50));
        timeDivision = new JComboBox(Sqlite4JavaTool.getTimeDivisions());
        timeDivision.setMaximumSize(new Dimension(300, 50));
        fields.add(logpath);
        labels.add(Box.createHorizontalStrut(1));
        fields.add(dbpath);
        labels.add(Box.createHorizontalStrut(1));
        fields.add(timeDivision);

        /* Buttons */
        JPanel buttons = new JPanel();
        buttons.setLayout(new BoxLayout(buttons, BoxLayout.Y_AXIS));
        JStatsButton openDialog = new JStatsButton("...", "logfiles");
        openDialog.addActionListener(this);
        JStatsButton openDialog2 = new JStatsButton("...", "dbfiles");
        openDialog2.addActionListener(this);
        buttons.add(openDialog);
        buttons.add(openDialog2);
        buttons.add(Box.createHorizontalStrut(10));

        /* Controls */
        JPanel controls = new JPanel();
        controls.setLayout(new BoxLayout(controls, BoxLayout.Y_AXIS));
        JStatsButton execBt = new JStatsButton(main.getProperties().getProperty("go"), "exec_dumpdb");
        execBt.setToolTipText(main.getProperties().getProperty("exec"));
        execBt.addActionListener(this);
        JStatsButton cancelBt = new JStatsButton(main.getProperties().getProperty("cancel"), "cancel_dumpdb");
        cancelBt.addActionListener(this);
        controls.add(execBt);
        controls.add(cancelBt);
        controls.add(Box.createHorizontalStrut(10));

        dumpDBPanel.add(labels);
        dumpDBPanel.add(fields);
        dumpDBPanel.add(buttons);
        dumpDBPanel.add(controls);
    }

    /**
     * @see java.awt.event.ActionListener#actionPerformed(java.awt.event.ActionEvent)
     */
    @Override
    public void actionPerformed(ActionEvent evtArg) {
        Object source = evtArg.getSource();
        if (source instanceof JStatsButton) {
            String action = ((JStatsButton) source).getCustomAction();
            /* Dump databases */
            if ("dumpdb".equals(action)) {
                if (!dumpDBPanel.isVisible()) {
                    dumpDBPanel.setVisible(true);
                } else {
                    dumpDBPanel.setVisible(false);
                }
            }
            /* Select database files */
            else if ("logfiles".equals(action) || "dbfiles".equals(action)) {
                /* Select only folders */
                fc.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
                fc.setAcceptAllFileFilterUsed(false);
                int ret = fc.showOpenDialog(this);
                if (ret == JFileChooser.APPROVE_OPTION) {
                    File f = fc.getSelectedFile();
                    if ("logfiles".equals(action)) {
                        logpath.setText(f.getAbsolutePath());
                    } else {
                        dbpath.setText(f.getAbsolutePath());
                    }
                }
            }
            /* Exec DB dump */
            else if ("exec_dumpdb".equals(action)) {
                Date initDate = dateFilter.getInitDate();
                Date endDate = dateFilter.getEndDate();
                String logDir = logpath.getText();
                String dbDir = null;
                if (!"".equals(dbpath.getText())) {
                    dbDir = dbpath.getText() + System.getProperty("file.separator")
                            + main.getProperties().getProperty("sqlitebasename");
                }
                TimeDivision timeDivisionValue = TimeDivision.valueOf((String) timeDivision.getSelectedItem());

                launcher = new Launcher(initDate, endDate, timeDivisionValue, logDir, dbDir, output);
                launcher.start();
            }
            /* Cancel DB dump */
            else if ("cancel_dumpdb".equals(action)) {
                launcher.cancelProcess();
            }
        }
    }

    /**
     * This class can launch processes.
     * 
     * @author lagarcia
     */
    class Launcher extends Thread {

        /**
         * Dates.
         */
        private Date from, to;

        /**
         * Time division.
         */
        private TimeDivision division;

        /**
         * Log and DB folders.
         */
        private String logDir, dbDir;

        /** The launcher operation */
        private Operation operation;

        /**
         * Output area
         */
        private JTextArea output;

        /** SQLite tool */
        private Sqlite4JavaTool sqltool;

        /**
         * Constructor.
         * 
         * @param from
         * @param to
         * @param division
         * @param logDir
         * @param dbDir
         * @param output
         */
        public Launcher(Date from, Date to, TimeDivision division, String logDir, String dbDir, JTextArea output) {
            this.from = from;
            this.to = to;
            this.division = division;
            this.logDir = logDir;
            this.dbDir = dbDir;
            this.output = output;
        }

        @Override
        public void run() {
            sqltool = new Sqlite4JavaTool();
            /* Clean output */
            output.setText("");
            /* Set output */
            sqltool.setTextArea(output);

            /* Start progress bar */
            progressBar.setIndeterminate(true);

            if (logDir != null && !"".equals(logDir)) {
                output.append(main.getProperties().getProperty("filetodb"));
                sqltool.dumpFilesToDB(from, to, division, logDir, dbDir);
            } else {
                output.append(main.getProperties().getProperty("httptodb"));
                sqltool.dumpFilesToDBbyHTTP(from, to, division, dbDir);
            }

            /* Stop progress bar */
            progressBar.setIndeterminate(false);
        }

        /**
         * Cancel process launcher.
         */
        public void cancelProcess() {
            sqltool.setCancel(true);
        }

        /**
         * Set the launcher operation.
         * 
         * @param operation
         */
        public void setOperation(Operation operation) {
            this.operation = operation;
        }
    }
}
