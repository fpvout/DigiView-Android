package com.fpvout.digiview.tutorial;

import android.os.Bundle;
import android.view.View;

import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentTransaction;

import com.fpvout.digiview.R;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

public class TutorialActivity extends AppCompatActivity {

    // Layout
    private FloatingActionButton nextFab;

    // Tutorial Fragments - Add here additional fragments to appear in the tutorial
    private final Fragment[] tutorialFragments = {
            new WelcomeFragment(),
            new GogglesFragment(),
            new DroneFragment(),
            new PlugUsbFragment(),
            new GesturesFragment()
    };
    private int currentFragmentIndex = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_tutorial);

        // Fab
        nextFab = findViewById(R.id.tutorial_activity_fab_next);
        nextFab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                nextFragment();
            }
        });

        // Show first fragment
        showFragment(false);
    }

    /**
     * Displays the next fragment in the tutorialFragment array
     */
    private void nextFragment() {
        if (currentFragmentIndex + 1 < tutorialFragments.length) {
            currentFragmentIndex++;
            showFragment(false);
        } else {
            // Just quit the activity
            finish();
        }
    }

    /**
     * Displays the previous fragment in the tutorialFragment array
     */
    private void previousFragment() {
        if (currentFragmentIndex > 0) {
            currentFragmentIndex--;
            showFragment(true);
        }
    }

    /**
     * Displays the fragment using a slide in/out animation
     *
     * @param isBackwards Indicates whether the animations slides to the left or to the right
     */
    private void showFragment(boolean isBackwards) {
        FragmentTransaction ft = getSupportFragmentManager().beginTransaction();
        if (isBackwards) {
            ft.setCustomAnimations(android.R.anim.slide_in_left, android.R.anim.slide_out_right);
        } else {
            ft.setCustomAnimations(R.animator.slide_in_right, R.animator.slide_out_left);
        }
        ft.replace(R.id.tutorial_activity_frame_layout, tutorialFragments[currentFragmentIndex]);
        ft.commit();
    }

    /**
     * If the user calls onBackPressed() the gallery switches backwards first.
     * super.onBackPressed() is called when the first fragment is shown
     */
    @Override
    public void onBackPressed() {
        if (currentFragmentIndex > 0) {
            previousFragment();
        } else {
            super.onBackPressed();
        }
    }
}