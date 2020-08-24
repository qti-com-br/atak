
package com.atakmap.android.vehicle.model.icon;

import android.graphics.Bitmap;
import android.graphics.Color;
import android.opengl.GLES30;
import android.opengl.Matrix;

import com.atakmap.android.maps.graphics.GLCapture;
import com.atakmap.android.model.viewer.processing.ShaderHelper;
import com.atakmap.android.model.viewer.processing.TextureHelper;
import com.atakmap.android.vehicle.model.VehicleModelCache;
import com.atakmap.android.vehicle.model.VehicleModelInfo;
import com.atakmap.coremap.filesystem.FileSystemUtils;
import com.atakmap.coremap.log.Log;
import com.atakmap.lang.Unsafe;
import com.atakmap.map.layer.feature.geometry.Envelope;
import com.atakmap.map.layer.model.Material;
import com.atakmap.map.layer.model.Mesh;
import com.atakmap.map.layer.model.Model;
import com.atakmap.map.layer.model.Models;
import com.atakmap.map.layer.model.VertexDataLayout;
import com.atakmap.opengl.GLES20FixedPipeline;

import java.io.File;
import java.nio.Buffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Render a vehicle model to a bitmap
 */
public class VehicleModelCaptureRequest implements GLOffscreenCaptureRequest {

    private static final String TAG = "VehicleModelCaptureRequest";

    protected static final Comparator<Mesh> COMP_Z = new Comparator<Mesh>() {
        @Override
        public int compare(Mesh o1, Mesh o2) {
            Envelope e1 = o1.getAABB();
            Envelope e2 = o2.getAABB();
            return Double.compare(e1.minZ, e2.minZ);
        }
    };

    public interface Callback {
        void onCaptureFinished(File file, Bitmap bmp);
    }

    private static Integer _program, _strokeProgram;

    /** Store model matrix. Moves models from object space to world space */
    protected float[] modelMatrix = new float[16];

    /** Ussed to project the scene onto a 2D viewport*/
    protected float[] projectionMatrix = new float[16];

    /** Used to pass their respective data into the shaders*/
    private int uProjection;
    private int uModelView;
    private int aPosition;
    private int aTexCoords;
    private int aNormals;
    private final Map<String, Integer> textureHandles = new HashMap<>();

    protected final String _uid;
    protected final VehicleModelInfo _vehicle;
    protected int _width;
    protected int _height;
    protected boolean _crop;
    protected Model _model;
    protected Envelope _aabb;

    private boolean _lighting;
    private boolean _stroke;
    private float[] _strokeColor = {
            0, 0, 0, 1
    };
    private int _strokeWidth = 4;
    private boolean _strokeOnly;

    private File _outFile;
    private Callback _callback;

    public VehicleModelCaptureRequest(VehicleModelInfo vehicle) {
        _uid = UUID.randomUUID().toString();
        _vehicle = vehicle;
    }

    /**
     * Set the file to save the finished icon to
     * @param file Output file
     */
    public void setOutputFile(File file) {
        _outFile = file;
    }

    /**
     * Set the desired output size of the capture
     * Note: This method will automatically disable cropping
     * @param width Width in pixels
     * @param height Height in pixels
     */
    public void setOutputSize(int width, int height) {
        _width = width;
        _height = height;
        _crop = false;
    }

    /**
     * Set the desired output size of the capture
     * @param size The max width and height in pixels
     * @param crop True to automatically crop the image to the vehicle bounds
     */
    public void setOutputSize(int size, boolean crop) {
        _width = _height = size;
        _crop = crop;
    }

    /**
     * Set the callback to be run when this icon is finished being generated
     * @param cb Callback
     */
    public void setCallback(Callback cb) {
        _callback = cb;
    }

    /**
     * Set whether model lighting is enabled
     * @param lighting True if enabled
     */
    public void setLightingEnabled(boolean lighting) {
        _lighting = lighting;
    }

    /**
     * Set whether the model should have a stroke behind it
     * @param stroke True to enable stroke
     */
    public void setStrokeEnabled(boolean stroke) {
        _stroke = stroke;
    }

    /**
     * Set the color of the underlying stroke
     * @param color Stroke color
     */
    public void setStrokeColor(int color) {
        _strokeColor[0] = Color.red(color) / 255f;
        _strokeColor[1] = Color.green(color) / 255f;
        _strokeColor[2] = Color.blue(color) / 255f;
        _strokeColor[3] = Color.alpha(color) / 255f;
    }

    /**
     * Set the width of the stroke
     * @param width Width in pixels
     */
    public void setStrokeWidth(int width) {
        _strokeWidth = width;
    }

    /**
     * Set whether to only draw the outer stroke of the model
     * @param strokeOnly True for stroke only
     */
    public void setStrokeOnly(boolean strokeOnly) {
        _strokeOnly = strokeOnly;
    }

    @Override
    public void onStart() {
        VehicleModelCache.getInstance().registerUsage(_vehicle, _uid);
        _model = _vehicle.getModel();
        if (_model == null)
            return;

        _aabb = _model.getAABB();
        if (_crop) {
            // Crop the output to the vehicle dimensions
            // Assumes the width and height are equally set to the max size
            // The ortho width and height in meters
            float var = (float) (_aabb.maxX - _aabb.minX)
                    / (float) (_aabb.maxY - _aabb.minY);
            if (var > 1.0)
                _height = Math.round(_width / var);
            else
                _width = Math.round(_height * var);
        }
    }

    /**
     * Return's the width of this renderer's surface
     *
     * @return The width of the surface
     */
    @Override
    public int getWidth() {
        return _width;
    }

    /**
     * Return's the height of this renderer's surface
     *
     * @return The height of the surface
     */
    @Override
    public int getHeight() {
        return _height;
    }

    @Override
    public void onFinished(Bitmap bmp) {
        if (_outFile != null) {
            // Create icon output directory
            File dir = _outFile.getParentFile();
            if (dir == null || !dir.exists() && !dir.mkdirs()) {
                Log.w(TAG, "Failed to create dirs: " + dir);
                return;
            }

            // Compress to icon file
            try {
                GLCapture.compress(bmp, 100, Bitmap.CompressFormat.PNG,
                        _outFile, false);
            } catch (Exception e) {
                Log.e(TAG, "Failed to compress icon for " + _vehicle.name, e);
            }
        }

        if (_callback != null)
            _callback.onCaptureFinished(_outFile, bmp);
    }

    @Override
    public void onDraw(GLOffscreenCaptureParams params) {
        if (_model == null) {
            Log.w(TAG, "No model for vehicle " + _vehicle.name);
            return;
        }

        //Log.d(TAG, "Starting capture request for " + _vehicle.name);

        GLES30.glClearColor(0, 0, 0, 0);
        GLES30.glClear(GLES30.GL_DEPTH_BUFFER_BIT | GLES30.GL_COLOR_BUFFER_BIT);
        GLES30.glEnable(GLES30.GL_DEPTH_TEST);
        GLES30.glDepthFunc(GLES30.GL_LEQUAL);
        GLES30.glEnable(GLES30.GL_BLEND);
        GLES30.glBlendFunc(GLES30.GL_SRC_ALPHA, GLES30.GL_ONE_MINUS_SRC_ALPHA);
        GLES30.glViewport(0, 0, params.width, params.height);

        //GLES30.glBlendEquationSeparate(GLES30.GL_FUNC_ADD, GLES30.GL_FUNC_ADD);
        //GLES30.glBlendFuncSeparate(GLES30.GL_SRC_ALPHA, GLES30.GL_ONE_MINUS_SRC_ALPHA, GLES30.GL_ONE, GLES30.GL_ZERO);

        // Prepare stroke filter
        int[] strokeMask = new int[1];
        int[] maskFB = new int[1];
        if (_stroke) {
            int level = 0;
            int border = 0;
            GLES20FixedPipeline.glGenTextures(1, strokeMask, 0);
            GLES30.glBindTexture(GLES30.GL_TEXTURE_2D, strokeMask[0]);
            GLES30.glTexImage2D(GLES30.GL_TEXTURE_2D, level, GLES30.GL_RGBA,
                    _width, _height, border,
                    GLES30.GL_RGBA, GLES30.GL_UNSIGNED_BYTE, null);

            GLES30.glTexParameteri(GLES30.GL_TEXTURE_2D,
                    GLES30.GL_TEXTURE_MIN_FILTER, GLES30.GL_NEAREST);
            GLES30.glTexParameteri(GLES30.GL_TEXTURE_2D,
                    GLES30.GL_TEXTURE_WRAP_S, GLES30.GL_CLAMP_TO_EDGE);
            GLES30.glTexParameteri(GLES30.GL_TEXTURE_2D,
                    GLES30.GL_TEXTURE_WRAP_T, GLES30.GL_CLAMP_TO_EDGE);
            GLES30.glGenFramebuffers(1, maskFB, 0);
            GLES30.glBindFramebuffer(GLES30.GL_FRAMEBUFFER, maskFB[0]);
            GLES30.glFramebufferTexture2D(GLES30.GL_FRAMEBUFFER,
                    GLES30.GL_COLOR_ATTACHMENT0, GLES30.GL_TEXTURE_2D,
                    strokeMask[0], level);
            GLES30.glBindFramebuffer(GLES30.GL_FRAMEBUFFER, 0);
            GLES30.glBindTexture(GLES30.GL_TEXTURE_2D, 0);
        }

        setupCamera();

        //read, compile, and link shaders to our program handle
        synchronized (VehicleModelCaptureRequest.class) {
            if (_program == null) {
                int vertexHandle = ShaderHelper.compileShader(
                        GLES30.GL_VERTEX_SHADER, VERTEX_SHADER_SOURCE);
                int fragmentHandle = ShaderHelper.compileShader(
                        GLES30.GL_FRAGMENT_SHADER, FRAGMENT_SHADER_SOURCE);
                _program = ShaderHelper.createAndLinkProgram(
                        vertexHandle, fragmentHandle, new String[] {
                                "aPosition", "aTexCoords"
                        });
                //Log.d(TAG, "Compiled base shaders for " + _vehicle.name);
            }
            if (_strokeProgram == null) {
                int vertexHandle = ShaderHelper.compileShader(
                        GLES30.GL_VERTEX_SHADER, STROKE_VERTEX_SHADER);
                int fragmentHandle = ShaderHelper.compileShader(
                        GLES30.GL_FRAGMENT_SHADER, STOKE_FRAGMENT_SHADER);
                _strokeProgram = ShaderHelper.createAndLinkProgram(
                        vertexHandle, fragmentHandle, null);
                //Log.d(TAG, "Compiled outline shaders for " + _vehicle.name);
            }
        }

        GLES30.glUseProgram(_program);

        //set program handles for model drawing
        this.uProjection = GLES30.glGetUniformLocation(_program, "uProjection");
        this.uModelView = GLES30.glGetUniformLocation(_program, "uModelView");
        int uTexture = GLES30.glGetUniformLocation(_program, "uTexture");
        this.aPosition = GLES30.glGetAttribLocation(_program, "aPosition");
        this.aTexCoords = GLES30.glGetAttribLocation(_program, "aTexCoords");
        this.aNormals = GLES30.glGetAttribLocation(_program, "aNormals");
        int bLighting = GLES30.glGetUniformLocation(_program, "bLighting");
        int bStroke = GLES30.glGetUniformLocation(_program, "bStroke");

        GLES30.glUniform1i(bLighting, _lighting ? GLES30.GL_TRUE
                : GLES30.GL_FALSE);
        GLES30.glUniform1i(uTexture, 0);

        //set and bind the active texture unit, then pass texture into shader
        GLES30.glActiveTexture(GLES30.GL_TEXTURE0);

        // Get all valid meshes
        int numMeshes = _model.getNumMeshes();
        List<Mesh> meshes = new ArrayList<>();
        for (int m = 0; m < numMeshes; m++) {
            Mesh mesh = _model.getMesh(m);
            if (mesh != null)
                meshes.add(mesh);
        }

        // Sort meshes by z-order so we draw them properly
        Collections.sort(meshes, COMP_Z);

        if (_stroke) {
            GLES30.glBindFramebuffer(GLES30.GL_FRAMEBUFFER, maskFB[0]);
            GLES30.glClear(
                    GLES30.GL_DEPTH_BUFFER_BIT | GLES30.GL_COLOR_BUFFER_BIT);
        }
        GLES30.glUniform1i(bStroke, _stroke ? GLES30.GL_TRUE : GLES30.GL_FALSE);

        // Draw meshes
        drawMeshes(meshes);

        // Now that the vehicle has been drawn to a texture, draw the stroke around it
        if (_stroke) {

            // Bind original frame buffer again
            GLES30.glBindFramebuffer(GLES30.GL_FRAMEBUFFER,
                    params.frameBufferID);

            GLES30.glUseProgram(_strokeProgram);

            int textureLoc = GLES30.glGetUniformLocation(_strokeProgram,
                    "texture");
            int pixelSizeLoc = GLES30.glGetUniformLocation(_strokeProgram,
                    "pixelSize");
            int colorLoc = GLES30.glGetUniformLocation(_strokeProgram, "color");
            int verticesLoc = GLES30.glGetAttribLocation(_strokeProgram, "pos");
            int strokeWidth = GLES30.glGetUniformLocation(_strokeProgram,
                    "strokeWidth");

            // Build the quad to render over the entire screen
            float[] quadVerts = {
                    0, 0,
                    1, 0,
                    1, 1,
                    1, 1,
                    0, 1,
                    0, 0
            };
            int size = quadVerts.length * 4;
            FloatBuffer fb = Unsafe.allocateDirect(size)
                    .order(ByteOrder.nativeOrder())
                    .asFloatBuffer();
            fb.put(quadVerts);
            fb.position(0);

            int[] quadBuf = new int[1];
            float[] pixelSizeArr = new float[] {
                    1f / params.width,
                    1f / params.height
            };

            GLES30.glBindTexture(GLES30.GL_TEXTURE_2D, strokeMask[0]);

            GLES30.glGenBuffers(1, quadBuf, 0);
            GLES30.glBindBuffer(GLES30.GL_ARRAY_BUFFER, quadBuf[0]);
            GLES30.glBufferData(GLES30.GL_ARRAY_BUFFER, fb.capacity() * 4, fb,
                    GLES30.GL_STATIC_DRAW);
            GLES30.glVertexAttribPointer(verticesLoc, 2, GLES30.GL_FLOAT, false,
                    0, 0);
            GLES30.glEnableVertexAttribArray(verticesLoc);
            GLES30.glUniform1i(textureLoc, 0);
            GLES30.glUniform2fv(pixelSizeLoc, 1, pixelSizeArr, 0);
            GLES30.glUniform4fv(colorLoc, 1, _strokeColor, 0);
            GLES30.glUniform1i(strokeWidth, _strokeWidth);

            GLES30.glDisable(GLES30.GL_DEPTH_TEST);
            GLES30.glEnable(GLES30.GL_BLEND);
            GLES30.glDrawArrays(GLES30.GL_TRIANGLES, 0, 6);
            GLES30.glDisable(GLES30.GL_BLEND);
            GLES30.glEnable(GLES30.GL_DEPTH_TEST);

            // Cleanup stroke
            GLES30.glDisableVertexAttribArray(verticesLoc);
            GLES30.glBindTexture(GLES30.GL_TEXTURE_2D, 0);
            GLES30.glDeleteTextures(1, strokeMask, 0);
            GLES30.glDeleteFramebuffers(1, maskFB, 0);
            GLES30.glDeleteBuffers(1, quadBuf, 0);

            if (!_strokeOnly) {
                // Draw vehicle again
                // XXX - Yes this is inefficient, but for some reason the existing
                // vehicle render looks like crap. It only looks normal when rendering
                // to the proper buffer
                GLES30.glUseProgram(_program);
                GLES30.glUniform1i(bStroke, GLES30.GL_FALSE);
                drawMeshes(meshes);
            }
        }

        // Cleanup base
        GLES30.glDisableVertexAttribArray(aPosition);
        GLES30.glDisableVertexAttribArray(aNormals);
        GLES30.glDisableVertexAttribArray(aTexCoords);

        int h = 0;
        int[] handles = new int[textureHandles.size()];
        for (Integer handle : textureHandles.values())
            handles[h++] = handle;
        GLES30.glDeleteTextures(handles.length, handles, 0);

        // No longer need the vehicle model from this point forward
        VehicleModelCache.getInstance().unregisterUsage(_vehicle, _uid);

        //Log.d(TAG, "Finished capture request for " + _vehicle.name);
    }

    private void drawMeshes(List<Mesh> meshes) {
        int m = 0;
        for (Mesh mesh : meshes) {
            m++;

            Material mat = null;
            for (int t = 0; t < mesh.getNumMaterials(); t++) {
                mat = mesh.getMaterial(t);
                if (mat == null)
                    continue;
                String textureUri = mat.getTextureUri();
                if (FileSystemUtils.isEmpty(textureUri))
                    mat = null;
            }
            if (mat == null) {
                Log.w(TAG, "Failed to find valid material for mesh " + m + "/"
                        + meshes.size() + " (" + _vehicle.name + ")");
                continue;
            }

            String textureUri = mat.getTextureUri();

            Integer handle = textureHandles.get(textureUri);
            if (handle == null) {
                String texUri = mat.getTextureUri();
                handle = TextureHelper.loadTextureFromPath(texUri);
                if (handle == 0)
                    continue;
                textureHandles.put(textureUri, handle);
            }

            GLES30.glBindTexture(GLES30.GL_TEXTURE_2D, handle);

            //Log.d(TAG, "Drawing mesh #" + m + " for " + _vehicle.name);
            drawMesh(mesh);
        }
    }

    /*
     * helper method to draw and connect the model's vertices
     */
    private void drawMesh(Mesh mesh) {

        final VertexDataLayout layout = mesh.getVertexDataLayout();

        // XXX - this is assuming that buffer is ByteBuffer
        Buffer modelVertices = mesh.getVertices(Model.VERTEX_ATTR_POSITION);
        modelVertices.position(layout.position.offset);

        GLES30.glVertexAttribPointer(aPosition, 3, GLES30.GL_FLOAT, false,
                layout.position.stride,
                mesh.getVertices(Model.VERTEX_ATTR_POSITION));
        GLES30.glEnableVertexAttribArray(aPosition);

        if (_lighting) {
            Buffer normals = mesh.getVertices(Mesh.VERTEX_ATTR_NORMAL);
            normals.position(layout.normal.offset);
            GLES30.glVertexAttribPointer(aNormals, 3, GLES30.GL_FLOAT, false,
                    layout.normal.stride, normals);
            GLES30.glEnableVertexAttribArray(aNormals);
        }

        //pass in texture coordinate information
        Buffer modelTexCoords = mesh.getVertices(Model.VERTEX_ATTR_TEXCOORD_0);
        modelTexCoords.position(layout.texCoord0.offset);

        GLES30.glVertexAttribPointer(aTexCoords, 2, GLES30.GL_FLOAT, false,
                layout.texCoord0.stride, modelTexCoords);
        GLES30.glEnableVertexAttribArray(aTexCoords);

        GLES30.glUniformMatrix4fv(uModelView, 1, false, modelMatrix, 0);
        GLES30.glUniformMatrix4fv(uProjection, 1, false, projectionMatrix, 0);

        int mode;
        switch (mesh.getDrawMode()) {
            case Triangles:
                mode = GLES30.GL_TRIANGLES;
                break;
            case TriangleStrip:
                mode = GLES30.GL_TRIANGLE_STRIP;
                break;
            default:
                return;
        }

        if (mesh.isIndexed()) {
            //Log.d(TAG, "glDrawElements (" + mode + ") " + _vehicle.name);
            GLES30.glDrawElements(mode, Models.getNumIndices(mesh),
                    GLES30.GL_UNSIGNED_INT, mesh.getIndices());
        } else {
            //Log.d(TAG, "glDrawArrays (" + mode + ") " + _vehicle.name);
            GLES30.glDrawArrays(mode, 0, mesh.getNumVertices());
        }
    }

    protected void setupCamera() {
        // Vehicle aspect ratio
        float w = (float) (_aabb.maxX - _aabb.minX);
        float h = (float) (_aabb.maxY - _aabb.minY);
        float var = w / h;

        // Adjust camera size by meters relative to image aspect ratio
        if (!_crop) {
            float iar = (float) _width / (float) _height;
            if (iar < var)
                h *= var / iar;
            else if (iar > var)
                w *= iar / var;
        }

        // Add padding so the stroke doesn't get cut off
        if (_stroke) {
            w *= 1 + ((_strokeWidth * 2f) / _width);
            h *= 1 + ((_strokeWidth * 2f) / _height);
        }

        w /= 2f;
        h /= 2f;
        Matrix.orthoM(projectionMatrix, 0, -w, w, -h, h, 0, 1000f);
        Matrix.setLookAtM(modelMatrix, 0, 0, 0, (float) (_aabb.maxZ + 1),
                0, 0, 0, 0, 1, 0);
    }

    private static final String VERTEX_SHADER_SOURCE = ""
            //a constant representing the combined model/view/projection matrix
            + "uniform mat4 uProjection;\n"
            //a constant representing the combined model/view matrix
            + "uniform mat4 uModelView;\n"
            //per-vertex position information we will pass in
            + "attribute vec4 aPosition;\n"
            //per-vertex normal information we will pass in
            + "attribute vec2 aTexCoords;\n"
            // These will be passed into the fragment shader
            + "varying vec2 vTexCoordinate;\n"
            // Model lighting
            + "attribute vec3 aNormals;\n"
            + "varying vec3 vNormal;\n"
            // Main function
            + "void main()\n"
            + "{\n"
            //pass through the texture coordinate
            + "  vTexCoordinate = aTexCoords;\n"
            //store final position of model
            + "  gl_Position = uProjection * uModelView * aPosition;\n"
            // Normals for lighting
            + "  vNormal = normalize(mat3(uProjection * uModelView) * aNormals);\n"
            + "}\n";

    private static final String FRAGMENT_SHADER_SOURCE = ""
            //set the default precision to medium
            + "precision mediump float;\n"
            //the input texture
            + "uniform sampler2D uTexture;\n"
            //interpolated texture coordinate per fragment
            + "varying vec2 vTexCoordinate;\n"
            // Normals for lighting
            + "varying vec3 vNormal;\n"
            // Lighting toggle
            + "uniform bool bLighting;\n"
            // Drawing for stroke calculation
            + "uniform bool bStroke;\n"
            // Main function
            + "void main()\n"
            + "{\n"
            + "  vec4 color = texture2D(uTexture, vTexCoordinate);\n"
            + "  if (color.a < 0.3)\n"
            + "    discard;\n"
            + "  if (bStroke) {\n"
            + "    color = vec4(0.0, 0.0, 0.0, 1.0);\n"
            + "  }\n"
            // Lighting
            + "  else if (bLighting) {\n"
            + "    vec3 sun_position = vec3(3.0, 10.0, -5.0);\n"
            + "    vec3 sun_color = vec3(1.0, 1.0, 1.0);\n"
            + "    float lum = max(dot(vNormal, normalize(sun_position)), 0.0);\n"
            + "    color = color * vec4((0.6 + 0.4 * lum) * sun_color, 1.0);\n"
            + "  }\n"
            //final output color
            + "  gl_FragColor = color;\n"
            + "}\n";

    private static final String STROKE_VERTEX_SHADER = ""
            + "uniform vec2 pixelSize;\n"
            + "attribute vec2 pos;\n"
            + "varying vec2 texCoord;\n"
            + "void main()\n"
            + "{\n"
            + "  texCoord = pos;\n"
            + "  gl_Position = vec4(2.0 * pos.x - 1.0, 2.0 * pos.y - 1.0, 0.0, 1.0);\n"
            + "}\n";

    private static final String STOKE_FRAGMENT_SHADER = ""
            + "precision highp float;\n"
            + "uniform sampler2D texture;\n"
            + "uniform vec2 pixelSize;\n"
            + "uniform vec4 color;\n"
            + "uniform int strokeWidth;\n"
            + "varying vec2 texCoord;\n"
            + "void main() {\n"
            + "  int w = strokeWidth;\n"
            + "  bool isInside = false;\n"
            + "  int count = 0;\n"
            + "  float coverage = 0.0;\n"
            + "  float dist = 1e6;\n"
            + "  for (int y = -w;  y <= w;  ++y) {\n"
            + "    for (int x = -w;  x <= w;  ++x) {\n"
            + "      vec2 dUV = vec2(float(x) * pixelSize.x, float(y) * pixelSize.y);\n"
            + "      float mask = texture2D(texture, texCoord + dUV).a;\n"
            + "      coverage += mask;\n"
            + "      if (mask >= 0.5) {\n"
            + "        dist = min(dist, sqrt(float(x * x + y * y)));\n"
            + "      }\n"
            + "      if (x == 0 && y == 0) {\n"
            + "        isInside = (mask > 0.5);\n"
            + "      }\n"
            + "      count += 1;\n"
            + "    }\n"
            + "  }\n"
            + "  coverage /= float(count);\n"
            + "  float a;\n"
            + "  if (isInside) {\n"
            + "    a = min(1.0, (1.0 - coverage) / 0.75);\n"
            + "  } else {\n"
            + "    float solid = float(w);\n"
            + "    a = 1.0 - min(1.0, max(0.0, dist - solid));\n"
            + "  }\n"
            + "  if (a < 1.0) {\n"
            + "    a = 0.0;\n"
            + "  }\n"
            + "  gl_FragColor = color;\n"
            + "  gl_FragColor.a = a;\n"
            + "}\n";
}
