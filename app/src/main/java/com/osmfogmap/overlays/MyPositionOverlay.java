package com.osmfogmap.overlays;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Point;
import android.graphics.drawable.Drawable;


import com.osmfogmap.R;

import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.MapView;
import org.osmdroid.views.Projection;
import org.osmdroid.views.overlay.Overlay;

public class MyPositionOverlay extends Overlay {
    private GeoPoint currentLocation;
    private final Drawable icon;

    public MyPositionOverlay(Context context) {
        super();
        icon = context.getResources().getDrawable(R.drawable.icon_dot, null);
        icon.setBounds(0, 0, icon.getIntrinsicWidth(), icon.getIntrinsicHeight());
    }

    public void setLocation(GeoPoint location) {
        this.currentLocation = location;
    }

    @Override
    public void draw(Canvas canvas, MapView mapView, boolean shadow) {
        if (shadow || currentLocation == null) return;

        Projection projection = mapView.getProjection();
        Point screenPoint = projection.toPixels(currentLocation, null);

        int offsetX = icon.getIntrinsicWidth() / 2;
        int offsetY = icon.getIntrinsicHeight() / 2;

        canvas.save();
        canvas.translate(screenPoint.x - offsetX, screenPoint.y - offsetY);
        icon.draw(canvas);
        canvas.restore();
    }
}

