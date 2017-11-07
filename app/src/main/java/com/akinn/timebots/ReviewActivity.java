package com.akinn.timebots;

import android.content.Intent;
import android.graphics.Bitmap;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.TextView;

import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.io.ByteArrayOutputStream;
import java.util.Calendar;
import java.util.Date;
import java.util.Map;
import java.util.TimeZone;

public class ReviewActivity extends AppCompatActivity {

    boolean mHasCamera = false;
    boolean mHasLocation = false;

    String mMode = "";
    String mEmpId = "";
    String mEmpName = "";
    long mStampedTime = -1;
    String mComment = "";

    double mLongitude = 0.0;
    double mLatitude = 0.0;

    Bitmap mPicData;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_review);

        Intent intent = getIntent();
        mMode = intent.getStringExtra("MODE");
        mStampedTime = intent.getLongExtra("STAMPED_TIME", -1);
        mEmpId = intent.getStringExtra("EMP_ID");
        mEmpName = intent.getStringExtra("EMP_NAME");
        mHasCamera = intent.getBooleanExtra("HAS_CAMERA", false);
        mHasLocation = intent.getBooleanExtra("HAS_LOC", false);
        mComment = intent.getStringExtra("COMMENT");
        mLongitude = intent.getDoubleExtra("LONGITUDE", 0.0);
        mLatitude = intent.getDoubleExtra("LATITUDE", 0.0);

        Bitmap bmp = (Bitmap) intent.getExtras().get("PIC");
        //ByteArrayOutputStream stream = new ByteArrayOutputStream();
        //bmp.compress(Bitmap.CompressFormat.PNG, 100, stream);
        //byte[] byteArray = stream.toByteArray();

        TextView textViewEmpId = (TextView) findViewById(R.id.textViewEmpId);
        TextView textViewEmpName = (TextView) findViewById(R.id.textViewEmpName);
        TextView textViewComment = (TextView) findViewById(R.id.textViewComment);
        textViewEmpId.setText(mEmpId);
        textViewEmpName.setText(mEmpName);
        textViewComment.setText(mComment);
        CheckBox checkBoxHasPicture = (CheckBox) findViewById(R.id.checkBoxHasPicture);
        CheckBox checkBoxHasLocation = (CheckBox) findViewById(R.id.checkBoxHasLocation);
        checkBoxHasPicture.setChecked(mHasCamera);
        checkBoxHasLocation.setChecked(mHasLocation);

        Button buttonConfirm = (Button) findViewById(R.id.buttonConfirm);
        buttonConfirm.setOnClickListener(
                new View.OnClickListener(){
                    @Override
                    public void onClick(View v) {
                        writeScan(mMode, mEmpId, "", mComment,
                                mStampedTime, mLongitude, mLatitude,
                                mHasCamera, mHasLocation);
                }
            }
        );
    }

    private void writeScan(String mode, String empId, String imgURI, String comment,
                           long scannedTime, double lng, double lat,
                           boolean hasPic, boolean hasLoc) {
        DatabaseReference dbRef = FirebaseDatabase.getInstance().getReference();
        Calendar c = Calendar.getInstance();
        c.setTimeInMillis(scannedTime);
        c.setTimeZone(TimeZone.getTimeZone("Thailand/Bangkok"));
        Date date = c.getTime();
        int year = c.get(Calendar.YEAR);
        int month = c.get(Calendar.MONTH);
        int d = c.get(Calendar.DATE);
        Log.d("###", String.format("Year %d, Month %d, Date %d", year, month, d));
        ScanData scanData = new ScanData(empId, imgURI, mode, comment,
                scannedTime, lng, lat, hasPic, hasLoc, "");
        Map<String, Object> scanDataValues = scanData.toMap();
        try {
            dbRef.child("Scans").child(String.valueOf(year)).child(String.valueOf(month)).
                    child(String.valueOf(d)).child(empId).setValue(scanDataValues);
        } catch (Exception e) {
            Log.d("###", e.getMessage());
        }
    }
}
