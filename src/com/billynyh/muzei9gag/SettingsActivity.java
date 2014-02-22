package com.billynyh.muzei9gag;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.annotation.TargetApi;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import android.text.TextUtils;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewGroupOverlay;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.AbsListView;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.ArrayAdapter;
import android.widget.CheckedTextView;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.CompoundButton;

import android.util.Log;

import java.util.HashSet;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.billynyh.muzei9gag.util.PreferenceHelper;

public class SettingsActivity extends FragmentActivity {

    private static final String TAG = "SettingsActivity";

    Spinner mFreqSpinner;
    CheckedTextView mWifiOnly;
    LinearLayout mSourceContainer;

    HashSet<String> mSelectedSet;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        PreferenceHelper.limitConfigFreq(this);

        setContentView(R.layout.activity_settings);


        setupActionBar();

        initView();
        bindView();
        setupConfig();
    }

    private void initView() {
        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(this,
                R.array.freq_array, R.layout.spinner_item);
        mFreqSpinner = (Spinner)findViewById(R.id.freqSpinner);
        mFreqSpinner.setAdapter(adapter);

        mWifiOnly = (CheckedTextView)findViewById(R.id.wifiConfig);

        mSourceContainer = (LinearLayout) findViewById(R.id.sourceContainer);
    }

    private FragmentActivity getA() { return SettingsActivity.this; }

    private void bindView() {
        mFreqSpinner.setOnItemSelectedListener(new OnItemSelectedListener() {
            public void onItemSelected(AdapterView<?> parent, View view, int pos, long id) {
                PreferenceHelper.setConfigFreq(getA(), PreferenceHelper.FREQ_HR_OPTIONS[pos]);
            }

            public void onNothingSelected(AdapterView<?> parent) {
                // Another interface callback
            }  
        });

        mWifiOnly.setOnClickListener(new View.OnClickListener(){
            public void onClick(View v) {
                mWifiOnly.toggle();
                int value = mWifiOnly.isChecked() ? PreferenceHelper.CONNECTION_WIFI : PreferenceHelper.CONNECTION_ALL;
                PreferenceHelper.setConfigConnection(SettingsActivity.this, value);        
            }
        });


        mSourceContainer.removeAllViews();
        List<String> available = PreferenceHelper.getAvailableLists(this);
        LayoutInflater inflater = getLayoutInflater();
        for (String item : available) {
            mSourceContainer.addView(newSourceRow(inflater, item), fpwc());
        }
    }

    private void setupActionBar() {
        final LayoutInflater inflater = getLayoutInflater();
        View actionBarView = inflater.inflate(R.layout.ab_activity_settings, null);
        actionBarView.findViewById(R.id.actionbar_done).setOnClickListener(mOnActionBarDoneClickListener);
        getActionBar().setCustomView(actionBarView);
    }

    private View newSourceRow(LayoutInflater inflater, String item) {
        CheckedTextView v = (CheckedTextView) inflater.inflate(R.layout.source_item, null);
        String[] s = item.split("\\|");
        v.setText(s[1]);
        v.setTag(s[0]);
        v.setOnClickListener(mSourceRowClickListener);
        return v;
    }

    View.OnClickListener mSourceRowClickListener = new View.OnClickListener() {
        public void onClick(View v) {
            CheckedTextView tv = (CheckedTextView) v;
            tv.toggle();
            PreferenceHelper.setSelectedLists(SettingsActivity.this, getUiSelectedSource());
        }
    };

    private List<String> getUiSelectedSource() {
        ArrayList<String> results = new ArrayList<String>();
        int n = mSourceContainer.getChildCount();
        for (int i=0;i<n;i++) {
            CheckedTextView tv = (CheckedTextView)mSourceContainer.getChildAt(i);
            if (tv.isChecked()) {
                results.add((String) tv.getTag());
            }
        }
        return results;
    }

    private ViewGroup.LayoutParams fpwc() {
        return new ViewGroup.LayoutParams(ViewGroup.LayoutParams.FILL_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
    }

    private View.OnClickListener mOnActionBarDoneClickListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            finish();
        }
    };


    private void setupConfig() {

        updateConfigFreq();
        updateConfigConnection();
        updateConfigSource();
    }
    private void updateConfigFreq() {
        int configFreq = PreferenceHelper.getConfigFreq(this);
 
        int n = PreferenceHelper.FREQ_HR_OPTIONS.length;
        for (int i=0;i<n;i++) {
            if (configFreq == PreferenceHelper.FREQ_HR_OPTIONS[i]) {
                mFreqSpinner.setSelection(i);
            }
        }

        // Send an intent to communicate the update with the service
        Intent intent = new Intent(this, GagArtSource.class);
        intent.putExtra("configFreq", configFreq);
        startService(intent);
    }

    private void updateConfigConnection() {

        switch (PreferenceHelper.getConfigConnection(this)) {
            case PreferenceHelper.CONNECTION_ALL:
                mWifiOnly.setChecked(false);
                break;
            case PreferenceHelper.CONNECTION_WIFI:
                mWifiOnly.setChecked(true);
                break;
        }
    }

    private void updateConfigSource() {
        List<String> list = PreferenceHelper.getSelectedLists(this);
        mSelectedSet = new HashSet<String>();
        for (String s : list) {
            //Log.d(TAG, "add " + s);
            mSelectedSet.add(s);
        }

        int n = mSourceContainer.getChildCount();
        for (int i=0;i<n;i++) {
            CheckedTextView tv = (CheckedTextView) mSourceContainer.getChildAt(i);
            tv.setChecked(mSelectedSet.contains((String)tv.getTag()));
        }
    }


}
