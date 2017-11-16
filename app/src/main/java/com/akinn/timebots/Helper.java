package com.akinn.timebots;

import android.app.Activity;
import android.os.Build;
import android.os.Bundle;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Display;

import com.google.firebase.analytics.FirebaseAnalytics;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.Calendar;
import java.util.HashMap;
import java.util.Map;

/**
 * Created by champ on 11/10/2017 AD.
 */

public class Helper {
    public static void sendGoogleAnalyticInfo(FirebaseAnalytics fa, Activity activity,
                                              String mode, String empId, String empName,
                                              Boolean hasFrontCamera, Boolean hasRearCamera,
                                              Boolean hasLocation) {
        Bundle params = new Bundle();
        params.putString("activity", activity.getLocalClassName());
        params.putString("mode", mode);
        params.putString("emp_id", empId);
        params.putString("emp_name", empName);
        params.putLong("time", Calendar.getInstance().getTimeInMillis());
        params.putString("sn", Build.SERIAL);
        params.putString("brand", Build.BRAND);
        params.putString("model", Build.MODEL);
        params.putString("device", Build.DEVICE);
        params.putString("codename", Build.VERSION.CODENAME);
        params.putInt("sdk_int", Build.VERSION.SDK_INT);
        Display display = activity.getWindowManager().getDefaultDisplay();
        DisplayMetrics dm = new DisplayMetrics();
        display.getMetrics(dm);
        params.putInt("density_dpi", dm.densityDpi);
        params.putInt("width_pixels", dm.widthPixels);
        params.putInt("height_pixels", dm.heightPixels);
        params.putBoolean("front_camera", hasFrontCamera);
        params.putBoolean("rear_camera", hasRearCamera);
        params.putBoolean("has_location", hasLocation);
        fa.logEvent("send_info", params);
    }

    public static void sendGoogleAnalyticError(FirebaseAnalytics fa, String activityName,
                                               String empName, String message) {
        Bundle params = new Bundle();
        params.putString("activity", activityName);
        params.putString("emp_name", empName);
        params.putLong("time", Calendar.getInstance().getTimeInMillis());
        params.putString("brand", Build.BRAND);
        params.putString("model", Build.MODEL);
        params.putString("device", Build.DEVICE);
        params.putString("codename", Build.VERSION.CODENAME);
        params.putInt("sdk_int", Build.VERSION.SDK_INT);
        params.putString("message", message);
        fa.logEvent("found_error", params);
    }

    public static void sendGoogleAnalyticPermission(FirebaseAnalytics fa, String activityName,
                                               String empName, String message) {
        Bundle params = new Bundle();
        params.putString("activity", activityName);
        params.putString("emp_name", empName);
        params.putLong("time", Calendar.getInstance().getTimeInMillis());
        params.putString("brand", Build.BRAND);
        params.putString("model", Build.MODEL);
        params.putString("device", Build.DEVICE);
        params.putString("codename", Build.VERSION.CODENAME);
        params.putInt("sdk_int", Build.VERSION.SDK_INT);
        params.putString("message", message);
        fa.logEvent("send_permission", params);
    }

    public static void sendSimpleGoogleAnalyticInfo(FirebaseAnalytics fa, Activity activity) {
        Bundle params = new Bundle();
        params.putString("activity", activity.getLocalClassName());
        params.putLong("time", Calendar.getInstance().getTimeInMillis());
        params.putString("sn", Build.SERIAL);
        params.putString("brand", Build.BRAND);
        params.putString("model", Build.MODEL);
        params.putString("device", Build.DEVICE);
        params.putString("codename", Build.VERSION.CODENAME);
        params.putInt("sdk_int", Build.VERSION.SDK_INT);
        Display display = activity.getWindowManager().getDefaultDisplay();
        DisplayMetrics dm = new DisplayMetrics();
        display.getMetrics(dm);
        params.putInt("density_dpi", dm.densityDpi);
        params.putInt("width_pixels", dm.widthPixels);
        params.putInt("height_pixels", dm.heightPixels);
        fa.logEvent("send_basic_info", params);
    }

    public static void sendInfo(FirebaseDatabase db, Activity activity, String empName) {
        final long milliTime = Calendar.getInstance().getTimeInMillis();

        String msg;
        msg = "Serial: " + Build.SERIAL;
        msg += "\nBrand: " + Build.BRAND;
        msg += "\nModel: " + Build.MODEL;
        msg += "\nDevice: " + Build.DEVICE;
        msg += "\nCode Name: " + Build.VERSION.CODENAME;
        msg += String.format("\nSDK INT: %d", Build.VERSION.SDK_INT);
        Display display = activity.getWindowManager().getDefaultDisplay();
        DisplayMetrics dm = new DisplayMetrics();
        display.getMetrics(dm);
        msg += String.format("\ndensity_dpi: %d", dm.densityDpi);
        msg += String.format("\nwidth_pixels: %d", dm.widthPixels);
        msg += String.format("\nheight_pixels: %d", dm.heightPixels);
        Log.d("Logging", msg);

        final DeviceData deviceData = new DeviceData(Build.SERIAL, Build.BRAND, Build.MODEL,
                Build.DEVICE, Build.VERSION.CODENAME, Build.VERSION.SDK_INT, dm.densityDpi,
                dm.widthPixels, dm.heightPixels, empName);
        final DatabaseReference ref = db.getReference("DeviceInfo");
        ref.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                if (dataSnapshot.exists()) {
                    Log.d("Helper","DataSnapShot doesn't exist");
                    ref.child(String.valueOf(milliTime)).setValue(deviceData);
                } else {
                    Log.d("Helper","DataSnapShot exists");
                    DatabaseReference ref2 = ref.child("DeviceInfo").push();
                    ref2.child(String.valueOf(milliTime)).setValue(deviceData);
                }
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
                Log.d("Helper", databaseError.getDetails());
            }
        });
    }

    public static void logLoginActivity(FirebaseAnalytics fa, String id, String name) {
        Bundle bundle = new Bundle();
        bundle.putString(FirebaseAnalytics.Param.ITEM_ID, id);
        bundle.putString(FirebaseAnalytics.Param.ITEM_NAME, name);
        bundle.putString(FirebaseAnalytics.Param.CONTENT_TYPE, "VIEW");
        fa.logEvent(FirebaseAnalytics.Event.LOGIN, bundle);
    }
}
