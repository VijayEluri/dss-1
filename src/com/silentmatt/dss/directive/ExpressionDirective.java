package com.silentmatt.dss.directive;

import com.silentmatt.dss.Expression;
import com.silentmatt.dss.Rule;

/**
 *
 * @author Matthew Crumley
 */
public abstract class ExpressionDirective extends Rule {
    private final Expression expression;

    public ExpressionDirective(Expression expression) {
        super();
        this.expression = expression;
    }

    public Expression getExpression() {
        return expression;
    }

    public String toString(int nesting) {
        return Rule.getIndent(nesting) + toString();
    }
}
