
package com.atakmap.android.tilecapture;

import com.atakmap.coremap.maps.coords.GeoPoint;

/**
 * Parameters for tile capture
 */
public class TileCaptureParams {

    // Points along which to capture tiles
    public GeoPoint[] points;

    // True if the points form a closed shape - false if they form a line
    public boolean closedPoints;

    // Map level
    public int level = -1;

    // Map resolution
    public double mapResolution;

    // Capture resolution (lowest = 1, highest = 5)
    public int captureResolution = 1;

    // True to fit the imagery to the provided closed points quad
    // Requires that points.length == 4
    public boolean fitToQuad;

    // The aspect ratio of the quad
    public double fitAspect = 1;

    // If the minor dimension of the full capture is less than this value,
    // it will be uniformly scaled so the minor dimension matches this size
    public int minImageSize;
}
