package net.luisalbertogh.log4jstats.utils;

/**
 * This class implements an application event combo box item, with its label and value.
 * 
 * @author lagarcia
 */
public class AppEventItem {

    /** The label */
    private String label;

    /** The value */
    private String value;

    /**
     * The constructor
     * 
     * @param labelArg
     * @param valueArg
     */
    public AppEventItem(String labelArg, String valueArg) {
        this.label = labelArg;
        this.value = valueArg;
    }

    /**
     * Return label attribute.
     * 
     * @return label - Attribute returned
     */
    public final String getLabel() {
        return label;
    }

    /**
     * Set attribute label.
     * 
     * @param labelArg - Set value
     */
    public final void setLabel(String labelArg) {
        label = labelArg;
    }

    /**
     * Return value attribute.
     * 
     * @return value - Attribute returned
     */
    public final String getValue() {
        return value;
    }

    /**
     * Set attribute value.
     * 
     * @param valueArg - Set value
     */
    public final void setValue(String valueArg) {
        value = valueArg;
    }

    /**
     * @see Object#toString()
     */
    @Override
    public final String toString() {
        return label;
    }
}
