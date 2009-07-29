package com.silentmatt.dss.expression;

import com.silentmatt.dss.DSSEvaluator;

/**
 * An expression from a calc(...) Term.
 *
 * @author Matthew Crumley
 */
public interface CalcExpression {
    /**
     * Performs the calculation.
     * In the calculation, the <code>variables</code> parameter will be used to
     * substitute 'const()' terms, and the <code>parameters</code> parameter will
     * be used for 'param()' terms.
     *
     * @param variables Scope for const lookups.
     * @param parameters Scope for param lookups.
     * @return The result of the calculation.
     * @throws CalculationException The expression attempts invalid unit operations,
     * has invalid Terms, or a const/param lookup fails.
     */
    Value calculateValue(DSSEvaluator.EvaluationState state);

    /**
     * Gets the relative precidence of the expression.
     *
     * @return A positive integer, with higher numbers representing higher precidence.
     * -1 if the expression is invalid (i.e. a {@link BinaryExpression} has an null {@link Operation}).
     */
    int getPrecidence();

    /**
     * Replaces any const/param values available.
     * Any missing constants/parameters will not be replaced, but will not cause an exception.
     *
     * @param variables Scope for const lookups.
     * @param parameters Scope for param lookups.
     * @throws CalculationException The expression has invalid Terms.
     */
    void substituteValues(DSSEvaluator.EvaluationState state);
}
