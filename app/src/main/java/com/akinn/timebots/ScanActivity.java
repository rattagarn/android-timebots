package com.akinn.timebots;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.ContextWrapper;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.hardware.Camera;
import android.os.Build;
import android.provider.MediaStore;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.Display;
import android.view.Surface;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.Toast;

import java.io.File;
import java.io.FileOutputStream;

public class ScanActivity extends AppCompatActivity {
    final static String TAG = ScanActivity.class.getSimpleName();
    ImageButton buttonTakePicture;
    Button buttonCheckIn;
    Preview mPreview;
    boolean mHasCamera = false;

    String mMode = "";
    String mEmpId = "";
    String mEmpName = "";
    long mStampedTime = -1;

    double mLongitude = 0.0;
    double mLatitude = 0.0;

    Bitmap mPicData;

    boolean mPreviewMode = true;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_scan);

        Intent intent = getIntent();
        mMode = intent.getStringExtra("MODE");
        mStampedTime = intent.getLongExtra("STAMPED_TIME", -1);
        mEmpId = intent.getStringExtra("EMP_ID");
        mEmpName = intent.getStringExtra("EMP_NAME");

        buttonTakePicture = (ImageButton) findViewById(R.id.buttonTakePicture);
        buttonCheckIn = (Button) findViewById(R.id.buttonCheckIn);

        //checkLocationPermissionAndStartService();

        if (checkCameraHardware(getApplicationContext())) {
            FrameLayout previewPane = (FrameLayout) findViewById(R.id.cameraPreview);
            mPreview = new Preview(getApplicationContext(), null,
                    null, previewPane, mEmpName);
        } else {
            FrameLayout previewPane = (FrameLayout) findViewById(R.id.cameraPreview);
            mPreview = new Preview(getApplicationContext(), null,
                    null, previewPane, mEmpName);
            buttonTakePicture.setEnabled(true);
            Toast.makeText(getApplicationContext(), "No front camera!!!", Toast.LENGTH_LONG)
                    .show();
        }

        Log.d(TAG, String.format("buttonTakePicture is %b", mHasCamera));

        // Add a listener to the Capture button
        buttonTakePicture.setOnClickListener(
                new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        // get an image from the camera
                        Camera camera = mPreview.getCamera();
                        if (camera != null) {
                            if (mPreviewMode) {
                                mPreviewMode = false;
                                Log.d(TAG, "Taking picture");
                                //camera.stopPreview();
                                Log.d(TAG, "Stop Preview");
                                try {
                                    camera.takePicture(null, null, mPicture);
                                } catch (Exception e) {
                                    Log.d(TAG, e.getMessage());
                                }
                            } else {
                                Log.d(TAG, "Preview");
                                mPreviewMode = true;
                                FrameLayout frame = (FrameLayout) findViewById(R.id.cameraPreview);
                                frame.setVisibility(View.VISIBLE);
                                ImageView imageVIew = (ImageView) findViewById(R.id.imageViewLive);
                                imageVIew.setVisibility(View.INVISIBLE);
                                imageVIew.setImageBitmap(null);
                                camera.startPreview();
                            }
                        }
                    }
                }
        );

        buttonCheckIn.setOnClickListener(
                new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        stopLocationService();
                        gotoReviewActivity();
                    }
                }
        );
        buttonCheckIn.requestFocus();
    }

    @Override
    protected void onResume() {
        super.onResume();
        Log.d(TAG, "onResume");
        checkLocationPermissionAndStartService();
        if (mBroadcastReceiver == null) {
            mBroadcastReceiver = new BroadcastReceiver() {
                @Override
                public void onReceive(Context context, Intent intent) {
                    mLongitude = intent.getDoubleExtra("LONGITUDE", 0.0);
                    mLatitude = intent.getDoubleExtra("LATITUDE", 0.0);
                    Log.d(TAG, String.format("Location (%f,%f)", mLatitude, mLongitude));
                }
            };
        }
        registerReceiver(mBroadcastReceiver,
                new IntentFilter(getResources().getString(R.string.location_broadcast_name)));
    }

    @Override
    protected void onPause() {
        super.onPause();
        Log.d(TAG, "onPause");
        //releaseCameraAndPreview();
        stopLocationService();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (mBroadcastReceiver != null) {
            unregisterReceiver(mBroadcastReceiver);
        }
    }

    /* Camera functions */
    Camera mCamera;
    private boolean safeCameraOpen(int id) {
        boolean qOpened = false;
        Log.d(TAG, "found camera id = " + String.valueOf(id));
        try {
            releaseCameraAndPreview();
            mCamera = Camera.open(id);
            qOpened = (mCamera != null);
        } catch (Exception e) {
            Log.e(getString(R.string.app_name), "failed to open Camera");
            e.printStackTrace();
        }

        return qOpened;
    }

    private void releaseCameraAndPreview() {
        Log.d(TAG, "Release camera");
        if (mCamera != null) {
            Log.d(TAG, "mCamera is not null");
            mCamera.stopPreview();
            mCamera.release();
            Log.d(TAG, "released camera");
            mCamera = null;

        }
    }

    /* Check Camera Configure */
    /** Check if this device has a camera */
    private boolean checkCameraHardware(Context context) {
        if (context.getPackageManager().hasSystemFeature(PackageManager.FEATURE_CAMERA_FRONT)){
            // this device has a camera
            return true;
        } else {
            // no camera on this device
            return false;
        }
    }

    private void initializeCamera() {
        if (checkCameraHardware(getApplicationContext())) {
            // Create an instance of Camera
            int cameraId = findFrontFacingCamera();
            if (safeCameraOpen(cameraId)) {
                // Create our Preview view and set it as the content of our activity.
                mHasCamera = true;
                Camera.CameraInfo ci = new Camera.CameraInfo();
                Camera.getCameraInfo(cameraId, ci);
                //mPreview = new Preview(getApplicationContext(), mCamera, ci);
                FrameLayout previewPane = (FrameLayout) findViewById(R.id.cameraPreview);
                previewPane.addView(mPreview);
                Log.d(TAG, "Front camera and preview created");
            } else {
                mHasCamera = false;
                Toast.makeText(getApplicationContext(), "Cannot open camera!!!", Toast.LENGTH_LONG)
                    .show();
            }
        } else {
            mHasCamera = false;
            Toast.makeText(getApplicationContext(), "No front camera!!!", Toast.LENGTH_LONG)
                .show();
        }
        Log.d(TAG, String.format("buttonTakePicture is %b", mHasCamera));
        buttonTakePicture.setEnabled(mHasCamera);
    }

    private int findFrontFacingCamera() {
        int cameraId = -1;
        // Search for the front facing camera
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

    private Camera.PictureCallback mPicture = new Camera.PictureCallback() {

        @Override
        public void onPictureTaken(byte[] data, Camera camera) {
            Log.d(TAG, "onPictureTaken");
            ContextWrapper wrapper = new ContextWrapper(getApplicationContext());

            File picFileDir = wrapper.getDir("Images",MODE_PRIVATE);

            Log.d(TAG, "--Directory = " + picFileDir.getAbsolutePath());

            FrameLayout frame = (FrameLayout) findViewById(R.id.cameraPreview);
            frame.setVisibility(View.INVISIBLE);
            ImageView v = (ImageView) findViewById(R.id.imageViewLive);

            Bitmap bmp = convertImage(data);
            //Bitmap bmp = BitmapFactory.decodeByteArray(data, 0, data.length);
            //String path = MediaStore.Images.Media.insertImage(getContentResolver(),
            //        bmp, "name" , "description");
            //Log.e("tag", "path: " + path); // prints something like "path: content://media/external/images/media/819"

            v.setImageBitmap(bmp);
            v.setVisibility(View.VISIBLE);

            String filePath = String.format("/sdcard/%d.jpg", System.currentTimeMillis());
            // /data/data/com.akinn.leverage/app_Images
            Log.d(TAG, "--Directory = " + filePath);
            mPicData = bmp;
        }
    };

    final static int REQUEST_LOCATION = 1;
    boolean mCanGetLocation = false;
    /*
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if(requestCode == REQUEST_LOCATION){
            if (resultCode == RESULT_OK) {
                Bundle extras = data.getExtras();
                mLongitude = extras.getDouble("Longitude");
                mLatitude = extras.getDouble("Latitude");
                Log.d(TAG, String.format("Location (%f, %f)", mLongitude, mLatitude));
                mCanGetLocation = true;
            } else {
                Log.d(TAG, "User didn't turn on GPS");
                mCanGetLocation = false;
            }
            gotoReviewActivity();
        }
    }
    */

    private Bitmap convertImage(byte[] data) {
        int degrees = 0;
        Display display = ((WindowManager)getApplicationContext()
                .getSystemService(WINDOW_SERVICE)).getDefaultDisplay();
        int rotation = display.getRotation();
        switch (rotation) {
            case Surface.ROTATION_0: degrees = 0; break;
            case Surface.ROTATION_90: degrees = 90; break;
            case Surface.ROTATION_180: degrees = 180; break;
            case Surface.ROTATION_270: degrees = 270; break;
        }

        int result;
        int orientation = mPreview.getCameraOrientation();
        Log.d(TAG, String.format("degree = %d, orientation = %d", degrees, orientation));
        //result = (orientation + degrees) % 360;
        //Log.d(TAG, String.format("result = %d", result));
        //result = (360 - result); //% 360;  // compensate the mirror
        result = orientation;
        Bitmap bmp = BitmapFactory.decodeByteArray(data , 0, data.length);
        Log.d(TAG, String.format("Bitmap: (%d, %d), Rotation: %d", bmp.getWidth(), bmp.getHeight(), result));

        Matrix mat = new Matrix();
        mat.postRotate(result);
        return Bitmap.createBitmap(bmp, 0, 0, bmp.getWidth(), bmp.getHeight(), mat, true);
    }

    private void gotoReviewActivity() {
        Log.d(TAG, ">gotoReviewActivity");
        Intent intent = new Intent(ScanActivity.this, ReviewActivity.class);
        intent.putExtra("MODE", mMode);
        intent.putExtra("STAMPED_TIME", mStampedTime);
        intent.putExtra("EMP_ID", mEmpId);
        intent.putExtra("EMP_NAME", mEmpName);
        EditText editText = (EditText) findViewById(R.id.editTextComment);
        intent.putExtra("COMMENT", editText.getText().toString().trim());
        intent.putExtra("PIC", mPicData);
        intent.putExtra("LONGITUDE", mLongitude);
        intent.putExtra("LATITUDE", mLatitude);
        intent.putExtra("HAS_CAMERA", mHasCamera);
        intent.putExtra("HAS_LOC", mCanGetLocation);
        Log.d(TAG, "<gotoReviewActivity");
        startActivity(intent);
    }

    final static int ACCESS_FINE_LOCATION_INTENT_ID = 3;
    BroadcastReceiver mBroadcastReceiver;
    private void checkLocationPermissionAndStartService() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            Log.d(TAG, "Android M or M+++!!");
            if (ContextCompat.checkSelfPermission(getApplicationContext(),
                    android.Manifest.permission.ACCESS_FINE_LOCATION)
                    != PackageManager.PERMISSION_GRANTED) {
                requestLocationPermission();
            } else {
                // Permission to access fine location is granted //
                startLocationService();
            }
        } else {
            startLocationService();
        }
    }

    private void requestLocationPermission() {
        if (ActivityCompat.shouldShowRequestPermissionRationale(ScanActivity.this,
                android.Manifest.permission.ACCESS_FINE_LOCATION)) {
            ActivityCompat.requestPermissions(ScanActivity.this,
                    new String[]{android.Manifest.permission.ACCESS_FINE_LOCATION},
                    ACCESS_FINE_LOCATION_INTENT_ID);

        } else {
            ActivityCompat.requestPermissions(ScanActivity.this,
                    new String[]{android.Manifest.permission.ACCESS_FINE_LOCATION},
                    ACCESS_FINE_LOCATION_INTENT_ID);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == ACCESS_FINE_LOCATION_INTENT_ID) {
            if(grantResults.length == 1
                    && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                startLocationService();
            } else {
                // Permission was denied or request was cancelled
                mCanGetLocation = false;
            }
        }
    }

    private void startLocationService() {
        Log.d(TAG, "startLocationService");
        stopLocationService();
        mCanGetLocation = true;
        Intent intent = new Intent(getApplicationContext(), MyLocationService.class);
        startService(intent);
    }

    private void stopLocationService() {
        Log.d(TAG, "stopLocationService");
        Intent intent = new Intent(getApplicationContext(), MyLocationService.class);
        stopService(intent);
    }
}
