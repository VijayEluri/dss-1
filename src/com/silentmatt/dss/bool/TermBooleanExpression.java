package com.silentmatt.dss.bool;

import com.silentmatt.dss.DeclarationList;
import com.silentmatt.dss.EvaluationState;
import com.silentmatt.dss.Expression;
import com.silentmatt.dss.calc.Value;
import com.silentmatt.dss.term.CalculationTerm;
import com.silentmatt.dss.term.ClassReferenceTerm;
import com.silentmatt.dss.term.FunctionTerm;
import com.silentmatt.dss.term.NumberTerm;
import com.silentmatt.dss.term.ReferenceTerm;
import com.silentmatt.dss.term.Term;
import com.silentmatt.dss.term.UrlTerm;

/**
 *
 * @author matt
 */
public class TermBooleanExpression implements BooleanExpression {
    private final Term value;

    /**
     * Constructs a TermExpression from a CSS Term.
     *
     * @param value The Term this expression will return.
     */
    public TermBooleanExpression(Term value) {
        this.value = value;
    }

    private static Boolean truthiness(Expression value) {
        if (value == null) {
            return Boolean.FALSE;
        }
        if (value.getTerms().size() == 1) {
            Term term = value.getTerms().get(0);
            if (term instanceof NumberTerm) {
                return ((NumberTerm) term).getDoubleValue() != 0;
            }
        }
        String exprString = value.toString();
        return !(exprString.length() == 0 ||
                 exprString.equalsIgnoreCase("false") ||
                 exprString.equalsIgnoreCase("no"));
    }

    private static Boolean evaluateAsString(EvaluationState state, Term value) {
        String str = value.toString();
        if (str.equalsIgnoreCase("true") || str.equalsIgnoreCase("false")) {
            return Boolean.valueOf(str);
        }

        Expression constValue = state.getVariables().get(str);
        if (constValue == null) {
            return false;
        }
        return truthiness(constValue);
    }

    public Boolean evaluate(EvaluationState state, DeclarationList container) {
        if (value instanceof NumberTerm) {
            return ((NumberTerm) value).getDoubleValue() != 0;
        }
        else if (value instanceof CalculationTerm) {
            Value result = ((CalculationTerm) value).getCalculation().calculateValue(state, container);
            return result == null ? null : result.getScalarValue() != 0;
        }
        else if (value instanceof FunctionTerm) {
            Expression expr = ((FunctionTerm) value).applyFunction(state);
            return truthiness(expr);
        }
        else if (value instanceof ReferenceTerm) {
            Expression expr = ((ReferenceTerm) value).evaluate(state, container);
            return truthiness(expr);
        }
        else if (value instanceof ClassReferenceTerm) {
            return state.getClasses().containsKey(((ClassReferenceTerm) value).getName());
        }
        else if (value instanceof UrlTerm) {
            state.getErrors().SemErr("Invalid term in calculation: '" + value + "'");
        }

        return evaluateAsString(state, value);
    }

    public int getPrecidence() {
        return 4;
    }

    /**
     * Gets the Term's string representation.
     *
     * @return The Term as a String.
     */
    @Override
    public String toString() {
        return this.value.toString();
    }

}
