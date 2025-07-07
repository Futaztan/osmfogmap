package com.osmfogmap.settings;

import android.content.Context;

import android.os.Bundle;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CompoundButton;

import androidx.appcompat.widget.SwitchCompat;
import androidx.fragment.app.DialogFragment;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;


import com.osmfogmap.R;

public class SettingsDialogFragment extends DialogFragment {
    private SettingsManager settingsManager;
    private Context context;

    public SettingsDialogFragment(SettingsManager sm, Context cont) {
        settingsManager = sm;
        context = cont;
    }


    @Override
    public View onCreateView(
            LayoutInflater inflater,
            ViewGroup container,
            Bundle savedInstanceState) {

        View view = inflater.inflate(R.layout.fragment_settings_dialog, container, false);

        SwitchCompat switch_CameraFollowing = view.findViewById(R.id.switch_cameraFollowing);
        Button btn_delete = view.findViewById(R.id.btn_delete);
        Button btn_marker = view.findViewById(R.id.btn_marker);

       // Button btn_color = view.findViewById(R.id.btn_color);

        switch_CameraFollowing.setChecked(settingsManager.CAMERA_FOLLOWING);

        switch_CameraFollowing.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                settingsManager.CAMERA_FOLLOWING = isChecked;
            }
        });
//        btn_color.setOnClickListener(v -> {
//            AmbilWarnaDialog dialog = new AmbilWarnaDialog(context,  0xff000000 , new AmbilWarnaDialog.OnAmbilWarnaListener() {
//                @Override
//                public void onOk(AmbilWarnaDialog dialog, int color) {
//                    // color is the color selected by the user.
//                    Toast.makeText(context, "SADADSASD", Toast.LENGTH_SHORT).show();
//
//                }
//
//                @Override
//                public void onCancel(AmbilWarnaDialog dialog) {
//                    // cancel was selected by the user
//                }
//                });
//            dialog.show();
//
//
//
//
//        });

        btn_delete.setOnClickListener(v -> {

            new MaterialAlertDialogBuilder(context)
                    .setTitle("Are you sure?")
                    .setMessage("You will lose all of your uncovered area.")
                    .setPositiveButton("Yes", (dialog, which) -> {
                        settingsManager.deleteProgress();
                    })
                    .setNegativeButton("No", (dialog, which) -> {
                        dialog.dismiss();
                    })
                    .show();


        });

        btn_marker.setOnClickListener(v -> {
            settingsManager.marker();
        });

        return view;
    }

    @Override
    public void onStart() {
        super.onStart();
        // Teljes szélesség
        if (getDialog() != null && getDialog().getWindow() != null) {
            getDialog().getWindow().setLayout(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT);
        }
    }


}
