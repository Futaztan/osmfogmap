package com.osmfogmap.area;

import android.widget.Button;

import com.osmfogmap.overlays.FogOverlayPolygon;

public class AreaManager {
    private final Button btn_area;
    private double maxArea = 0;
    public AreaManager(Button area)
    {
        btn_area = area;
        btn_area.setText("0 km²");
        FogOverlayPolygon.setOnAreaChangeListener(this::printArea);
    }
    private void printArea(double newArea)
    {
        maxArea = newArea;
        btn_area.setText(newArea + " km²");
        if(newArea > maxArea)
        {

        }
    }

}
