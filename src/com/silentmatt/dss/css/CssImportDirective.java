package com.silentmatt.dss.css;

import java.net.MalformedURLException;
import java.net.URL;

/**
 *
 * @author Matthew Crumley
 */
public class CssImportDirective extends CssExpressionDirective {
    private final CssMediaQuery medium;

    public CssImportDirective(CssTerm url, CssMediaQuery medium) {
        super(new CssExpression());
        getExpression().getTerms().add(url);
        this.medium = medium;
    }

    public CssMediaQuery getMedium() {
        return medium;
    }

    @Override
    public String toString() {
        return "@import " + getExpression() + " " + medium + ";";
    }

    public String getURLString() {
        return getExpression().getTerms().get(0).toString();
    }

    public URL getURL() throws MalformedURLException {
        return new URL(getURLString());
    }
}
