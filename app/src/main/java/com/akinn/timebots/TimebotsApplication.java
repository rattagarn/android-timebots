package com.akinn.timebots;

import android.app.Application;
import android.content.Context;

import org.acra.ACRA;
import org.acra.ReportingInteractionMode;
import org.acra.annotation.ReportsCrashes;
import org.acra.sender.ReportSenderFactory;

import static org.acra.ReportField.APP_VERSION_CODE;
import static org.acra.ReportField.ANDROID_VERSION;
import static org.acra.ReportField.CUSTOM_DATA;
import static org.acra.ReportField.LOGCAT;
import static org.acra.ReportField.PHONE_MODEL;
import static org.acra.ReportField.STACK_TRACE;

/**
 * Created by ratta on 10/31/2017.
 */
@ReportsCrashes(
        formUri = "http://www.backendofyourchoice.com/reportpath",
        mailTo = "reports@yourdomain.com",
        mode = ReportingInteractionMode.TOAST,
        customReportContent = { APP_VERSION_CODE, ANDROID_VERSION, PHONE_MODEL, CUSTOM_DATA, STACK_TRACE, LOGCAT }
)

public class TimebotsApplication extends Application {
    @Override
    protected void attachBaseContext(Context base) {
        super.attachBaseContext(base);

        // The following line triggers the initialization of ACRA
        ACRA.init(this);
        CrashSender crashSender = new CrashSender();
        //ACRA.getErrorReporter();
        //ACRA.getErrorReporter().setReportSender(crashSender)
    }
}
