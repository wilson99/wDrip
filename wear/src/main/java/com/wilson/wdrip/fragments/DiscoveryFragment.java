package com.wilson.wdrip.fragments;

import android.app.Fragment;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.wilson.wdrip.R;

/**
 * Created by wzhan025 on 3/14/2018.
 */

public class DiscoveryFragment extends Fragment {

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        return inflater.inflate(R.layout.discovery_fragment, container, false);
    }
}

