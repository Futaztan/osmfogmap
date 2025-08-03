package com.osmfogmap.settings;

import android.os.Bundle;

import androidx.annotation.Nullable;
import androidx.preference.Preference;
import androidx.preference.PreferenceFragmentCompat;
import androidx.preference.SwitchPreferenceCompat;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.osmfogmap.MainActivity;
import com.osmfogmap.R;

public class SettingsFragment extends PreferenceFragmentCompat {


    @Override
    public void onCreatePreferences(@Nullable Bundle savedInstanceState, @Nullable String rootKey) {
        setPreferencesFromResource(R.xml.preferences, rootKey);

        SwitchPreferenceCompat following = findPreference("location_following");
        following.setChecked(MainActivity.settingsManager.LOCATION_FOLLOW);
        following.setOnPreferenceChangeListener((preference, newValue) ->
        {
            MainActivity.settingsManager.LOCATION_FOLLOW = (boolean) newValue;
            return true;

        });

        Preference delete = findPreference("delete_progress");

        delete.setOnPreferenceClickListener(preference -> {
            new MaterialAlertDialogBuilder(requireContext())
                    .setTitle("Are you sure?")
                    .setMessage("You will lose all of your uncovered area.")
                    .setPositiveButton("Yes", (dialog, which) -> {

                        MainActivity.settingsManager.deleteProgress();
                    })
                    .setNegativeButton("No", (dialog, which) -> {
                        dialog.dismiss();
                    })
                    .show();

            return false;
        });
    }
}
