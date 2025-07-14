package com.osmfogmap.area;

import android.widget.Button;

import com.osmfogmap.overlays.FogOverlayPolygon;

public class AreaManager {
    private final Button btn_area;
    private double maxArea = 0;

    public interface OnAreaChangeListener {
        void onAreaChanged(double newArea);
    }
    public AreaManager(Button area)
    {
        btn_area = area;
        btn_area.setText("0 km²");
        FogOverlayPolygon.setOnAreaChangeListener(this::printArea);
    }
    private void printArea(double newArea)
    {
        if(newArea == -1)
        {
            maxArea = 0.0;
            btn_area.setText(maxArea + " km²");
        }
        else if(newArea > maxArea)
        {
            maxArea = newArea;
            btn_area.setText(maxArea + " km²");
        }
    }

}
