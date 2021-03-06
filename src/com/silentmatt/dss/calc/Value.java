package com.silentmatt.dss.calc;

import com.silentmatt.dss.Immutable;
import com.silentmatt.dss.term.NumberTerm;
import com.silentmatt.dss.term.Term;

/**
 * Represents a dimensioned value.
 *
 * @author Matthew Crumley
 */
@Immutable
public final class Value {
    private final double scalar;
    private final CalculationUnit unit;

    /**
     * Constructs a Value from a number and a unit.
     *
     * @param scalar The scalar (numeric) part of the value.
     * @param unit The associated {@link CalculationUnit}.
     */
    public Value(double scalar, CalculationUnit unit) {
        this.scalar = scalar * unit.getScale();
        this.unit = CalculationUnit.getCanonicalUnit(unit);
    }

    /**
     * Constructs a Value from a CSS {@link NumberTerm}.
     *
     * @param term The CSS Term to convert.
     * @throws IllegalArgumentException <code>term</code> is not a number.
     */
    public Value(NumberTerm term) {
        CalculationUnit thisUnit = CalculationUnit.fromCssUnit(term.getUnit());
        if (thisUnit == null) {
            throw new IllegalArgumentException("term");
        }
        this.scalar = term.getValue() * thisUnit.getScale();
        this.unit = CalculationUnit.getCanonicalUnit(thisUnit);
    }

    /**
     * Constructs a Value from a number and a CSS {@link Unit}.
     *
     * @param scalar The scalar (numeric) part of the value.
     * @param unit The associated CSS Unit.
     */
    public Value(double scalar, Unit unit) {
        CalculationUnit thisUnit = CalculationUnit.fromCssUnit(unit);
        this.scalar = scalar * thisUnit.getScale();
        this.unit = CalculationUnit.getCanonicalUnit(thisUnit);
    }

    /**
     * Gets the scalar part of the value.
     *
     * @return The scalar (numeric) part of the value.
     */
    public double getScalarValue() {
        return this.scalar;
    }

    /**
     * Adds two Values.
     * The Values must have compatible units.
     *
     * @param other The Value to add to <code>this</code>
     * @return The sum, <code>this</code> + <code>other</code>.
     * @throws IllegalArgumentException CalculationUnits are not compatible.
     *
     * @see CalculationUnit#isAddCompatible(com.silentmatt.dss.expression.CalculationUnit)
     */
    public Value add(Value other) {
        if (unit.isAddCompatible(other.unit)) {
            return new Value(scalar + other.scalar, unit);
        }
        else {
            throw new IllegalArgumentException("other");
        }
    }

    /**
     * Subtracts two Values.
     * The Values must have compatible units.
     *
     * @param other The Value to subtract from <code>this</code>
     * @return The difference, <code>this</code> - <code>other</code>.
     * @throws IllegalArgumentException CalculationUnits are not compatible.
     *
     * @see CalculationUnit#isAddCompatible(com.silentmatt.dss.expression.CalculationUnit)
     */
    public Value subtract(Value other) {
        if (unit.isAddCompatible(other.unit)) {
            return new Value(scalar - other.scalar, unit);
        }
        else {
            throw new IllegalArgumentException("other");
        }
    }

    /**
     * Multiplies two Values.
     *
     * Unlike add and subtract, the units do not have to be compatible.
     *
     * @param other The Value to multiply <code>this</code> by.
     * @return The product, <code>this</code> * <code>other</code>.
     */
    public Value multiply(Value other) {
        return new Value(scalar * other.scalar, unit.multiply(other.unit));
    }

    /**
     * Divides two Values.
     *
     * Unlike add and subtract, the units do not have to be compatible.
     *
     * @param other The Value to divide <code>this</code> by.
     * @return The quotient, <code>this</code> / <code>other</code>.
     */
    public Value divide(Value other) {
        return new Value(scalar / other.scalar, unit.divide(other.unit));
    }

    /**
     * Negates a Value.
     *
     * @return -<code>this</code>
     */
    public Value negate() {
        return new Value(-scalar, unit);
    }

    /**
     * Converts a Value into a CSS {@link Term}.
     *
     * The Value's unit must be compatible with a valid CSS unit.
     *
     * @return A CSS Term that represents this Value.
     * @throws CalculationException <code>this</code> cannot be represented by a valid CSS unit.
     */
    public NumberTerm toTerm() throws CalculationException {
        NumberTerm term = new NumberTerm(scalar);
        Unit cssUnit = CalculationUnit.toCssUnit(unit);
        if (cssUnit == null) {
            throw new CalculationException("not a valid CSS unit: " + toString());
        }
        term = term.withUnit(cssUnit);
        return term;
    }

    /**
     * Gets a String representation of this Value.
     *
     * @return This Value as a String.
     */
    @Override
    public String toString() {
        return scalar + unit.toString();
    }
}
