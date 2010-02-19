package com.silentmatt.dss.directive;

import com.silentmatt.dss.EvaluationState;
import com.silentmatt.dss.Declaration;
import com.silentmatt.dss.DeclarationBlock;
import com.silentmatt.dss.DeclarationList;
import com.silentmatt.dss.Expression;
import com.silentmatt.dss.Medium;
import com.silentmatt.dss.Rule;
import com.silentmatt.dss.css.CssGenericDirective;
import com.silentmatt.dss.css.CssMedium;
import com.silentmatt.dss.css.CssRule;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 *
 * @author Matthew Crumley
 */
public class GenericDirective extends Rule {
    private final DeclarationBlock declarations = new DeclarationBlock();
    private final List<Rule> rules = new ArrayList<Rule>();
    private final List<Medium> mediums = new ArrayList<Medium>();
    private String name;
    private Expression expression;

    public void addRule(Rule rule) {
        this.rules.add(rule);
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public List<Medium> getMediums() {
        return mediums;
    }

    public void addMedium(Medium medium) {
        this.mediums.add(medium);
    }

    public Expression getExpression() {
        return expression;
    }

    public void setExpression(Expression expression) {
        this.expression = expression;
    }

    public DeclarationList getDeclarations() {
        return declarations.getDeclarations();
    }

    public void addDeclaration(Declaration declaration) {
        this.declarations.addDeclaration(declaration);
    }

    @Override
    public String toString() {
        return toString(0);
    }

    public String toString(int nesting) {
        String start = Rule.getIndent(nesting);

        StringBuilder txt = new StringBuilder();

        txt.append(name);
        txt.append(" ");

        if (getExpression() != null) {
            txt.append(getExpression().toString());
            txt.append(" ");
        }

        boolean first = true;
        for (Medium m : mediums) {
            if (first) {
                first = false;
                txt.append(" ");
            } else {
                txt.append(", ");
            }
            txt.append(m.toString());
        }

        boolean hasBlock = (this.declarations.getDeclarations().size() > 0 || this.rules.size() > 0);

        if (!hasBlock) {
            txt.append(";");
            return txt.toString();
        }

        txt.append(" {\n").append(start);

        for (Rule dir : rules) {
            txt.append(dir.toString(nesting + 1));
            txt.append("\n");
        }

        first = true;
        for (Declaration dec : declarations.getDeclarations()) {
            if (first) { first = false; } else { txt.append(";"); }
            txt.append("\n\t" + start);
            txt.append(dec.toString());
        }

        txt.append("\n").append(start).append("}");

        return txt.toString();
    }

    public String toCssString(int nesting) {
        return toString(nesting);
    }

    @Override
    public CssRule evaluate(EvaluationState state, List<Rule> container) throws IOException {
        CssGenericDirective result = new CssGenericDirective();

        result.getDeclarations().addAll(declarations.evaluateStyle(state, true).getCssDeclarations(state));
        for (Rule r : rules) {
            result.addRule(r.evaluate(state, container));
        }
        for (Medium m : mediums) {
            result.addMedium(CssMedium.valueOf(m.toString()));
        }
        result.setName(name);
        result.setExpression(expression.evaluate(state, declarations.getDeclarations()));

        return result;
    }

}
