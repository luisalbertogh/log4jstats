/**
 * © 2009-2012 Tex Toll Services, LLC
 */
package net.luisalbertogh.log4jstats.utils;

import java.awt.Color;

/**
 * Some RGB colors.
 * 
 * @author lagarcia
 */
public enum LogColors {
    /** A magenta color. */
    RED(249, 99, 136),
    /** A dark yellow. */
    YELLOW(241, 237, 126),
    /** A light green. */
    GREEN(169, 242, 206);

    /** RGB color code. */
    private int r, g, b;

    /** Default constructor. */
    LogColors(int r, int g, int b) {
        this.r = r;
        this.g = g;
        this.b = b;
    }

    /**
     * Get R.
     * 
     * @return
     */
    public int getR() {
        return this.r;
    }

    /**
     * Get G.
     * 
     * @return
     */
    public int getG() {
        return this.g;
    }

    /**
     * Get B.
     * 
     * @return
     */
    public int getB() {
        return this.b;
    }

    /**
     * Return a color instance.
     * 
     * @return
     */
    public Color getColor() {
        return new Color(r, g, b);
    }
}
