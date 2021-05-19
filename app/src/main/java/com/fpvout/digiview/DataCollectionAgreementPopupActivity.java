package com.fpvout.digiview;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.Button;

import androidx.appcompat.app.AppCompatActivity;

public class DataCollectionAgreementPopupActivity extends AppCompatActivity {
    private SharedPreferences preferences;

    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        preferences = getApplicationContext().getSharedPreferences("com.fpvout.digiview", Context.MODE_PRIVATE);
        setContentView(R.layout.datacollection_agreement_popup);
        initializeUI();
    }

    private void initializeUI() {
        Button dataCollectionCancelButton = findViewById(R.id.data_collection_cancel);
        Button dataCollectionConfirmButton = findViewById(R.id.data_collection_confirm);

        dataCollectionCancelButton.setOnClickListener(v -> cancelDataCollection());
        dataCollectionConfirmButton.setOnClickListener(v -> confirmDataCollection());
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
