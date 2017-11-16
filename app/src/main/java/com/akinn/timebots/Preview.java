package com.akinn.timebots;

import android.content.Context;
import android.hardware.Camera;
import android.util.Log;
import android.view.Display;
import android.view.Surface;
import android.view.SurfaceView;
import android.view.SurfaceHolder;
import android.view.WindowManager;
import android.widget.FrameLayout;
import android.widget.Toast;

import com.google.firebase.analytics.FirebaseAnalytics;

import java.io.IOException;
import java.util.List;

import static android.content.Context.WINDOW_SERVICE;

/**
 * Created by ratta on 10/26/2017.
 */

public class Preview extends SurfaceView implements SurfaceHolder.Callback {
    final static String TAG = Preview.class.getSimpleName();
    SurfaceHolder mHolder;
    Camera mCamera;
    Context mContext;
    Camera.CameraInfo mCameraInfo;
    int mCameraId = 0;
    String mEmpName;

    Preview(Context context, Camera camera, Camera.CameraInfo cameraInfo, FrameLayout layout,
            String empName) {
        super(context);
        mContext = context;
        mCamera = camera;
        mCameraInfo = cameraInfo;
        mEmpName = empName;
        // Install a SurfaceHolder.Callback so we get notified when the
        // underlying surface is created and destroyed.

        mHolder = getHolder();
        mHolder.addCallback(this);
        mHolder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);

        if (layout == null) {
            Log.d(TAG, "FrameLayout is null");
            Helper.sendGoogleAnalyticError(FirebaseAnalytics.getInstance(mContext),
                    mContext.toString() + ":Preview:Constructor", mEmpName,
                    "FrameLayout is null");
        }
        layout.addView(Preview.this);
    }

    @Override
    public void surfaceCreated(SurfaceHolder holder) {
        Log.d(TAG, "surfaceCreated");
        // The Surface has been created, now tell the camera where to draw the preview.
        if (mHolder.getSurface() == null) {
            return;
        }

        try {
            mCameraId = getFrontCameraId();
            mCamera = openCamara(mCameraId);
            if (mCamera == null) {
                Toast.makeText(mContext, "Cannot open camera!!!", Toast.LENGTH_LONG)
                        .show();
            } else {
                Log.d(TAG, "mCamera is not null");
                Camera.CameraInfo mCameraInfo = new Camera.CameraInfo();
                Camera.getCameraInfo(mCameraId, mCameraInfo);
                mCamera.setPreviewDisplay(mHolder);
                mCamera.startPreview();
            }
        } catch (IOException e) {
            Log.e(TAG, "surfacedCreated: " + e.getMessage());
            Helper.sendGoogleAnalyticError(FirebaseAnalytics.getInstance(mContext),
                    mContext.toString() + ":Preview:surfacedCreated", mEmpName,
                    e.getMessage());
        }
    }

    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
        Log.d(TAG, "surfaceChanged");
        // If your preview can change or rotate, take care of those events here.
        // Make sure to stop the preview before resizing or reformatting it.

        if (mHolder.getSurface() == null){
            // preview surface does not exist
            return;
        }
        FrameLayout f = (FrameLayout) findViewById(R.id.cameraPreview);
        Log.d(TAG, String.format("FrameLayout size (%d,%d)", width, height));

        // stop preview before making changes
        try {
            mCamera.stopPreview();
        } catch (Exception e){
            // ignore: tried to stop a non-existent preview
        }

        // set preview size and make any resize, rotate or
        // reformatting changes here
        Camera.Parameters parameters = mCamera.getParameters();
        List<Camera.Size> mSupportedPreviewSizes = parameters.getSupportedPictureSizes();
/*        Camera.Size previewSize = getOptimalPreviewSize(mSupportedPreviewSizes,
                getResources().getDisplayMetrics().widthPixels,
                getResources().getDisplayMetrics().heightPixels);*/
        //Log.d(TAG, String.format("Preview Size (%d, %d)", previewSize.width, previewSize.height));
        //parameters.setPreviewSize(previewSize.width, previewSize.height);

        Display display = ((WindowManager)mContext.getSystemService(WINDOW_SERVICE)).getDefaultDisplay();
        int rotation = display.getRotation();
        int degrees = 0;
        switch (rotation) {
            case Surface.ROTATION_0: degrees = 0; break;
            case Surface.ROTATION_90: degrees = 90; break;
            case Surface.ROTATION_180: degrees = 180; break;
            case Surface.ROTATION_270: degrees = 270; break;
        }

        Log.d(TAG, "mCameraInfo is " + (mCameraInfo == null ? "null" : "not null"));
        Camera.CameraInfo mCameraInfo = new Camera.CameraInfo();
        Camera.getCameraInfo(mCameraId, mCameraInfo);
        Log.d(TAG, "mCameraInfo is " + (mCameraInfo == null ? "null" : "not null"));
        int result;
        if (mCameraInfo.facing == Camera.CameraInfo.CAMERA_FACING_FRONT) {
            result = (mCameraInfo.orientation + degrees) % 360;
            result = (360 - result) % 360;  // compensate the mirror
        } else {  // back-facing
            result = (mCameraInfo.orientation - degrees + 360) % 360;
        }

        //parameters.setRotation(result);
        Log.d(TAG, "Orientation = " + result);

        mCamera.setDisplayOrientation(result);
        setSmallestPictureSize(parameters);
/*        for (Camera.Size size : parameters.getSupportedPictureSizes()) {
            Camera.Size prefSize = size;
            double apr = (double) size.width / size.height;
            Log.d(TAG, String.format("Prefer Size (%d, %d) : %.3f", prefSize.width, prefSize.height, apr));
            parameters.setPreviewSize(prefSize.width, prefSize.height);
            try {
                mCamera.setParameters(parameters);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }*/

        // start preview with new settings
        try {
            mCamera.setPreviewDisplay(mHolder);
            mCamera.startPreview();

        } catch (Exception e){
            Log.e(TAG, "surfaceChanged: " + e.getMessage());
            Helper.sendGoogleAnalyticError(FirebaseAnalytics.getInstance(mContext),
                    mContext.toString() + ":Preview:surfaceChanged", mEmpName,
                    e.getMessage());
        }
    }

    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {
        Log.d(TAG, "surfaceDestroyed");
        if (mHolder.getSurface() == null) {
            return;
        }
        // Surface will be destroyed when we return, so stop the preview.
        // stop preview before making changes
        try {
            releaseCamera(mCamera);
            /*
            if (mCamera != null) {
                Log.d(TAG, "mCamera is not null");
                mCamera.stopPreview();
            }*/
        } catch (Exception e){
            // ignore: tried to stop a non-existent preview
        }
    }

    private Camera.Size getOptimalPreviewSize(List<Camera.Size> sizes, int w, int h) {
        final double ASPECT_TOLERANCE = 0.05;
        double targetRatio = (double) w/h;
        if (h > w) targetRatio = (double) h/w;
        int limitedWidth = 300;

        Log.d(TAG, "w: " + String.valueOf(w) + ", h: " + String.valueOf(h));
        if (sizes==null) return null;

        Camera.Size optimalSize = null;

        double minDiff = Double.MAX_VALUE;

        int targetHeight = h;

        // Find size
        for (Camera.Size size : sizes) {
            Log.d(TAG, String.format("Display Size: (%d,%d)", size.width, size.height));
            double ratio = (double) size.width / size.height;
            if (Math.abs(ratio - targetRatio) > ASPECT_TOLERANCE) continue;
            if (Math.abs(size.height - targetHeight) < minDiff) {
                optimalSize = size;
                minDiff = Math.abs(size.height - targetHeight);
            }
        }

        if (optimalSize == null) {
            minDiff = Double.MAX_VALUE;
            for (Camera.Size size : sizes) {
                if (Math.abs(size.height - targetHeight) < minDiff) {
                    optimalSize = size;
                    minDiff = Math.abs(size.height - targetHeight);
                }
            }
        }

        if (h > w) {
            optimalSize = mCamera.new Size(optimalSize.height, optimalSize.width);
        }

        return optimalSize;
    }

    private Camera.Size setSmallestPictureSize(Camera.Parameters parameters) {
        Camera.Size result = null;

        for (Camera.Size size : parameters.getSupportedPictureSizes()) {
            if (result == null) {
                result = size;
            }
            else {
                int resultArea=result.width * result.height;
                int newArea=size.width * size.height;

                if (newArea < resultArea) {
                    result=size;
                }
            }
            try {
                parameters.setPreviewSize(result.width, result.height);
                mCamera.setParameters(parameters);
            } catch (Exception e) {
                Helper.sendGoogleAnalyticError(FirebaseAnalytics.getInstance(mContext),
                        mContext.toString() + ":Preview:setSmallestPictureSize",
                        mEmpName, e.getMessage());
            }
        }

        return result;
    }

    private int getFrontCameraId() {
        int cameraId = 0;
        int numberOfCameras = Camera.getNumberOfCameras();
        for (int i = 0; i < numberOfCameras; i++) {
            Camera.CameraInfo info = new Camera.CameraInfo();
            Camera.getCameraInfo(i, info);
            if (info.facing == Camera.CameraInfo.CAMERA_FACING_FRONT) {
                cameraId = i;
                break;
            }
        }
        return cameraId;
    }

    private Camera openCamara(int id) {
        Camera camera = null;
        try {
            camera = Camera.open(id);
            Log.d(TAG, "Camera is open");
        } catch (Exception e) {
            Log.e(TAG, "failed to open Camera");
            Helper.sendGoogleAnalyticError(FirebaseAnalytics.getInstance(mContext),
                    mContext.toString() + ":Preview:openCamara", mEmpName, e.getMessage());
            e.printStackTrace();
        }
        return camera;
    }

    private void releaseCamera(Camera camera) {
        Log.d(TAG, "Release camera");
        mCameraInfo = null;
        if (camera != null) {
            Log.d(TAG, "Camera is not null");
            camera.stopPreview();
            camera.release();
            camera = null;
        }
    }

    public void stopCamera() {
        Log.d(TAG, "stopCamera");
        if (mCamera != null) {
            Log.d(TAG, "Camera is not null");
            mCamera.stopPreview();
            mCamera.release();
            mCamera = null;
        }
    }

    public int getCameraOrientation() {
        Camera.CameraInfo mCameraInfo = new Camera.CameraInfo();
        Camera.getCameraInfo(mCameraId, mCameraInfo);
        return mCameraInfo.orientation;
    }

    public boolean hasCamera() {
        return (mCamera != null);
    }

    public Camera getCamera() {
        return mCamera;
    }
}
