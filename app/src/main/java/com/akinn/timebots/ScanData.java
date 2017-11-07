package com.akinn.timebots;

import com.google.firebase.database.Exclude;

import java.util.HashMap;
import java.util.Map;

/**
 * Created by ratta on 10/28/2017.
 */

public class ScanData {
    public String EmployeeID;
    public long ScannedTime;
    public String ImageURI;
    public String Mode;
    public String Comment;
    public double Longitude;
    public double Latitude;
    public boolean HasPicture;
    public boolean HasLocation;
    public String MobileTimeZone;
    public ScanData(
            String empId, String imgURI, String mode, String comment,
            long scannedTime, double lng, double lat,
            boolean hasPic, boolean hasLoc, String timeZone) {
        this.EmployeeID = empId;
        this.ImageURI = imgURI;
        this.Mode = mode;
        this.Comment = comment;
        this.ScannedTime = scannedTime;
        this.Longitude = lng;
        this.Latitude = lat;
        this.HasPicture = hasPic;
        this.HasLocation = hasLoc;
        this.MobileTimeZone = timeZone;
    }

    public ScanData() {}

    @Exclude
    public Map<String, Object> toMap() {
        HashMap<String, Object> result = new HashMap<>();
        result.put("mode", Mode);
        result.put("imagURI", ImageURI);
        result.put("comment", Comment);
        result.put("scannedTime", ScannedTime);
        result.put("longitude", Longitude);
        result.put("latitude", Latitude);
        result.put("hasPic", HasPicture);
        result.put("hasLoc", HasLocation);
        result.put("timeZone", MobileTimeZone);
        return result;
    }
}
