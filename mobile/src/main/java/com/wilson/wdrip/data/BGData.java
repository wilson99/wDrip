package com.wilson.wdrip.data;

/**
 * Created by wzhan025 on 3/19/2018.
 */


public class BGData {
    public int iBG;
    public double dDirection;
    public String sDirection;
    public String cDirection;
    public long lTime;

    public BGData(int iBG, double dDirection, String sDirection, String cDirection, long lTime) {
        this.iBG = iBG;
        this.dDirection = dDirection;
        this.sDirection = sDirection;
        this.cDirection = cDirection;
        this.lTime = lTime;
    }
}
