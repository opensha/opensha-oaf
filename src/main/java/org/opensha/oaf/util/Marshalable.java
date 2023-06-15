package org.opensha.oaf.util;


// Interface represeting an object that can be marshaled and unmarshaled.
// Author: Michael Barall 06/14/2023.

public interface Marshalable {


	// Marshal object.
	// Parameters:
	//  writer = Data destination for marshaling.
	//  name = Tag if in a map context, or null if in an array context.

	public void marshal (MarshalWriter writer, String name);


	// Unmarshal object.
	// Parameters:
	//  reader = Data source for marshaling.
	//  name = Tag if in a map context, or null if in an array context.
	// Returns this object.
	// Note: The subclass typically declares the return type to be its own type.

	public Marshalable unmarshal (MarshalReader reader, String name);

}
