package com.akinn.timebots;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.TextView;

import com.google.firebase.auth.FirebaseAuth;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Locale;

public class MainActivity extends AppCompatActivity {
    static final String TAG = MainActivity.class.getSimpleName();
    ArrayList<MenuItem> mMenuItemList;
    ListView mListView;
    private static MenuAdapter mMenuAdapter;
    String mEmpId = "";
    String mEmpName = "";
    long mStampedTime = 0;
    String mMode = "KIOSK";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Intent intent = getIntent();
        mMode = intent.getStringExtra("MODE");
        mStampedTime = intent.getLongExtra("STAMPED_TIME", -1);
        mEmpId = intent.getStringExtra("EMP_ID");
        mEmpName = intent.getStringExtra("EMP_NAME");
        Date d = new Date(mStampedTime);
        Log.d(TAG, String.format("Millisec = %d, Date = %s", mStampedTime, d.toString()));
        SimpleDateFormat sdf = new SimpleDateFormat("dd-MMM-yyyy HH:mm:ss", Locale.US);
        String dateString = sdf.format(d);
        Log.d(TAG, "Date is " + dateString);

        TextView textViewEmpId = (TextView) findViewById(R.id.textViewEmpId);
        textViewEmpId.setText(mEmpId);
        TextView textViewEmpName = (TextView) findViewById(R.id.textViewEmpName);
        textViewEmpName.setText(mEmpName);

        mListView = (ListView) findViewById(R.id.listViewMenus);
        mMenuItemList = new ArrayList<MenuItem>();
        mMenuItemList.add(new MenuItem("scan", getString(R.string.menu_scan), R.mipmap.ic_scan));
        mMenuItemList.add(new MenuItem("exit", getString(R.string.menu_exit), R.mipmap.ic_exit));

        mMenuAdapter = new MenuAdapter(mMenuItemList,
                MainActivity.this, mStampedTime, mEmpId, mEmpName, mMode);

        mListView = (ListView) findViewById(R.id.listViewMenus);
        mListView.setAdapter(mMenuAdapter);
        mListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                MenuItem item = mMenuItemList.get(position);
                Log.d(TAG, "onItemClick: Position = " + String.valueOf(position));
                switch (item.getId()) {
                    case "scan":
                        Log.d(TAG, "onItemClick: item is SCAN");
                        Intent intent = new Intent(MainActivity.this, ScanActivity.class);
                        intent.putExtra("STAMPED_TIME", mStampedTime);
                        intent.putExtra("EMP_ID", mEmpId);
                        intent.putExtra("EMP_NAME", mEmpName);
                        startActivity(intent);

                        break;
                    case "exit":
                        Log.d(TAG, "onItemClick: item is EXIT");
                        exitApp();

                        break;
                }
            }
        });
    }

    public void exitApp() {
        AlertDialog.Builder alertDialog = new AlertDialog.Builder(MainActivity.this);

        alertDialog.setPositiveButton("Yes", new DialogInterface.OnClickListener() {

            @Override
            public void onClick(DialogInterface dialog, int which) {
                FirebaseAuth mAuth = FirebaseAuth.getInstance();
                mAuth.signOut();
                finish();
            }
        });

        alertDialog.setNegativeButton("No", null);

        alertDialog.setMessage("Do you want to exit?");
        alertDialog.setTitle("Exit App");
        alertDialog.show();
    }
}
