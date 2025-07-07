package com.osmfogmap.save_load;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import com.osmfogmap.overlays.FogOverlay;
import com.osmfogmap.settings.SettingsManager;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.osmdroid.util.GeoPoint;

import java.util.ArrayList;
import java.util.List;

public class SaveLoadManager {

    private Context context;
    private FogOverlay fogOverlay;
    private SettingsManager settingsManager;

    public SaveLoadManager(Context _context, FogOverlay fogoverlay, SettingsManager sm)
    {
        context=_context;
        fogOverlay=fogoverlay;
        settingsManager=sm;
    }
    public void saveEverything()
    {
        saveHoles();
        saveSettings();
    }
    public void loadEverything()
    {
        loadHoles();
        loadSettings();
    }

    private void saveSettings()
    {
        SharedPreferences prefs = context.getSharedPreferences("openstreetmap_fogmap", Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();
        editor.putBoolean("settings_camerafollowing", settingsManager.CAMERA_FOLLOWING);
        editor.putString("last_location_lat",String.valueOf(settingsManager.lastloc.getLatitude()));
        editor.putString("last_location_long",String.valueOf(settingsManager.lastloc.getLongitude()));
        editor.apply();
    }

    private void loadSettings()
    {
        SharedPreferences prefs = context.getSharedPreferences("openstreetmap_fogmap", Context.MODE_PRIVATE);
        settingsManager.CAMERA_FOLLOWING =  prefs.getBoolean("settings_camerafollowing",false);
        settingsManager.lastloc.setLatitude(Double.parseDouble(prefs.getString("last_location_lat","0.0")));
        settingsManager.lastloc.setLongitude(Double.parseDouble(prefs.getString("last_location_long","0.0")));
    }
    private void saveHoles() {
        Log.d("log-save","SIKERES save");
        List<GeoPoint> holes = fogOverlay.getHoles();
        JSONArray jsonArray = new JSONArray();
        for (GeoPoint point : holes) {
            JSONObject obj = new JSONObject();
            try {
                obj.put("lat", point.getLatitude());
                obj.put("lon", point.getLongitude());
                jsonArray.put(obj);
            } catch (JSONException e) {
                Log.e("tag-error",e.toString());
            }
        }

        SharedPreferences prefs = context.getSharedPreferences("openstreetmap_fogmap", Context.MODE_PRIVATE);
        prefs.edit().putString("holes", jsonArray.toString()).apply();
    }

    private void loadHoles()
    {
        Log.d("log-load","SIKERES LOAD");
        SharedPreferences prefs = context.getSharedPreferences("openstreetmap_fogmap", Context.MODE_PRIVATE);
        String json = prefs.getString("holes", "[]");


        try {
            List<GeoPoint> load = new ArrayList<>();
            JSONArray jsonArray = new JSONArray(json);
            for (int i = 0; i < jsonArray.length(); i++) {
                JSONObject obj = jsonArray.getJSONObject(i);
                double lat = obj.getDouble("lat");
                double lon = obj.getDouble("lon");
                //fogOverlay.loadHoles(new GeoPoint(lat, lon));
                load.add(new GeoPoint(lat,lon));
            }
            fogOverlay.loadHoles(load);

        } catch (JSONException e) {
            Log.e("tag-preferror",e.toString());
        }

    }




}
