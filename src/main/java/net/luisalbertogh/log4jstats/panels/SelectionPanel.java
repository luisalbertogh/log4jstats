/**
 * © 2009-2012 Tex Toll Services, LLC
 */
package net.luisalbertogh.log4jstats.panels;

import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.text.SimpleDateFormat;
import java.util.Date;

import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.border.TitledBorder;

import net.luisalbertogh.log4jstats.Log4JStats;

/**
 * This class implements the main panel for the data table list selection pop-up dialog box.
 * 
 * @author lagarcia
 */
public class SelectionPanel extends JPanel implements ActionListener {

    /**
     * Serial Version UID.
     */
    private static final long serialVersionUID = 1L;

    /** Dialog reference. */
    private JDialog dialog;

    /**
     * Default constructor.
     * 
     * @param mainArg
     * @param data
     * @param dialog
     */
    public SelectionPanel(Log4JStats mainArg, Object[] data, JDialog dialog) {
        this.dialog = dialog;
        JPanel hPanel = new JPanel();
        hPanel.setLayout(new BoxLayout(hPanel, BoxLayout.X_AXIS));
        String title = mainArg.getProperties().getProperty("logEvent");

        /* Labels */
        JPanel labelsPanel = new JPanel();
        labelsPanel.setLayout(new BoxLayout(labelsPanel, BoxLayout.Y_AXIS));
        labelsPanel.add(new JLabel(mainArg.getChartProperties().getProperty("datatable_col01")));
        labelsPanel.add(Box.createVerticalStrut(15));
        labelsPanel.add(new JLabel(mainArg.getChartProperties().getProperty("datatable_col02")));
        labelsPanel.add(Box.createVerticalStrut(15));
        labelsPanel.add(new JLabel(mainArg.getChartProperties().getProperty("datatable_col04")));
        labelsPanel.add(Box.createVerticalStrut(15));
        labelsPanel.add(new JLabel(mainArg.getChartProperties().getProperty("datatable_col03")));
        labelsPanel.add(Box.createVerticalStrut(200));

        /* Fields */
        JPanel fieldsPanel = new JPanel();
        fieldsPanel.setLayout(new BoxLayout(fieldsPanel, BoxLayout.Y_AXIS));
        Date date = (Date) data[0];
        SimpleDateFormat sdf = new SimpleDateFormat(mainArg.getDatePattern());
        fieldsPanel.add(new FieldInfo(sdf.format(date)));
        fieldsPanel.add(new FieldInfo((String) data[1]));
        fieldsPanel.add(new FieldInfo((String) data[2]));
        JTextArea txtArea = new JTextArea((String) data[3]);
        txtArea.setEditable(false);
        txtArea.setPreferredSize(new Dimension(400, 200));
        txtArea.setLineWrap(true);
        fieldsPanel.add(txtArea);

        /* Close Button */
        JButton closeButton = new JButton(mainArg.getProperties().getProperty("close"));
        closeButton.addActionListener(this);
        fieldsPanel.add(closeButton);

        hPanel.add(labelsPanel);
        hPanel.add(Box.createHorizontalStrut(10));
        hPanel.add(fieldsPanel);

        hPanel.setBorder(new TitledBorder(title));

        add(hPanel);
    }

    /**
     * @see java.awt.event.ActionListener#actionPerformed(java.awt.event.ActionEvent)
     */
    @Override
    public void actionPerformed(ActionEvent evtArg) {
        Object source = evtArg.getSource();
        if (source instanceof JButton) {
            /* Close dialog */
            dialog.dispose();
        }
    }

    /**
     * Implements a custom JTextField.
     * 
     * @author lagarcia
     */
    class FieldInfo extends JTextField {
        /**
         * Default constructor.
         */
        public FieldInfo() {
            super();
            doCustom();
        }

        /**
         * Constructor with args.
         * 
         * @param text
         */
        public FieldInfo(String text) {
            super(text);
            doCustom();
        }

        /**
         * Customize the field.
         */
        private void doCustom() {
            this.setEditable(false);
        }
    }
}
