package org.opensha.oaf.util;

import java.util.List;
import java.util.ArrayList;

import java.util.regex.Matcher;


// Class to construct a table that can be displayed in monospaced Ascii text..
// Author: Michael Barall.

public class AsciiTable {


	//----- Constants -----


	// Enumeration of horizontal alignments.

	public static enum HorzAlign {
		//DEFAULT("Default"),
		LEFT("Left"),
		CENTER("Center"),
		RIGHT("Right");
		
		private String label;
		
		private HorzAlign(String label) {
			this.label = label;
		}
		
		@Override
		public String toString() {
			return label;
		}
	}


	// Enumeration of vertical alignments.

	public static enum VertAlign {
		//DEFAULT("Default"),
		TOP("Top"),
		MIDDLE("Middle"),
		BOTTOM("Bottom");
		
		private String label;
		
		private VertAlign(String label) {
			this.label = label;
		}
		
		@Override
		public String toString() {
			return label;
		}
	}


	// Enumeration of corner types.

	public static enum CornerType {
		SOLID("Solid"),		// solid row
		SPLIT("Split"),		// row split by column separators
		MIXED("Mixed");		// mix row and column separators
		
		private String label;
		
		private CornerType(String label) {
			this.label = label;
		}
		
		@Override
		public String toString() {
			return label;
		}
	}




	//----- Internal subroutines -----




	// Append the specified number of spaces.

	private static void append_spaces (StringBuilder sb, int num) {
		for (int j = 0; j < num; ++j) {
			sb.append (" ");
		}
		return;
	}




	// Append the specifid string the specified number of times.

	private static void append_multiple (StringBuilder sb, String x, int num) {
		for (int j = 0; j < num; ++j) {
			sb.append (x);
		}
		return;
	}




	// Intersect a row separator with a column separator.
	// Parameters:
	//  row_sep = Row separator, a single-character string.
	//  col_sep = Column separator.
	// Returns a column separator to be used within the row.

	private static String intersect_row_col_sep (String row_sep, String col_sep) {

		// Replace each space in the column separator with the row separator

		String result = col_sep.replace (' ', row_sep.charAt(0));

		// If the row separator is dash then replace each vertical bar in the column separator with a plus

		if (row_sep.equals ("-")) {
			result = result.replace ('|', '+');
		}

		return result;
	}




	//----- Data Structures -----




	// One cell in the table.

	private static class Cell {

		// Horizontal alignment.

		private HorzAlign horz_align;

		// Vertical aligment.

		private VertAlign vert_align;

		// The text in the cell, an array containing the text for each line within the cell.

		private String[] cell_text;

		// The horizontal size of the cell.

		private int horz_size;

		// The vertical size of the cell.

		private int vert_size;

		// Construct the cell, if the text is empty or null then make an empty cell.
		// The text is split on newlines to create multiple lines.

		public Cell (HorzAlign horz_align, VertAlign vert_align, String text) {
			this.horz_align = horz_align;
			this.vert_align = vert_align;
			if (text == null || text.isEmpty()) {
				cell_text = new String[0];
			} else {
				cell_text = text.split ("\\n");
			}

			horz_size = 0;
			for (String s : cell_text) {
				horz_size = Math.max (horz_size, s.length());
			}

			vert_size = cell_text.length;
		}

		// Get the horizontal size of the cell.

		public final int get_horz_size () {
			return horz_size;
		}

		// Get the vertical size of the cell.

		public final int get_vert_size () {
			return vert_size;
		}

		// Append text for this cell to the string builder.
		// Parameters:
		//  sb = Destination.
		//  col_width = Column width.
		//  row_height = Row height.
		//  row_line = Line within the row (0-based).

		public final void append_text (StringBuilder sb, int col_width, int row_height, int row_line) {

			// Calculate the vertical index

			int vert_ix = row_line;
			int blank_lines = row_height - vert_size;
			if (blank_lines > 0) {
				switch (vert_align) {
				case TOP:
					break;
				case MIDDLE:
					vert_ix -= (blank_lines / 2);
					break;
				case BOTTOM:
					vert_ix -= blank_lines;
				}
			}

			// Handle blank line

			if (vert_ix < 0 || vert_ix >= vert_size) {
				append_spaces (sb, col_width);
				return;
			}

			// Calculate padding

			String text = cell_text[vert_ix];
			int len = text.length();

			// If the text is wider than the column (should not happen), just display the initial substring

			if (len > col_width) {
				sb.append (text.substring (0, col_width));
				return;
			}

			// Left and right padding

			int pad_left = 0;
			int pad_right = 0;
			switch (horz_align) {
			case LEFT:
				pad_right = col_width - len;
				break;
			case CENTER:
				pad_left = (col_width - len) / 2;
				pad_right = col_width - (len + pad_left);
				break;
			case RIGHT:
				pad_left = col_width - len;
			}

			// Write line with padding

			append_spaces (sb, pad_left);
			sb.append (text);
			append_spaces (sb, pad_right);
			return;
		}

	}




	// One row in the table, either a row of cells or a filler row.

	private static class Row {

		// For a row of cells, the cells.

		private List<Cell> cells;

		// For a filler row, the row separator character (a one-character string)

		private String row_sep;

		// For a filler row, the corner type for row-column separator intersections.

		private CornerType corner_type;

		// The height of this row.

		private int row_height;

		// Make a row of cells.

		public Row () {
			this.cells = new ArrayList<Cell>();
			this.row_sep = null;
			this.corner_type = null;
			this.row_height = 0;
		}

		// Add a cell to the row.

		public final void add_cell (Cell cell) {
			if (cells == null) {
				throw new RuntimeException ("AsciiTable.Row.add_cell: Attempt to add cell to a filler row");
			}
			cells.add (cell);
			row_height = Math.max (row_height, cell.get_vert_size());
			return;
		}

		// Get the width of the given cell.
		// Returns 0 if the index is after the last cell, or if this is a filler row.

		public final int get_cell_width (int cell_index) {
			if (cells == null || cell_index >= cells.size()) {
				return 0;
			}
			return cells.get(cell_index).get_horz_size();
		}

		// Get the number of cells.
		// Returns 0 if this is a filler row.

		public final int get_cell_count () {
			if (cells == null) {
				return 0;
			}
			return cells.size();
		}

		// Return true if this is a row of cells.

		public final boolean has_cells () {
			return cells != null;
		}

		// Make a filler row with the given separator and intersection flag.

		public Row (String row_sep, CornerType corner_type) {
			if (!( row_sep != null && row_sep.length() == 1 )) {
				throw new IllegalArgumentException ("AsciiTable.Row: Invalid row separator");
			}
			this.cells = null;
			this.row_sep = row_sep;
			this.corner_type = corner_type;
			this.row_height = 1;
		}

		// Update column widths, so they are large enough for this row's cells.

		public final void update_column_widths (List<Integer> col_widths) {
			if (cells == null) {
				return;
			}
			for (int j = 0; j < cells.size(); ++j) {
				int cell_width = cells.get(j).get_horz_size();
				if (j < col_widths.size()) {
					int wj = col_widths.get(j);
					col_widths.set (j, Math.max (wj, cell_width));
				} else {
					col_widths.add (cell_width);
				}
			}
			return;
		}

		// Append a column separator, for a filler row.

		private void append_col_sep (StringBuilder sb, String col_sep) {
			switch (corner_type) {
			case SOLID:
				append_multiple (sb, row_sep, col_sep.length());
				break;
			case SPLIT:
				sb.append (col_sep);
				break;
			case MIXED:
				sb.append (intersect_row_col_sep (row_sep, col_sep));
				break;
			}
			return;
		}

		// Append the text of the row.
		// Parameters:
		//  sb = Destination.
		//  col_widths = Width of each column.
		//  col_seps = Column separators, must have one entry more that col_widths..

		public final void append_text (StringBuilder sb, List<Integer> col_widths, List<String> col_seps) {

			// Handle filler row

			if (cells == null) {
				for (int j = 0; j < col_widths.size(); ++j) {
					append_col_sep (sb, col_seps.get(j));
					append_multiple (sb, row_sep, col_widths.get(j));
				}
				append_col_sep (sb, col_seps.get(col_widths.size()));
				sb.append ("\n");
				return;
			}

			// Loop over lines in the row

			for (int row_line = 0; row_line < row_height; ++row_line) {

				// Write one line of the row

				for (int j = 0; j < col_widths.size(); ++j) {
					sb.append (col_seps.get(j));
					if (j < cells.size()) {
						cells.get(j).append_text (sb, col_widths.get(j), row_height, row_line);
					} else {
						append_spaces (sb, col_widths.get(j));
					}
				}
				sb.append (col_seps.get(col_widths.size()));
				sb.append ("\n");
			}

			return;
		}
	}




	//----- Contents -----


	// The list of rows.

	private List<Row> rows;


	// The column separators for the left and right borders.

	private String left_col_sep;
	private String right_col_sep;

	// The interior column separators.
	// The last separator is repeated as many times as needed.

	private String[] interior_col_sep;




	//----- Construction -----




	// Clear the contents.

	public final void clear () {
		rows = new ArrayList<Row>();

		left_col_sep = "";
		right_col_sep = "";
		interior_col_sep = new String[]{" | "};
		return;
	}




	// Construct an empty table.

	public AsciiTable () {
		clear();
	}




	// Get the number of rows.

	public final int get_row_count () {
		return rows.size();
	}




	// Begin a row of cells.

	public final void add_row () {
		rows.add (new Row());
		return;
	}




	// Make a filler row.

	public final void add_row (String row_sep, CornerType corner_type) {
		rows.add (new Row (row_sep, corner_type));
		return;
	}




	// Make a filler row with the indicated corner type.

	public final void add_row_solid (String row_sep) {
		add_row (row_sep, CornerType.SOLID);
		return;
	}

	public final void add_row_split (String row_sep) {
		add_row (row_sep, CornerType.SPLIT);
		return;
	}

	public final void add_row_mixed (String row_sep) {
		add_row (row_sep, CornerType.MIXED);
		return;
	}




	// Add a cell to the current row.

	public final void add_cell (HorzAlign horz_align, VertAlign vert_align, String text) {
		rows.get(rows.size() - 1).add_cell (new Cell (horz_align, vert_align, text));
		return;
	}




	// Add a cell to the current row, with designated horiztonal aligment and top vertical alignment.

	public final void add_cell_left (String text) {
		add_cell (HorzAlign.LEFT, VertAlign.TOP, text);
		return;
	}

	public final void add_cell_center (String text) {
		add_cell (HorzAlign.CENTER, VertAlign.TOP, text);
		return;
	}

	public final void add_cell_right (String text) {
		add_cell (HorzAlign.RIGHT, VertAlign.TOP, text);
		return;
	}




	// Set the column separators.

	public final void set_column_seps (String left_col_sep, String right_col_sep, String... interior_col_sep) {
		if (!( interior_col_sep != null && interior_col_sep.length >= 1 )) {
			throw new IllegalArgumentException ("AsciiTable.set_column_seps: Interior column separates are not specified");
		}

		this.left_col_sep = left_col_sep;
		this.right_col_sep = right_col_sep;
		this.interior_col_sep = interior_col_sep.clone();
		return;
	}




	// Append the table.

	public final void append_table (StringBuilder sb) {

		// Construct the column widths

		List<Integer> col_widths = new ArrayList<Integer>();
		for (Row row : rows) {
			row.update_column_widths (col_widths);
		}

		// Construct the column separators

		List<String> col_seps = new ArrayList<String>();
		col_seps.add (left_col_sep);
		for (int j = 0; j < col_widths.size() - 1; ++j) {
			col_seps.add (interior_col_sep[Math.min (j, interior_col_sep.length - 1)]);
		}
		col_seps.add (right_col_sep);

		// Write the rows

		for (Row row : rows) {
			row.append_text (sb, col_widths, col_seps);
		}

		return;
	}




	// Write table as a string.

	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder();
		append_table (sb);
		return sb.toString();
	}




	//----- Testing -----




	// Make a test table.

	private static AsciiTable make_test_table_1 () {
		AsciiTable table = new AsciiTable();

		table.add_row_solid ("^");

		table.add_row();
		table.add_cell_left ("abcdefghi");
		table.add_cell_left ("L");
		table.add_cell_center ("C");
		table.add_cell_right ("R");

		table.add_row_mixed ("=");

		table.add_row();
		table.add_cell_left ("ABCDEFGHI");
		table.add_cell_left ("LL");
		table.add_cell_center ("CC");
		table.add_cell_right ("RR");

		table.add_row_mixed ("-");

		table.add_row();
		table.add_cell_left ("123456789");
		table.add_cell_left ("987654321");
		table.add_cell_center ("QWERTY");
		table.add_cell_right ("SHRDLU");

		table.add_row_mixed ("-");

		table.add_row();
		table.add_cell_left ("");
		table.add_cell_left ("one\ntwo\nthree");
		table.add_cell (HorzAlign.LEFT, VertAlign.TOP, "T");
		table.add_cell (HorzAlign.CENTER, VertAlign.MIDDLE, "M");
		table.add_cell (HorzAlign.RIGHT, VertAlign.BOTTOM, "B");
		table.add_cell_center ("four\nfifteen\nsix");
		table.add_cell_right ("seven\neighteen\nnine");

		table.add_row_split ("~");

		table.set_column_seps ("> ", " <", " ' ", " | ");

		return table;
	}




	public static void main(String[] args) {
		try {
		TestArgs testargs = new TestArgs (args, "AsciiTable");




		// Subcommand : Test #1
		// Command format:
		//  test1
		// Construct an object of type AsciiTable and display it.

		if (testargs.is_test ("test1")) {

			// Read arguments

			System.out.println ("Constructing AsciiTable with test values");
			testargs.end_test();

			// Create the named value

			AsciiTable table = make_test_table_1();

			// Display

			System.out.println ();
			System.out.println (table.toString());

			// Done

			System.out.println ();
			System.out.println ("Done");

			return;
		}



		
		// Unrecognized subcommand, or exception

		testargs.unrecognized_test();
		} catch (Exception e) {
		e.printStackTrace();
		}
		return;
	}




}
