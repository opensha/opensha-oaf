package org.opensha.oaf.util.gui;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Component;
import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.LayoutManager;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JButton;

import javax.swing.border.Border;
import javax.swing.border.LineBorder;
import javax.swing.border.MatteBorder;

// Panel with a header and a scrolling area.
// Author: Michael Barall.
//
// This code is copied and then modified from the OpenSHA class LabeledBoxPanel.
//
// This is extended to support a help button in the header.

/**
 * <b>Title:</b> LabeledBoxPanel<p>
 *
 * <b>Description:</b> GUI Widget that contains a Header panel with
 * a title lable and an editor panel withing a scroll pane. Is the
 * base class for the ParameterListEditor. This is a generic
 * component so it was useful to pull this functionality out of
 * the ParameterListEditor and make that a subclass. <p>
 *
 * The main use is to add any component to the editor panel, and
 * the results will be scrollable. See the add() functions below.<p>
 *
 * @author Steven W. Rock
 * @version 1.0
 */

public class GUILabeledBoxPanel extends JPanel{

	protected final static String C = "GUILabeledBoxPanel";
	protected final static boolean D = false;

	protected JScrollPane jScrollPane1 = new JScrollPane();
	protected JPanel editorPanel = new JPanel();
	protected JPanel headerPanel = new JPanel();
	protected JLabel headerLabel = new JLabel();

	private Border border = null;
	private Border border2 = null;

	protected static GridBagLayout GBL = new GridBagLayout();

	protected String title;
	protected Color borderColor = new Color( 80, 80, 133 );
	protected Color headerPanelBackgroundColor = new Color( 200, 200, 255 );

	protected boolean addDefault = true;

	protected GUIHelpListener help_listener = null;
	protected JButton help_button = null;


	/**
	 * Creates a new JPanel with the specified layout manager and buffering
	 * strategy.
	 *
	 * @param layout  the LayoutManager to use
	 * @param isDoubleBuffered  a boolean, true for double-buffering, which
	 *        uses additional memory space to achieve fast, flicker-free
	 *        updates
	 */
	public GUILabeledBoxPanel(LayoutManager layout, boolean isDoubleBuffered) {
		super(layout, isDoubleBuffered);
		try { jbInit(); }
		catch ( Exception e ) { e.printStackTrace(); }
		if( editorPanel != null ) editorPanel.setLayout(layout);
	}

	/**
	 * Create a new buffered JPanel with the specified layout manager
	 *
	 * @param layout  the LayoutManager to use
	 */
	public GUILabeledBoxPanel(LayoutManager layout) {
		super(layout);
		try { jbInit(); }
		catch ( Exception e ) { e.printStackTrace(); }
		if( editorPanel != null ) editorPanel.setLayout(layout);
	}

	/**
	 * Creates a new <code>JPanel</code> with <code>FlowLayout</code>
	 * and the specified buffering strategy.
	 * If <code>isDoubleBuffered</code> is true, the <code>JPanel</code>
	 * will use a double buffer.
	 *
	 * @param layout  the LayoutManager to use
	 * @param isDoubleBuffered  a boolean, true for double-buffering, which
	 *        uses additional memory space to achieve fast, flicker-free
	 *        updates
	 */
	public GUILabeledBoxPanel(boolean isDoubleBuffered) {
		super(isDoubleBuffered);
		try { jbInit(); }
		catch ( Exception e ) { e.printStackTrace(); }
		if( editorPanel != null ) editorPanel.setLayout(new FlowLayout());
	}

	/**
	 * Creates a new <code>JPanel</code> with a double buffer
	 * and a flow layout.
	 */
	public GUILabeledBoxPanel() {
		super();
		try { jbInit(); }
		catch ( Exception e ) { e.printStackTrace(); }

		if( editorPanel != null ) editorPanel.setLayout(new FlowLayout());
	}


	/**
	 * Creates a new JPanel with the specified layout manager and buffering
	 * strategy.
	 *
	 * @param layout  the LayoutManager to use
	 * @param isDoubleBuffered  a boolean, true for double-buffering, which
	 *        uses additional memory space to achieve fast, flicker-free
	 *        updates
	 * @param helpListener  the help listener to use
	 */
	public GUILabeledBoxPanel(LayoutManager layout, boolean isDoubleBuffered, GUIHelpListener helpListener) {
		super(layout, isDoubleBuffered);
		try { jbInit(helpListener); }
		catch ( Exception e ) { e.printStackTrace(); }
		if( editorPanel != null ) editorPanel.setLayout(layout);
	}

	/**
	 * Create a new buffered JPanel with the specified layout manager
	 *
	 * @param layout  the LayoutManager to use
	 * @param helpListener  the help listener to use
	 */
	public GUILabeledBoxPanel(LayoutManager layout, GUIHelpListener helpListener) {
		super(layout);
		try { jbInit(helpListener); }
		catch ( Exception e ) { e.printStackTrace(); }
		if( editorPanel != null ) editorPanel.setLayout(layout);
	}

	/**
	 * Creates a new <code>JPanel</code> with <code>FlowLayout</code>
	 * and the specified buffering strategy.
	 * If <code>isDoubleBuffered</code> is true, the <code>JPanel</code>
	 * will use a double buffer.
	 *
	 * @param layout  the LayoutManager to use
	 * @param isDoubleBuffered  a boolean, true for double-buffering, which
	 *        uses additional memory space to achieve fast, flicker-free
	 *        updates
	 * @param helpListener  the help listener to use
	 */
	public GUILabeledBoxPanel(boolean isDoubleBuffered, GUIHelpListener helpListener) {
		super(isDoubleBuffered);
		try { jbInit(helpListener); }
		catch ( Exception e ) { e.printStackTrace(); }
		if( editorPanel != null ) editorPanel.setLayout(new FlowLayout());
	}

	/**
	 * Creates a new <code>JPanel</code> with a double buffer
	 * and a flow layout.
	 *
	 * @param helpListener  the help listener to use
	 */
	public GUILabeledBoxPanel(GUIHelpListener helpListener) {
		super();
		try { jbInit(helpListener); }
		catch ( Exception e ) { e.printStackTrace(); }

		if( editorPanel != null ) editorPanel.setLayout(new FlowLayout());
	}


	/**
	 * Sets the layout manager for this container.
	 * @param mgr the specified layout manager
	 * @see #doLayout
	 * @see #getLayout
	 */
	public void setLayout(LayoutManager mgr) {
		if( addDefault ) super.setLayout(mgr);
		else if( editorPanel != null ) editorPanel.setLayout(mgr);

	}

	/**
	 *  Sets the title in this boxPanel
	 *
	 * @param  newTitle  The new title value
	 */
	public void setTitle( String newTitle ) {
		title = newTitle;
		String labelTitle = title;
		if (help_button != null) {
			labelTitle = "     " + labelTitle;	// improves centering when help button is displayed
		}
		headerLabel.setText( labelTitle );
	}

	public void setHeaderPanelBackgroundColor( Color background ){
		headerPanelBackgroundColor = background;
		if ( headerPanel != null ) headerPanel.setBackground( headerPanelBackgroundColor );
	}

	public Color getHeaderPanelBackgroundColor(  ){
		return headerPanelBackgroundColor;
	}

	/**
	 *  Gets the title in this boxPanel
	 *
	 * @return    The title value
	 */
	public String getTitle() { return title; }

	/**
	 *  Gets the borderColor of this boxPanel
	 *
	 * @return    The borderColor value
	 */
	public Color getBorderColor() { return borderColor; }


	/**
	 * Initializes the GUI components and layout
	 * @throws Exception
	 */
	protected final void jbInit() throws Exception {

		addDefault = true;

		border = new MatteBorder(0,0,1,0,borderColor);
		border2 = new LineBorder(borderColor);

		//this.setBackground( Color.white );
		this.setBorder( border2 );
		this.setLayout( GBL );

		editorPanel.setLayout( GBL );

		headerPanel.setLayout( GBL );
		headerLabel.setFont( new java.awt.Font( "SansSerif", 1, 12 ) );
		headerLabel.setForeground( borderColor );
		headerLabel.setText( "Title" );
		headerPanel.setBackground( headerPanelBackgroundColor );
		headerPanel.setBorder( border );

		//editorPanel.setBackground( Color.white );
		jScrollPane1.setBorder( null );

		add( jScrollPane1, new GridBagConstraints( 0, 1, 1, 1, 1.0, 1.0
				, GridBagConstraints.NORTH, GridBagConstraints.BOTH, new Insets( 5, 7, 3, 8 ), 0, 0 ) );
		add( headerPanel, new GridBagConstraints( 0, 0, 1, 1, 1.0, 0.0
				, GridBagConstraints.CENTER, GridBagConstraints.HORIZONTAL, new Insets( 0, 0, 0, 0 ), 0, 0 ) );

		headerPanel.add( headerLabel,
				new GridBagConstraints( 0, 0, 1, 1, 0.0, 0.0, GridBagConstraints.CENTER, GridBagConstraints.NONE,
				new Insets( 1, 1, 0, 1 ), 0, 0 ) );

		jScrollPane1.getViewport().add( editorPanel, ( Object ) null );
		addDefault = false;
	}


	/**
	 * Initializes the GUI components and layout, with a help button, first version.
	 * This version lays out the panel in a two-column grid.
	 * @throws Exception
	 */
	protected final void jbInit_v1 (GUIHelpListener helpListener) throws Exception {

		help_listener = helpListener;

		addDefault = true;

		border = new MatteBorder(0,0,1,0,borderColor);
		border2 = new LineBorder(borderColor);

		//this.setBackground( Color.white );
		this.setBorder( border2 );
		this.setLayout( GBL );

		editorPanel.setLayout( GBL );

		headerPanel.setLayout( GBL );
		headerLabel.setFont( new java.awt.Font( "SansSerif", java.awt.Font.BOLD, 12 ) );
		headerLabel.setForeground( borderColor );
		headerLabel.setText( "Title" );
		headerPanel.setBackground( headerPanelBackgroundColor );
		headerPanel.setBorder( border );

		//editorPanel.setBackground( Color.white );
		jScrollPane1.setBorder( null );

		help_button = new JButton (" ? ");
		help_button.setFont( new java.awt.Font( "SansSerif", java.awt.Font.BOLD, 12 ) );
		help_button.setMargin(new Insets(0, 0, 0, 0));

		Dimension headerPref = headerPanel.getPreferredSize();
		Dimension headerMin = headerPanel.getMinimumSize();
		Dimension buttonPref = help_button.getPreferredSize();
		Dimension buttonMin = help_button.getMinimumSize();
		// Make button height no taller than the header
		buttonPref.height = Math.min (buttonPref.height, headerPref.height);
		buttonMin.height = Math.min (buttonMin.height, headerMin.height);
		//// Make button width at least as large as the height (produces a square)
		//buttonPref.width = Math.max (buttonPref.height, buttonPref.width);
		//buttonMin.width = Math.max (buttonMin.height, buttonMin.width);
		help_button.setPreferredSize (buttonPref);
		help_button.setMinimumSize (buttonMin);

		help_button.addActionListener (new ActionListener(){
			@Override
			public void actionPerformed (ActionEvent e) {
				if (help_listener != null) {
					help_listener.invoke_help_request (help_button);
				}
				return;
			}
		});

		add( jScrollPane1, new GridBagConstraints( 0, 1, 2, 1, 1.0, 1.0
				, GridBagConstraints.NORTH, GridBagConstraints.BOTH, new Insets( 5, 7, 3, 8 ), 0, 0 ) );
		add( headerPanel, new GridBagConstraints( 0, 0, 1, 1, 1.0, 0.0
				, GridBagConstraints.CENTER, GridBagConstraints.HORIZONTAL, new Insets( 0, 0, 0, 0 ), 0, 0 ) );
		add( help_button, new GridBagConstraints( 1, 0, 1, 1, 0.0, 0.0
				, GridBagConstraints.CENTER, GridBagConstraints.BOTH, new Insets( 0, 0, 0, 0 ), 0, 0 ) );

		headerPanel.add( headerLabel,
				new GridBagConstraints( 0, 0, 1, 1, 0.0, 0.0, GridBagConstraints.CENTER, GridBagConstraints.NONE,
				new Insets( 1, 1, 0, 1 ), 0, 0 ) );

		jScrollPane1.getViewport().add( editorPanel, ( Object ) null );
		addDefault = false;
	}


	/**
	 * Initializes the GUI components and layout, with a help button
	 * @throws Exception
	 */
	protected final void jbInit (GUIHelpListener helpListener) throws Exception {

		help_listener = helpListener;

		addDefault = true;

		border = new MatteBorder(0,0,1,0,borderColor);
		border2 = new LineBorder(borderColor);

		//this.setBackground( Color.white );
		this.setBorder( border2 );
		this.setLayout( GBL );

		editorPanel.setLayout( GBL );

		headerPanel.setLayout( GBL );
		headerLabel.setFont( new java.awt.Font( "SansSerif", java.awt.Font.BOLD, 12 ) );
		headerLabel.setForeground( borderColor );
		headerLabel.setText( "Title" );
		headerPanel.setBackground( headerPanelBackgroundColor );
		headerPanel.setBorder( border );

		//editorPanel.setBackground( Color.white );
		jScrollPane1.setBorder( null );

		help_button = new JButton (" ? ");
		help_button.setFont( new java.awt.Font( "SansSerif", java.awt.Font.BOLD, 12 ) );
		help_button.setMargin(new Insets(0, 0, 0, 0));

		Dimension headerPref = headerPanel.getPreferredSize();
		Dimension headerMin = headerPanel.getMinimumSize();
		Dimension buttonPref = help_button.getPreferredSize();
		Dimension buttonMin = help_button.getMinimumSize();
		// Make button height no taller than the header
		buttonPref.height = Math.min (buttonPref.height, headerPref.height);
		buttonMin.height = Math.min (buttonMin.height, headerMin.height);
		//// Make button width at least as large as the height (produces a square)
		//buttonPref.width = Math.max (buttonPref.height, buttonPref.width);
		//buttonMin.width = Math.max (buttonMin.height, buttonMin.width);
		help_button.setPreferredSize (buttonPref);
		help_button.setMinimumSize (buttonMin);

		help_button.addActionListener (new ActionListener(){
			@Override
			public void actionPerformed (ActionEvent e) {
				if (help_listener != null) {
					help_listener.invoke_help_request (help_button);
				}
				return;
			}
		});

		JPanel title_panel = new JPanel();
		title_panel.setLayout( GBL );

		add( jScrollPane1, new GridBagConstraints( 0, 1, 1, 1, 1.0, 1.0
				, GridBagConstraints.NORTH, GridBagConstraints.BOTH, new Insets( 5, 7, 3, 8 ), 0, 0 ) );
		add( title_panel, new GridBagConstraints( 0, 0, 1, 1, 1.0, 0.0
				, GridBagConstraints.CENTER, GridBagConstraints.HORIZONTAL, new Insets( 0, 0, 0, 0 ), 0, 0 ) );

		title_panel.add( headerPanel, new GridBagConstraints( 0, 0, 1, 1, 1.0, 1.0
				, GridBagConstraints.CENTER, GridBagConstraints.HORIZONTAL, new Insets( 0, 0, 0, 0 ), 0, 0 ) );
		title_panel.add( help_button, new GridBagConstraints( 1, 0, 1, 1, 0.0, 1.0
				, GridBagConstraints.CENTER, GridBagConstraints.BOTH, new Insets( 0, 0, 0, 0 ), 0, 0 ) );

		headerPanel.add( headerLabel,
				new GridBagConstraints( 0, 0, 1, 1, 0.0, 0.0, GridBagConstraints.CENTER, GridBagConstraints.NONE,
				new Insets( 1, 1, 0, 1 ), 0, 0 ) );

		jScrollPane1.getViewport().add( editorPanel, ( Object ) null );
		addDefault = false;
	}
	

	public JScrollPane getScrollPane() {
		return jScrollPane1;
	}

	public JComponent getContents() {
		return editorPanel;
	}

	public void addPanel(JPanel panel){

		// editorPanel.add( panel, new GridBagConstraints( 0, counter, 1, 1, 1.0, 0.0
//                    /, GridBagConstraints.CENTER, GridBagConstraints.HORIZONTAL, new Insets( 0, 0, 0, 0 ), 0, 0 ) );

	}


	/**
	 * Appends the specified component to the end of this container.
	 * This is a convenience method for {@link #addImpl}.
	 *
	 * @param     comp   the component to be added
	 * @see #addImpl
	 * @return    the component argument
	 */
	public Component add(Component comp) {
		if( addDefault ) return super.add(comp);
		else return editorPanel.add(comp);
	}

	/**
	 * Adds the specified component to this container.
	 * This is a convenience method for {@link #addImpl}.
	 * <p>
	 * This method is obsolete as of 1.1.  Please use the
	 * method <code>add(Component, Object)</code> instead.
	 */
	public Component add(String name, Component comp) {
		if( addDefault ) return super.add(name, comp);
		else return editorPanel.add(name, comp);
	}

	/**
	 * Adds the specified component to this container at the given
	 * position.
	 * This is a convenience method for {@link #addImpl}.
	 *
	 * @param     comp   the component to be added
	 * @param     index    the position at which to insert the component,
	 *                   or <code>-1</code> to append the component to the end
	 * @return    the component <code>comp</code>
	 * @see #addImpl
	 * @see	  #remove
	 */
	public Component add(Component comp, int index) {
		if( addDefault ) return super.add(comp, index);
		else return editorPanel.add(comp, index);
	}


	/**
	 * Adds the specified component to the end of this container.
	 * Also notifies the layout manager to add the component to
	 * this container's layout using the specified constraints object.
	 * This is a convenience method for {@link #addImpl}.
	 *
	 * @param     comp the component to be added
	 * @param     constraints an object expressing
	 *                  layout contraints for this component
	 * @see #addImpl
	 * @see       LayoutManager
	 * @since     JDK1.1
	 */
	public void add(Component comp, Object constraints) {
		if( addDefault ) super.add(comp, constraints);
		else editorPanel.add(comp, constraints);
	  }


	  /**
	   * Removes the component, specified by index, from this container
	   *
	   * @param index the index of the component to be removed
	   */
	  public void remove(int index) {
		if( addDefault ) super.remove(index);
		else editorPanel.remove(index);
	  }


	  /**
	   * Removes the specified component from this container.
	   *
	   * @param comp
	   */
	  public void remove(Component comp) {
		if( addDefault ) super.remove(comp);
		else editorPanel.remove(comp);
	  }

	  /**
	   * Removes all the components from this container.
	   */
	  public void removeAll() {
		if( addDefault ) super.removeAll();
		else editorPanel.removeAll();
	  }


	/**
	 * Adds the specified component to this container with the specified
	 * constraints at the specified index.  Also notifies the layout
	 * manager to add the component to the this container's layout using
	 * the specified constraints object.
	 * This is a convenience method for {@link #addImpl}.
	 *
	 * @param comp the component to be added
	 * @param constraints an object expressing layout contraints for this
	 * @param index the position in the container's list at which to insert
	 * the component. -1 means insert at the end.
	 * component
	 * @see #addImpl
	 * @see #remove
	 * @see LayoutManager
	 */
	public void add(Component comp, Object constraints, int index) {
		if( addDefault ) super.add(comp, constraints, index);
		else editorPanel.add(comp, constraints, index);
	}


}
