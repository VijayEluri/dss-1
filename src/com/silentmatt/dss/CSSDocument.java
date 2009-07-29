package com.silentmatt.dss;

import com.silentmatt.dss.parser.ErrorReporter;
import com.silentmatt.dss.parser.Parser;
import com.silentmatt.dss.parser.Scanner;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

/**
 *
 * @author Matthew Crumley
 */
public class CSSDocument {
    private String charset;
    private final List<Rule> rules = new ArrayList<Rule>();

    public static CSSDocument parse(String url, ErrorReporter errors) throws IOException {
        return parse(new URL(url), errors);
    }

    public static CSSDocument parse(URL url, ErrorReporter errors) throws IOException {
        return parse(url.openStream(), errors);
    }

    public static CSSDocument parse(InputStream input, ErrorReporter errors) throws IOException {
        Scanner scanner = new Scanner(input);
        return parse(scanner, errors);
    }

    public static CSSDocument parse(Scanner scanner, ErrorReporter errors) {
        Parser parser = new Parser(scanner);
        parser.errors = errors;
        parser.Parse();
        if (parser.errors.getErrorCount() > 0) {
            return null;
        }
        else {
            return parser.CSSDoc;
        }
    }

    public void addRule(Rule rule) {
        this.rules.add(rule);
    }

    public String getCharset() {
        return charset;
    }

    public void setCharset(String charset) {
        this.charset = charset;
    }

    public List<Rule> getRules() {
        return rules;
    }

    @Override
    public String toString() {
        StringBuilder txt = new StringBuilder();
        for (Rule r : rules) {
            txt.append(r).append("\n");
        }
        return txt.toString();
    }

    public String toCssString() {
        StringBuilder txt = new StringBuilder();
        for (Rule r : rules) {
            String ruleString = r.toCssString(0);
            if (ruleString.length() > 0) {
                txt.append(ruleString).append("\n");
            }
        }
        return txt.toString();
    }
}
