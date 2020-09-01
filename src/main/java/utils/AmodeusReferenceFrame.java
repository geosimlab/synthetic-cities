package utils;

import org.matsim.core.utils.geometry.CoordinateTransformation;
import org.matsim.core.utils.geometry.transformations.GeotoolsTransformation;
import org.matsim.core.utils.geometry.transformations.IdentityTransformation;

import ch.ethz.idsc.amodeus.data.ReferenceFrame;
import ch.ethz.idsc.amodeus.util.math.SI;
import ch.ethz.idsc.tensor.qty.Unit;

public enum AmodeusReferenceFrame implements ReferenceFrame {
    IDENTITY( //
            new IdentityTransformation(), //
            new IdentityTransformation()), //
    SANFRANCISCO( //
            new GeotoolsTransformation("EPSG:26743", "WGS84"), //
            new GeotoolsTransformation("WGS84", "EPSG:26743"), //
            Unit.of("ft")), //
    TEL_AVIV( //
            new GeotoolsTransformation("EPSG:2039", "WGS84"), //
            new GeotoolsTransformation("WGS84", "EPSG:2039")),//
    ;
    // ---
    private final CoordinateTransformation coords_toWGS84;
    private final CoordinateTransformation coords_fromWGS84;
    private final Unit unit;

    private AmodeusReferenceFrame(CoordinateTransformation c1, CoordinateTransformation c2) {
        this(c1, c2, SI.METER);
    }

    private AmodeusReferenceFrame(CoordinateTransformation c1, CoordinateTransformation c2, Unit unit) {
        coords_toWGS84 = c1;
        coords_fromWGS84 = c2;
        this.unit = unit;
    }

    @Override
    public CoordinateTransformation coords_fromWGS84() {
        return coords_fromWGS84;
    }

    @Override
    public CoordinateTransformation coords_toWGS84() {
        return coords_toWGS84;
    }

    @Override
    public Unit unit() {
        return unit;
    }
}
