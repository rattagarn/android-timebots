package com.akinn.timebots;

import com.google.firebase.database.Exclude;

import java.util.HashMap;
import java.util.Map;

/**
 * Created by champ on 11/11/2017 AD.
 */

public class DeviceData {
    public String Serial;
    public String Brand;
    public String Model;
    public String Device;
    public String CodeName;
    public int SDK;
    public int DpiDensity;
    public int WidthPixels;
    public int HeightPixels;
    public String EmpName;

    public DeviceData(
            String serial, String brand, String model, String device, String codename,
            int sdk, int dpi, int width, int height, String empName) {
        this.Serial = serial;
        this.Brand = brand;
        this.Model = model;
        this.Device = device;
        this.CodeName = codename;
        this.SDK = sdk;
        this.DpiDensity = dpi;
        this.WidthPixels = width;
        this.HeightPixels = height;
        this.EmpName = empName;
    }
    public DeviceData() {}


    @Exclude
    public Map<String, Object> toMap() {
        HashMap<String, Object> result = new HashMap<>();
        result.put("Serial", Serial);
        result.put("Brand", Brand);
        result.put("Model", Model);
        result.put("Device", Device);
        result.put("CodeName", CodeName);
        result.put("SDK", SDK);
        result.put("DpiDensity", DpiDensity);
        result.put("WidthPixels", WidthPixels);
        result.put("HeightPixels", HeightPixels);
        return result;
    }
}
