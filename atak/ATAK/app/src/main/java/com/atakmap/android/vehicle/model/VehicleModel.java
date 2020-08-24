
package com.atakmap.android.vehicle.model;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PointF;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffColorFilter;
import android.graphics.drawable.Drawable;
import android.os.Bundle;

import com.atakmap.android.cot.detail.CotDetailManager;
import com.atakmap.android.icons.UserIcon;
import com.atakmap.android.imagecapture.CanvasHelper;
import com.atakmap.android.imagecapture.Capturable;
import com.atakmap.android.imagecapture.CapturePP;
import com.atakmap.android.maps.MapGroup;
import com.atakmap.android.maps.MapItem;
import com.atakmap.android.maps.Marker;
import com.atakmap.android.maps.PointMapItem;
import com.atakmap.android.rubbersheet.data.ModelProjection;
import com.atakmap.android.rubbersheet.data.RubberModelData;
import com.atakmap.android.rubbersheet.data.RubberSheetUtils;
import com.atakmap.android.rubbersheet.maps.LoadState;
import com.atakmap.android.rubbersheet.maps.RubberModel;
import com.atakmap.android.util.ATAKUtilities;
import com.atakmap.android.vehicle.VehicleMapItem;
import com.atakmap.android.vehicle.model.icon.GLOffscreenCaptureService;
import com.atakmap.android.vehicle.model.icon.VehicleModelCaptureRequest;
import com.atakmap.app.R;
import com.atakmap.coremap.cot.event.CotDetail;
import com.atakmap.coremap.cot.event.CotEvent;
import com.atakmap.coremap.cot.event.CotPoint;
import com.atakmap.coremap.filesystem.FileSystemUtils;
import com.atakmap.coremap.log.Log;
import com.atakmap.coremap.maps.coords.DistanceCalculations;
import com.atakmap.coremap.maps.coords.GeoPoint;
import com.atakmap.coremap.maps.coords.GeoPointMetaData;
import com.atakmap.coremap.maps.coords.NorthReference;
import com.atakmap.coremap.maps.time.CoordinatedTime;
import com.atakmap.map.layer.feature.geometry.Envelope;
import com.atakmap.map.layer.model.Model;
import com.atakmap.map.layer.model.ModelInfo;

import java.io.File;
import java.io.FileOutputStream;
import java.util.List;

/**
 * 3D vehicle model
 */
public class VehicleModel extends RubberModel implements Capturable,
        VehicleMapItem, Marker.OnTrackChangedListener {

    private static final String TAG = "VehicleModel";
    public static final String COT_TYPE = "u-d-v-m";

    // In order to satisfy the rubber model constructor we need to pass
    // a struct containing some background info
    private static class VehicleModelData extends RubberModelData {

        String uid;
        VehicleModelInfo info;

        VehicleModelData(VehicleModelInfo info, GeoPointMetaData center,
                String uid) {

            this.info = info;
            this.label = info.name;
            this.center = center;
            this.uid = uid;
            this.rotation = new double[] {
                    0, 0, 0
            };
            this.scale = new double[] {
                    1, 1, 1
            };
            this.projection = ModelProjection.ENU_FLIP_YZ;
            this.dimensions = new double[] {
                    10, 10, 10
            };
            this.file = info.file;
            Model model = info.getModel();
            if (model != null) {
                Envelope e = model.getAABB();
                this.dimensions[0] = Math.abs(e.maxX - e.minX);
                this.dimensions[1] = Math.abs(e.maxY - e.minY);
                this.dimensions[2] = Math.abs(e.maxZ - e.minZ);
            }

            this.points = RubberSheetUtils.computeCorners(center.get(),
                    this.dimensions[1], this.dimensions[0], 0);
        }

        @Override
        public String getUID() {
            return this.uid;
        }
    }

    private VehicleModelInfo _info;
    private double _width, _length, _height;

    public VehicleModel(VehicleModelInfo info, GeoPointMetaData center,
            String uid) {
        this(new VehicleModelData(info, center, uid));
    }

    public VehicleModel(VehicleModelData data) {
        super(data);
        setType(COT_TYPE);
        setSharedModel(true);
        showLines(false);
        setMetaString("iconUri", ATAKUtilities.getResourceUri(
                R.drawable.pointtype_aircraft));
        removeMetaData("nevercot");
        setMetaBoolean("archive", true);
        setLabelVisibility(true);
        setVehicleInfo(data.info);
    }

    public void setVehicleInfo(VehicleModelInfo info) {
        if (_info == info)
            return;

        _info = info;
        VehicleModelCache.getInstance().registerUsage(info, getUID());
        onLoad(new ModelInfo(info.getInfo()), info.getModel());
        double[] dim = getModelDimensions(false);
        _width = dim[0];
        _length = dim[1];
        _height = dim[2];
        updateIconPath();
        setLoadState(LoadState.SUCCESS);
    }

    public VehicleModelInfo getVehicleInfo() {
        return _info;
    }

    @Override
    protected String getCotType() {
        return COT_TYPE;
    }

    @Override
    public Drawable getIconDrawable() {
        return _info.getIcon();
    }

    @Override
    protected String getMenuPath() {
        return "menus/vehicle_model_menu.xml";
    }

    @Override
    public double getWidth() {
        return _width;
    }

    @Override
    public double getLength() {
        return _length;
    }

    @Override
    public double getHeight() {
        return _height;
    }

    @Override
    public void setCenterMarker(Marker marker) {
        Marker existing = getCenterMarker();
        if (marker != null && existing == marker)
            return;

        if (existing != null)
            existing.removeOnTrackChangedListener(this);

        super.setCenterMarker(marker);

        if (marker != null && !isCenterShapeMarker()) {
            setHeading(marker.getTrackHeading());
            marker.addOnTrackChangedListener(this);
        }
    }

    public void setCenter(GeoPointMetaData point) {
        setPoints(point, getWidth(), getLength(), getHeading());
    }

    /**
     * Set the azimuth/heading of this vehicle model
     * @param deg Degrees
     * @param ref Reference the degrees are in
     */
    @Override
    public void setAzimuth(double deg, NorthReference ref) {
        double trueDeg = deg;
        if (ref.equals(NorthReference.MAGNETIC))
            trueDeg = ATAKUtilities.convertFromMagneticToTrue(
                    getCenterPoint(), deg);
        trueDeg = (trueDeg % 360) + (trueDeg < 0 ? 360 : 0);
        if (Double.compare(getAzimuth(NorthReference.TRUE), trueDeg) != 0)
            setHeading(deg);
    }

    /**
     * Get the azimuth/heading of this vehicle model
     * @param ref Desired reference
     * @return Azimuth in degrees
     */
    @Override
    public double getAzimuth(NorthReference ref) {
        double azi = getHeading();
        if (ref.equals(NorthReference.MAGNETIC))
            return ATAKUtilities.convertFromTrueToMagnetic(
                    getCenterPoint(), azi);
        return azi;
    }

    @Override
    public void setVisible(boolean visible) {
        super.setVisible(visible);
        for (PointMapItem pmi : getAnchorMarkers())
            pmi.setVisible(false);
    }

    @Override
    public void onAdded(MapGroup parent) {
        super.onAdded(parent);
        parent.addGroup(getChildMapGroup());
    }

    @Override
    public void onRemoved(MapGroup parent) {
        super.onRemoved(parent);
        parent.removeGroup(getChildMapGroup());
    }

    @Override
    public void onTrackChanged(Marker marker) {
        setHeading(marker.getTrackHeading());
    }

    @Override
    public void onItemRemoved(MapItem item, MapGroup group) {
        if (item == getCenterMarker()) {
            if (isCenterShapeMarker() || !hasMetaValue("archive"))
                removeFromGroup();
            else
                setCenterMarker(null);
        }
    }

    /**
     * Set whether to show the vehicle outline
     * @param outline True to show outline
     */
    public boolean setShowOutline(boolean outline) {
        if (showOutline() != outline) {
            toggleMetaData("outline", outline);
            notifyMetadataChanged("outline");
            return true;
        }
        return false;
    }

    public boolean showOutline() {
        return hasMetaValue("outline");
    }

    @Override
    protected boolean isModelVisible() {
        return super.isModelVisible() || showOutline();
    }

    /**
     * For legacy devices receiving this CoT that don't have the ability to
     * show the 3-D model, set a reasonable icon
     */
    private void updateIconPath() {
        String iconPath = "34ae1613-9645-4222-a9d2-e5f243dea2865/";
        switch (_info.category) {
            case "Aircraft":
                if (_info.name.contains("H-"))
                    iconPath += "Military/LittleBird.png";
                else
                    iconPath += "Transportation/Plane8.png";
                break;
            default:
            case "Automobiles":
                iconPath += "Transportation/Car8.png";
                break;
            case "Maritime":
                iconPath += "Transportation/Ship3.png";
                break;
        }
        setMetaString(UserIcon.IconsetPath, iconPath);
    }

    @Override
    public CotEvent toCot() {
        CotEvent event = new CotEvent();

        CoordinatedTime time = new CoordinatedTime();
        event.setTime(time);
        event.setStart(time);
        event.setStale(time.addDays(1));

        event.setUID(getUID());
        event.setVersion("2.0");
        event.setHow("h-e");

        event.setPoint(new CotPoint(getCenterPoint()));
        event.setType(getCotType());

        CotDetail detail = new CotDetail("detail");
        event.setDetail(detail);

        // Model info
        CotDetail model = new CotDetail("model");
        model.setAttribute("type", "vehicle");
        model.setAttribute("name", _info.name);
        model.setAttribute("category", _info.category);
        model.setAttribute("outline", String.valueOf(showOutline()));
        detail.addChild(model);

        // Course (true degrees)
        CotDetail track = new CotDetail("track");
        track.setAttribute("course",
                Double.toString(getAzimuth(NorthReference.TRUE)));
        detail.addChild(track);

        CotDetailManager.getInstance().addDetails(this, event);

        return event;
    }

    // Maximum width/height for a vehicle capture
    private static final int MAX_RENDER_SIZE = 1024;

    // Temporary storage for render captures, so we can read from a file
    // instead of capturing every time
    private static final File RENDER_CACHE_DIR = FileSystemUtils.getItem(
            FileSystemUtils.TMP_DIRECTORY + "/vehicle_model_captures");

    @Override
    public Bundle preDrawCanvas(CapturePP cap) {
        Bundle data = new Bundle();
        PointF[] corners = new PointF[4];
        for (int i = 0; i < 4; i++)
            corners[i] = cap.forward(getPoint(i));
        data.putSerializable("corners", corners);

        if (showOutline()) {
            // Vehicle outline points, in meters offset from center
            List<PointF> points = getOutline();
            if (!FileSystemUtils.isEmpty(points)) {
                // Need to get geo point and forward
                GeoPoint center = getCenterPoint();
                double heading = getHeading();
                PointF[] outline = new PointF[points.size()];
                PointF pCen = new PointF();
                for (int i = 0; i < outline.length; i++) {
                    PointF src = points.get(i);
                    double a = CanvasHelper.angleTo(pCen, src);
                    double d = CanvasHelper.length(pCen, src);
                    a += heading;
                    outline[i] = cap.forward(DistanceCalculations
                            .computeDestinationPoint(center, a, d));
                }
                data.putSerializable("outline", outline);
            }
        }

        return data;
    }

    @Override
    public void drawCanvas(final CapturePP cap, Bundle data) {
        PointF[] corners = (PointF[]) data.getSerializable("corners");
        if (corners == null)
            return;

        Canvas can = cap.getCanvas();
        Paint paint = cap.getPaint();
        Path path = cap.getPath();
        int color = getStrokeColor();
        int alpha = getAlpha();
        float lineWeight = cap.getLineWeight();
        float dr = cap.getResolution();

        // Check if the outline is visible and available
        PointF[] outline = (PointF[]) data.getSerializable("outline");
        if (showOutline() && outline != null) {

            // Draw outline
            for (int i = 0; i < outline.length; i++) {
                PointF p = outline[i];
                if (i == 0)
                    path.moveTo(dr * p.x, dr * p.y);
                else
                    path.lineTo(dr * p.x, dr * p.y);
            }
            paint.setColor(color);
            paint.setStyle(Paint.Style.STROKE);
            paint.setStrokeWidth(3f * lineWeight);
            can.drawPath(path, paint);
            path.reset();
        }

        // Check if the vehicle model is visible
        if (getAlpha() > 0) {

            // Get the vehicle render bitmap
            Bitmap bmp = getBitmap();

            // Map vehicle bitmap to on screen coordinates
            int w = bmp.getWidth(), h = bmp.getHeight();
            float[] srcPoints = new float[] {
                    0, 0,
                    w, 0,
                    w, h,
                    0, h
            };
            int j = 0;
            float[] dstPoints = new float[8];
            for (int i = 0; i < 4; i++) {
                dstPoints[j] = corners[i].x * dr;
                dstPoints[j + 1] = corners[i].y * dr;
                j += 2;
            }
            Matrix imgMat = new Matrix();
            imgMat.setPolyToPoly(srcPoints, 0, dstPoints, 0, 4);

            // Draw the vehicle bitmap
            paint.setColorFilter(new PorterDuffColorFilter(color,
                    PorterDuff.Mode.MULTIPLY));
            paint.setAlpha(alpha);
            paint.setStyle(Paint.Style.FILL);
            can.drawBitmap(bmp, imgMat, paint);
        }
    }

    /**
     * Get or create bitmap display of the vehicle
     * If a cached image of the bitmap does not exist, it is created
     * @return Bitmap of the vehicle
     */
    private Bitmap getBitmap() {
        File cacheDir = new File(RENDER_CACHE_DIR, _info.category);
        File cacheFile = new File(cacheDir, _info.name + ".bmp");

        // Check if cached screenshot already exists we can quickly read from
        if (cacheFile.exists()) {
            try {
                // XXX - BitmapFactory doesn't take bytes as input,
                // for some (probably stupid) reason
                byte[] d = FileSystemUtils.read(cacheFile);
                int[] pixels = new int[(d.length / 4) - 1];
                int p = 0;
                int width = ((d[0] & 0xFF) << 8) | (d[1] & 0xFF);
                int height = ((d[2] & 0xFF) << 8) | (d[3] & 0xFF);
                for (int i = 4; i < d.length; i += 4) {
                    int a = (d[i] & 0xFF) << 24;
                    int r = (d[i + 1] & 0xFF) << 16;
                    int g = (d[i + 2] & 0xFF) << 8;
                    int b = (d[i + 3] & 0xFF);
                    pixels[p++] = a | (r + g + b);
                }
                return Bitmap.createBitmap(pixels, width, height,
                        Bitmap.Config.ARGB_8888);
            } catch (Exception e) {
                FileSystemUtils.delete(cacheFile);
                Log.e(TAG, "Failed to read vehicle render cache: " + cacheFile);
            }
        }

        // Setup capture request for the model
        final Bitmap[] image = new Bitmap[1];
        VehicleModelCaptureRequest req = new VehicleModelCaptureRequest(
                _info);
        req.setOutputSize(MAX_RENDER_SIZE, true);
        req.setLightingEnabled(true);
        req.setCallback(new VehicleModelCaptureRequest.Callback() {
            @Override
            public void onCaptureFinished(File file, Bitmap bmp) {
                image[0] = bmp;
                synchronized (VehicleModel.this) {
                    VehicleModel.this.notify();
                }
            }
        });
        GLOffscreenCaptureService.getInstance().request(req);

        // Wait for renderer for finish
        synchronized (this) {
            while (image[0] == null) {
                try {
                    this.wait();
                } catch (Exception ignored) {
                }
            }
        }

        // Save bitmap data straight to file (no header data needed)
        if (cacheDir.exists() || cacheDir.mkdirs()) {
            int width = image[0].getWidth();
            int height = image[0].getHeight();
            FileOutputStream fos = null;
            try {
                int[] pixels = new int[width * height];
                image[0].getPixels(pixels, 0, width, 0, 0, width, height);
                byte[] b = new byte[(pixels.length + 1) * 4];
                b[0] = (byte) (width >> 8);
                b[1] = (byte) (width & 0xFF);
                b[2] = (byte) (height >> 8);
                b[3] = (byte) (height & 0xFF);
                int i = 4;
                for (int p : pixels) {
                    b[i] = (byte) (p >> 24);
                    b[i + 1] = (byte) ((p >> 16) & 0xFF);
                    b[i + 2] = (byte) ((p >> 8) & 0xFF);
                    b[i + 3] = (byte) (p & 0xFF);
                    i += 4;
                }
                fos = new FileOutputStream(cacheFile);
                fos.write(b);
            } catch (Exception e) {
                FileSystemUtils.delete(cacheFile);
                Log.e(TAG, "Failed to read vehicle render cache: " + cacheFile);
            } finally {
                try {
                    if (fos != null)
                        fos.close();
                } catch (Exception ignored) {
                }
            }
        }

        return image[0];
    }

    private List<PointF> getOutline() {
        List<PointF> points = _info.getOutline(new Runnable() {
            @Override
            public void run() {
                synchronized (VehicleModel.this) {
                    VehicleModel.this.notify();
                }
            }
        });

        if (points != null)
            return points;

        // Wait for points generation to finish if we need
        synchronized (this) {
            while (true) {
                try {
                    this.wait();
                    break;
                } catch (Exception ignored) {
                }
            }
        }

        return _info.getOutline(null);
    }
}
