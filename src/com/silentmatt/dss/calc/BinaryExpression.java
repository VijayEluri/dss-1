package com.silentmatt.dss.calc;

import com.silentmatt.dss.DeclarationList;
import com.silentmatt.dss.EvaluationState;

/**
 * A CalcExpression that represents a binary operation.
 *
 * @author Matthew Crumley
 */
public class BinaryExpression implements CalcExpression {
    private final Operation operation;
    private final CalcExpression left;
    private final CalcExpression right;

    /**
     * Constructs a BinaryExpression with an operator and the two operands.
     *
     * @param operation The oparation to perform.
     * @param left The left operand.
     * @param right The right operand.
     */
    public BinaryExpression(Operation operation, CalcExpression left, CalcExpression right) {
        this.operation = operation;
        this.left = left;
        this.right = right;
    }

    public Value calculateValue(EvaluationState state, DeclarationList container) {
        try {
            Value leftValue = left.calculateValue(state, container);
            if (leftValue == null) {
                return null;
            }
            Value rightValue = right.calculateValue(state, container);
            if (rightValue == null) {
                return null;
            }
            switch (operation) {
            case Add:
                return leftValue.add(rightValue);
            case Subtract:
                return leftValue.subtract(rightValue);
            case Multiply:
                return leftValue.multiply(rightValue);
            case Divide:
                return leftValue.divide(rightValue);
            default:
                state.getErrors().SemErr("Unrecognized operation");
                break;
            }
        } catch (IllegalArgumentException ex) {
            state.getErrors().SemErr("incompatible units");
        }
        return null;
    }

    public void substituteValues(EvaluationState state, DeclarationList container) {
        left.substituteValues(state, container);
        right.substituteValues(state, container);
    }

    /**
     * Gets the expression as a string.
     *
     * The resulting String will be parsable to an identical BinaryExpression.
     *
     * @return The String repressentation of this expression.
     */
    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        boolean leftPrecidence = left.getPrecidence() < getPrecidence();
        boolean rightPrecidence = right.getPrecidence() <= getPrecidence();

        if (leftPrecidence) { sb.append('('); }
        sb.append(left);
        if (leftPrecidence) { sb.append(')'); }

        switch (operation) {
            case Add:
                sb.append(" + ");
                break;
            case Subtract:
                sb.append(" - ");
                break;
            case Multiply:
                sb.append(" * ");
                break;
            case Divide:
                sb.append(" / ");
                break;
            default:
                throw new IllegalStateException("Unknown operator: " + operation);
        }

        if (rightPrecidence) { sb.append('('); }
        sb.append(right);
        if (rightPrecidence) { sb.append(')'); }

        return sb.toString();
    }

    public int getPrecidence() {
        int precidence;
        switch (operation) {
        case Multiply: precidence =  2; break;
        case Divide:   precidence =  2; break;
        case Add:      precidence =  1; break;
        case Subtract: precidence =  1; break;
        default:       precidence = -1; break;
        }
        return precidence;
    }
}
