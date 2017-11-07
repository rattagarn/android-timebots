package com.akinn.timebots;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Build;
import android.support.annotation.NonNull;
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
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.GetTokenResult;
import com.google.firebase.database.ChildEventListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;

import io.fabric.sdk.android.Fabric;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;

public class LoginActivity extends AppCompatActivity {
    static final String TAG = LoginActivity.class.getSimpleName();
    Button button;

    FirebaseAuth mAuth;
    FirebaseAuth.AuthStateListener mAuthListener;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Fabric.with(this, new Crashlytics());

        requestWindowFeature(getWindow().FEATURE_NO_TITLE);
        setContentView(R.layout.activity_login);

        Log.d(TAG, "SDK: " + String.valueOf(Build.VERSION.SDK_INT));
        mAuth = FirebaseAuth.getInstance();
        mAuthListener = new FirebaseAuth.AuthStateListener() {
            @Override
            public void onAuthStateChanged(@NonNull FirebaseAuth firebaseAuth) {
                FirebaseUser user = firebaseAuth.getCurrentUser();
                if (user != null) {
                    // User is signed in
                    Log.d(TAG, "onAuthStateChanged:signed_in:" + user.getUid());
                } else {
                    // User is signed out
                    Log.d(TAG, "onAuthStateChanged:signed_out");
                }
                // ...
            }
        };

        TextView textViewVersion = (TextView) findViewById(R.id.textViewVersion);
        textViewVersion.setText(getVersionLabel());
        button = (Button) findViewById(R.id.buttonLogin);
        button.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                EditText userText = (EditText) findViewById(R.id.editTextUsername);
                EditText pwdText = (EditText) findViewById(R.id.editTextPassword);
                String userName = userText.getText().toString().trim();
                String password = pwdText.getText().toString().trim();
                loginAdminUser(userName, password, false);
            }
        });

        if (this.isOnline()) {
            Log.d(TAG, "Sigining In");
            signIn();
        } else {
            Log.d(TAG, "Not online");
            Toast.makeText(getApplicationContext(), "No internet connection.\nPlease turn on internet connection.", Toast.LENGTH_LONG).show();
//            popupInvalidMessage(getApplicationContext(),
//                    "No internet connection.\nPlease turn on internet connection.");
        }

        Log.d(TAG, "DPI: " + String.valueOf(getApplicationContext().getResources().getDisplayMetrics().density));
    }

    public void forceCrash(View view) {
        throw new RuntimeException("This is a crash");
    }
    private void logUser(String empName, String empId) {
        // TODO: Use the current user's information
        // You can call any combination of these three methods
        Crashlytics.setUserIdentifier(empId);
        Crashlytics.setUserName(empName);
    }

    @Override
    public void onStart() {
        super.onStart();
        Log.d(TAG, "onStart");
        mAuth.addAuthStateListener(mAuthListener);
        doAutoLogin();
    }

    @Override
    public void onStop() {
        super.onStop();
        Log.d(TAG, "onStop");
        if (mAuthListener != null) {
            mAuth.removeAuthStateListener(mAuthListener);
        }
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
                            popupCannotSignIn(LoginActivity.this);
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

    private void popupCannotSignIn(Context context) {
        TextView textViewStatus = (TextView) findViewById(R.id.textViewStatus);
        textViewStatus.setText("Cannot sign in");
        textViewStatus.setVisibility(View.VISIBLE);
        //Toast.makeText(getApplicationContext(),
        //        "Cannot sign in", Toast.LENGTH_LONG).show();
        /*
        AlertDialog.Builder dlg;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            dlg = new AlertDialog.Builder(context, android.R.style.Theme_Material_Dialog_Alert);
        } else {
            dlg = new AlertDialog.Builder(context);
        }
        dlg.setTitle("Login Failure")
            .setMessage("Cannot login to server")
            .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int which) {
                    // continue with delete
                    finish();
                }
            })
            .setIcon(android.R.drawable.ic_dialog_alert)
            .show();
            */
    }

    private void loginAdminUser(final String userName, final String password, final boolean auto) {
        if (userName == "") {
            Log.d(TAG, "loginAdminUser: username is empty");
            popupInvalidLogin(LoginActivity.this);
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
                        CheckBox checkBoxRemember = (CheckBox) findViewById(R.id.checkBoxRemember);
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
                Toast.makeText(getApplicationContext(),
                        databaseError.getDetails(), Toast.LENGTH_LONG).show();
            }
        };

        Log.d(TAG, "loginAdminUser: attached ValueEventListener");
        adminRef.addListenerForSingleValueEvent(adminListener);
    }

    private void gotoAnonymousLogin() {
        Log.d(TAG, "Go to Anonymous Login");
        Intent intent = new Intent(LoginActivity.this, AnonymousLoginActivity.class);
        startActivity(intent);
        finish();
    }

    private void loginNormalUser(final String userName, final String password, final boolean auto) {
        if (userName == "") {
            Log.d(TAG, "loginNormalUser: username is empty");
            popupInvalidLogin(LoginActivity.this);
            return;
        }

        // Get a reference to our DB
        final FirebaseDatabase database = FirebaseDatabase.getInstance();
        final DatabaseReference ref = database.getReference("Employees/" + userName);

        if (ref == null) {
            Log.d(TAG, "loginNormalUser: Cannot find this Normal's username = " + userName);
            popupInvalidLogin(LoginActivity.this);
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
                        CheckBox checkBoxRemember = (CheckBox) findViewById(R.id.checkBoxRemember);
                        if((!auto) && checkBoxRemember.isChecked()) {
                            saveAutoLogin(userName, password);
                        }
                        gotoNormalActivity(userName, empName);
                    } else {
                        popupInvalidLogin(LoginActivity.this);
                    }
                } else {
                    popupInvalidLogin(LoginActivity.this);
                }
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
                Log.d(TAG, "ref:onCancelled", databaseError.toException());
                Toast.makeText(getApplicationContext(),
                        databaseError.getDetails(), Toast.LENGTH_LONG).show();
            }
        };
        ref.addListenerForSingleValueEvent(queryListner);
    }

    private void popupInvalidLogin(Context context) {
        TextView textViewStatus = (TextView) findViewById(R.id.textViewStatus);
        EditText userText = (EditText) findViewById(R.id.editTextUsername);
        textViewStatus.setText("Cannot Login: " + userText.getText().toString());
        textViewStatus.setVisibility(View.VISIBLE);
        //Toast.makeText(context,"Cannot login", Toast.LENGTH_LONG).show();
        //return;
        /*
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

                    }
                })
                .setIcon(android.R.drawable.ic_dialog_alert)
                .show();
                */
    }

    private void gotoNormalActivity(String empId, String empName) {
        Intent intent;
        Log.d(TAG, "Go to Scan2 Activity");
        intent = new Intent(LoginActivity.this, Scan2Activity.class);
        long msec = Calendar.getInstance().getTimeInMillis();
        Log.d(TAG, "Time = " + String.valueOf(msec));
        intent.putExtra("MODE","NORMAL");
        intent.putExtra("STAMPED_TIME", msec);
        intent.putExtra("EMP_ID", empId);
        intent.putExtra("EMP_NAME", empName);
        finish();
        startActivity(intent);
    }

    public boolean isOnline() {
        ConnectivityManager cm =
                (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo netInfo = cm.getActiveNetworkInfo();
        return netInfo != null && netInfo.isConnectedOrConnecting();
    }

    private void popupInvalidMessage(Context context, String message) {
        AlertDialog.Builder dlg;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            dlg = new AlertDialog.Builder(context, android.R.style.Theme_Material_Dialog_Alert);
        } else {
            dlg = new AlertDialog.Builder(context);
        }
        dlg.setTitle("Sign-In Failure")
                .setMessage(message)
                .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        // continue with delete
                        finish();
                    }
                })
                .setIcon(android.R.drawable.ic_dialog_alert)

                .show();
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

    private void doAutoLogin() {
        try {
            SharedPreferences sp = this.getSharedPreferences("LOGIN", MODE_PRIVATE);
            String user = sp.getString("user", "");
            String pwd = sp.getString("pwd", "");
            Log.d(TAG, String.format("Preference %s, %s", user, pwd));
            if (user != "") {
                loginAdminUser(user, pwd, true);
            }
        } catch (Exception e) {
            Log.d(TAG, e.getMessage());
        }
    }

    private void saveAutoLogin(String user, String pwd) {
        SharedPreferences sp = this.getSharedPreferences("LOGIN", MODE_PRIVATE);
        SharedPreferences.Editor ed = sp.edit();
        ed.putString("user", user );
        ed.putString("pwd", pwd);
        ed.commit();
    }
}
