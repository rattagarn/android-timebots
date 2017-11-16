package com.akinn.timebots;

import android.annotation.TargetApi;
import android.app.AlertDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.IntentSender;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.hardware.Camera;
import android.location.Location;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.provider.MediaStore;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.design.widget.Snackbar;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.*;
import android.view.MenuItem;
import android.widget.Button;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.crashlytics.android.Crashlytics;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.analytics.FirebaseAnalytics;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageMetadata;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.TimeZone;

import io.fabric.sdk.android.Fabric;

public class ScanCompatActivity extends AppCompatActivity implements
        GoogleApiClient.ConnectionCallbacks,
        GoogleApiClient.OnConnectionFailedListener,
        LocationListener {

    final static String TAG = Scan2Activity.class.getSimpleName();

    private FirebaseAnalytics mFirebaseAnalytics;
    private FirebaseDatabase mFireDB;

    String analyticCameraMode = "";
    String analyticCameraPermission = "";
    String analyticLocationPermission = "";

    String mFileName = "";

    Preview mPreview;
    boolean mHasFrontCamera = false;
    boolean mHasCamera = false;
    boolean mHasLocation = false;
    boolean mUseIntentCamera = false;

    String mMode = "";
    String mEmpId = "";
    String mEmpName = "";
    String mLocale = "";
    String mComment = "";
    long mStampedTime = -1;

    double mLongitude = 0.0;
    double mLatitude = 0.0;

    String mImgURI = "";

    Button buttonCheckIn;
    ProgressBar pbUpload;
    ImageButton buttonCapture;

    Bitmap mBmp;

    LocationRequest mLocationRequest;
    private GoogleApiClient mGoogleApiClient;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Fabric.with(this, new Crashlytics());
        Log.d(TAG, "onCreate");

        setContentView(R.layout.activity_scan_compat);

        // Obtain the FirebaseAnalytics instance.
        mFirebaseAnalytics = FirebaseAnalytics.getInstance(this);
        mFireDB = FirebaseDatabase.getInstance();

        Intent intent = getIntent();
        mMode = intent.getStringExtra("MODE");
        mEmpId = intent.getStringExtra("EMP_ID");
        mEmpName = intent.getStringExtra("EMP_NAME");
        mStampedTime = Calendar.getInstance().getTimeInMillis();
        mLocale = TimeZone.getDefault().getID();


        buttonCheckIn = (Button) findViewById(R.id.buttonCheckIn);
        buttonCheckIn.requestFocus();
        buttonCapture = (ImageButton) findViewById(R.id.buttonCapture);

        pbUpload = (ProgressBar) findViewById(R.id.progressUpload);

        Toolbar myToolbar = (Toolbar) findViewById(R.id.my_toolbar);
        setSupportActionBar(myToolbar);
        // Display icon in the toolbar
        getSupportActionBar().setDisplayShowHomeEnabled(true);
        getSupportActionBar().setLogo(R.mipmap.ic_launcher);
        getSupportActionBar().setDisplayUseLogoEnabled(true);
        // Remove default title text
        getSupportActionBar().setDisplayShowTitleEnabled(false);

        TextView tvEmpId = (TextView) myToolbar.findViewById(R.id.textViewEmpId);
        tvEmpId.setText(mEmpId);
        TextView tvEmpName = (TextView) myToolbar.findViewById(R.id.textViewEmpName);
        tvEmpName.setText(mEmpName);

        queryLatestScan(mEmpId);

        mUseIntentCamera = false;
        if (checkFrontCameraHardware(getApplicationContext())) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                mUseIntentCamera = true;
                buttonCapture.setVisibility(View.VISIBLE);
                buttonCapture.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        dispatchTakePictureIntent();
                    }
                });
                analyticCameraMode = "FRONT_INTENT";
            } else {
                buttonCapture.setVisibility(View.INVISIBLE);
                FrameLayout previewPane = (FrameLayout) findViewById(R.id.cameraPreview);
                mPreview = new Preview(getApplicationContext(), null,
                        null, previewPane, mEmpName);
                analyticCameraMode = "FRONT";
            }
        } else if (checkRearCameraHardware(getApplicationContext())) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                mUseIntentCamera = true;
                buttonCapture.setVisibility(View.VISIBLE);
                buttonCapture.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        dispatchTakePictureIntent();
                    }
                });
                analyticCameraMode = "REAR_INTENT";
            } else {
                buttonCapture.setVisibility(View.INVISIBLE);
                FrameLayout previewPane = (FrameLayout) findViewById(R.id.cameraPreview);
                mPreview = new Preview(getApplicationContext(), null,
                        null, previewPane, mEmpName);
                analyticCameraMode = "REAR";
            }
        } else {
            analyticCameraMode = "NONE";
            buttonCapture.setVisibility(View.INVISIBLE);
            showMessage("No camera support");
        }

        buttonCheckIn.setOnClickListener( new View.OnClickListener() {
            public void onClick(View v) {
                buttonCapture.setEnabled( false );
                mStampedTime = Calendar.getInstance().getTimeInMillis();
                TextView textView = (TextView) findViewById(R.id.editTextComment);
                mComment = textView.getText().toString().trim();
                if (mUseIntentCamera) {
                    if (mBmp != null) {
                        ImageView imgView = (ImageView) findViewById(R.id.imageViewLive);
                        editPictureAndUpload(mBmp, imgView);
                        //saveImage(mBmp);
                    } else {
                        writeScan(mMode, mEmpId, "", mComment,
                                mStampedTime, mLongitude, mLatitude,
                                false, mHasLocation, mLocale, "", "");

                    }
                } else {
                    Camera camera = mPreview.getCamera();
                    if (camera != null) {
                        Log.d(TAG, "Taking picture");
                        try {
                            camera.takePicture(null, null, mPicture);
                        } catch (Exception e) {
                            Log.d(TAG, e.getMessage());
                        }
                    } else {
                        writeScan(mMode, mEmpId, "", mComment,
                                mStampedTime, mLongitude, mLatitude,
                                false, mHasLocation, mLocale, "", "");
                    }
                }
            }
        });

        //Helper.sendGoogleAnalyticInfo(mFirebaseAnalytics, this, mMode, mEmpId, mEmpName,
        //        mHasFrontCamera, mHasCamera, mHasLocation);
    }

    @Override
    protected void onDestroy() {
        Log.d(TAG, "onPause");
        super.onDestroy();
        if (mBroadcastReceiver != null) {
            unregisterReceiver(mBroadcastReceiver);
        }
    }

    @Override
    protected void onPause() {
        Log.d(TAG, "onPause");
        super.onPause();
        if (mPreview != null) {
            mPreview.stopCamera();
        }
        stopLocationService();
        sendAnalytic("exit");
    }

    @Override
    protected void onResume() {
        Log.d(TAG, "onResume");
        super.onResume();
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
    public void onBackPressed() {
        Log.d(TAG, "onBackPressed");
        AlertDialog.Builder alertDialog = new AlertDialog.Builder(ScanCompatActivity.this);
        alertDialog.setPositiveButton(getString(R.string.yes), new DialogInterface.OnClickListener() {

            @Override
            public void onClick(DialogInterface dialog, int which) {
                FirebaseAuth mAuth = FirebaseAuth.getInstance();
                mAuth.signOut();
                ScanCompatActivity.super.onBackPressed();
            }
        });
        alertDialog.setNegativeButton(getString(R.string.no), null);
        alertDialog.setMessage(getString(R.string.exit_message));
        alertDialog.setTitle(getString(R.string.exit_title));
        alertDialog.show();
    }

    private void logoutConfirm() {
        AlertDialog.Builder alertDialog = new AlertDialog.Builder(ScanCompatActivity.this);
        alertDialog.setPositiveButton(getString(R.string.yes), new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                Bundle bundle = new Bundle();
                bundle.putString("USER", mEmpId);
                bundle.putString("MODE", mMode);
                bundle.putString("ACTIVITY", TAG);
                mFirebaseAnalytics.logEvent("employee_logout", bundle);
                clearAutoLogin();
                gotoLoginActivity();
            }
        });
        alertDialog.setNegativeButton(getString(R.string.no), null);
        alertDialog.setMessage(getString(R.string.logout_message));
        alertDialog.setTitle(getString(R.string.logout_title));
        alertDialog.show();
    }

    private void clearAutoLogin() {
        Log.d(TAG, "clearAutoLogin");
        SharedPreferences sp = this.getSharedPreferences("LOGIN", MODE_PRIVATE);
        SharedPreferences.Editor ed = sp.edit();
        ed.clear();
        ed.commit();
    }

    private void gotoLoginActivity() {
        Intent intent;
        Log.d(TAG, "Go to Login Activity");
        intent = new Intent(ScanCompatActivity.this, Login2Activity.class);
        finish();
        startActivity(intent);
    }

    ////////////////////////////
    // Menu                   //
    //------------------------//
    // Menu icons are inflated just as they were with actionbar
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.main_menu, menu);
        menu.findItem(R.id.action_scan).setEnabled(false);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_scan:
                return true;

            case R.id.action_logout:
                logoutConfirm();
                return true;

            default:
                return super.onOptionsItemSelected(item);
        }
    }


    ////////////////////////////////////
    // Firebase DB                    //
    ////////////////////////////////////
    private void queryLatestScan(final String empId) {
        final FirebaseDatabase database = FirebaseDatabase.getInstance();
        final DatabaseReference ref = database.getReference("Scans/LastScan/" + empId);
        Log.d(TAG, "queryLatestScan: " + "Scans/LastScan/" + empId);
        ValueEventListener queryListner = new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                Log.d(TAG, "queryLatestScan: onDataChange");
                TextView textViewTime = (TextView) findViewById(R.id.textViewScan);
                if (dataSnapshot.exists()) {
                    long lastScannedTime = (long) dataSnapshot.child("Scanned").getValue();
                    Log.d(TAG, "This emp: " + empId + " scanned in " + String.valueOf(lastScannedTime));
                    String zoneId = TimeZone.getDefault().getID();
                    Log.d(TAG, "Time zone: " + zoneId);
                    SimpleDateFormat sdf =
                            new SimpleDateFormat("dd-MMM-yyyy hh:mm",
                                    new Locale("th", "TH"));
                    sdf.setTimeZone(TimeZone.getTimeZone(zoneId));
                    String dateWithFormat = sdf.format(new Date(lastScannedTime));
                    textViewTime.setText(dateWithFormat);
                } else {
                    textViewTime.setText("");
                }
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
                Log.d(TAG, "queryLatestScan: " + databaseError.getMessage() + "\n" + databaseError.getDetails());
                sendAnalytic("error", "queryLatestScan", databaseError.getDetails());
                //Helper.sendGoogleAnalyticError(mFirebaseAnalytics, TAG, mEmpName,
                //        databaseError.getDetails());
            }
        };
        ref.addListenerForSingleValueEvent(queryListner);

    }

    private void writeScan(String mode, String empId, String imgURI, String comment,
                           long scannedTime, double lng, double lat,
                           boolean hasPic, boolean hasLoc, String timeZone,
                           final String localFilePath, final String fileName) {

        Calendar c = Calendar.getInstance();
        c.setTimeInMillis(scannedTime);
        c.setTimeZone(TimeZone.getTimeZone(timeZone));
        int year = c.get(Calendar.YEAR);
        int month = c.get(Calendar.MONTH) + 1;
        int d = c.get(Calendar.DATE);

        final String refInPath = String.format(Locale.ENGLISH,"Scans/%d/%d/%d/In/%s/", year, month, d, empId);
        Log.d(TAG, refInPath);
        final String refOutPath = String.format(Locale.ENGLISH,"Scans/%d/%d/%d/Out/%s", year, month, d, empId);

        final ScanData scanData = new ScanData(empId, imgURI, mode, comment,
                scannedTime, lng, lat, hasPic, hasLoc, timeZone);
        try {
            final DatabaseReference dbRef = mFireDB.getReference(refInPath);
            ValueEventListener queryListner = new ValueEventListener() {
                @Override
                public void onDataChange(DataSnapshot dataSnapshot) {
                    Log.d(TAG, "writeScan: onDataChange[" + refInPath+ "]");
                    Log.d(TAG, "writeScan: Children[" + refInPath + "] = "
                            + String.valueOf(dataSnapshot.getChildrenCount()));

                    if (dataSnapshot.getChildrenCount() > 0) {
                        // Scan In already had record //
                        // add Scan Out //
                        DatabaseReference dbOutRef = mFireDB.getReference(refOutPath);
                        Map<String, Object> scanDataValues = scanData.toMap();
                        dbOutRef.setValue(scanDataValues);
                        Log.d(TAG, "Done update scan out data");
                    } else {
                        // add Scan In //
                        DatabaseReference dbInRef = mFireDB.getReference(refInPath);
                        Map<String, Object> scanDataValues = scanData.toMap();
                        dbInRef.setValue(scanDataValues);
                        Log.d(TAG, "Done update scan in data");
                    }

                    String lastScanPath = "Scans/LastScan/" + scanData.EmployeeID;
                    DatabaseReference dbLastScanRef = mFireDB.getReference(lastScanPath);
                    HashMap<String, Object> lastScanValue = new HashMap<>();
                    lastScanValue.put("Scanned", scanData.ScannedTime);
                    lastScanValue.put("TimeZone", scanData.MobileTimeZone);
                    dbLastScanRef.setValue(lastScanValue);
                    Log.d(TAG, "Done update last scan data");

                    if (mImgURI.equalsIgnoreCase("")) {
                        sendAnalytic("scan", analyticCameraMode, analyticCameraPermission,
                                analyticLocationPermission, false, true, "");
                    } else {
                        sendAnalytic("scan", analyticCameraMode, analyticCameraPermission,
                                analyticLocationPermission, true, true, "");
                    }
                    finishUpAllUpload(localFilePath, fileName);
                }

                @Override
                public void onCancelled(DatabaseError databaseError) {
                    Log.d(TAG, "writeScan: onCancelled", databaseError.toException());
                    Helper.sendGoogleAnalyticError(mFirebaseAnalytics, TAG, mEmpName,
                            "DatabaseError in writeScan: " + databaseError.getDetails());
                    sendAnalytic("error", "writeScan", databaseError.getDetails());
                    showMessageLongTime(getString(R.string.fire_db_error_query_scan));
                }
            };
            dbRef.addListenerForSingleValueEvent(queryListner);


        } catch (Exception e) {
            Log.d("###", e.getMessage());
            if (mImgURI.equalsIgnoreCase("")) {
                sendAnalytic("scan", analyticCameraMode, analyticCameraPermission,
                        analyticLocationPermission, false, false, e.getMessage());
            } else {
                sendAnalytic("scan", analyticCameraMode, analyticCameraPermission,
                        analyticLocationPermission, true, false, e.getMessage());
            }
        }
    }

    private void uploadImageHandler(final ImageView v, final String localFilePath, final String fileName,
                                    final String empId, long scannedTime, String timeZone) {

        Calendar c = Calendar.getInstance();
        c.setTimeInMillis(scannedTime);
        c.setTimeZone(TimeZone.getTimeZone(timeZone));
        Date date = c.getTime();
        int year = c.get(Calendar.YEAR);
        int month = c.get(Calendar.MONTH) + 1;
        int d = c.get(Calendar.DATE);
        final String dateObjStr = String.valueOf(d);
        final String refInPath = String.format(Locale.ENGLISH,
                "Scans/%d/%d/%d/In/%s", year, month, d, empId);
        try {
            final DatabaseReference dbRef = mFireDB.getReference(refInPath);
            ValueEventListener queryListner = new ValueEventListener() {
                @Override
                public void onDataChange(DataSnapshot dataSnapshot) {
                    Log.d(TAG, "unloadImageHandler: onDataChange[" + refInPath + "]");
                    Log.d(TAG, "unloadImageHandler: Children[" + refInPath + "] = "
                            + String.valueOf(dataSnapshot.getChildrenCount()));
                    if (dataSnapshot.getChildrenCount() > 0) {
                        // Scan In already had record //
                        // add Scan Out //
                        doImageUpload(v, localFilePath, fileName, empId, "Out/" + dateObjStr);
                    } else {
                        // add Scan In //
                        doImageUpload(v, localFilePath, fileName, empId, "In/" + dateObjStr);
                    }
                }

                @Override
                public void onCancelled(DatabaseError databaseError) {
                    Log.d(TAG, "unloadImageHandler: onCancelled", databaseError.toException());
                    showMessage(getString(R.string.fire_db_error_query_scan));
                    sendAnalytic("error", "uploadImageHandler",
                            databaseError.getMessage());
                }
            };
            dbRef.addListenerForSingleValueEvent(queryListner);
        } catch (Exception e) {
            Log.d("###", e.getMessage());
            sendAnalytic("exception", "uploadImageHandler", e.getMessage());
        }
    }

    private void doImageUpload(ImageView v, final String localFilePath, final String fileName,
                               String empId, String pathPic) {
        FirebaseStorage storage = FirebaseStorage.getInstance();
        StorageReference storageRef = storage.getReference(pathPic);
        StorageMetadata metadata = new StorageMetadata.Builder()
                .setContentType("image/jpg")
                .setCustomMetadata("id", empId)
                .build();

        String filePath = String.format("%s/%s.jpg", localFilePath, fileName);
        final Uri file = Uri.fromFile(new File(filePath));
        Log.d(TAG, "File Path: " + filePath);

        pbUpload.setVisibility(View.VISIBLE);

        UploadTask uploadTask = storageRef.child(empId).putFile(file, metadata);
        uploadTask.addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception e) {
                Log.d(TAG, "Fail to upload picture " + e.getMessage());
                showMessageLongTime(getString(R.string.fire_str_upload_image));
                sendAnalytic("error", "doImageUpload", e.getMessage());
                pbUpload.setVisibility(View.GONE);

                writeScan(mMode, mEmpId, "", mComment,
                        mStampedTime, mLongitude, mLatitude,
                        mHasCamera, mHasLocation, mLocale , localFilePath, fileName);

            }
        }).addOnSuccessListener(ScanCompatActivity.this, new OnSuccessListener<UploadTask.TaskSnapshot>() {
            @Override
            public void onSuccess(UploadTask.TaskSnapshot taskSnapshot) {
                pbUpload.setVisibility(View.GONE);
                Uri downloadUri = taskSnapshot.getDownloadUrl();
                Log.d(TAG, "Success to upload: " + downloadUri.toString());

                mImgURI = downloadUri.toString();
                writeScan(mMode, mEmpId, mImgURI, mComment,
                        mStampedTime, mLongitude, mLatitude,
                        mHasCamera, mHasLocation, mLocale, localFilePath, fileName);
            }
        });
    }

    private void finishUpAllUpload(String localFilePath, String fileName) {
        showMessageLongTime(getString(R.string.succes_scanin));
        deleteImageFile(localFilePath, fileName + ".jpg");

        Log.d(TAG, "mMode = " + mMode);
        if (mMode.equals("KIOSK")) {
            gotoAnonymousLogin();
        } else {
            reloadCurrentActivity();
        }
    }

    private void deleteImageFile(String dirPath, String fileName) {
        Log.d(TAG, "Delete image file: " + fileName);
        File dir = new File(dirPath);
        File[] files = dir.listFiles();
        try {
            for (int i = 0; i < files.length; ++i) {
                File file = files[i];
                if (!file.isDirectory()) {
                    if (file.getName().equalsIgnoreCase(fileName)) {
                        Log.d(TAG, fileName + " is deleted");
                        file.delete();
                        break;
                    }
                }
            }
        } catch (Exception e) {
            sendAnalytic("exception", "deleteImageFile", e.getMessage());
        }
    }

    ////////////////////////////
    // Camera Feature         //

    Camera mCamera = null;
    private boolean checkFrontCameraHardware(Context context) {
        if (context.getPackageManager().hasSystemFeature(PackageManager.FEATURE_CAMERA_FRONT)){
            // this device has a camera
            mHasFrontCamera = true;
            return true;
        } else {
            // no camera on this device
            mHasFrontCamera = false;
            return false;
        }
    }
    private boolean checkRearCameraHardware(Context context) {
        if (context.getPackageManager().hasSystemFeature(PackageManager.FEATURE_CAMERA)){
            // this device has a camera
            mHasCamera = true;
            return true;
        } else {
            // no camera on this device
            mHasCamera = false;
            return false;
        }
    }

    private void releaseCameraAndStopPreview() {
        Log.d(TAG, "Release camera");
        if (mCamera != null) {
            Log.d(TAG, "mCamera is not null");
            mCamera.stopPreview();
            mCamera.release();
            Log.d(TAG, "released camera");
            mCamera = null;

        }
    }

    private Camera.PictureCallback mPicture = new Camera.PictureCallback() {

        @Override
        public void onPictureTaken(byte[] data, Camera camera) {
            Log.d(TAG, "onPictureTaken");

            FrameLayout frame = (FrameLayout) findViewById(R.id.cameraPreview);
            frame.setVisibility(View.INVISIBLE);
            ImageView v = (ImageView) findViewById(R.id.imageViewLive);
            Log.d(TAG, String.format("Image VIew (%d, %d", v.getWidth(), v.getHeight()));

            Bitmap bmp = convertImage(data, 150, 150);

            editPictureAndUpload(bmp, v);
        }
    };

    private void editPictureAndUpload(Bitmap bmp, ImageView v) {
        int maxHeight = bmp.getScaledHeight(bmp.getDensity());
        int maxWidth = bmp.getScaledWidth(bmp.getDensity());

        Log.d(TAG, String.format("Bitmap dimension in px (%d, %d)", maxWidth, maxHeight));

        int posX = 5;
        int posY = maxHeight - 15;
        String  msg = String.format(Locale.ENGLISH,"(%f, %f)", mLatitude, mLongitude);
        bmp = putWaterMark(bmp, msg, posX, posY);
        posY -= 15;
        msg = mEmpName;
        bmp = putWaterMark(bmp, msg, posX, posY);
        msg = getDateString(mStampedTime);
        posY -= 15;
        bmp = putWaterMark(bmp, msg, posX, posY);

        v.setImageBitmap(bmp);
        v.setVisibility(View.VISIBLE);

        String imageFolderPath = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES)
                .getPath();
        imageFolderPath += "/Timebots";
        Log.d(TAG, "Folder == " + imageFolderPath);

        File dir = new File(imageFolderPath);
        if (!dir.exists()) {
            dir.mkdirs();
        }
        mFileName = String.valueOf(System.currentTimeMillis());
        String filePath = String.format("%s/%s.jpg", imageFolderPath, mFileName);
        // /data/data/com.akinn.leverage/app_Images
        Log.d(TAG, "--Directory = " + filePath);
        try {
            bmp.compress(Bitmap.CompressFormat.JPEG, 80,
                    new FileOutputStream(new File(filePath)));
            uploadImageHandler(v, imageFolderPath, mFileName, mEmpId, mStampedTime, mLocale);
        } catch (FileNotFoundException e) {
            // TODO Auto-generated catch block
            //Helper.sendGoogleAnalyticError(mFirebaseAnalytics, TAG, mEmpName,
            //        "FileNotFoundException in editPictureAndSave: " + e.getMessage());
            sendAnalytic("scan", analyticCameraMode, analyticCameraPermission,
                    analyticLocationPermission, true,false, e.getMessage());
            e.printStackTrace();
        }
    }

    private Bitmap convertImage(byte[] data, int picFrameWidth, int picFrameHeight) {

        Log.d(TAG, String.format("Picture Frame: (%d, %d)", picFrameWidth, picFrameHeight));

        Display display = ((WindowManager)getApplicationContext()
                .getSystemService(WINDOW_SERVICE)).getDefaultDisplay();
        int dispOrientation = display.getRotation();
        /*
        switch (dispOrientation) {
            case Surface.ROTATION_0: degrees = 0; break;
            case Surface.ROTATION_90: degrees = 90; break;
            case Surface.ROTATION_180: degrees = 180; break;
            case Surface.ROTATION_270: degrees = 270; break;
        }

        */
        int result;
        int camOrientation = mPreview.getCameraOrientation();
        Log.d(TAG, String.format("Display Orientation = %d, Camera Orientation = %d", dispOrientation, camOrientation));

        int diffDegress = Math.abs(dispOrientation - camOrientation);

        Bitmap bmp = BitmapFactory.decodeByteArray(data , 0, data.length);
        Log.d(TAG, String.format("Bitmap: (%d, %d), Diff Rotation: %d", bmp.getWidth(), bmp.getHeight(), diffDegress));
        int bmpWidth = bmp.getWidth();
        int bmpHeight = bmp.getHeight();
        if ((diffDegress == 90) || (diffDegress == 270)) {
            bmpWidth = bmp.getHeight();
            bmpHeight = bmp.getWidth();
        }

        // Scaling //
        Log.d(TAG, String.format("Cal Scale: picFrameWidth = %d, bmpWidth = %d", picFrameWidth, bmpWidth));
        double scaleX = ((double)picFrameWidth / (double)bmpWidth);
        Log.d(TAG, String.format("Cal Scale: picFrameHeight = %d, bmpHeight = %d", picFrameHeight, bmpHeight));
        double scaleY = ((double)picFrameHeight / (double)bmpHeight);
        Bitmap bmp2 = null;
        if (scaleX < scaleY) {
            Log.d(TAG, String.format("New Scale: (%d, %d), Scale X: %f", picFrameWidth, (int) (bmpHeight * scaleX), scaleX));
            bmp2 = bmp.createScaledBitmap(bmp, picFrameWidth, (int) (bmpHeight * scaleX), true);
        } else {
            Log.d(TAG, String.format("New Scale: (%d, %d), Scale Y: %f", picFrameHeight, (int) (bmpWidth * scaleY), scaleY));
            bmp2 = bmp.createScaledBitmap(bmp, (int) (bmpHeight * scaleY), picFrameWidth, true);
        }
        Log.d(TAG, String.format("Bitmap2: (%d, %d)", bmp2.getWidth(), bmp2.getHeight()));

        // Rotating //
        Matrix mat = new Matrix();
        mat.postRotate(diffDegress);
        return Bitmap.createBitmap(bmp2, 0, 0, bmp2.getWidth(), bmp2.getHeight(), mat, true);
    }

    private Bitmap putWaterMark(Bitmap bmp, String message, int posX, int posY) {
        Canvas cs = new Canvas(bmp);
        Paint tPaint = new Paint();
        tPaint.setTextSize(14);
        tPaint.setColor(Color.BLUE);
        tPaint.setStyle(Paint.Style.FILL);
        cs.drawBitmap(bmp, 0f, 0f, null);
        float height = tPaint.measureText("yY");
        cs.drawText(message, posX, posY + height, tPaint);
        return bmp;
    }


    // For Android M ++++ //
    static final int REQUEST_IMAGE_CAPTURE = 10;
    static final int CAMERA_REQUEST_CODE = 11;
    @TargetApi(23)
    private void dispatchTakePictureIntent() {
        mUseIntentCamera = true;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (checkSelfPermission(android.Manifest.permission.CAMERA)
                    != PackageManager.PERMISSION_GRANTED) {
                Log.d(TAG, "SDK23: Camera Permission not Granted");
                requestPermissions(new String[]{android.Manifest.permission.CAMERA},
                        CAMERA_REQUEST_CODE);
            } else {
                Log.d(TAG, "SDK23: Camera Permission Granted");
                analyticCameraPermission = "GRANTED";
                Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
                if (takePictureIntent.resolveActivity(getPackageManager()) != null) {
                    startActivityForResult(takePictureIntent, REQUEST_IMAGE_CAPTURE);
                } else {
                    sendAnalytic("error", "dispatchTakePictureIntent",
                            "Call Take Picture Intent return null");
                }
            }
        } else {
            if (ContextCompat.checkSelfPermission(getApplicationContext(), android.Manifest.permission.CAMERA)
                    != PackageManager.PERMISSION_GRANTED) {
                Log.d(TAG, "SDK23: Camera Permission not Granted");
                requestPermissions(new String[]{android.Manifest.permission.CAMERA},
                        CAMERA_REQUEST_CODE);
            } else {
                Log.d(TAG, "SDK23: Camera Permission Granted");
                analyticCameraPermission = "GRANTED";
                Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
                if (takePictureIntent.resolveActivity(getPackageManager()) != null) {
                    startActivityForResult(takePictureIntent, REQUEST_IMAGE_CAPTURE);
                } else {
                    sendAnalytic("error", "dispatchTakePictureIntent",
                            "Call Take Picture Intent return null");
                }
            }
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        Log.d(TAG, String.format("SDK23: onActivityResult requestCode = %d, resultCode = %d", requestCode, resultCode));
        if (requestCode == REQUEST_IMAGE_CAPTURE && resultCode == RESULT_OK) {
            Bundle extras = data.getExtras();
            Bitmap imageBitmap = (Bitmap) extras.get("data");
            Bitmap bmp = null;

            if (imageBitmap == null) {
                bmp = imageBitmap.copy(Bitmap.Config.ARGB_8888, true);
                if (bmp != null) {
                    int maxHeight = bmp.getScaledHeight(bmp.getDensity());
                    int maxWidth = bmp.getScaledWidth(bmp.getDensity());
                    Log.d(TAG, String.format("BMP: %d, %d", maxHeight, maxWidth));
                    String msg = mEmpName;
                    int posX = 5;
                    int posY = maxHeight - 15;
                    msg = String.format(Locale.ENGLISH, "(%f, %f)", mLatitude, mLongitude);
                    bmp = putWaterMark(bmp, msg, posX, posY);
                    posY -= 15;
                    bmp = putWaterMark(bmp, msg, posX, posY);
                    msg = getDateString(mStampedTime);
                    posY -= 15;
                    bmp = putWaterMark(bmp, msg, posX, posY);
                } else {
                    sendAnalytic("fail_convert_image", "onActivityResult",
                            "Bitmap.copy return null");
                }
            } else {
                //Helper.sendGoogleAnalyticPermission(mFirebaseAnalytics, TAG, mEmpName,
                //        "Intent Camera return null image");
                sendAnalytic("fail_get_intent_image", "onActivityResult",
                        "Intent camera return null");
            }
            FrameLayout frame = (FrameLayout) findViewById(R.id.cameraPreview);
            frame.setVisibility(View.INVISIBLE);
            ImageView v = (ImageView) findViewById(R.id.imageViewLive);
            v.setImageBitmap(bmp);
            v.setVisibility(View.VISIBLE);
        } else {
            sendAnalytic("fail_get_intent_image");
        }
    }



    ///////////////////////////////////////////
    // Location                              //
    final static int ACCESS_FINE_LOCATION_INTENT_ID = 2;
    final static int CONNECTION_FAILURE_RESOLUTION_REQUEST = 9000;
    BroadcastReceiver mBroadcastReceiver;
    private void checkLocationPermissionAndStartService() {
        mGoogleApiClient = new GoogleApiClient.Builder(this)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .addApi(LocationServices.API)
                .build();
        // Create the LocationRequest object
        mLocationRequest = LocationRequest.create()
                .setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY)
                .setInterval(10 * 1000)        // 10 seconds, in milliseconds
                .setFastestInterval(1 * 1000); // 1 second, in milliseconds
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            Log.d(TAG, "Android M or M+++!!");
            if (ContextCompat.checkSelfPermission(getApplicationContext(),
                    android.Manifest.permission.ACCESS_FINE_LOCATION)
                    != PackageManager.PERMISSION_GRANTED) {
                requestLocationPermission();
            } else {
                // Permission to access fine location is granted //
                analyticLocationPermission = "GRANTED";
                startLocationService();
            }
        } else {
            analyticLocationPermission = "GRANTED";
            startLocationService();
        }
    }

    private void requestLocationPermission() {
        if (ActivityCompat.shouldShowRequestPermissionRationale(ScanCompatActivity.this,
                android.Manifest.permission.ACCESS_FINE_LOCATION)) {
            ActivityCompat.requestPermissions(ScanCompatActivity.this,
                    new String[]{android.Manifest.permission.ACCESS_FINE_LOCATION},
                    ACCESS_FINE_LOCATION_INTENT_ID);

        } else {
            ActivityCompat.requestPermissions(ScanCompatActivity.this,
                    new String[]{android.Manifest.permission.ACCESS_FINE_LOCATION},
                    ACCESS_FINE_LOCATION_INTENT_ID);
        }
    }

    private void startLocationService() {
        Log.d(TAG, "startLocationService");
        sendAnalytic("enter", analyticCameraMode, analyticCameraPermission, analyticLocationPermission);
        stopLocationService();
        mGoogleApiClient.connect();
        mHasLocation = true;
        Intent intent = new Intent(getApplicationContext(), MyLocationService.class);
        startService(intent);
    }

    private void stopLocationService() {
        Log.d(TAG, "stopLocationService");
        if (mGoogleApiClient.isConnected()) {
            LocationServices.FusedLocationApi.removeLocationUpdates(mGoogleApiClient, this);
            mGoogleApiClient.disconnect();
        }
        Intent intent = new Intent(getApplicationContext(), MyLocationService.class);
        stopService(intent);
    }

    @Override
    public void onConnected(@Nullable Bundle bundle) {
        Log.d(TAG, "GoogleApiClient onConnected");
        try {
            Location location = LocationServices.FusedLocationApi.getLastLocation(mGoogleApiClient);
            if (location == null) {
                LocationServices.FusedLocationApi.requestLocationUpdates(mGoogleApiClient,
                        mLocationRequest, this);
            } else {
                onLocationChanged(location);
            }
        } catch (SecurityException e) {
            Log.d(TAG, e.getMessage());
            Helper.sendGoogleAnalyticPermission(mFirebaseAnalytics, TAG, mEmpName,
                    "onConnected: " + e.getMessage());
        }
    }

    @Override
    public void onConnectionSuspended(int i) {
        Log.d(TAG, "GoogleApiClient onConnectionSuspended");
    }

    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {
        Log.d(TAG, "GoogleApiClient onConnectionFailed");
        /*
         * Google Play services can resolve some errors it detects.
         * If the error has a resolution, try sending an Intent to
         * start a Google Play services activity that can resolve
         * error.
         */
        if (connectionResult.hasResolution()) {
            try {
                // Start an Activity that tries to resolve the error
                connectionResult.startResolutionForResult(this, CONNECTION_FAILURE_RESOLUTION_REQUEST);
                /*
                 * Thrown if Google Play services canceled the original
                 * PendingIntent
                 */
            } catch (IntentSender.SendIntentException e) {
                // Log the error
                Helper.sendGoogleAnalyticPermission(mFirebaseAnalytics, TAG, mEmpName,
                        "Google Play connection failed: " + e.getMessage());
                e.printStackTrace();
            }
        } else {
            /*
             * If no resolution is available, display a dialog to the
             * user with the error.
             */
            String msg = "Location services connection failed with code " + connectionResult.getErrorCode();
            Log.i(TAG, msg);
            Helper.sendGoogleAnalyticPermission(mFirebaseAnalytics, TAG, mEmpName,
                    msg);
            Toast.makeText(getApplicationContext(), msg, Toast.LENGTH_LONG).show();
        }
    }

    @Override
    public void onLocationChanged(Location location) {
        Log.d(TAG, location.toString());

        mLatitude = location.getLatitude();
        mLongitude = location.getLongitude();

        Log.d(TAG, String.format("onLocationChanged: (%f, %f)", mLatitude, mLongitude));
    }

    //=======================================//

    ///////////////////////////////////////////
    // Permission                            //
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == ACCESS_FINE_LOCATION_INTENT_ID) {
            if(grantResults.length == 1
                    && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                mHasLocation = true;
                analyticLocationPermission = "REQUESTED_AND_GRANTED";
                startLocationService();
            } else {
                // Permission was denied or request was cancelled
                mHasLocation = false;
                analyticLocationPermission = "REQUESTED_AND_DENIED";
                //Helper.sendGoogleAnalyticPermission(mFirebaseAnalytics, TAG, mEmpName,
                //        "Location permission is not granted");
            }
        } else if (requestCode == CAMERA_REQUEST_CODE) {
            Log.d(TAG, "SDK23: Camera Permission Grant?: " + grantResults[0]);
            if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // Now user should be able to use camera
                analyticCameraPermission = "REQUESTED_AND_GRANTED";
                dispatchTakePictureIntent();
            }
            else {
                // Your app will not have this permission. Turn off all functions
                // that require this permission or it will force close like your
                // original question
                analyticCameraPermission = "REQUESTED_AND_DENIED";
                mHasCamera = false;
                mHasFrontCamera = false;
                //Helper.sendGoogleAnalyticPermission(mFirebaseAnalytics, TAG, mEmpName,
                //        "Camera permission is not granted");
            }
        }
    }

    ///////////////////////////////
    //                           //
    private String getDateString(long millisecTime) {
        String zoneId = TimeZone.getDefault().getID();
        Locale currentLocale = getResources().getConfiguration().locale;
        SimpleDateFormat sdf =
                new SimpleDateFormat("dd-MMM-yyyy hh:mm",
                        currentLocale);
        sdf.setTimeZone(TimeZone.getTimeZone(zoneId));
        return sdf.format(new Date(millisecTime));
    }

    private void gotoAnonymousLogin() {
        Log.d(TAG, "Go to Anonymous Login");
        Intent intent = new Intent(ScanCompatActivity.this, AnonymousLoginActivity.class);
        finish();
        startActivity(intent);
    }

    private void reloadCurrentActivity() {
        Log.d(TAG, "Reload Scan2Activity");
        FrameLayout frame = (FrameLayout) findViewById(R.id.cameraPreview);
        frame.setVisibility(View.VISIBLE);
        ImageView imageView = (ImageView) findViewById(R.id.imageViewLive);
        imageView.setVisibility(View.INVISIBLE);
        imageView.setImageBitmap(null);
        EditText editComment = (EditText) findViewById(R.id.editTextComment);
        editComment.setText("");
        if (mPreview != null) {
            mPreview.getCamera().startPreview();
        }
        mStampedTime = Calendar.getInstance().getTimeInMillis();
        queryLatestScan(mEmpId);
    }

    private void showMessage(String message) {
        //View view = findViewById(R.id.scroll_layout);
        View view = findViewById(android.R.id.content);
        Snackbar snackbar = Snackbar.make(view, message, Snackbar.LENGTH_INDEFINITE);
        snackbar.show();
    }

    private void showMessageLongTime(String message) {
        //View view = findViewById(R.id.scroll_layout);
        View view = findViewById(android.R.id.content);
        Snackbar snackbar = Snackbar.make(view, message, Snackbar.LENGTH_LONG);
        snackbar.show();
    }

    private void showMessageShortTime(String message) {
        //View view = findViewById(R.id.scroll_layout);
        View view = findViewById(android.R.id.content);
        Snackbar snackbar = Snackbar.make(view, message, Snackbar.LENGTH_SHORT);
        snackbar.show();
    }

    private void sendAnalytic(String eventName,
                              String cameraMode, String cameraPermission, String locationPermission) {
        Log.d(TAG, String.format("sendAnalytic with permission data for event: %s", eventName));
        Bundle bundle = new Bundle();
        bundle.putString("USER", mEmpId);
        bundle.putString("MODE", mMode);
        bundle.putInt("SDK", Build.VERSION.SDK_INT);
        bundle.putString("ACTIVITY", TAG);
        bundle.putString("CAMERA", cameraMode);
        bundle.putString("CAMERA_PERMISSION", cameraPermission);
        bundle.putString("LOCATION_PERMISSION", locationPermission);
        mFirebaseAnalytics.logEvent(eventName, bundle);
    }

    private void sendAnalytic(String eventName) {
        Log.d(TAG, String.format("sendAnalytic with no data for event: %s", eventName));
        Bundle bundle = new Bundle();
        bundle.putString("USER", mEmpId);
        bundle.putString("MODE", mMode);
        bundle.putInt("SDK", Build.VERSION.SDK_INT);
        bundle.putString("ACTIVITY", TAG);
        mFirebaseAnalytics.logEvent(eventName, bundle);
    }

    private void sendAnalytic(String eventName, String funcName, String message) {
        Log.d(TAG, String.format("sendAnalytic with message data for event: %s", eventName));
        Bundle bundle = new Bundle();
        bundle.putString("USER", mEmpId);
        bundle.putString("MODE", mMode);
        bundle.putInt("SDK", Build.VERSION.SDK_INT);
        bundle.putString("ACTIVITY", TAG);
        bundle.putString("FUNCNAME", funcName);
        bundle.putString("MESSAGE", message);
        mFirebaseAnalytics.logEvent(eventName, bundle);
    }

    private void sendAnalytic(String eventName,
                              String cameraMode, String cameraPermission, String locationPermission,
                              boolean haveImage,
                              boolean result, String message) {
        Log.d(TAG, String.format("sendAnalytic with full data for event: %s", eventName));
        Bundle bundle = new Bundle();
        bundle.putString("USER", mEmpId);
        bundle.putString("MODE", mMode);
        bundle.putInt("SDK", Build.VERSION.SDK_INT);
        bundle.putString("ACTIVITY", TAG);
        bundle.putString("CAMERA", cameraMode);
        bundle.putString("CAMERA_PERMISSION", cameraPermission);
        bundle.putString("LOCATION_PERMISSION", locationPermission);
        bundle.putBoolean("HAVE_IMAGE", haveImage);
        bundle.putBoolean("SUCCESSFUL_SUBMIT", result);
        bundle.putString("MESSAGE", message);
        mFirebaseAnalytics.logEvent(eventName, bundle);
    }
}
