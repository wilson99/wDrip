package com.wilson.wdrip;

import android.app.Activity;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;

import com.wilson.wdrip.utils.BgGraphBuilder;

import java.util.Date;

import lecho.lib.hellocharts.gesture.ZoomType;
import lecho.lib.hellocharts.listener.ViewportChangeListener;
import lecho.lib.hellocharts.model.Viewport;
import lecho.lib.hellocharts.view.LineChartView;
import lecho.lib.hellocharts.view.PreviewLineChartView;


public class Home extends AppCompatActivity {


    public BgGraphBuilder bgGraphBuilder;
    private Viewport tempViewport = new Viewport();
    public Viewport holdViewport = new Viewport();

    public LineChartView chart;
    private PreviewLineChartView previewChart;

    private boolean updateStuff;
    private boolean updatingPreviewViewport = false;
    private boolean updatingChartViewport = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setTheme(R.style.AppThemeToolBarLite); // for toolbar mode
        setContentView(R.layout.activity_home);

        Toolbar mToolbar = (Toolbar) findViewById(R.id.my_toolbar);
        setSupportActionBar(mToolbar);

        setupCharts();
    }

    private void setupCharts() {
        bgGraphBuilder = new BgGraphBuilder(this);
        updateStuff = false;
        chart = (LineChartView) findViewById(R.id.chart);
        chart.setZoomType(ZoomType.HORIZONTAL);
        previewChart = (PreviewLineChartView) findViewById(R.id.chart_preview);

        chart.setLineChartData(bgGraphBuilder.lineData());
        previewChart.setZoomType(ZoomType.HORIZONTAL);
        previewChart.setLineChartData(bgGraphBuilder.previewLineData(chart.getLineChartData()));
        previewChart.setViewportCalculationEnabled(true);
        previewChart.setViewportChangeListener(new PreviewViewportListener());
        chart.setViewportCalculationEnabled(true);
        chart.setViewportChangeListener(new ChartViewPortListener());
        updateStuff = true;
        setViewport();
    }

    public void setViewport() {
        if (tempViewport.left == 0.0 || holdViewport.left == 0.0 || holdViewport.right >= (new Date().getTime())) {
            previewChart.setCurrentViewport(bgGraphBuilder.advanceViewport(chart, previewChart));
        } else {
            previewChart.setCurrentViewport(holdViewport);
        }
    }

    private class ChartViewPortListener implements ViewportChangeListener {
        @Override
        public synchronized void onViewportChanged(Viewport newViewport) {
            if (!updatingPreviewViewport) {
                updatingChartViewport = true;
                previewChart.setZoomType(ZoomType.HORIZONTAL);
                previewChart.setCurrentViewport(newViewport);
                updatingChartViewport = false;
            }
        }
    }

    public class PreviewViewportListener implements ViewportChangeListener {
        @Override
        public synchronized void onViewportChanged(Viewport newViewport) {
            if (!updatingChartViewport) {
                updatingPreviewViewport = true;
                chart.setZoomType(ZoomType.HORIZONTAL);
                chart.setCurrentViewport(newViewport);
                tempViewport = newViewport;
                updatingPreviewViewport = false;
            }
            if (updateStuff) {
                holdViewport.set(newViewport.left, newViewport.top, newViewport.right, newViewport.bottom);
            }
        }

    }
}
