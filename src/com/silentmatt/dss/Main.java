package com.silentmatt.dss;

import com.martiansoftware.jsap.*;
import com.martiansoftware.jsap.stringparsers.FileStringParser;
import com.martiansoftware.jsap.stringparsers.URLStringParser;
import com.silentmatt.dss.calc.CalculationException;
import com.silentmatt.dss.term.CalculationTerm;
import com.silentmatt.dss.term.FunctionTerm;
import com.silentmatt.dss.term.NumberTerm;
import com.silentmatt.dss.term.StringTerm;
import com.silentmatt.dss.term.Term;
import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.net.*;

public final class Main {
    private Main() {
    }

    private static void printUsage(JSAP jsap) {
        System.err.println("usage: java -jar dss.jar");
        System.err.println("       " + jsap.getUsage());
        System.err.println();
        System.err.println(jsap.getHelp());
    }

    public static void main(String[] args) {
        JSAP jsap = new JSAP();

        Switch debugFlag = new Switch("debug")
                .setLongFlag("debug");
        debugFlag.setHelp("Don't remove DSS directives from output");

        FlaggedOption outOpt = new FlaggedOption("out")
                .setStringParser(FileStringParser.getParser())
                .setRequired(false)
                .setAllowMultipleDeclarations(false)
                .setShortFlag('o');
        outOpt.setHelp("File to save outout to");

        UnflaggedOption urlOpt = new UnflaggedOption("url")
                .setStringParser(URLStringParser.getParser())
                .setDefault("")
                .setRequired(true);
        urlOpt.setHelp("Valid URL containing CSS2-stylesheet.");

        try {
            jsap.registerParameter(urlOpt);
            jsap.registerParameter(outOpt);
            jsap.registerParameter(debugFlag);
        } catch (JSAPException j) {
            System.err.println("Unexpected Error: Illegal JSAP parameter.");
            System.exit(2);
        }

        JSAPResult config = jsap.parse(args);
        if (!config.success()) {
            printUsage(jsap);
        }

        URL url = config.getURL("url");
        File out = config.getFile("out", null);

        if (out != null) {
            try {
                if (url.sameFile(out.toURI().toURL())) {
                    System.err.println("Input and output are the same file.");
                    System.exit(1);
                }
            } catch (MalformedURLException ex) {
                System.err.println("Invalid file: " + out.getPath());
                System.exit(1);
            }
        }

        ErrorReporter errors = new PrintStreamErrorReporter();

        if (url != null) {
            try {
                CSSDocument css = CSSDocument.parse(url, errors);
                if (css != null) {
                    DSSEvaluator.Options opts = new DSSEvaluator.Options(url);
                    opts.setErrors(errors);
                    opts.getFunctions().put("string", new Function() {
                        public Expression call(FunctionTerm function, EvaluationState state) {
                            return new StringTerm("\"" + function.getExpression() + "\"").toExpression();
                        }
                    });
                    Function mathFunction = new Function() {
                        public Expression call(FunctionTerm function, EvaluationState state) {
                            String name = function.getName();
                            Term term = function.getExpression().getTerms().get(0);
                            NumberTerm value;
                            if (term instanceof NumberTerm) {
                                value = (NumberTerm) term;
                            }
                            else if (term instanceof CalculationTerm) {
                                try {
                                    value = ((CalculationTerm) term).getCalculation().calculateValue(state).toTerm();
                                } catch (CalculationException ex) {
                                    return null;
                                }
                            }
                            else {
                                return null;
                            }
                            double x = value.getDoubleValue();
                            switch (value.getUnit()) {
                            case None:
                            case DEG:
                                x = Math.toRadians(x);
                                break;
                            case GRAD:
                                x = Math.toRadians(0.9 * x);
                                break;
                            case RAD:
                                break;
                            default:
                                return null;
                            }

                            double res;
                            if      (name.equals("sin")) { res = Math.sin(x); }
                            else if (name.equals("cos")) { res = Math.cos(x); }
                            else if (name.equals("tan")) { res = Math.tan(x); }
                            else                         { return function.toExpression(); }

                            return new NumberTerm(res).toExpression();
                        }
                    };
                    opts.getFunctions().put("sin", mathFunction);
                    opts.getFunctions().put("cos", mathFunction);
                    opts.getFunctions().put("tan", mathFunction);

                    new DSSEvaluator(opts).evaluate(css);

                    String cssString;
                    if (config.getBoolean("debug")) {
                        cssString = css.toString();
                    }
                    else {
                        cssString = css.toCssString();
                    }

                    if (out == null) {
                        System.out.println(cssString);
                    }
                    else {
                        PrintStream pout = new PrintStream(out);
                        pout.println(cssString);
                        pout.close();
                    }
                }
            } catch (MalformedURLException ex) {
                errors.SemErr("DSS: Invalid URL");
            } catch (IOException ex) {
                errors.SemErr("DSS: I/O error: " + ex.getMessage());
            }
        }
        else {
            System.err.println("Missing url parameter.");
            printUsage(jsap);
        }

        if (errors.getErrorCount() > 0) {
            System.exit(1);
        }
    }
}
