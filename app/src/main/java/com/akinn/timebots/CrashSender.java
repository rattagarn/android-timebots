package com.akinn.timebots;

import android.content.Context;
import android.support.annotation.NonNull;

import org.acra.ACRA;
import org.acra.ReportField;
import org.acra.collector.CrashReportData;
import org.acra.config.ACRAConfiguration;
import org.acra.sender.ReportSender;
import org.acra.sender.ReportSenderException;
import org.acra.sender.ReportSenderFactory;

import java.util.Set;

/**
 * Created by ratta on 10/31/2017.
 */

public class CrashSender implements ReportSender {
    @Override
    public void send(@NonNull Context context, @NonNull CrashReportData errorContent) throws ReportSenderException {
        //
    }
}