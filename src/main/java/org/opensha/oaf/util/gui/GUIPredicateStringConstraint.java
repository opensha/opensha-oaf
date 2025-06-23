package org.opensha.oaf.util.gui;

import java.util.function.Predicate;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.opensha.commons.exceptions.ConstraintException;
import org.opensha.commons.exceptions.EditableException;
import org.opensha.commons.param.constraint.AbstractParameterConstraint;


// Constraint for a String-valued parameter that can be constrained using a Predicate<String>.
// Author: Michael Barall.
// Based on: StringConstraint from OpenSHA.
//
// Note: Allowance of null strings is controlled by super.nullAllowed.
// So, the predicate is never called with a null string.
// Convenience functions are supplied to create a predicate that recognizes a regular expression.

public class GUIPredicateStringConstraint
		extends AbstractParameterConstraint<String>
{

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	/** Class name for debugging. */
	protected final static String C = "GUIPredicateStringConstraint";
	/** If true print out debug statements. */
	protected final static boolean D = false;


	// The predicate that determines which strings area allowed.
	// If null, then all strings are allowed.
	private Predicate<String> predicate = null;


	/** No-Arg constructor for the GUIPredicateStringConstraint object. Calls the super() constructor. */
	public GUIPredicateStringConstraint() { super(); }


	// Constructor that sets the predicate.
	public GUIPredicateStringConstraint (Predicate<String> predicate) {
		this.predicate = predicate;
	}


	// Constructor that sets the predicate from a regular expression.
	public GUIPredicateStringConstraint (Pattern pattern) {
		this (pattern.asPredicate());
	}


	// Constructor that sets the predicate from a regular expression.
	public GUIPredicateStringConstraint (String regex) {
		this (Pattern.compile (regex));
	}


	// Set the predicate.
	public void setPredicate (Predicate<String> predicate) {
		String S = C + ": setPredicate(): ";
		checkEditable(S);
		this.predicate = predicate;
		return;
	}


	// Set a predicate that recognizes a regular expression.
	public void setPredicate (Pattern pattern) {
		setPredicate (pattern.asPredicate());
		return;
	}


	// Set a predicate that recognizes a regular expression.
	public void setPredicate (String regex) {
		setPredicate (Pattern.compile (regex));
		return;
	}


	/**
	 * Determine if the new value being set is allowed. First checks
	 * if null and if nulls are allowed. Then verifies the Object is
	 * a String. Finally the code verifies that the String is
	 * in the allowed strings vector. If any of these checks fails, false
	 * is returned.
	 *
	 * @param  obj  Object to check if allowed String
	 * @return      True if the value is allowed
	 */
	@Override
	public boolean isAllowed( String obj ) {
		if (obj == null) {
			return nullAllowed;
		}
		if (predicate == null) {
			return true;
		}
		return predicate.test (obj);
	}


	/**
	 *  Prints out the current state of this parameter.
	 *  Useful for debugging.
	 */
	@Override
	public String toString() {
		String TAB = "    ";
		StringBuilder b = new StringBuilder();
		b.append( C );

		if( name != null) b.append( TAB + "Name = " + name + '\n' );
		//b.append( TAB + "Is Editable = " + this.editable + '\n' );
		b.append( TAB + "Null Allowed = " + this.nullAllowed+ '\n' );
		return b.toString();
	}


	/** Returns a copy so you can't edit or damage the origial. */
	// Note: It is not possible to clone an arbitrary Predicate, and so we copy it.
	@Override
	public Object clone() {
		GUIPredicateStringConstraint c1 = new GUIPredicateStringConstraint();
		c1.name = name;
		c1.predicate = predicate;
		c1.setNullAllowed( nullAllowed );
		c1.editable = true;
		return c1;
	}
}
