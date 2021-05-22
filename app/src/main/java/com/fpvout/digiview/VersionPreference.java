package com.fpvout.digiview;

import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.util.AttributeSet;

import androidx.preference.Preference;

// From https://stackoverflow.com/a/20157577/877465
public class VersionPreference extends Preference {
    public VersionPreference(Context context, AttributeSet attrs) {
        super(context, attrs);
        String versionName;
        final PackageManager packageManager = context.getPackageManager();
        if (packageManager != null) {
            try {
                PackageInfo packageInfo = packageManager.getPackageInfo(context.getPackageName(), 0);
                versionName = packageInfo.versionName;
            } catch (PackageManager.NameNotFoundException e) {
                versionName = null;
            }
            setSummary(versionName);
        }

        setSelectable(false);
    }
}