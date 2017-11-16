package com.akinn.timebots;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.Toolbar;
import android.text.Editable;
import android.text.InputFilter;
import android.text.InputType;
import android.text.TextWatcher;
import android.util.Log;
import android.view.*;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.crashlytics.android.Crashlytics;
import com.google.firebase.analytics.FirebaseAnalytics;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import io.fabric.sdk.android.Fabric;
import java.util.Calendar;
import java.util.Date;

public class AnonymousLoginActivity extends AppCompatActivity {
    static final String TAG = AnonymousLoginActivity.class.getSimpleName();
    Button button;

    FirebaseAnalytics mFirebaseAnalytics;
    FirebaseDatabase mFirebaseDB;
    Bundle mLoginBundle;
    String mEmpID = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Fabric.with(this, new Crashlytics());

        setContentView(R.layout.activity_anonymous_login);

        mFirebaseAnalytics = FirebaseAnalytics.getInstance(this);
        mLoginBundle = new Bundle();

        mFirebaseDB = FirebaseDatabase.getInstance();
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user != null) {
            Log.d(TAG, "firebase user: " + user.getUid());
        }

        Toolbar myToolbar = (Toolbar) findViewById(R.id.anomynous_toolbar);
        setSupportActionBar(myToolbar);
        // Display icon in the toolbar
        getSupportActionBar().setDisplayShowHomeEnabled(true);
        getSupportActionBar().setLogo(R.mipmap.ic_launcher);
        getSupportActionBar().setDisplayUseLogoEnabled(true);
        // Remove default title text
        getSupportActionBar().setDisplayShowTitleEnabled(false);

        button = (Button) findViewById(R.id.buttonLogin);
        button.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                button.setEnabled(false);
                LinearLayout view = (LinearLayout) findViewById(R.id.empIdContainer) ;
                EditText editText = null;
                String empId = "";
                for (int i = 0; i < mEmpIdLength; i++) {
                    editText = (EditText) view.getChildAt(i);
                    empId += editText.getText().toString();
                }
                mEmpID = empId;
                Log.d(TAG, "EmpID = " + empId);
                loginNormalUser(empId);
                button.setEnabled(true);
            }
        });

        createEmployeeIDInput();
        Helper.sendInfo(mFirebaseDB, this, "KIOSK");
    }

    private void loginNormalUser(final String userName) {
        // Get a reference to our DB
        final FirebaseDatabase database = FirebaseDatabase.getInstance();
        final DatabaseReference ref = database.getReference("Employees/" + userName);

        if (ref == null) {
            popupInvalidLogin();
            return;
        }

        // Attach a listener to read the data at our Admin reference
        ValueEventListener queryListner = new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                Log.d(TAG, "onDataChange");
                Log.d(TAG, "Children = " + String.valueOf(dataSnapshot.getChildrenCount()));
                if (dataSnapshot.getChildrenCount() > 0) {
                    boolean isActive = (boolean) dataSnapshot.child("Active").getValue();

                    mLoginBundle.putString("USER", userName);
                    mLoginBundle.putString("MODE", "KIOSK");
                    mLoginBundle.putString("ACTIVITY", TAG);
                    mLoginBundle.putBoolean("ACTIVE_USER", isActive);
                    if (isActive) {
                        String empName = (String) dataSnapshot.child("Fullname").getValue();
                        mLoginBundle.putBoolean("SUCCESS", true);
                        mFirebaseAnalytics.logEvent("kiosk_login", mLoginBundle);
                        gotoNormalActivity(userName, empName);
                    } else {
                        mLoginBundle.putBoolean("SUCCESS", false);
                        mFirebaseAnalytics.logEvent("kiosk_login", mLoginBundle);
                        popupInvalidLogin();
                    }
                } else {
                    mLoginBundle.putBoolean("SUCCESS", false);
                    mFirebaseAnalytics.logEvent("kiosk_login", mLoginBundle);
                    popupInvalidLogin();
                }
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
                Log.d(TAG, "ref:onCancelled", databaseError.toException());
                Helper.sendGoogleAnalyticError(mFirebaseAnalytics, TAG, userName,
                    databaseError.getDetails());
            }
        };
        ref.addListenerForSingleValueEvent(queryListner);
    }


    private void gotoNormalActivity(String empId, String empName) {
        Intent intent;
        Log.d(TAG, "Go to Scan2 Activity");
        intent = new Intent(AnonymousLoginActivity.this, ScanCompatActivity.class);
        long msec = Calendar.getInstance().getTimeInMillis();
        Log.d(TAG, "Time = " + String.valueOf(msec));
        intent.putExtra("MODE", "KIOSK");
        intent.putExtra("STAMPED_TIME", msec);
        intent.putExtra("EMP_ID", empId);
        intent.putExtra("EMP_NAME", empName);
        finish();
        startActivity(intent);
    }

    private void logoutConfirm() {
        AlertDialog.Builder alertDialog = new AlertDialog.Builder(AnonymousLoginActivity.this);
        alertDialog.setPositiveButton(getString(R.string.yes), new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                clearAutoLogin();
                mLoginBundle.putString("USER", mEmpID);
                mLoginBundle.putString("MODE", "KIOSK");
                mLoginBundle.putString("ACTIVITY", TAG);
                mFirebaseAnalytics.logEvent("employee_logout", mLoginBundle);
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
        intent = new Intent(AnonymousLoginActivity.this, Login2Activity.class);
        finish();
        startActivity(intent);
    }

    int mEmpIdLength = 6;
    private void createEmployeeIDInput() {
        // Get char. length of employee id
        // Get a reference to our DB
        final FirebaseDatabase database = FirebaseDatabase.getInstance();
        final DatabaseReference ref = database.getReference("Configures/Employee ID Length");

        // Attach a listener to read the data at our Admin reference
        ValueEventListener queryListner = new ValueEventListener() {
            int numChars = 6;
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                if (dataSnapshot.exists()) {
                    Log.d(TAG, "get employee id length = " + dataSnapshot.getValue().toString());
                    mEmpIdLength = dataSnapshot.getValue(int.class);
                }
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
                Log.d(TAG, "ref:onCancelled", databaseError.toException());
                Helper.sendGoogleAnalyticError(mFirebaseAnalytics, TAG,
                    "KIOSK", databaseError.getDetails());
            }
        };
        ref.addListenerForSingleValueEvent(queryListner);

        // Create Edit Text box array //
        LinearLayout myLayout = (LinearLayout) findViewById(R.id.empIdContainer);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams( LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT);

        final EditText[] edtEmpId = new EditText[mEmpIdLength];
        for(int i=0; i<mEmpIdLength; i++)
        {
            edtEmpId[i] = new EditText(this);
            edtEmpId[i].setTextSize(30);
            edtEmpId[i].setLayoutParams(lp);
            edtEmpId[i].setId(i);
            edtEmpId[i].setInputType(InputType.TYPE_CLASS_NUMBER);
            edtEmpId[i].setFilters(new InputFilter[] {new InputFilter.LengthFilter(1)});
            edtEmpId[i].setHint("0");
            edtEmpId[i].addTextChangedListener(new TextWatcher() {
                @Override
                public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {

                }

                @Override
                public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {

                }

                @Override
                public void afterTextChanged(Editable editable) {
                    String s = editable.toString();
                    Log.d(TAG, " s = " + s);
                    if (!s.equalsIgnoreCase("")) {
                        View view = getCurrentFocus();
                        int currentId = view.getId();
                        if ((currentId + 1) >= mEmpIdLength) {
                            Button button = (Button) findViewById(R.id.buttonLogin);
                            button.requestFocus();
                        } else {
                            edtEmpId[currentId].clearFocus();
                            edtEmpId[currentId + 1].requestFocus();
                            Log.d(TAG, "move focus");
                        }
                    }
                }
            });

            edtEmpId[i].setOnKeyListener(new View.OnKeyListener() {
                @Override
                public boolean onKey(View view, int i, KeyEvent keyEvent) {
                    if (i == 67) {
                        view = getCurrentFocus();
                        int currentId = view.getId();
                        if (currentId > 0) {
                            edtEmpId[currentId].clearFocus();
                            edtEmpId[currentId - 1].requestFocus();
                            Log.d(TAG, "move focus");
                        }
                    } else {
                        Log.d(TAG, "onKey i = " + String.valueOf(i));
                        Log.d(TAG, "onKey getKeyCode = " + keyEvent.getKeyCode());
                        Log.d(TAG, "onKey getCharacters = " + keyEvent.getCharacters());
                        Log.d(TAG, "onKey view = " + String.valueOf(view.getId()));
                    }
                    return false;
                }
            });
            myLayout.addView(edtEmpId[i]);
        }
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
    public boolean onOptionsItemSelected(android.view.MenuItem item) {
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


    private void popupInvalidLogin() {
        View view = findViewById(android.R.id.content);
        Snackbar snackbar = Snackbar.make(view, R.string.invalid_userid, Snackbar.LENGTH_INDEFINITE);
        snackbar.show();
    }
}
