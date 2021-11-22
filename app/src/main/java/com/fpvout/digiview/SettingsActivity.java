package com.fpvout.digiview;

import android.os.Build;
import android.os.Bundle;
import android.view.MenuItem;

import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.preference.ListPreference;
import androidx.preference.PreferenceFragmentCompat;

import com.fpvout.digiview.streaming.StreamAudioSource;

import java.util.ArrayList;
import java.util.Arrays;

public class SettingsActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.settings_activity);
        if (savedInstanceState == null) {
            getSupportFragmentManager()
                    .beginTransaction()
                    .replace(R.id.settings, new SettingsFragment())
                    .commit();
        }
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true);
        }
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {

        if (item.getItemId() == android.R.id.home) {
            this.finish();
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    public static class SettingsFragment extends PreferenceFragmentCompat {
        @Override
        public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
            setPreferencesFromResource(R.xml.root_preferences, rootKey);

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                ListPreference audioSourcePreference = findPreference("StreamAudioSource");

                ArrayList<CharSequence> entries = new ArrayList<>(Arrays.asList(audioSourcePreference.getEntries()));
                ArrayList<CharSequence> entryValues = new ArrayList<>(Arrays.asList(audioSourcePreference.getEntryValues()));

                entries.add(getString(R.string.stream_audio_source_performance));
                entryValues.add(StreamAudioSource.PERFORMANCE);

                entries.add(getString(R.string.stream_audio_source_internal));
                entryValues.add(StreamAudioSource.INTERNAL);

                audioSourcePreference.setEntries(entries.toArray(new CharSequence[0]));
                audioSourcePreference.setEntryValues(entryValues.toArray(new CharSequence[0]));
            }
        }
    }
}