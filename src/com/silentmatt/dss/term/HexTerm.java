package com.silentmatt.dss.term;

import com.silentmatt.dss.Immutable;
import com.silentmatt.dss.color.Color;
import com.silentmatt.dss.color.RGBIColor;
import java.util.Locale;

/**
 * A hexadecimal value, usually a color.
 *
 * @author Matthew Crumley
 */
@Immutable
public final class HexTerm extends Term {
    /**
     * The hex digits.
     */
    private final String value;

    /**
     * Constructs a HexTerm from a String.
     *
     * @param value The hexadecimal digits. The first character should be a '#'
     * and the remaining characters <em>should</em> be valid hexadecimal digits.
     * This isn't enforced currently, but it may be in the future.
     */
    public HexTerm(String value) {
        super(null);
        this.value = value;
    }

    /**
     * Constructs a HexTerm from a String.
     *
     * @param value The hexadecimal digits. The first character should be a '#'
     * and the remaining characters <em>should</em> be valid hexadecimal digits.
     * This isn't enforced currently, but it may be in the future.
     */
    public HexTerm(Character sep, String value) {
        super(sep);
        this.value = value;
    }

    /**
     * Gets the hexadecimal digits.
     *
     * @return A String of the form "#digits"
     */
    public String getValue() {
        return value;
    }

    /**
     * Converts this term into a String.
     * If the value is a valid color, it may be converted to that color's
     * shortest form.
     *
     * @return The hex string
     */
    @Override
    public String toString() {
        if (isColor()) {
            return toColor().toString();
        }
        return value.toUpperCase(Locale.ENGLISH);
    }

    @Override
    public boolean isColor() {
        return toColor() != null;
    }

    @Override
    public Color toColor() {
        String hex = "000000";
        if ((value.length() == 7 || value.length() == 4) && value.charAt(0) == '#') {
            hex = value.substring(1);
        } else if (value.length() == 6 || value.length() == 3) {
            hex = value;
        }

        if (hex.length() == 3) {
            char red = hex.charAt(0);
            char green = hex.charAt(1);
            char blue = hex.charAt(2);

            hex = new String(new char[] { red, red, green, green, blue, blue});
        }

        try {
            int r = Integer.parseInt(hex.substring(0, 2), 16);
            int g = Integer.parseInt(hex.substring(2, 4), 16);
            int b = Integer.parseInt(hex.substring(4), 16);
            return new RGBIColor(r, g, b, 1.0);
        }
        catch (NumberFormatException ex) {
            return null;
        }
    }

    @Override
    public HexTerm withSeparator(Character separator) {
        return new HexTerm(separator, getValue());
    }
}
