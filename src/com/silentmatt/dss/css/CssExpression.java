package com.silentmatt.dss.css;

import java.util.ArrayList;
import java.util.List;

/**
 * Represents the value part of a declaration (right-hand side).
 *
 * An expression is a list of {@link Term}s, separated by spaces, commas, or slashes.
 *
 * @author Matthew Crumley
 */
public class CssExpression {
    private final List<CssTerm> terms = new ArrayList<>();

    public CssExpression() {
    }

    public CssExpression(CssTerm term) {
        terms.add(term);
    }

    /**
     * Gets the child terms of the expression.
     *
     * @return The Terms contained in the expression
     */
    public List<CssTerm> getTerms() {
        return terms;
    }

    /**
     * Gets the expression as a String.
     *
     * @return A string of the form "term [&lt;separator&gt; term]*".
     */
    @Override
    public String toString() {
        return toString(false);
    }

    public String toString(boolean compact) {
        StringBuilder txt = new StringBuilder();
        boolean first = true;
        for (CssTerm t : terms) {
            if (first) {
                first = false;
            } else {
                if (t.getSeperator() == null) {
                    txt.append(" ");
                }
                else {
                    txt.append(t.getSeperator());
                    if (!compact && t.getSeperator().equals(',')) {
                        txt.append(" ");
                    }
                }
            }
            txt.append(t.toString(compact));
        }
        return txt.toString();
    }
}
