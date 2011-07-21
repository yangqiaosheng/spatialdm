package de.fraunhofer.iais.ta;

import java.util.HashMap;

import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.PrecisionModel;
import com.vividsolutions.jts.io.WKTWriter;

public class GeoConfigContext {

	final public static int SRID = 4326;
	final public static GeometryFactory GEOMETRY_FACTORY = new GeometryFactory(new PrecisionModel(PrecisionModel.FLOATING), SRID);
}
