package com.silentmatt.dss.term;

import com.silentmatt.dss.DeclarationList;
import com.silentmatt.dss.EvaluationState;
import com.silentmatt.dss.Expression;
import com.silentmatt.dss.Immutable;
import com.silentmatt.dss.calc.CalcExpression;

/**
 * A "calc(...)" term.
 *
 * @author Matthew Crumley
 */
@Immutable
public class CalculationLiteralTerm extends Term {
    /**
     * The expression to evaluate.
     */
    private final CalcExpression calculation;

    /**
     * Constructs a CalculationLiteralTerm from an expression.
     *
     * @param calculation The expression to evaluate
     */
    public CalculationLiteralTerm(CalcExpression calculation) {
        super(null);
        this.calculation = calculation;
    }

    /**
     * Constructs a CalculationLiteralTerm from an expression.
     *
     * @param sep The separator
     * @param calculation The expression to evaluate
     */
    public CalculationLiteralTerm(Character sep, CalcExpression calculation) {
        super(sep);
        this.calculation = calculation;
    }

    /**
     * Gets the expression to evaluate.
     *
     * @return The CalcExpression to evaluate
     */
    public CalcExpression getCalculation() {
        return calculation;
    }

    /**
     * Gets the term as a String.
     *
     * @return A String of the form "calc(expression)"
     */
    @Override
    public String toString() {
        return "calc(" + calculation.toString() + ")";
    }

    /**
     * Substitute values in the calculation, and optionally evaluate the result.
     *
     * @param state Current evaluation state
     * @param withParams <code>true</code> if parameters should be substituted
     * @param doCalculations <code>true</code> if the expression should be evaluated
     * @return The resulting calculation, or its result, or <code>null</code> if there was an error
     */
    @Override
    public Expression substituteValues(EvaluationState state, DeclarationList container, boolean withParams, boolean doCalculations) {
        CalcExpression calcExp = calculation.withSubstitutedValues(state, container, withParams, true);
        return new CalculationLiteralTerm(getSeperator(), calcExp).toExpression();
    }

    @Override
    public CalculationLiteralTerm withSeparator(Character separator) {
        return new CalculationLiteralTerm(separator, calculation);
    }
}
