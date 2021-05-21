package com.fpvout.digiview.logging;

import android.util.Log;


import com.google.firebase.crashlytics.FirebaseCrashlytics;

import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;


public final class CustomLogger{
    public static final void i(String tag, String message){
        Log.i(tag,message);
        sendToCrashlytics(tag,message,null);
    }

    public static final void d(String tag, String message){
        Log.d(tag,message);
        sendToCrashlytics(tag,message,null);
    }

    public static final void d(String tag, String message,Throwable ex){
        Log.d(tag,message,ex);
        sendToCrashlytics(tag,message,ex);
    }

    public static final void e(String tag, String message){
        Log.e(tag,message);
        sendToCrashlytics(tag,message,null);
    }

    public static final void e(String tag, String message,Throwable ex){
        Log.e(tag,message,ex);
        sendToCrashlytics(tag,message,ex);
    }

    public static final void wtf(String tag, String message){
        Log.wtf(tag,message);
        sendToCrashlytics(tag,message,null);
    }

    public static final void v(String tag, String message){
        Log.v(tag,message);
        sendToCrashlytics(tag,message,null);
    }

    private static void sendToCrashlytics(String tag, String message, Throwable ex){
        try {
            FirebaseCrashlytics.getInstance().log(new Date().getTime() + ": Thread:<" + Thread.currentThread().getName() + "> " +  tag +  " : " + message);
            if (ex != null) {
                FirebaseCrashlytics.getInstance().recordException(ex);
            }
        }catch (Exception ex2){
            try {
                FirebaseCrashlytics.getInstance().recordException(ex2);
            }catch (Exception ex3){
                //keep the APP from crashing
            }
        }
    }
}
