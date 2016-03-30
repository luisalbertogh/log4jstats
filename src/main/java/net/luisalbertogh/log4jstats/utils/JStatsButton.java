package net.luisalbertogh.log4jstats.utils;

import javax.swing.JButton;

/**
 * Implements a customized JButton.
 * 
 * @author lagarcia
 */
public class JStatsButton extends JButton {

    /** The action that fires */
    private String customAction;

    /**
     * Constructor
     * 
     * @param label
     */
    public JStatsButton(String label) {
        super(label);
    }

    /**
     * Constructor
     * 
     * @param label
     * @param action
     */
    public JStatsButton(String label, String action) {
        super(label);
        this.customAction = action;
    }

    /**
     * Return customAction attribute.
     * 
     * @return customAction - Attribute returned
     */
    public final String getCustomAction() {
        return customAction;
    }

    /**
     * Set attribute customAction.
     * 
     * @param customActionArg - Set value
     */
    public final void setCustomAction(String customActionArg) {
        customAction = customActionArg;
    }

}
