package com.akinn.timebots;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.support.annotation.NonNull;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.crashlytics.android.Crashlytics;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.analytics.FirebaseAnalytics;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.Calendar;

import io.fabric.sdk.android.Fabric;

public class Login2Activity extends AppCompatActivity {
    static final String TAG = LoginActivity.class.getSimpleName();
    Button button;

    FirebaseAuth mAuth;
    FirebaseAuth.AuthStateListener mAuthListener;
    FirebaseAnalytics mFirebaseAnalytics;
    FirebaseDatabase mFirebaseDB;

    Bundle mLoginBundle;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Fabric.with(this, new Crashlytics());
        setContentView(R.layout.activity_login2);

        mFirebaseAnalytics = FirebaseAnalytics.getInstance(this);
        mLoginBundle = new Bundle();

        mAuth = FirebaseAuth.getInstance();
        mAuthListener = new FirebaseAuth.AuthStateListener() {
            @Override
            public void onAuthStateChanged(@NonNull FirebaseAuth firebaseAuth) {
                FirebaseUser user = firebaseAuth.getCurrentUser();
                if (user != null) {
                    // User is signed in
                    Log.d(TAG, "onAuthStateChanged:signed_in:" + user.getUid());
                    doAutoLogin();
                } else {
                    // User is signed out
                    Log.d(TAG, "onAuthStateChanged:signed_out");
                }
            }
        };

        mFirebaseDB = FirebaseDatabase.getInstance();

        TextView textViewVersion = (TextView) findViewById(R.id.txt_version);
        textViewVersion.setText(getVersionLabel());
        button = (Button) findViewById(R.id.btn_login);
        button.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                Helper.logLoginActivity(mFirebaseAnalytics,
                        String.valueOf(R.id.btn_login),
                        button.getText().toString());
                button.setEnabled(false);
                EditText userText = (EditText) findViewById(R.id.input_user);
                EditText pwdText = (EditText) findViewById(R.id.input_password);
                String userName = userText.getText().toString().trim();
                String password = pwdText.getText().toString().trim();
                loginAdminUser(userName, password, false);
                button.setEnabled(true);
            }
        });

        //Helper.sendSimpleGoogleAnalyticInfo(mFirebaseAnalytics, this);
        Helper.sendInfo(mFirebaseDB, this, "unknown");
        if (this.isOnline()) {
            Log.d(TAG, "Can connect to internet");
            signIn();
        } else {
            Log.d(TAG, "Not online");
            showMessage("No internet connection.\nPlease turn on internet connection.");
        }
    }

    @Override
    public void onStart() {
        super.onStart();
        Log.d(TAG, "onStart");
        mAuth.addAuthStateListener(mAuthListener);
        //doAutoLogin();
    }

    @Override
    public void onStop() {
        super.onStop();
        Log.d(TAG, "onStop");
        if (mAuthListener != null) {
            mAuth.removeAuthStateListener(mAuthListener);
        }
    }

    ///////////////////////////////////////////
    // Firebase Database                     //

    private void loginAdminUser(final String userName, final String password, final boolean auto) {
        if (userName == "") {
            Log.d(TAG, "loginAdminUser: username is empty");
            popupInvalidLogin();
            return;
        }

        // Get a reference to our DB
        DatabaseReference adminRef = FirebaseDatabase.getInstance().getReference("Admin/" + userName);
        Log.d(TAG, String.format("loginAdminUser: userName = %s, password = %s", userName, password));
        if (adminRef == null) { // if cannot find user at admin login, it may be normal login //
            Log.d(TAG, "loginAdminUser: Cannot find this Admin's username = " + userName);
            loginNormalUser(userName, password, auto);
            return;
        }

        // Attach a listener to read the data at our Admin reference
        ValueEventListener adminListener = new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                boolean foundData = false;
                Log.d(TAG, "loginAdminUser: onDataChange called");
                if ((dataSnapshot != null) && dataSnapshot.exists()) {
                    Log.d(TAG, "loginAdminUser: getChildrenCount = " + String.valueOf(dataSnapshot.getChildrenCount()));
                    String pwd = (String) dataSnapshot.getValue();
                    Log.d(TAG, "loginAdminUser: password = " + pwd);
                    if (pwd.equals(password)) {
                        Log.d(TAG, "loginAdminUser: password matched");
                        logUser("Admin", userName);
                        mLoginBundle.putString("USER", userName);
                        mLoginBundle.putString("MODE", "KIOSK");
                        mLoginBundle.putBoolean("SUCCESS", true);
                        mLoginBundle.putString("ACTIVITY", TAG);
                        CheckBox checkBoxRemember = (CheckBox) findViewById(R.id.chk_login);
                        if ((!auto) && checkBoxRemember.isChecked()) {
                            Log.d(TAG, "Save Auto Login");
                            saveAutoLogin(userName, password);
                        }
                        gotoAnonymousLogin();
                    } else {
                        loginNormalUser(userName, password, auto);
                    }
                } else {
                    loginNormalUser(userName, password, auto);
                }
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
                Log.d(TAG, "loginAdminUser:onCancelled", databaseError.toException());
                Helper.sendGoogleAnalyticError(mFirebaseAnalytics, TAG, "",
                        databaseError.getDetails());
                showMessage("Found error during Admin Login: " + databaseError.getMessage());
            }
        };

        Log.d(TAG, "loginAdminUser: attached ValueEventListener");
        adminRef.addListenerForSingleValueEvent(adminListener);
    }

    private void logUser(String empName, String empId) {
        // TODO: Use the current user's information
        // You can call any combination of these three methods
        Crashlytics.setUserIdentifier(empId);
        Crashlytics.setUserName(empName);
    }

    private void loginNormalUser(final String userName, final String password, final boolean auto) {
        if (userName == "") {
            Log.d(TAG, "loginNormalUser: username is empty");
            popupInvalidLogin();
            return;
        }

        // Get a reference to our DB
        final FirebaseDatabase database = FirebaseDatabase.getInstance();
        final DatabaseReference ref = database.getReference("Employees/" + userName);

        if (ref == null) {
            Log.d(TAG, "loginNormalUser: Cannot find this Normal's username = " + userName);
            popupInvalidLogin();
            return;
        }

        Log.d(TAG, String.format("loginNormalUser: username: %s, password : %s", userName, password));
        // Attach a listener to read the data at our Admin reference
        final ValueEventListener queryListner = new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                Log.d(TAG, "loginNormalUser: onDataChange");
                Log.d(TAG, "loginNormalUser: getChildrenCount = " + String.valueOf(dataSnapshot.getChildrenCount()));
                if (dataSnapshot.getChildrenCount() > 0) {
                    boolean isActive = (boolean) dataSnapshot.child("Active").getValue();
                    String dataPwd = (String) dataSnapshot.child("Password").getValue();
                    if (isActive && dataPwd.equals(password)) {
                        String empName = (String) dataSnapshot.child("Fullname").getValue();
                        logUser(userName, empName);
                        mLoginBundle.putString("USER", userName);
                        mLoginBundle.putString("MODE", "NORMAL");
                        mLoginBundle.putBoolean("SUCCESS", true);
                        mLoginBundle.putString("ACTIVITY", TAG);
                        CheckBox checkBoxRemember = (CheckBox) findViewById(R.id.chk_login);
                        if((!auto) && checkBoxRemember.isChecked()) {
                            saveAutoLogin(userName, password);
                        }
                        mFirebaseAnalytics.logEvent("employee_login", mLoginBundle);
                        gotoNormalActivity(userName, empName);
                    } else {
                        mLoginBundle.putString("USER", userName);
                        mLoginBundle.putString("MODE", "NORMAL");
                        mLoginBundle.putBoolean("SUCCESS", false);
                        mLoginBundle.putString("ACTIVITY", TAG);
                        mFirebaseAnalytics.logEvent("employee_login", mLoginBundle);
                        popupInvalidLogin();
                    }
                } else {
                    mLoginBundle.putString("USER", userName);
                    mLoginBundle.putString("MODE", "NORMAL");
                    mLoginBundle.putBoolean("SUCCESS", false);
                    mFirebaseAnalytics.logEvent("employee_login", mLoginBundle);
                    popupInvalidLogin();
                }
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
                Log.d(TAG, "ref:onCancelled", databaseError.toException());
                Helper.sendGoogleAnalyticError(mFirebaseAnalytics, TAG, "",
                        databaseError.getDetails());
                showMessage("Found error during Admin Login: " + databaseError.getMessage());
            }
        };
        ref.addListenerForSingleValueEvent(queryListner);
    }

    private void signIn() {
        String email = "rattagarn@gmail.com";
        String password = "timebot4114";
        mAuth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener(this, new OnCompleteListener<AuthResult>() {
                    @Override
                    public void onComplete(@NonNull Task<AuthResult> task) {
                        Log.d(TAG, "signInWithEmail:onComplete:" + task.isSuccessful());

                        // If sign in fails, display a message to the user. If sign in succeeds
                        // the auth state listener will be notified and logic to handle the
                        // signed in user can be handled in the listener.
                        if (!task.isSuccessful()) {
                            Log.w(TAG, "signInWithEmail:failed", task.getException());
                            popupCannotSignIn();
                            //Helper.sendGoogleAnalyticError(mFirebaseAnalytics, TAG, "",
                            //        "Cannot sign in to Firebase");
                        }

                        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
                        if (user != null) {
                            // Name, email address, and profile photo Url
                            String name = user.getDisplayName();
                            String email = user.getEmail();
                            String uid = user.getUid();

                            // The user's ID, unique to the Firebase project. Do NOT use this value to
                            // authenticate with your backend server, if you have one. Use
                            // FirebaseUser.getToken() instead.
                            String msg = "name: " + name + "\n";
                            msg += "email: " + email + "\n";
                            msg += "uid: " + uid + "\n";
                            Log.d(TAG, msg);
                        }
                    }
                });
    }

    ////////////////////////////////////////////////
    // Auto Login and Save Parameters             //
    private void doAutoLogin() {
        try {
            SharedPreferences sp = this.getSharedPreferences("LOGIN", MODE_PRIVATE);
            String user = sp.getString("user", "");
            String pwd = sp.getString("pwd", "");
            Log.d(TAG, String.format("Preference %s, %s", user, pwd));
            if (user != "") {
                mLoginBundle.putBoolean("AUTO_LOGIN", true);
                loginAdminUser(user, pwd, true);
            } else {
                mLoginBundle.putBoolean("AUTO_LOGIN", false);
            }
        } catch (Exception e) {
            Log.d(TAG, e.getMessage());
            Helper.sendGoogleAnalyticError(mFirebaseAnalytics, TAG, "",
                    e.getMessage());
        }
    }

    private void saveAutoLogin(String user, String pwd) {
        SharedPreferences sp = this.getSharedPreferences("LOGIN", MODE_PRIVATE);
        SharedPreferences.Editor ed = sp.edit();
        ed.putString("user", user );
        ed.putString("pwd", pwd);
        ed.commit();
        mFirebaseAnalytics.setUserProperty("AUTO_LOGIN", "SAVED");
    }

    ////////////////////////////////////////////////
    // General function                           //
    public boolean isOnline() {
        ConnectivityManager cm =
                (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo netInfo = cm.getActiveNetworkInfo();
        return netInfo != null && netInfo.isConnectedOrConnecting();
    }

    private String getVersionLabel() {
        try {
            PackageInfo pInfo = this.getPackageManager().getPackageInfo(getPackageName(), 0);
            String versionName = pInfo.versionName;
            int versionCode = pInfo.versionCode;
            return String.format("Version: %s, Build: %d", versionName, versionCode);
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
        }
        return "Version: unknown, Build: unknown";
    }

    ////////////////////////////////////////////////
    // Move to next activity                      //
    private void gotoAnonymousLogin() {
        Log.d(TAG, "Go to Anonymous Login");
        Intent intent = new Intent(Login2Activity.this, AnonymousLoginActivity.class);
        finish();
        startActivity(intent);
    }

    private void gotoNormalActivity(String empId, String empName) {
        Intent intent;
        Log.d(TAG, "Go to Scan2 Activity");
        intent = new Intent(Login2Activity.this, ScanCompatActivity.class);
        long msec = Calendar.getInstance().getTimeInMillis();
        Log.d(TAG, "Time = " + String.valueOf(msec));
        intent.putExtra("MODE","NORMAL");
        intent.putExtra("STAMPED_TIME", msec);
        intent.putExtra("EMP_ID", empId);
        intent.putExtra("EMP_NAME", empName);
        finish();
        startActivity(intent);
    }

    ////////////////////////////////////////////////
    // Invalid message                            //
    private void showMessage(String message) {
        View view = findViewById(R.id.scroll_layout);
        Snackbar snackbar = Snackbar.make(view, message, Snackbar.LENGTH_INDEFINITE);
        snackbar.show();
    }

    private void popupCannotSignIn() {
        View view = findViewById(R.id.scroll_layout);
        Snackbar snackbar = Snackbar.make(view, "Cannot connect to a server",
                Snackbar.LENGTH_INDEFINITE);
        snackbar.show();

    }
    private void popupInvalidLogin() {
        View view = findViewById(R.id.scroll_layout);
        Snackbar snackbar = Snackbar.make(view, R.string.invalid_login, Snackbar.LENGTH_INDEFINITE);
        snackbar.show();
    }
}
