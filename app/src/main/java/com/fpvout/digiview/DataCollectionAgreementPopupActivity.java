package com.fpvout.digiview;

import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;

public class DataCollectionAgreementPopupActivity extends AppCompatActivity {
    private SharedPreferences preferences;
    private AlertDialog.Builder builder;

    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        preferences = this.getApplicationContext().getSharedPreferences("com.fpvout.digiview", Context.MODE_PRIVATE);
        builder = new AlertDialog.Builder(this);
        initializeUI();
    }

    private void initializeUI() {
        builder.setTitle(R.string.data_collection_header)
                .setMessage(R.string.data_collection_text)
                .setPositiveButton(R.string.data_collection_agree_button, (dialog, which) -> confirmDataCollection())
                .setNegativeButton(R.string.data_collection_deny_button, (dialog, which) -> cancelDataCollection());
        AlertDialog dialog = builder.create();
        dialog.show();
    }

    private void cancelDataCollection() {
        preferences.edit()
                .putBoolean("dataCollectionAccepted", false)
                .putBoolean("dataCollectionReplied", true).apply();

        Intent intent = new Intent();
        setResult(RESULT_OK, intent);

        finish();
    }

    // NOTE: Here you would call your api to save the status
    private void confirmDataCollection() {
        preferences.edit()
                .putBoolean("dataCollectionAccepted", true)
                .putBoolean("dataCollectionReplied", true).apply();
        Intent intent = new Intent();
        setResult(RESULT_OK, intent);
        finish();
    }
}
