package com.wilson.wdrip.fragments;

import android.app.Fragment;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import com.wilson.wdrip.R;

/**
 * Created by wzhan025 on 3/14/2018.
 */


public class AssetFragment extends Fragment {

    private ImageView mPhoto;

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.asset_fragment, container, false);
        mPhoto = (ImageView) view.findViewById(R.id.photo);
        return view;
    }

    public void setBackgroundImage(Bitmap bitmap) {
        mPhoto.setImageBitmap(bitmap);
    }
}

