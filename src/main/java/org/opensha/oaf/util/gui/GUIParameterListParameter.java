package org.opensha.oaf.util.gui;

import java.util.ListIterator;

import java.awt.Color;
import java.awt.Dimension;

import org.dom4j.Element;
import org.opensha.commons.exceptions.ParameterException;
import org.opensha.commons.param.AbstractParameter;
import org.opensha.commons.param.Parameter;
import org.opensha.commons.param.ParameterList;
import org.opensha.commons.param.editor.ParameterEditor;
//import org.opensha.commons.param.editor.impl.ParameterListParameterEditor;
//import org.opensha.commons.param.impl.ParameterListParameter;


/**
 * GUI parameter that contains a list of parameters.
 * Author: Michael Barall 07/29/2021.
 *
 * This code is copied and then extended from OpenSHA class ParameterListParameter.
 *
 * This parameter appears as a button.  When the button is pressed, a dialog opens
 * which displays all the parameters in the list.  The dialog also has a close button.
 *
 * This is extended from the OpenSHA version in that it permits setting various text
 * strings and overriding the remembered dialog size and position, as well as positioning
 * the dialog over the GUI window instead of the top left corner of the screen.
 * It also properly supports modeless dialogs, fixes a number of issues, and includes
 * an option to print debugging messages.
 *
 * The parameters appearing within the dialog can be changed in two ways: by creating a new
 * ParameterList and passing it to setValue(); or by changing the contents of the existing
 * ParameterList.  Either way, call refreshParamEditor on the editor to activate the new list;
 * if the dialog is open the call repaints the dialog.
 */

public class GUIParameterListParameter extends AbstractParameter<ParameterList> implements GUIDialogParameter {


	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	/** Class name for debugging. */
	protected final static String C = "GUIParameterListParameter";
	/** If true print out debug statements. */
	protected final static boolean D = false;

	protected final static String PARAM_TYPE ="GUIParameterListParameter";

	private transient ParameterEditor<ParameterList> paramEdit = null;


	// The button text that appears on the main screen.
	// Null or empty means to use a default string, which is the name of the parameter.
	// If changed after construction, call refreshParamEditor on the editor to refresh the on-screen text.

	protected String buttonText;

	public String getButtonText () {
		if (buttonText == null || buttonText.isEmpty()) {
			return getName();
		}
		return buttonText;
	}

	public void setButtonText (String the_buttonText) {
		buttonText = the_buttonText;
		return;
	}


	// The title text that appears at the top of the dialog.
	// Null means to use a default string, which is the name of the parameter.
	// (An empty dialog title is permitted.)
	// This is examined at the time the dialog box is created.
	// If changed while the dialog is open, call refreshParamEditor on the editor to refresh the on-screen text.

	protected String dialogTitleText = null;

	public String getDialogTitleText () {
		if (dialogTitleText == null) {
			return getName();
		}
		return dialogTitleText;
	}

	public void setDialogTitleText (String the_dialogTitleText) {
		dialogTitleText = the_dialogTitleText;
		return;
	}


	// The title text that appears at the top of the list.
	// Null means to use a default string, which is "Set " concatenated with the name of the parameter.
	// (An empty list title is permitted.)
	// If changed after construction, call refreshParamEditor on the editor to make it be used.

	protected String listTitleText = null;

	public String getListTitleText () {
		if (listTitleText == null) {
			return "Set " + getName();
		}
		return listTitleText;
	}

	public void setListTitleText (String the_listTitleText) {
		listTitleText = the_listTitleText;
		return;
	}


	// The OK button text that appears at the bottom of the dialog.
	// Null means not to display the button.
	// Empty means to use a default string, which is "Update " concatenated with the name of the parameter.
	// This is examined at the time the dialog box is created.
	// If changed while the dialog is open, call refreshParamEditor on the editor to refresh the on-screen text.
	// (However, the button cannot be added or removed while the dialog is open.)

	protected String okButtonText = "";

	public String getOkButtonText () {
		if (okButtonText == null || okButtonText.isEmpty()) {
			return "Update " + getName();
		}
		return okButtonText;
	}

	public void setOkButtonText (String the_okButtonText) {
		okButtonText = the_okButtonText;
		return;
	}

	public boolean useOkButton () {
		return (okButtonText != null);
	}


	// The CANCEL button text that appears at the bottom of the dialog.
	// Null means not to display the button.
	// Empty means to use a default string, which is "Cancel".
	// This is examined at the time the dialog box is created.
	// If changed while the dialog is open, call refreshParamEditor on the editor to refresh the on-screen text.
	// (However, the button cannot be added or removed while the dialog is open.)

	protected String cancelButtonText = null;

	public String getCancelButtonText () {
		if (cancelButtonText == null || cancelButtonText.isEmpty()) {
			return "Cancel";
		}
		return cancelButtonText;
	}

	public void setCancelButtonText (String the_cancelButtonText) {
		cancelButtonText = the_cancelButtonText;
		return;
	}

	public boolean useCancelButton () {
		return (cancelButtonText != null);
	}


	// Set the dimensions of the dialog to use the next time it is created.
	// Null means to use default dimensions.
	// If not called, the dimensions and position are remembered from the last time
	// the dialog box was created, or default if it was never created.
	// If called while the dialog is open, call refreshParamEditor on the editor to resize the on-screen dialog.
	
	public void setDialogDimensions(Dimension dialogDims) {
		((GUIParameterListParameterEditor)getEditor()).setDialogDimensions(dialogDims);
		return;
	}
	
	public void setDialogDimensions(int width, int height) {
		setDialogDimensions (new Dimension (width, height));
		return;
	}


	// True to use a modal dialog.
	// This is set only during construction.

	protected boolean modalDialog = true;

	@Override
	public boolean getModalDialog () {
		return modalDialog;
	}


	// True to emit trace messages.
	// This is set only during construction.

	protected boolean enableTrace = false;


	// The foreground (text) color for the activation button, null for default.
	// This is only examined when the button is created.

	protected Color buttonForeground = null;

	public Color getButtonForeground () {
		return buttonForeground;
	}

	public void setButtonForeground (Color color) {
		buttonForeground = color;
		return;
	}


	// The foreground (text) color for the OK button, null for default.
	// This is only examined when the button is created.

	protected Color okButtonForeground = null;

	public Color getOkButtonForeground () {
		return okButtonForeground;
	}

	public void setOkButtonForeground (Color color) {
		okButtonForeground = color;
		return;
	}


	// The foreground (text) color for the cancel button, null for default.
	// This is only examined when the button is created.

	protected Color cancelButtonForeground = null;

	public Color getCancelButtonForeground () {
		return cancelButtonForeground;
	}

	public void setCancelButtonForeground (Color color) {
		cancelButtonForeground = color;
		return;
	}


	// The termination code, typically indicating why the dialog was closed.

	protected int dialogTermCode = GUIDialogParameter.TERMCODE_NONE;

	@Override
	public int getDialogTermCode () {
		return dialogTermCode;
	}

	public void setDialogTermCode (int the_dialogTermCode) {
		dialogTermCode = the_dialogTermCode;
		return;
	}


	// The status code, indicating the current dialog state.

	protected int dialogStatus = GUIDialogParameter.DLGSTAT_NONE;

	@Override
	public int getDialogStatus () {
		return dialogStatus;
	}

	public void setDialogStatus (int the_dialogStatus) {
		dialogStatus = the_dialogStatus;
		return;
	}





	/**
	 *  No constraints specified for this parameter. Sets the name of this
	 *  parameter.
	 *
	 * @param  name  Name of the parameter
	 */
	public GUIParameterListParameter(String name) {
		super(name,null,null,null);
		buttonText = null;
	}


	/**
	 * No constraints specified, all values allowed. Sets the name and value.
	 *
	 * @param  name   Name of the parameter
	 * @param  paramList  ParameterList  object
	 */
	public GUIParameterListParameter(String name, ParameterList paramList){
		super(name,null,null,paramList);
		buttonText = null;
		//setting the independent Param List for this parameter
		setIndependentParameters(paramList);
	}


	/**
	 * No constraints specified, all values allowed. Sets the name and value.
	 *
	 * @param  name   Name of the parameter
	 * @param  paramList  ParameterList  object
	 * @param  the_buttonText  Text to appear on the button, null or empty uses name.
	 * @param  the_dialogTitleText  Text to appear on dialog title, null uses name.
	 * @param  the_listTitleText  Text to appear at top of list, null uses "Set "+name.
	 * @param  the_okButtonText  Text to appear on dialog OK button, null omits the button, empty uses "Update "+name.
	 * @param  the_cancelButtonText  Text to appear on dialog CANCEL button, null omits the button, empty uses "Cancel".
	 * @param  the_modalDialog  True to use a modal dialog (the normal usage).
	 * @param  the_enableTrace  True to emit trace messages for debugging.
	 */
	public GUIParameterListParameter(String name, ParameterList paramList, String the_buttonText,
			String the_dialogTitleText, String the_listTitleText, String the_okButtonText,
			String the_cancelButtonText, boolean the_modalDialog, boolean the_enableTrace){
		super(name,null,null,paramList);
		buttonText = the_buttonText;
		dialogTitleText = the_dialogTitleText;
		listTitleText = the_listTitleText;
		okButtonText = the_okButtonText;
		cancelButtonText = the_cancelButtonText;
		modalDialog = the_modalDialog;
		enableTrace = the_enableTrace;
		buttonForeground = null;
		okButtonForeground = null;
		cancelButtonForeground = null;
		//setting the independent Param List for this parameter
		setIndependentParameters(paramList);
	}



	/**
	 *  Compares the values to if this is less than, equal to, or greater than
	 *  the comparing objects.
	 *
	 * @param  obj                     The object to compare this to
	 * @return                         -1 if this value < obj value, 0 if equal,
	 *      +1 if this value > obj value
	 * @exception  ClassCastException  Is thrown if the comparing object is not
	 *      a GUIParameterListParameter.
	 */
//	@Override
//	public int compareTo(Parameter<ParameterList> param) {
//		String S = C + ":compareTo(): ";
//
//		if ( !( obj instanceof GUIParameterListParameter ) ) {
//			throw new ClassCastException( S + "Object not a GUIParameterListParameter, unable to compare" );
//		}
//
//		GUIParameterListParameter param = ( GUIParameterListParameter ) obj;
//
//		if( ( this.value == null ) && ( param.value == null ) ) return 0;
//		int result = 0;
//
//		ParameterList n1 = ( ParameterList) this.getValue();
//		ParameterList n2 = ( ParameterList) param.getValue();
//
//		return n1.compareTo( n2 );
//		
//		if (param == null) return 1;
//		if (value == null && param.getValue() == null) return 0;
//		if (value == null) return -1;
//		if (param.getValue() == null) return 1;
//		return  value.compareTo(param.getValue());
//
//	}


	/**
	 * Set's the parameter's value, which is basically a parameterList.
	 *
	 * @param  value                 The new value for this Parameter
	 * @throws  ParameterException   Thrown if the object is currenlty not
	 *      editable
	 */
	public void setValue( ParameterList value ) throws ParameterException {

		//ListIterator it  = value.getParametersIterator();
		super.setValue(value);
		//setting the independent Param List for this parameter
		this.setIndependentParameters(value);
	}

	/**
	 * Compares value to see if equal.
	 *
	 * @param  obj                     The object to compare this to
	 * @return                         True if the values are identical
	 * @exception  ClassCastException  Is thrown if the comparing object is not
	 *      a GUIParameterListParameter.
	 */
//	@Override
//	public boolean equals(Object obj) {
//		String S = C + ":equals(): ";
//
//		if (! (obj instanceof GUIParameterListParameter)) {
//			throw new ClassCastException(S +
//			"Object not a GUIParameterListParameter, unable to compare");
//		}
//
//		String otherName = ( (GUIParameterListParameter) obj).getName();
//		if ( (compareTo(obj) == 0) && getName().equals(otherName)) {
//			return true;
//		}
//		else {
//			return false;
//		}
		
//		if (this == obj) return true;
//		if (!(obj instanceof GUIParameterListParameter)) return false;
//		GUIParameterListParameter plp = (GUIParameterListParameter) obj;
//		return (value.equals(plp.getValue()) && getName().equals(plp.getName()));
//
//	}

	/**
	 *  Returns a copy so you can't edit or damage the origial.
	 *
	 * @return    Exact copy of this object's state.
	 *
	 * Note: The copy contains the same list and so the same contained parameters.
	 * Perhaps this should be a deep copy?
	 */
	public Object clone(){

		GUIParameterListParameter param = null;
		if (value == null) {
			param = new GUIParameterListParameter(name);
		}
		else {
			param = new GUIParameterListParameter(name, (ParameterList)value);
		}
		if (param == null) {
			return null;
		}
		param.buttonText = buttonText;
		param.dialogTitleText = dialogTitleText;
		param.listTitleText = listTitleText;
		param.okButtonText = okButtonText;
		param.cancelButtonText = cancelButtonText;
		param.modalDialog = modalDialog;
		param.enableTrace = enableTrace;
		param.buttonForeground = buttonForeground;
		param.okButtonForeground = okButtonForeground;
		param.cancelButtonForeground = cancelButtonForeground;
		param.dialogTermCode = dialogTermCode;
		param.dialogStatus = dialogStatus;
		param.editable = true;
		return param;
	}

	/**
	 * Returns the ListIterator of the parameters included within this parameter
	 * @return
	 */
	public ListIterator getParametersIterator(){
		return ((ParameterList)this.getValue()).getParametersIterator();
	}

	/**
	 *
	 * @return the parameterList contained in this parameter
	 */
	public ParameterList getParameter(){
		return (ParameterList)getValue();
	}

	/**
	 * Returns the name of the parameter class
	 */
	public String getType() {
		String type = this.PARAM_TYPE;
		return type;
	}

	/**
	 * This overrides the getMetadataString() method because the value here
	 * does not have an ASCII representation (and we need to know the values
	 * of the independent parameter instead).
	 * @return String
	 */
	public String getMetadataString() {
		return getDependentParamMetadataString();
	}

	public boolean setIndividualParamValueFromXML(Element el) {
		// just return true, param values are actually stored as independent params and will be set
		return true;
	}

	public ParameterEditor<ParameterList> getEditor() {
		if (paramEdit == null) {
			paramEdit = new GUIParameterListParameterEditor(this, true, modalDialog, enableTrace);
		}
		return paramEdit;
	}


	// Open the dialog.
	// Returns true if the dialog was created, false if not (because it is open or in the process of closing).
	// If a modal dialog is created, this does not return until the dialog begins the process of closing
	// (at which time the termination code will be available).

	@Override
	public boolean openDialog () {
		return ((GUIParameterListParameterEditor)getEditor()).openDialog();
	}


	// Close the dialog, and set the termination code.
	// Return true if success, false if not open or already in the process of closing.
	// Upon return, the dialog will have begun the process of closing, and may or may not be closed.
	// The termination code will available through getDialogTermCode.

	@Override
	public boolean closeDialog (int the_dialogTermCode) {
		return ((GUIParameterListParameterEditor)getEditor()).closeDialog (the_dialogTermCode);
	}


}


