package com.silentmatt.dss.calc;

import java.util.Locale;

/**
 * A CSS unit.
 *
 * Units are distinct from {@link CalculationUnit}s, because not all CSS units
 * can be used in calculations currently.
 *
 * @todo Once all CSS units can be involved in calculations, the CalculationUnit
 * functionality should probably be moved to the Unit enum.
 *
 * @see CalculationUnit
 *
 * @author Matthew Crumley
 */
public enum Unit {
    /**
     * No units (scalar).
     */
    None,
    /**
     * Percentage length: %.
     */
    Percent,
    /**
     * font size: em.
     */
    EM,
    /**
     * x-height: ex.
     */
    EX,
    /**
     * Pixels: px.
     */
    PX,
    /**
     * Grads/gradians: gd.
     */
    GD,
    /**
     * "Root em", the font size of the root element: rem.
     */
    REM,
    /**
     * Viewport Width: vw.
     */
    VW,
    /**
     * Viewport Height: vh.
     */
    VH,
    /**
     * Viewport Minimum (smaller of vw and vh): vm.
     */
    VM,
    /**
     * Character width (width of "0", if present): ch.
     */
    CH,
    /**
     * Millimeters: mm.
     */
    MM,
    /**
     * Centimeters: cm.
     */
    CM,
    /**
     * Inches: in.
     */
    IN,
    /**
     * Points (postscript: 1/72 in): pt.
     */
    PT,
    /**
     * Picas: pc.
     */
    PC,
    /**
     * Degrees: deg.
     */
    DEG,
    /**
     * Grads/gradians: grad.
     */
    GRAD,
    /**
     * Radians: rad.
     */
    RAD,
    /**
     * Turns/revolutions/cycles: turn.
     */
    TURN,
    /**
     * Milliseconds: ms.
     */
    MS,
    /**
     * Seconds: s.
     */
    S,
    /**
     * Hertz: Hz.
     */
    Hz,
    /**
     * Kilohertz: kHz.
     */
    kHz,
    /**
     * Dots per inch: dpi.
     */
    DPI,
    /**
     * Dots per centimeter: dpcm.
     */
    DPCM,
    /**
     * Fractional Units: fr.
     */
    FR;

    /**
     * Gets the unit as a CSS-compatible String.
     *
     * Percent maps to "%", None maps to the empty string, and all other units
     * are their name in lower case (except Hz and kHz, which are returned in
     * their "normal" case).
     *
     * @return A CSS-compatible unit.
     */
    @Override
    public String toString() {
        if (this == Unit.Percent) {
            return "%";
        } else if (this == Unit.Hz || this == Unit.kHz) {
            return super.toString();
        } else if (this == Unit.None) {
            return "";
        }
        return super.toString().toLowerCase(Locale.ENGLISH);
    }

    /**
     * Parse a Unit using CSS syntax.
     *
     * Differences from {@link #valueOf} are that <code>Percent</code> is
     * represented by "%", <code>None</code> is represented by the empty string,
     * and case is ignored.
     *
     * @param unitString The String to parse.
     *
     * @return The Unit represented by <code>unitString</code>.
     *
     * @throws IllegalArgumentException The String cannot be parsed into a valid Unit.
     */
    public static Unit parse(String unitString) {
        for (Unit m : Unit.values()) {
            if (unitString.equalsIgnoreCase(m.toString())) {
                return m;
            }
        }
        throw new IllegalArgumentException(unitString);
    }
}
