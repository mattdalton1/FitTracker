package com.matthew.fittracker.fit_tracker.view;

import android.app.Fragment;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import com.matthew.fittracker.fit_tracker.R;
/**
 * Created by dalton on 12/08/2015.
 */
public class FitTrackerCommunity extends Fragment {
    public FitTrackerCommunity() {}

    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        View rootView = inflater.inflate(R.layout.fragment_fittracker_community, container, false);
        return rootView;
    }
}

