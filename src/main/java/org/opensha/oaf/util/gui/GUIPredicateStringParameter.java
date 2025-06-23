package org.opensha.oaf.util.gui;

import java.util.function.Predicate;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.dom4j.Element;
import org.opensha.commons.exceptions.ConstraintException;
import org.opensha.commons.exceptions.EditableException;
import org.opensha.commons.exceptions.ParameterException;
import org.opensha.commons.param.AbstractParameter;
import org.opensha.commons.param.Parameter;
import org.opensha.commons.param.constraint.ParameterConstraint;
import org.opensha.commons.param.editor.ParameterEditor;


// A String-valued parameter that can be constrained using a Predicate<String>.
// Author: Michael Barall.
// Based on: StringParameter from OpenSHA.
//
// Note: Allowance of null strings is controlled by the nullAllowed flag in the constaint.
// So, the predicate is never called with a null string.
// Convenience functions are provided to create the constraint from a predicate or regular expression.

public class GUIPredicateStringParameter extends AbstractParameter<String> {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	/** Class name for debugging. */
	protected final static String C = "GUIPredicateStringParameter";
	/** If true print out debug statements. */
	protected final static boolean D = false;
	
	private transient ParameterEditor<String> paramEdit = null;



	/**
	 * Constructor doesn't specify a constraint, all values allowed. This
	 * constructor sets the name of this parameter.
	 */
	public GUIPredicateStringParameter( String name ) {
		this( name, (GUIPredicateStringConstraint) null, null, null );
	}



	/**
	 * Constructor that sets the name and Constraint during initialization.
	 *
	 * @param  name                     Name of the parametet
	 * @param  constraint               Constraint object
	 * @exception  ConstraintException  Description of the Exception
	 * @throws  ConstraintException     Is thrown if the value is not allowed
	 */
	public GUIPredicateStringParameter( String name, GUIPredicateStringConstraint constraint ) throws ConstraintException {
		this( name, constraint, null, null );
	}

	// Constructor that makes the constraint from a predicate.
	public GUIPredicateStringParameter( String name, Predicate<String> predicate ) throws ConstraintException {
		this( name, new GUIPredicateStringConstraint (predicate), null, null );
	}

	// Constructor that makes the constraint from a regular expression.
	public GUIPredicateStringParameter( String name, Pattern pattern ) throws ConstraintException {
		this( name, new GUIPredicateStringConstraint (pattern), null, null );
	}

	// Constructor that makes the constraint from a regular expression.
	public GUIPredicateStringParameter( String name, String regex ) throws ConstraintException {
		this( name, new GUIPredicateStringConstraint (regex), null, null );
	}



	/**
	 * Constructor sets the name, constraint, and value.
	 *
	 * @param  name                     Name of the parametet
	 * @param  constraint               Constraint containing a predicate
	 * @param  value                    value of this parameter
	 * @exception  ConstraintException  Is thrown if the value is not allowed
	 * @throws  ConstraintException     Is thrown if the value is not allowed
	 */
	public GUIPredicateStringParameter( String name, GUIPredicateStringConstraint constraint, String value ) throws ConstraintException {
		this( name, constraint, null, value );
	}

	// Constructor that makes the constraint from a predicate.
	public GUIPredicateStringParameter( String name, Predicate<String> predicate, String value ) throws ConstraintException {
		this( name, new GUIPredicateStringConstraint (predicate), null, value );
	}

	// Constructor that makes the constraint from a regular expression.
	public GUIPredicateStringParameter( String name, Pattern pattern, String value ) throws ConstraintException {
		this( name, new GUIPredicateStringConstraint (pattern), null, value );
	}

	// Constructor that makes the constraint from a regular expression.
	public GUIPredicateStringParameter( String name, String regex, String value ) throws ConstraintException {
		this( name, new GUIPredicateStringConstraint (regex), null, value );
	}



	/**
	 *  This is the main constructor. All other constructors call this one.
	 *  Constraints must be set first, because the value may not be an allowed
	 *  one. Null values are always allowed in the constructor. All values are
	 *  set in this constructor; name, value, units, and constructor
	 *
	 * @param  name                     Name of the parametet
	 * @param  constraint               Constraint containing a predicate
	 * @param  units                    Units of this parameter
	 * @param  value                    Balue object of this parameter
	 * @exception  ConstraintException  Is thrown if the value is not allowed
	 * @throws  ConstraintException     Is thrown if the value is not allowed
	 */
	public GUIPredicateStringParameter( String name, GUIPredicateStringConstraint constraint, String units, String value ) throws ConstraintException {
		super( name, constraint, units, value );
	}

	// Constructor that makes the constraint from a predicate.
	public GUIPredicateStringParameter( String name, Predicate<String> predicate, String units, String value ) throws ConstraintException {
		this( name, new GUIPredicateStringConstraint (predicate), units, value );
	}

	// Constructor that makes the constraint from a regular expression.
	public GUIPredicateStringParameter( String name, Pattern pattern, String units, String value ) throws ConstraintException {
		this( name, new GUIPredicateStringConstraint (pattern), units, value );
	}

	// Constructor that makes the constraint from a regular expression.
	public GUIPredicateStringParameter( String name, String regex, String units, String value ) throws ConstraintException {
		this( name, new GUIPredicateStringConstraint (regex), units, value );
	}



	/**
	 * Sets the constraint reference if it is a GUIPredicateStringConstraint
	 * and the parameter is currently editable, else throws an exception.
	 */
	public void setConstraint(ParameterConstraint constraint) throws ParameterException, EditableException{

		String S = C + ": setConstraint(): ";
		checkEditable(S);

		if ( !(constraint instanceof GUIPredicateStringConstraint )) {
			throw new ParameterException( S +
				"This parameter only accepts GUIPredicateStringConstraint, unable to set the constraint."
			);
		}
		else super.setConstraint( constraint );
	}

	// Set the constraint from a predicate.
	public void setConstraint (Predicate<String> predicate) throws ParameterException, EditableException {
		setConstraint (new GUIPredicateStringConstraint (predicate));
		return;
	}

	// Set the constraint from a regular expression.
	public void setConstraint (Pattern pattern) throws ParameterException, EditableException {
		setConstraint (new GUIPredicateStringConstraint (pattern));
		return;
	}

	// Set the constraint from a regular expression.
	public void setConstraint (String regex) throws ParameterException, EditableException {
		setConstraint (new GUIPredicateStringConstraint (regex));
		return;
	}



	/**
	 *  Gets the type attribute of the GUIPredicateStringParameter object. Returns
	 * the class name if unconstrained, else "Constrained" + classname.
	 * This is used to determine which type of GUI editor applies to this
	 * parameter.
	 *
	 * @return    The GUI editor type
	 */
	public String getType() {
		String type = C;
		// Modify if constrained
		ParameterConstraint constraint = this.constraint;
		if (constraint != null) type = "Constrained" + type;
		return type;
	}



	/**
	 * Compares the values to if this is less than, equal to, or greater than
	 * the comparing objects. Implementation of comparable interface. Helps
	 * with sorting a list of parameters.
	 *
	 * @param  obj                     The object to compare this to
	 * @return                         -1 if this value < obj value, 0 if equal,
	 *      +1 if this value > obj value
	 * @exception  ClassCastException  Is thrown if the comparing object is not
	 *      a GUIPredicateStringParameter *
	 * @see                            Comparable
	 */
//    @Override
//    public int compareTo(Parameter<String> param) {
////
////        String S = C + ":compareTo(): ";
////
////        if ( !( obj instanceof GUIPredicateStringParameter ) ) {
////            throw new ClassCastException( S + "Object not a GUIPredicateStringParameter, unable to compare" );
////        }
////
////        GUIPredicateStringParameter param = ( GUIPredicateStringParameter ) obj;
////
////        if( ( this.value == null ) && ( param.value == null ) ) return 0;
////        int result = 0;
////
////        String n1 = ( String ) this.getValue();
////        String n2 = ( String ) param.getValue();
////
////        return n1.compareTo( n2 );
//        if (value == null && param.getValue() == null) return 0;
//        return value.compareTo(param.getValue());
//    }


	/**
	 * Compares the passed in String parameter to see if it has
	 * the same name and value. If the object is not a String parameter
	 * an exception is thrown. If the values and names are equal true
	 * is returned, otherwise false is returned.
	 *
	 * @param  obj                     The object to compare this to
	 * @return                         True if the values are identical
	 * @exception  ClassCastException  Is thrown if the comparing object is not
	 *      a GUIPredicateStringParameter
	 */
//    @Override
//    public boolean equals(Object obj) {
////        String S = C + ":equals(): ";
////
////        if ( !(obj instanceof GUIPredicateStringParameter ) ) {
////            throw new ClassCastException( S + "Object not a GUIPredicateStringParameter, unable to compare" );
////        }
////
////        String otherName = ( ( GUIPredicateStringParameter ) obj ).getName();
////        if ( ( compareTo( obj ) == 0 ) && getName().equals( otherName ) ) {
////            return true;
////        }
////        else { return false; }
//    	
//        if (!(obj  instanceof GUIPredicateStringParameter)) return false;
//        GUIPredicateStringParameter sp = (GUIPredicateStringParameter) obj;
//        return compareTo(sp) == 0 && getName().equals(sp.getName());
//    }



	/**
	 *  Returns a copy so you can't edit or damage the origial.
	 * Clones this object's value and all fields. The constraints
	 * are also cloned.
	 *
	 * @return    Description of the Return Value
	 */
	@Override
	public Object clone() {

		GUIPredicateStringConstraint c1 = null;
		if(constraint != null)
			c1 = ( GUIPredicateStringConstraint ) constraint.clone();

		GUIPredicateStringParameter param = null;
		if( value == null ) {
			param = new GUIPredicateStringParameter(name, c1, units, null);
		}
		else {
			param = new GUIPredicateStringParameter( name, c1, units, this.value.toString() );
		}
		if( param == null ) return null;
		param.editable = true;
		param.info = info;
		return param;

	}


	public boolean setIndividualParamValueFromXML(Element el) {
		String val = el.attributeValue("value");
		if (val.length() == 0) {
			try {
				this.setValue("");
			} catch (ConstraintException e) {
				System.err.println("Warning: could not set String Param to empty string from XML");
			} catch (ParameterException e) {
				System.err.println("Warning: could not set String Param to empty string from XML");
			}
		} else {
			this.setValue(val);
		}
		return true;
	}

	@Override
	public ParameterEditor<String> getEditor() {
		if (paramEdit == null) {
			try {
				paramEdit = new GUIPredicateStringParameterEditor(this);
			} catch (Exception e) {
				throw new RuntimeException(e);
			}
		}
		return paramEdit;
	}

	@Override
	public boolean isEditorBuilt() {
		return paramEdit != null;
	}

}
