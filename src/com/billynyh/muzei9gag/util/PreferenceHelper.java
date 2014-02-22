package com.billynyh.muzei9gag.util;

import android.content.Context;
import android.content.SharedPreferences;
import android.text.TextUtils;
import android.util.Log;

import org.json.JSONArray;
import org.json.JSONException;

import java.util.ArrayList;
import java.util.List;

/**
 *
 */
public class PreferenceHelper {
    private static final String TAG = "PreferenceHelper";
    private static final boolean DEBUG = false;

    public static final int CONNECTION_WIFI = 0;
    public static final int CONNECTION_ALL = 1;

    public static final int MIN_FREQ_HR = 1;

    private static final int DEFAULT_FREQ_HR = 1;
    public static final int[] FREQ_HR_OPTIONS = new int[]{ 1,3,6,24 };


    public static int getConfigConnection(Context context) {
        SharedPreferences preferences = getPreferences(context);
        return preferences.getInt("config_connection", CONNECTION_WIFI);
    }

    public static void setConfigConnection(Context context, int connection) {
        SharedPreferences preferences = getPreferences(context);
        preferences.edit().putInt("config_connection", connection).commit();
    }

    public static void setConfigFreq(Context context, int durationHr) {
        SharedPreferences preferences = getPreferences(context);
        preferences.edit().putInt("config_freq", durationHr).commit();
    }

    public static int getConfigFreq(Context context) {
        SharedPreferences preferences = getPreferences(context);
        return preferences.getInt("config_freq", DEFAULT_FREQ_HR);
    }

    public static void limitConfigFreq(Context context) {
        int configFreq = getConfigFreq(context);
        if(configFreq < MIN_FREQ_HR) {
            setConfigFreq(context, MIN_FREQ_HR);
        }
    }



    public static List<String> getAvailableLists(Context context) {
        ArrayList<String> data = new ArrayList<String>();
        SharedPreferences preferences = getPreferences(context);
        String prefData = preferences.getString("available_list", "[\"hot|Hot\",\"trending|Trending\"]");
        if(!TextUtils.isEmpty(prefData)) {
            try {
                JSONArray jsonArray = new JSONArray(prefData);
                for(int index = 0 ; index < jsonArray.length() ; index++) {
                    data.add(jsonArray.getString(index));
                }
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
        return data;
    
    }

    public static List<String> getSelectedLists(Context context) {
        ArrayList<String> data = new ArrayList<String>();
        SharedPreferences preferences = getPreferences(context);
        String prefData = preferences.getString("selected_list", "[\"hot\"]");

        if (DEBUG) Log.d(TAG, "getSelectedLists "  + prefData);
        if(!TextUtils.isEmpty(prefData)) {
            try {
                JSONArray jsonArray = new JSONArray(prefData);
                for(int index = 0 ; index < jsonArray.length() ; index++) {
                    data.add(jsonArray.getString(index));
                }
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
        return data;
    
    }

    public static void setAvailableLists(Context context, List<String> list) {
        SharedPreferences preferences = getPreferences(context);
        JSONArray jsonArray = new JSONArray(list);
        preferences.edit().putString("available_list", jsonArray.toString()).commit();
    }

    public static void setSelectedLists(Context context, List<String> list) {
        SharedPreferences preferences = getPreferences(context);
        JSONArray jsonArray = new JSONArray(list);
        preferences.edit().putString("selected_list", jsonArray.toString()).commit();
    }

    private static SharedPreferences getPreferences(Context context) {
        return context.getSharedPreferences("hash", Context.MODE_PRIVATE);
    }
}
