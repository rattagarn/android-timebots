package com.akinn.timebots;

import android.Manifest;
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
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.hardware.Camera;
import android.icu.text.LocaleDisplayNames;
import android.location.Location;
import android.net.Uri;
import android.os.Build;
import android.provider.MediaStore;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.Display;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.crashlytics.android.Crashlytics;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import com.google.android.gms.location.LocationRequest;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageMetadata;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;

import org.w3c.dom.Text;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.TimeZone;

import io.fabric.sdk.android.Fabric;

public class Scan2Activity extends AppCompatActivity implements
        GoogleApiClient.ConnectionCallbacks,
        GoogleApiClient.OnConnectionFailedListener,
        LocationListener {

    final static String TAG = Scan2Activity.class.getSimpleName();
    String mFileName = "";

    Preview mPreview;
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

    String mFolderPath = "";

    String mImgURI = "";

    Button buttonCheckIn;
    Button buttonExit;
    Button buttonLogout;
    TextView textViewLoc;
    ProgressBar pbUpload;
    ImageButton buttonCapture;

    Bitmap mBmp;

    LocationRequest mLocationRequest;
    private GoogleApiClient mGoogleApiClient;

    FirebaseDatabase mFireDB = FirebaseDatabase.getInstance();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Fabric.with(this, new Crashlytics());
        Log.d(TAG, "onCreate");

        setContentView(R.layout.activity_scan2);

        Intent intent = getIntent();
        mMode = intent.getStringExtra("MODE");
        mEmpId = intent.getStringExtra("EMP_ID");
        mEmpName = intent.getStringExtra("EMP_NAME");
        mStampedTime = Calendar.getInstance().getTimeInMillis();

        buttonCheckIn = (Button) findViewById(R.id.buttonCheckIn);
        buttonCheckIn.requestFocus();
        buttonExit = (Button) findViewById(R.id.buttonExit);
        buttonLogout = (Button) findViewById(R.id.buttonLogout);
        buttonCapture = (ImageButton) findViewById(R.id.buttonCapture);

        textViewLoc = (TextView) findViewById(R.id.textViewLoc);
        pbUpload = (ProgressBar) findViewById(R.id.progressUpload);

        TextView textViewEmpId = (TextView) findViewById(R.id.textViewEmpId);
        textViewEmpId.setText(mEmpId);
        TextView textViewEmpName = (TextView) findViewById(R.id.textViewEmpName);
        textViewEmpName.setText(mEmpName);

        queryLatestScan(mEmpId);

        if (checkCameraHardware(getApplicationContext())) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                buttonCapture.setVisibility(View.VISIBLE);
                buttonCapture.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        dispatchTakePictureIntent();
                    }
                });
            } else {
                FrameLayout previewPane = (FrameLayout) findViewById(R.id.cameraPreview);
                mPreview = new Preview(getApplicationContext(), null,
                        null, previewPane, mEmpName);
            }
        } else {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                buttonCapture.setVisibility(View.VISIBLE);
                buttonCapture.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        dispatchTakePictureIntent();
                    }
                });
            } else {
                FrameLayout previewPane = (FrameLayout) findViewById(R.id.cameraPreview);
                mPreview = new Preview(getApplicationContext(), null,
                        null, previewPane, mEmpName);
                Toast.makeText(getApplicationContext(), "No front camera!!!", Toast.LENGTH_LONG)
                        .show();
                buttonCapture.setVisibility(View.VISIBLE);
                buttonCapture.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        dispatchTakePictureIntent();
                    }
                });
            }
        }

        buttonCheckIn.setOnClickListener( new View.OnClickListener() {
            public void onClick(View v) {
                mStampedTime = Calendar.getInstance().getTimeInMillis();
                TextView textView = (TextView) findViewById(R.id.editTextComment);
                mComment = textView.getText().toString().trim();
                mHasCamera = false;
                if (mUseIntentCamera) {
                    if (mBmp != null) {
                        saveImage(mBmp);
                    } else {
                        writeScan(mMode, mEmpId, "", mComment,
                                mStampedTime, mLongitude, mLatitude,
                                mHasCamera, mHasLocation, mLocale);
                        if (mMode == "KIOSK") {
                            gotoAnonymousLogin();
                        } else {
                            reloadCurrentActivity();
                        }
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
                                false, mHasLocation, mLocale);
                        if (mMode == "KIOSK") {
                            gotoAnonymousLogin();
                        } else {
                            reloadCurrentActivity();
                        }
                    }
                }
            }
        });

        buttonExit.setOnClickListener( new View.OnClickListener() {
            public void onClick(View v) {
                Log.d(TAG, "Exit clicked");
                AlertDialog.Builder alertDialog = new AlertDialog.Builder(Scan2Activity.this);
                alertDialog.setPositiveButton(getString(R.string.yes), new DialogInterface.OnClickListener() {

                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        //FirebaseAuth mAuth = FirebaseAuth.getInstance();
                        //mAuth.signOut();
                        finish();
                    }
                });
                alertDialog.setNegativeButton(getString(R.string.no), null);
                alertDialog.setMessage(getString(R.string.exit_message));
                alertDialog.setTitle(getString(R.string.exit_title));
                alertDialog.show();
            }
        });

        buttonLogout.setOnClickListener( new View.OnClickListener() {
            public void onClick(View v) {
                logoutConfirm();
            }
        });

        mLocale = TimeZone.getDefault().getID();
        Log.d(TAG, "Time Zone = %s" + mLocale);
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
                    textViewLoc.setText(String.format("(%f,%f)", mLatitude, mLongitude));
                }
            };
        }
        registerReceiver(mBroadcastReceiver,
                new IntentFilter(getResources().getString(R.string.location_broadcast_name)));
    }

    @Override
    public void onBackPressed() {
        Log.d(TAG, "onBackPressed");
        AlertDialog.Builder alertDialog = new AlertDialog.Builder(Scan2Activity.this);
        alertDialog.setPositiveButton(getString(R.string.yes), new DialogInterface.OnClickListener() {

            @Override
            public void onClick(DialogInterface dialog, int which) {
                FirebaseAuth mAuth = FirebaseAuth.getInstance();
                mAuth.signOut();
                Scan2Activity.super.onBackPressed();
            }
        });
        alertDialog.setNegativeButton(getString(R.string.no), null);
        alertDialog.setMessage(getString(R.string.exit_message));
        alertDialog.setTitle(getString(R.string.exit_title));
        alertDialog.show();
    }

    private void queryLatestScan(final String empId) {
        final FirebaseDatabase database = FirebaseDatabase.getInstance();
        final DatabaseReference ref = database.getReference("Scans/LastScan/" + empId);

        ValueEventListener queryListner = new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                Log.d(TAG, "queryLatestScan: onDataChange");
                TextView textViewTime = (TextView) findViewById(R.id.textViewLastCheckIn);
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
                }
                else {
                    textViewTime.setText("");
                }
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
                Log.d(TAG, "queryLatestScan: " + databaseError.getMessage() + "\n" + databaseError.getDetails());
            }
        };
        ref.addListenerForSingleValueEvent(queryListner);
    }

    /*/////////////////*/
    /* Camera Feature */
    Camera mCamera = null;
    private boolean checkCameraHardware(Context context) {
        if (context.getPackageManager().hasSystemFeature(PackageManager.FEATURE_CAMERA_FRONT)){
            // this device has a camera
            return true;
        } else {
            // no camera on this device
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

    private boolean safeCameraOpen(int id) {
        boolean qOpened = false;
        Log.d(TAG, "found camera id = " + String.valueOf(id));
        try {
            releaseCameraAndStopPreview();
            mCamera = Camera.open(id);
            qOpened = (mCamera != null);
        } catch (Exception e) {
            Log.e(getString(R.string.app_name), "failed to open Camera");
            e.printStackTrace();
        }

        return qOpened;
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
            //ContextWrapper wrapper = new ContextWrapper(getApplicationContext());

            //File picFileDir = wrapper.getDir("Images",MODE_PRIVATE);

            //Log.d(TAG, "--Directory = " + picFileDir.getAbsolutePath());

            FrameLayout frame = (FrameLayout) findViewById(R.id.cameraPreview);
            frame.setVisibility(View.INVISIBLE);
            ImageView v = (ImageView) findViewById(R.id.imageViewLive);
            Log.d(TAG, String.format("Image VIew (%d, %d", v.getWidth(), v.getHeight()));

            Bitmap bmp = convertImage(data, 150, 150);
            //Bitmap bmp = convertImage(data, v.getWidth(), v.getHeight());
            String msg = mEmpName;
            bmp = putWaterMark(bmp, msg, 5, 110);
            msg = getDateString(mStampedTime);
            bmp = putWaterMark(bmp, msg, 5, 125);
            msg = String.format("(%f, %f)", mLatitude, mLongitude);
            bmp = putWaterMark(bmp, msg, 5, 140);
            //Bitmap bmp = BitmapFactory.decodeByteArray(data, 0, data.length);
            //String path = MediaStore.Images.Media.insertImage(getContentResolver(),
            //        bmp, "name" , "description");
            //Log.e("tag", "path: " + path); // prints something like "path: content://media/external/images/media/819"
            Log.d(TAG, String.format("Image View (%d, %d)", v.getWidth(), v.getHeight()));
            v.setImageBitmap(bmp);
            //scaleImage(v);
            v.setVisibility(View.VISIBLE);
            mHasCamera = true;

            File dir = new File(mFolderPath);
            if (!dir.exists()) {
                dir.mkdirs();
            }
            mFileName = String.valueOf(System.currentTimeMillis());
            String filePath = String.format("%s/%s.jpg", mFolderPath, mFileName);
            // /data/data/com.akinn.leverage/app_Images
            Log.d(TAG, "--Directory = " + filePath);
            try {
                bmp.compress(Bitmap.CompressFormat.JPEG, 80,
                        new FileOutputStream(new File(filePath)));
                unloadImageHandler(v, mFolderPath, mFileName, mEmpId, mStampedTime, mLocale);
            } catch (FileNotFoundException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        }
    };

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

    private void scaleImage(ImageView view) throws NoSuchElementException {
        // Get bitmap from the the ImageView.
        Bitmap bitmap = null;

        try {
            Drawable drawing = view.getDrawable();
            bitmap = ((BitmapDrawable) drawing).getBitmap();
        } catch (NullPointerException e) {
            throw new NoSuchElementException("No drawable on given view");
        } catch (ClassCastException e) {
            // Check bitmap is Ion drawable
            throw new ClassCastException(e.getMessage());
        }

        // Get current dimensions AND the desired bounding box
        int width = 0;

        try {
            width = bitmap.getWidth();
        } catch (NullPointerException e) {
            throw new NoSuchElementException("Can't find bitmap on given view/drawable");
        }

        int height = bitmap.getHeight();
        int bounding = dpToPx(200);
        Log.i("Test", "original width = " + Integer.toString(width));
        Log.i("Test", "original height = " + Integer.toString(height));
        Log.i("Test", "bounding = " + Integer.toString(bounding));

        // Determine how much to scale: the dimension requiring less scaling is
        // closer to the its side. This way the image always stays inside your
        // bounding box AND either x/y axis touches it.
        float xScale = ((float) bounding) / width;
        float yScale = ((float) bounding) / height;
        float scale = (xScale <= yScale) ? xScale : yScale;
        Log.i("Test", "xScale = " + Float.toString(xScale));
        Log.i("Test", "yScale = " + Float.toString(yScale));
        Log.i("Test", "scale = " + Float.toString(scale));

        // Create a matrix for the scaling and add the scaling data
        Matrix matrix = new Matrix();
        matrix.postScale(scale, scale);

        // Create a new bitmap and convert it to a format understood by the ImageView
        Bitmap scaledBitmap = Bitmap.createBitmap(bitmap, 0, 0, width, height, matrix, true);
        width = scaledBitmap.getWidth(); // re-use
        height = scaledBitmap.getHeight(); // re-use
        BitmapDrawable result = new BitmapDrawable(scaledBitmap);
        Log.i("Test", "scaled width = " + Integer.toString(width));
        Log.i("Test", "scaled height = " + Integer.toString(height));

        // Apply the scaled bitmap
        view.setImageDrawable(result);

        // Now change ImageView's dimensions to match the scaled image
        LinearLayout.LayoutParams params = (LinearLayout.LayoutParams) view.getLayoutParams();
        params.width = width;
        params.height = height;
        view.setLayoutParams(params);

        Log.i("Test", "done");
    }

    private int dpToPx(int dp) {
        float density = getApplicationContext().getResources().getDisplayMetrics().density;
        return Math.round((float)dp * density);
    }


    // For Android M ++++ //
    static final int REQUEST_IMAGE_CAPTURE = 10;
    static final int CAMERA_REQUEST_CODE = 11;
    @TargetApi(23)
    private void dispatchTakePictureIntent() {
        mUseIntentCamera = true;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (checkSelfPermission(Manifest.permission.CAMERA)
                    != PackageManager.PERMISSION_GRANTED) {
                Log.d(TAG, "SDK23: Camera Permission not Granted");
                requestPermissions(new String[]{Manifest.permission.CAMERA},
                        CAMERA_REQUEST_CODE);
            } else {
                Log.d(TAG, "SDK23: Camera Permission Granted");
                Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
                if (takePictureIntent.resolveActivity(getPackageManager()) != null) {
                    startActivityForResult(takePictureIntent, REQUEST_IMAGE_CAPTURE);
                }
            }
        } else {
            if (ContextCompat.checkSelfPermission(getApplicationContext(), Manifest.permission.CAMERA)
                    != PackageManager.PERMISSION_GRANTED) {
                Log.d(TAG, "SDK23: Camera Permission not Granted");
                requestPermissions(new String[]{Manifest.permission.CAMERA},
                        CAMERA_REQUEST_CODE);
            } else {
                Log.d(TAG, "SDK23: Camera Permission Granted");
                Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
                if (takePictureIntent.resolveActivity(getPackageManager()) != null) {
                    startActivityForResult(takePictureIntent, REQUEST_IMAGE_CAPTURE);
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

            Bitmap bmp = imageBitmap.copy(Bitmap.Config.ARGB_8888, true);
            String msg = mEmpName;
            bmp = putWaterMark(bmp, msg, 5, 10);
            msg = getDateString(mStampedTime);
            bmp = putWaterMark(bmp, msg, 5, 25);
            msg = String.format("(%f, %f)", mLatitude, mLongitude);
            bmp = putWaterMark(bmp, msg, 5, 40);

            FrameLayout frame = (FrameLayout) findViewById(R.id.cameraPreview);
            frame.setVisibility(View.INVISIBLE);
            ImageView v = (ImageView) findViewById(R.id.imageViewLive);
            v.setImageBitmap(bmp);
            v.setVisibility(View.VISIBLE);
        }
    }

    private Bitmap displayImage(byte[] data) {
        FrameLayout frame = (FrameLayout) findViewById(R.id.cameraPreview);
        frame.setVisibility(View.INVISIBLE);
        ImageView v = (ImageView) findViewById(R.id.imageViewLive);

        Bitmap bmp = convertImage2(data, 150, 150);
        String msg = mEmpName;
        bmp = putWaterMark(bmp, msg, 5, 110);
        msg = getDateString(mStampedTime);
        bmp = putWaterMark(bmp, msg, 5, 125);
        msg = String.format("(%f, %f)", mLatitude, mLongitude);
        bmp = putWaterMark(bmp, msg, 5, 140);
        Log.d(TAG, String.format("Image View (%d, %d)", v.getWidth(), v.getHeight()));
        v.setImageBitmap(bmp);
        v.setVisibility(View.VISIBLE);
        mHasCamera = true;
        return bmp;
    }

    private void saveImage(Bitmap bmp) {
        ImageView v = (ImageView) findViewById(R.id.imageViewLive);
        File dir = new File(mFolderPath);
        if (!dir.exists()) {
            dir.mkdirs();
        }
        mFileName = String.valueOf(System.currentTimeMillis());
        String filePath = String.format("%s/%s.jpg", mFolderPath, mFileName);
        // /data/data/com.akinn.leverage/app_Images
        Log.d(TAG, "--Directory = " + filePath);
        try {
            bmp.compress(Bitmap.CompressFormat.JPEG, 80,
                    new FileOutputStream(new File(filePath)));
            unloadImageHandler(v, mFolderPath, mFileName, mEmpId, mStampedTime, mLocale);
        } catch (FileNotFoundException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }

    private Bitmap convertImage2(byte[] data, int picFrameWidth, int picFrameHeight) {

        Log.d(TAG, String.format("Picture Frame: (%d, %d)", picFrameWidth, picFrameHeight));

        Display display = ((WindowManager)getApplicationContext()
                .getSystemService(WINDOW_SERVICE)).getDefaultDisplay();
        int dispOrientation = display.getRotation();

        int camOrientation = 0; //mPreview.getCameraOrientation();
        Log.d(TAG, String.format("Display Orientation = %d, Camera Orientation = %d", dispOrientation, camOrientation));

        int diffDegress = Math.abs(dispOrientation - camOrientation);
        Log.d(TAG, "bitmap data = " + String.valueOf(data.length));
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
    /*End of Camera Feature */


    private void gotoAnonymousLogin() {
        Log.d(TAG, "Go to Anonymous Login");
        Intent intent = new Intent(Scan2Activity.this, AnonymousLoginActivity.class);
        finish();
        startActivity(intent);
    }

    private void reloadCurrentActivity() {
        Intent intent;
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

    private String getDateString(long millisecTime) {
        String zoneId = TimeZone.getDefault().getID();
        SimpleDateFormat sdf =
                new SimpleDateFormat("dd-MMM-yyyy hh:mm",
                        new Locale("th", "TH"));
        sdf.setTimeZone(TimeZone.getTimeZone(zoneId));
        return sdf.format(new Date(millisecTime));
    }

    private void clearAutoLogin() {
        Log.d(TAG, "clearAutoLogin");
        SharedPreferences sp = this.getSharedPreferences("LOGIN", MODE_PRIVATE);
        SharedPreferences.Editor ed = sp.edit();
        ed.clear();
        ed.commit();
    }

    private void logoutConfirm() {
        AlertDialog.Builder alertDialog = new AlertDialog.Builder(Scan2Activity.this);
        alertDialog.setPositiveButton(getString(R.string.yes), new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                clearAutoLogin();
                gotoLoginActivity();
            }
        });
        alertDialog.setNegativeButton(getString(R.string.no), null);
        alertDialog.setMessage(getString(R.string.logout_message));
        alertDialog.setTitle(getString(R.string.logout_title));
        alertDialog.show();
    }

    private void gotoLoginActivity() {
        Intent intent;
        Log.d(TAG, "Go to Login Activity");
        intent = new Intent(Scan2Activity.this, LoginActivity.class);
        finish();
        startActivity(intent);
    }

    private void writeScan(String mode, String empId, String imgURI, String comment,
                           long scannedTime, double lng, double lat,
                           boolean hasPic, boolean hasLoc, String timeZone) {

        Calendar c = Calendar.getInstance();
        c.setTimeInMillis(scannedTime);
        c.setTimeZone(TimeZone.getTimeZone(timeZone));
        Date date = c.getTime();
        int year = c.get(Calendar.YEAR);
        int month = c.get(Calendar.MONTH) + 1;
        int d = c.get(Calendar.DATE);

        final String refInPath = new String(String.format("Scans/%d/%d/%d/In/%s/", year, month, d, empId));
        Log.d(TAG, refInPath.toString());
        final String refOutPath = new String(String.format("Scans/%d/%d/%d/Out/%s", year, month, d, empId));

        final ScanData scanData = new ScanData(empId, imgURI, mode, comment,
                scannedTime, lng, lat, hasPic, hasLoc, timeZone);
        try {
            final DatabaseReference dbRef = mFireDB.getReference(refInPath.toString());
            ValueEventListener queryListner = new ValueEventListener() {
                @Override
                public void onDataChange(DataSnapshot dataSnapshot) {
                    Log.d(TAG, "writeScan: onDataChange[" + refInPath.toString() + "]");
                    Log.d(TAG, "writeScan: Children[" + refInPath.toString() + "] = "
                            + String.valueOf(dataSnapshot.getChildrenCount()));

                    if (dataSnapshot.getChildrenCount() > 0) {
                        // Scan In already had record //
                        // add Scan Out //
                        DatabaseReference dbOutRef = mFireDB.getReference(refOutPath.toString());
                        Map<String, Object> scanDataValues = scanData.toMap();
                        dbOutRef.setValue(scanDataValues);
                        Log.d(TAG, "Done update scan out data");
                    } else {
                        // add Scan In //
                        DatabaseReference dbInRef = mFireDB.getReference(refInPath.toString());
                        Map<String, Object> scanDataValues = scanData.toMap();
                        dbInRef.setValue(scanDataValues);
                        Log.d(TAG, "Done update scan in data");
                    }
                }

                @Override
                public void onCancelled(DatabaseError databaseError) {
                    Log.d(TAG, "writeScan: onCancelled", databaseError.toException());
                    Toast.makeText(getApplicationContext(),
                            databaseError.getDetails(), Toast.LENGTH_SHORT).show();
                }
            };
            dbRef.addListenerForSingleValueEvent(queryListner);

            String lastScanPath = "Scans/LastScan/" + empId;
            DatabaseReference dbLastScanRef = mFireDB.getReference(lastScanPath);
            HashMap<String, Object> lastScanValue = new HashMap<>();
            lastScanValue.put("Scanned", scannedTime);
            lastScanValue.put("TimeZone", timeZone);
            dbLastScanRef.setValue(lastScanValue);
            Log.d(TAG, "Done update last scan data");

        } catch (Exception e) {
            Log.d("###", e.getMessage());
        }
    }

    /*//////////*/
    /* Location */
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
                startLocationService();
            }
        } else {
            startLocationService();
        }
    }

    private void requestLocationPermission() {
        if (ActivityCompat.shouldShowRequestPermissionRationale(Scan2Activity.this,
                android.Manifest.permission.ACCESS_FINE_LOCATION)) {
            ActivityCompat.requestPermissions(Scan2Activity.this,
                    new String[]{android.Manifest.permission.ACCESS_FINE_LOCATION},
                    ACCESS_FINE_LOCATION_INTENT_ID);

        } else {
            ActivityCompat.requestPermissions(Scan2Activity.this,
                    new String[]{android.Manifest.permission.ACCESS_FINE_LOCATION},
                    ACCESS_FINE_LOCATION_INTENT_ID);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == ACCESS_FINE_LOCATION_INTENT_ID) {
            if(grantResults.length == 1
                    && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                startLocationService();
            } else {
                // Permission was denied or request was cancelled
                mHasLocation = false;
            }
        } else if (requestCode == CAMERA_REQUEST_CODE) {
            Log.d(TAG, "SDK23: Camera Permission Grant?: " + grantResults[0]);
            if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // Now user should be able to use camera
                dispatchTakePictureIntent();
            }
            else {
                // Your app will not have this permission. Turn off all functions
                // that require this permission or it will force close like your
                // original question
            }
        }
    }

    private void startLocationService() {
        Log.d(TAG, "startLocationService");
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
                handleNewLocation(location);
            }
        } catch (SecurityException e) {
            Log.d(TAG, e.getMessage());
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
                e.printStackTrace();
            }
        } else {
            /*
             * If no resolution is available, display a dialog to the
             * user with the error.
             */
            Log.i(TAG, "Location services connection failed with code " + connectionResult.getErrorCode());
        }
    }

    @Override
    public void onLocationChanged(Location location) {
        handleNewLocation(location);
    }

    private void handleNewLocation(Location location) {
        Log.d(TAG, location.toString());

        mLatitude = location.getLatitude();
        mLongitude = location.getLongitude();

        Log.d(TAG, String.format("handleNewLocation: (%f, %f)", mLatitude, mLongitude));
        textViewLoc.setText(String.format("(%f, %f)", mLatitude, mLongitude));
    }
    /* End of Location */

    private void unloadImageHandler(final ImageView v, final String localFilePath, final String fileName,
                                    final String empId, long scannedTime, String timeZone) {

        Calendar c = Calendar.getInstance();
        c.setTimeInMillis(scannedTime);
        c.setTimeZone(TimeZone.getTimeZone(timeZone));
        Date date = c.getTime();
        int year = c.get(Calendar.YEAR);
        int month = c.get(Calendar.MONTH) + 1;
        int d = c.get(Calendar.DATE);
        final String dateObj = new String(String.valueOf(d));
        final String refInPath = new String(String.format("Scans/%d/%d/%d/In/%s", year, month, d, empId));
        try {
            final DatabaseReference dbRef = mFireDB.getReference(refInPath.toString());
            ValueEventListener queryListner = new ValueEventListener() {
                @Override
                public void onDataChange(DataSnapshot dataSnapshot) {
                    Log.d(TAG, "unloadImageHandler: onDataChange[" + refInPath.toString() + "]");
                    Log.d(TAG, "unloadImageHandler: Children[" + refInPath.toString() + "] = "
                            + String.valueOf(dataSnapshot.getChildrenCount()));
                    if (dataSnapshot.getChildrenCount() > 0) {
                        // Scan In already had record //
                        // add Scan Out //
                        doImageUpload(v, localFilePath, fileName, empId, "Out/" + dateObj.toString());
                    } else {
                        // add Scan In //
                        doImageUpload(v, localFilePath, fileName, empId, "In/" + dateObj.toString());
                    }
                }

                @Override
                public void onCancelled(DatabaseError databaseError) {
                    Log.d(TAG, "unloadImageHandler: onCancelled", databaseError.toException());
                    Toast.makeText(getApplicationContext(),
                            databaseError.getDetails(), Toast.LENGTH_SHORT).show();
                }
            };
            dbRef.addListenerForSingleValueEvent(queryListner);
        } catch (Exception e) {
            Log.d("###", e.getMessage());
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
        buttonCheckIn.setEnabled(false);

        UploadTask uploadTask = storageRef.child(empId).putFile(file, metadata);
        uploadTask.addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception e) {
                Log.d(TAG, "Fail to upload picture " + e.getMessage());
                Toast.makeText(getApplicationContext(), e.getMessage(), Toast.LENGTH_LONG);
                buttonCheckIn.setEnabled(true);
                pbUpload.setVisibility(View.GONE);

                writeScan(mMode, mEmpId, "", mComment,
                        mStampedTime, mLongitude, mLatitude,
                        mHasCamera, mHasLocation, mLocale);

                Toast.makeText(getApplicationContext(), getString(R.string.succes_scanin),
                        Toast.LENGTH_LONG).show();

                if (mMode.equals("KIOSK")) {
                    gotoAnonymousLogin();
                } else {
                    reloadCurrentActivity();
                }
            }
        }).addOnSuccessListener(Scan2Activity.this, new OnSuccessListener<UploadTask.TaskSnapshot>() {
            @Override
            public void onSuccess(UploadTask.TaskSnapshot taskSnapshot) {
                buttonCheckIn.setEnabled(true);
                pbUpload.setVisibility(View.GONE);
                Uri downloadUri = taskSnapshot.getDownloadUrl();
                Log.d(TAG, "Success to upload: " + downloadUri.toString());

                mImgURI = downloadUri.toString();
                writeScan(mMode, mEmpId, mImgURI, mComment,
                        mStampedTime, mLongitude, mLatitude,
                        mHasCamera, mHasLocation, mLocale);

                Toast.makeText(getApplicationContext(), getString(R.string.succes_scanin),
                        Toast.LENGTH_LONG).show();

                deleteImageFile(localFilePath, fileName + ".jpg");

                Log.d(TAG, "mMode = " + mMode);
                if (mMode.equals("KIOSK")) {
                    gotoAnonymousLogin();
                } else {
                    reloadCurrentActivity();
                }
            }
        });
    }

    private void deleteImageFile(String dirPath, String fileName) {
        Log.d(TAG, "Delete image file: " + fileName);
        File dir = new File(dirPath);
        File[] files = dir.listFiles();
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
    }
}
