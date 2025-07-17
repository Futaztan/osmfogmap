package com.osmfogmap;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;


import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdSize;
import com.google.android.gms.ads.AdView;
import com.google.android.gms.ads.MobileAds;
import com.google.android.gms.ads.admanager.AdManagerAdRequest;
import com.osmfogmap.area.AreaManager;
import com.osmfogmap.locationtrack.BackgroundService;
import com.osmfogmap.locationtrack.LocationUpdateCallback;
import com.osmfogmap.overlays.FogOverlayPolygon;
import com.osmfogmap.overlays.MyPositionOverlay;
import com.osmfogmap.save_load.SaveLoadManager;
import com.osmfogmap.settings.SettingsDialogFragment;
import com.osmfogmap.settings.SettingsManager;

import org.locationtech.jts.geom.Geometry;
import org.osmdroid.config.Configuration;
import org.osmdroid.tileprovider.tilesource.TileSourceFactory;
import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.MapController;
import org.osmdroid.views.MapView;
import org.osmdroid.views.overlay.Marker;
import org.osmdroid.views.overlay.Overlay;

import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity implements LocationUpdateCallback {
    private MapView mapView;
    private MapController mapController;
    private FogOverlayPolygon fogOverlay;
    private SaveLoadManager saveLoadManager;
    private SettingsManager settingsManager;
    private int tick = 0;
    private MyPositionOverlay myPositionOverlay;
    Intent serviceIntent;
    GeoPoint lastloc = null;

    private static final int LOCATION_PERMISSION_REQUEST_CODE = 1001;
    private static final boolean  SAVE_ENABLED = false;
    private static final boolean  LOADING_ENABLED = false;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        Configuration.getInstance().setUserAgentValue("fogmap/1.0");
        setContentView(R.layout.activity_main);

        new Thread(()->{
            MobileAds.initialize(this, initializationStatus -> {});
        });


        AdView mAdView = findViewById(R.id.adView);
        AdRequest adRequest = new AdRequest.Builder().build();
        mAdView.loadAd(adRequest);


        mapView =  findViewById(R.id.map);
        myPositionOverlay = new MyPositionOverlay(this);
        fogOverlay = new FogOverlayPolygon(this);
        settingsManager = new SettingsManager(fogOverlay,mapView);
        saveLoadManager = new SaveLoadManager(this,fogOverlay,settingsManager);
        Button btn_area = findViewById(R.id.btn_area);
        AreaManager areaManager = new AreaManager(btn_area);

        if(LOADING_ENABLED)
            saveLoadManager.loadEverything();

        // mapView.setLayerType(View.LAYER_TYPE_SOFTWARE, null);
        mapView.setLayerType(View.LAYER_TYPE_HARDWARE,null);
        mapView.setVerticalMapRepetitionEnabled(false);
        mapView.setHorizontalMapRepetitionEnabled(false);
        mapView.setTileSource(TileSourceFactory.MAPNIK);
        mapView.setMultiTouchControls(true);
        mapView.setBuiltInZoomControls(false);
        mapView.setMaxZoomLevel(16.6);

        mapController = (MapController) mapView.getController();
        mapController.setZoom(15);
        mapController.animateTo(settingsManager.lastloc,mapView.getZoomLevelDouble(), 1100L);

        serviceIntent = new Intent(this, BackgroundService.class);
        BackgroundService.registerCallback(this);

        mapView.getOverlays().add(fogOverlay);
        mapView.getOverlays().add(myPositionOverlay);

        mapView.invalidate();

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED)
            requestLocationPermission();
        else ContextCompat.startForegroundService(this, serviceIntent);



    }


    public void geometrymarker(Geometry g)
    {
        List<Overlay> toRemove = new ArrayList<>();
        for (Overlay overlay : mapView.getOverlays()) {
            if (overlay instanceof Marker) {
                toRemove.add(overlay);
            }
        }
        mapView.getOverlays().removeAll(toRemove);
        mapView.invalidate();



        for(int i = 0; i<g.getNumGeometries(); i++)
        {

            Geometry ge = g.getGeometryN(i);
            for(int j=0; j<ge.getCoordinates().length;j++)
            {
                Marker marker = new Marker(mapView);
                GeoPoint p = new GeoPoint(ge.getCoordinates()[j].y,ge.getCoordinates()[j].x);
                marker.setPosition(p);
                marker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM);
                mapView.getOverlays().add(marker);
            }
        }

        mapView.invalidate();

    }

    @Override
    public void onLocationUpdated(GeoPoint location) {
        lastloc=location;
        settingsManager.lastloc=location;
        runOnUiThread(() -> {
            myPositionOverlay.setLocation(location);
            mapView.invalidate();
        });

        if(settingsManager.LOCATION_FOLLOW)
        {
            MapController mapController = (MapController) mapView.getController();
            mapController.animateTo(location,mapView.getZoomLevelDouble(), 1100L);
        }

        fogOverlay.addHole(location);
      /*  List<Overlay> toRemove = new ArrayList<>();
        for (Overlay overlay : mapView.getOverlays()) {
            if (overlay instanceof Marker) {
                toRemove.add(overlay);
            }
        }
        mapView.getOverlays().removeAll(toRemove);
        mapView.invalidate();

        for(int i=0; i<fogOverlay.getHoles().size();i++)
        {

            Marker marker = new Marker(mapView);

            GeoPoint geoPoint = fogOverlay.getHoles().get(i);
            marker.setPosition(geoPoint);
            marker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM);
            mapView.getOverlays().add(marker);

        }*/

        mapView.invalidate();

        tick++;
        if(tick==10)
        {
            tick=0;
            if(SAVE_ENABLED)
                saveLoadManager.saveEverything();
        }
    }

    private void requestLocationPermission() {
        ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, LOCATION_PERMISSION_REQUEST_CODE);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == LOCATION_PERMISSION_REQUEST_CODE)
        {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED)
                ContextCompat.startForegroundService(this, serviceIntent);
            else
            {
                Toast.makeText(this, "Location tracking is required for this application!", Toast.LENGTH_SHORT).show();
                requestLocationPermission();
            }
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if(SAVE_ENABLED)
            saveLoadManager.saveEverything();
        stopService(serviceIntent);
    }


    public void onSettingsClicked(View view)
    {
        SettingsDialogFragment dialog = new SettingsDialogFragment(settingsManager,this);
        dialog.show(getSupportFragmentManager(), "SettingsDialog");
}

    public void onShowMyPositionClicked(View view) {
        if(lastloc==null)
            mapController.animateTo(settingsManager.lastloc,mapView.getZoomLevelDouble(), 1100L);
        else  mapController.animateTo(lastloc,mapView.getZoomLevelDouble(), 1100L);
    }

    public void onTestClicked(View view) {
       List<Overlay> toRemove = new ArrayList<>();
        for (Overlay overlay : mapView.getOverlays()) {
            if (overlay instanceof Marker) {
                toRemove.add(overlay);
            }
        }
        mapView.getOverlays().removeAll(toRemove);
        mapView.invalidate();



        for(int i = 0; i<fogOverlay.revealedGeometry.getNumGeometries(); i++)
        {

            Geometry g = fogOverlay.revealedGeometry.getGeometryN(i);
            //g = g.convexHull();
            for(int j=0; j<g.getCoordinates().length;j++)
            {
                Marker marker = new Marker(mapView);
                GeoPoint p = new GeoPoint(g.getCoordinates()[j].y,g.getCoordinates()[j].x);
                marker.setPosition(p);
                marker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM);
                mapView.getOverlays().add(marker);
            }
        }

        for(int i =0; i< fogOverlay.getHoles().size();i++)
        {
            Marker marker = new Marker(mapView);
            GeoPoint p = new GeoPoint(fogOverlay.getHoles().get(i));
            marker.setPosition(p);
            marker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM);
            mapView.getOverlays().add(marker);
        }
        //fogOverlay.simplifyHoles();

        mapView.invalidate();

    }

    public void onTest2Clicked(View view)
    {
        //fogOverlay.simplifytest();

        List<Overlay> toRemove = new ArrayList<>();
        for (Overlay overlay : mapView.getOverlays()) {
            if (overlay instanceof Marker) {
                toRemove.add(overlay);
            }
        }
        mapView.getOverlays().removeAll(toRemove);
        mapView.invalidate();
        List<Geometry> poly = new ArrayList<>();
        for(int i = 0; i<fogOverlay.revealedGeometry.getNumGeometries(); i++)
        {

            Geometry g = fogOverlay.revealedGeometry.getGeometryN(i);
            g = g.getBoundary();
            poly.add(g);
            for(int j=0; j<g.getCoordinates().length;j++)
            {
                Marker marker = new Marker(mapView);
                GeoPoint p = new GeoPoint(g.getCoordinates()[j].y,g.getCoordinates()[j].x);
                marker.setPosition(p);
                marker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM);
                mapView.getOverlays().add(marker);
            }
        }
        //fogOverlay.revealedGeometry = UnaryUnionOp.union(poly);

        mapView.invalidate();

    }
}