package com.akinn.timebots;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import com.crashlytics.android.Crashlytics;
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
    Button buttonExit;
    Button buttonLogout;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Fabric.with(this, new Crashlytics());

        setContentView(R.layout.activity_anonymous_login);

        button = (Button) findViewById(R.id.buttonLogin);
        button.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                EditText editText = (EditText) findViewById(R.id.editTextEmpId);
                String empId = editText.getText().toString();
                loginNormalUser(empId);
            }
        });

        buttonExit = (Button) findViewById(R.id.buttonExit);
        buttonExit.setOnClickListener( new View.OnClickListener() {
            public void onClick(View v) {
                Log.d(TAG, "Exit clicked");
                AlertDialog.Builder alertDialog = new AlertDialog.Builder(AnonymousLoginActivity.this);
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

        buttonLogout = (Button) findViewById(R.id.buttonLogout);
        buttonLogout.setOnClickListener( new View.OnClickListener() {
            public void onClick(View v) {
                logoutConfirm();
            }
        });


    }

    private void loginNormalUser(final String userName) {
        // Get a reference to our DB
        final FirebaseDatabase database = FirebaseDatabase.getInstance();
        final DatabaseReference ref = database.getReference("Employees/" + userName);

        if (ref == null) {
            popupInvalidLogin(AnonymousLoginActivity.this);
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
                    if (isActive) {
                        String empName = (String) dataSnapshot.child("Fullname").getValue();
                        gotoNormalActivity(userName, empName);
                    } else {
                        popupInvalidLogin(AnonymousLoginActivity.this);
                    }
                } else {
                    popupInvalidLogin(AnonymousLoginActivity.this);
                }
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
                Log.d(TAG, "ref:onCancelled", databaseError.toException());
                Toast.makeText(getApplicationContext(),
                        databaseError.getDetails(), Toast.LENGTH_SHORT).show();
            }
        };
        ref.addListenerForSingleValueEvent(queryListner);
    }

    private void popupInvalidLogin(Context context) {
        AlertDialog.Builder dlg;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            dlg = new AlertDialog.Builder(context, android.R.style.Theme_Material_Dialog_Alert);
        } else {
            dlg = new AlertDialog.Builder(context);
        }
        dlg.setTitle("Sign-In Failure")
                .setMessage("Wrong user name or password")
                .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        // continue with delete
                        //finish();
                    }
                })
                .setIcon(android.R.drawable.ic_dialog_alert)
                .show();
    }

    private void gotoNormalActivity(String empId, String empName) {
        Intent intent;
        Log.d(TAG, "Go to Scan2 Activity");
        intent = new Intent(AnonymousLoginActivity.this, Scan2Activity.class);
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
        intent = new Intent(AnonymousLoginActivity.this, LoginActivity.class);
        finish();
        startActivity(intent);
    }
}
