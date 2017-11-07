package com.akinn.timebots;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.content.ContextCompat;
import android.support.v4.content.res.ResourcesCompat;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AnimationUtils;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.google.firebase.auth.FirebaseAuth;

import java.util.ArrayList;

/**
 * Created by ratta on 10/25/2017.
 */

public class MenuAdapter extends ArrayAdapter<MenuItem> implements View.OnClickListener{

    static final String TAG = MenuAdapter.class.getSimpleName();
    ArrayList<MenuItem> mItems;
    Context mContext;

    long mTimeStamp;
    String mEmpId;
    String mEmpName;
    String mMode;
    MainActivity mActivity;

    public MenuAdapter(ArrayList<MenuItem> data, MainActivity activity,
                       long timeStamp, String empId, String empName, String mode) {
        super(activity.getApplicationContext(), R.layout.row_layout, data);
        this.mItems = data;

        mTimeStamp = timeStamp;
        mEmpId = empId;
        mEmpName = empName;
        mMode = mode;
        mActivity = activity;
        mContext = mActivity.getApplicationContext();
    }

    private static class ViewHolder {
        Button buttonMenu;
    }

    @Override
    public void onClick(View v) {
        int position = (Integer) v.getTag();
        Object object = getItem(position);
        MenuItem item = (MenuItem) object;
        Log.d(TAG, "Position = " + String.valueOf(position));
        Log.d(TAG, "View's Id = " + String.valueOf(v.getId()) + " vs R.id.buttonMenu = " + String.valueOf(R.id.buttonMenu));
        Log.d(TAG, "Menu Id = " + item.getId());
        switch (v.getId()) {
            case R.id.buttonMenu:
                switch (item.getId()) {
                    case "scan":
                        Intent intent = new Intent(mActivity, ScanActivity.class);
                        intent.putExtra("MODE", mMode);
                        intent.putExtra("STAMPED_TIME", mTimeStamp);
                        intent.putExtra("EMP_ID", mEmpId);
                        intent.putExtra("EMP_NAME", mEmpName);
                        mActivity.startActivity(intent);
                        break;
                    case "exit":
                        mActivity.exitApp();
                        break;
                }
                break;
        }
    }

    private int mLastPosition = -1;

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        //return super.getView(position, convertView, parent);

        MenuItem item = getItem(position);
        ViewHolder viewHolder;

        final View resultView;

        if (convertView == null) {
            viewHolder = new ViewHolder();
            LayoutInflater inflater = LayoutInflater.from(getContext());
            convertView = inflater.inflate(R.layout.row_layout, parent, false);
            viewHolder.buttonMenu = (Button) convertView.findViewById(R.id.buttonMenu);
            resultView = convertView;

            convertView.setTag(viewHolder);
        } else {
            viewHolder = (ViewHolder) convertView.getTag();
            resultView = convertView;
        }

        mLastPosition = position;
        viewHolder.buttonMenu.setOnClickListener(this);
        viewHolder.buttonMenu.setTag(position);
        viewHolder.buttonMenu.setText(item.getText());
        int imgResource = 0;
        int bgColor = 0;
        switch (item.getId()) {
            case "scan":
                imgResource = R.mipmap.ic_scan;
                bgColor = ContextCompat.getColor(mContext, R.color.colorMenuScan);
                break;
            case "exit":
                imgResource = R.mipmap.ic_exit;
                bgColor = ContextCompat.getColor(mContext, R.color.colorMenuExit);
                break;
        }
        viewHolder.buttonMenu.setCompoundDrawablesWithIntrinsicBounds(imgResource, 0, 0, 0);
        viewHolder.buttonMenu.setBackgroundColor(bgColor);
        return convertView;
    }
}

