package com.osmfogmap.save_load;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import com.osmfogmap.overlays.FogOverlayPolygon;
import com.osmfogmap.settings.SettingsManager;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.io.ParseException;
import org.locationtech.jts.io.WKBReader;
import org.locationtech.jts.io.WKBWriter;
import org.osmdroid.util.GeoPoint;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

public class SaveLoadManager {

    private final Context context;
    private final FogOverlayPolygon fogOverlay;
    private final SettingsManager settingsManager;

    public SaveLoadManager(Context _context, FogOverlayPolygon fogoverlay, SettingsManager sm)
    {
        context=_context;
        fogOverlay=fogoverlay;
        settingsManager=sm;
    }
    public void saveEverything()
    {
        saveHoles();
        savePolygon();
        saveSettings();
    }
    public void loadEverything()
    {
        loadHoles();
        //loadPolygon();
        loadSettings();
    }

    private void savePolygon() {
        File file = new File(context.getFilesDir(), "saved_geometry.wkb");

        try (FileOutputStream fos = new FileOutputStream(file)) {
            byte[] wkb = new WKBWriter().write(fogOverlay.revealedGeometry);
            fos.write(wkb);
        } catch (IOException e) {
            Log.e("error",e.toString());
        }
    }

    private void loadPolygon(){
        File file = new File(context.getFilesDir(), "saved_geometry.wkb");
        try (ByteArrayOutputStream bos = new ByteArrayOutputStream();
             FileInputStream fis = new FileInputStream(file)) {
            byte[] buffer = new byte[1024];
            int bytesRead;
            while ((bytesRead = fis.read(buffer)) != -1) {
                bos.write(buffer, 0, bytesRead);
            }
            byte[] bytes = bos.toByteArray();
            Geometry g = new WKBReader().read(bytes);
            fogOverlay.loadPolygon(g);
        } catch (IOException | ParseException e) {
            Log.e("error",e.toString());
        }

    }
    private void saveSettings()
    {
        SharedPreferences prefs = context.getSharedPreferences("openstreetmap_fogmap", Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();
        editor.putBoolean("settings_camerafollowing", settingsManager.LOCATION_FOLLOW);
        editor.putString("last_location_lat",String.valueOf(settingsManager.lastloc.getLatitude()));
        editor.putString("last_location_long",String.valueOf(settingsManager.lastloc.getLongitude()));
        editor.apply();
    }

    private void loadSettings()
    {
        SharedPreferences prefs = context.getSharedPreferences("openstreetmap_fogmap", Context.MODE_PRIVATE);
        settingsManager.LOCATION_FOLLOW =  prefs.getBoolean("settings_camerafollowing",false);
        settingsManager.lastloc.setLatitude(Double.parseDouble(prefs.getString("last_location_lat","0.0")));
        settingsManager.lastloc.setLongitude(Double.parseDouble(prefs.getString("last_location_long","0.0")));
    }
    private void saveHoles() {
        Log.d("log-save", "SIKERES save");
        List<GeoPoint> holes = fogOverlay.getHoles();
        JSONArray jsonArray = new JSONArray();

        for (GeoPoint point : holes) {
            JSONObject obj = new JSONObject();
            try {
                obj.put("lat", point.getLatitude());
                obj.put("lon", point.getLongitude());
                jsonArray.put(obj);
            } catch (JSONException e) {
                Log.e("tag-error", e.toString());
            }
        }

        File file = new File(context.getFilesDir(), "holes.json");
        try (FileOutputStream fos = new FileOutputStream(file)) {
            fos.write(jsonArray.toString().getBytes(StandardCharsets.UTF_8));
        } catch (IOException e) {
            Log.e("file-save", "Hiba a mentés közben", e);
        }
    }


    private void loadHoles() {
        File file = new File(context.getFilesDir(), "holes.json");
        List<GeoPoint> holes = new ArrayList<>();

        if (!file.exists()) return;

        try (FileInputStream fis = new FileInputStream(file)) {
            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            byte[] buffer = new byte[1024];
            int bytesRead;
            while ((bytesRead = fis.read(buffer)) != -1) {
                bos.write(buffer, 0, bytesRead);
            }

            String jsonString = bos.toString(StandardCharsets.UTF_8.name());
            JSONArray jsonArray = new JSONArray(jsonString);

            for (int i = 0; i < jsonArray.length(); i++) {
                JSONObject obj = jsonArray.getJSONObject(i);
                double lat = obj.getDouble("lat");
                double lon = obj.getDouble("lon");
                holes.add(new GeoPoint(lat, lon));
            }

        } catch (IOException | JSONException e) {
            Log.e("file-load", "Hiba a betöltés közben", e);
        }

        fogOverlay.loadHoles(holes);
    }





}
