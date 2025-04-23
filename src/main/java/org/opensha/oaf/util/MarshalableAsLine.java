package org.opensha.oaf.util;


// Interface represeting an object that can be marshaled and unmarshaled to a single line.
// This is generally a simple object that do not require versioning.
// Author: Michael Barall.

public interface MarshalableAsLine {


	// Marshal object to a single unadorned line of text.

	public String marshal_to_line ();

	// Unmarshal object from a single unadorned line of text.

	public MarshalableAsLine unmarshal_from_line (String line);

}
