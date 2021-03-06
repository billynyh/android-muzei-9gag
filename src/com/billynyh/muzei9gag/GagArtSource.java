package com.billynyh.muzei9gag;


import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.HashSet;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.net.NetworkInfo;
import android.net.ConnectivityManager;

import com.billynyh.muzei9gag.util.HttpRequest;
import com.billynyh.muzei9gag.util.PreferenceHelper;
import com.google.android.apps.muzei.api.Artwork;
import com.google.android.apps.muzei.api.RemoteMuzeiArtSource;
import com.google.android.apps.muzei.api.UserCommand;

import android.util.Log;


public class GagArtSource extends RemoteMuzeiArtSource {
    private static final String TAG = "GagArtSource";
    private static final boolean DEBUG = false;

    public static final int HR_TO_MS = 1000*60*60;

    public static final int COMMAND_ID_SHARE = 1;

    Random mRandom;
    String mCurrentList;
    
    public GagArtSource() {
        super("billynyh-9gag");
    }


    @Override
    public void onCreate() {
        super.onCreate();
        ArrayList<UserCommand> commands = new ArrayList<UserCommand>();
        commands.add(new UserCommand(BUILTIN_COMMAND_ID_NEXT_ARTWORK));
        commands.add(new UserCommand(COMMAND_ID_SHARE, getString(R.string.action_share_post)));

        setUserCommands(commands);
        mRandom = new Random();
        PreferenceHelper.limitConfigFreq(this);
    }
    
    @Override
    protected void onTryUpdate(int reason) throws RetryException {
        if (!isConnectedAsPreferred()) {
            scheduleUpdate(System.currentTimeMillis() + getRotateTimeMillis());
            return;
        }

        Artwork currentArtwork = getCurrentArtwork();
        String currentImageUrl = null;
        if (currentArtwork != null) {
            currentImageUrl = currentArtwork.getImageUri().toString();
        }
        if (DEBUG) Log.d(TAG, "current image: " + currentImageUrl);


        String url = pickPage();
        if (DEBUG) Log.d(TAG, "pick from page: " + url);
        try {
            HttpRequest request =  HttpRequest.get(url);
            
            String body = request.body();
            ArrayList<Gag> list = parsePage(body); 
            
            for (int i=0;i<5;i++) { // retry at most 5 times if get the same image
                int p = mRandom.nextInt(list.size());
                Gag gag = list.get(p);
                if (DEBUG) Log.d(TAG, "random picked: " + gag.imageUrl);
                if (currentImageUrl != null && currentImageUrl.equals(gag.imageUrl)) continue;
                
                publishArtwork(new Artwork.Builder()
                    .title(gag.title)
                    .byline(gag.tag)
                    .imageUri(Uri.parse(gag.imageUrl))
                    .token(gag.id)
                    .viewIntent(new Intent(Intent.ACTION_VIEW,
                        Uri.parse("http://9gag.com/gag/" + gag.id)))
                    .build());
             
                long next = System.currentTimeMillis() + getRotateTimeMillis();
                scheduleUpdate(next);
                if (DEBUG) Log.d(TAG, "finish publishArtWork");
                break;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onCustomCommand(int id) {
        super.onCustomCommand(id);

        if (id == COMMAND_ID_SHARE) {
            Artwork currentArtwork = getCurrentArtwork();
            String detailUrl = currentArtwork.getViewIntent().getDataString();
            Intent shareIntent = new Intent(Intent.ACTION_SEND);
            shareIntent.setType("text/plain");
            shareIntent.putExtra(Intent.EXTRA_TEXT, 
                    currentArtwork.getTitle().trim()
                    + " #Muzei9gag\n"
                    + detailUrl);
            shareIntent = Intent.createChooser(shareIntent, "Share post");
            shareIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(shareIntent);
        }
    }


    private String pickPage() {
        final String PREFIX = "http://9gag.com/";
        List<String> list = PreferenceHelper.getSelectedLists(this);
        int n = list.size();
        if (n > 0) {
            mCurrentList = list.get(mRandom.nextInt(n));
            return PREFIX + mCurrentList;
        }
        return PREFIX;
    }

    private ArrayList<Gag> parsePage(String body) {
        ArrayList<Gag> list = new ArrayList<Gag>();
        // fetch list
        String listPatternString = "<li><a class=\"[^\"]*\" href=\"http://9gag.com/([^\"]+)\">([^<]+)</a></li>";
        Pattern p0 = Pattern.compile(listPatternString);
        Matcher m0 = p0.matcher(body);

        ArrayList<String> availableList = newDefaultAvailableList();
        while (m0.find()) { 
            String t1 = m0.group(1), t2 = m0.group(2);
            if ("gif".equals(t1)) continue;
            availableList.add(t1+"|"+t2);
            if (DEBUG) Log.d(TAG, "~ find source: " + t1);
        }
        if (availableList.size() > 2) {
            //TODO sometimes cannot extract the list, dont override the list in that case
            PreferenceHelper.setAvailableLists(this, availableList);
        }

        // fetch long post information
        String heightPatternString = "style=\"min-height:([\\d\\.]+)[^\\n]*.([^\\n]*)";
        Pattern p1 = Pattern.compile(heightPatternString, Pattern.DOTALL);
        Matcher m1 = p1.matcher(body);
        if (DEBUG) Log.d(TAG, "m1 " + m1);
        HashSet<String> longSet = new HashSet<String>();
        while (m1.find()) { 
            String t1 = m1.group(1);
            String t2 = m1.group(2);
            String id = extractGagIdFromImageUrl(t2);
            
            if (DEBUG) Log.d(TAG, "- check long post: " + id + " " + t1);

            float val = Float.valueOf(t1);
            if (val > 2000) {
                if (DEBUG) Log.d(TAG, id + " could be long post");
                longSet.add(id);
            }
            
        }

        // fetch post
        //String patternString = "data-title=\"([^\"]*)\"\\s*data-img=\"([^\"]*)\"";
        if (DEBUG) Log.d(TAG, "Fetch post");
        String patternString = "img class=\"badge-item-img\" src=\"([^\"]*)\" alt=\"([^\"]*)\"";
        Pattern p = Pattern.compile(patternString, Pattern.MULTILINE);
        Matcher m = p.matcher(body);
        while (m.find()) { 
            String title = m.group(2);
            String imageUrl = m.group(1);

            imageUrl = imageUrl.replace("460s", "700b");

            String id = extractGagIdFromImageUrl(imageUrl);
            if (DEBUG) Log.d(TAG, "~ extracted post: " + imageUrl);
            if (!longSet.contains(id)) {
                String tag = mCurrentList == null ? "hot" : mCurrentList;

                Gag g = new Gag(id, title, imageUrl, "#" + tag);
                list.add(g);
            } else {
                if (DEBUG) Log.d(TAG, "~~ skip long post: " + id);
            }
        }

        return list;
    }

    private ArrayList<String> newDefaultAvailableList() {
        ArrayList<String> results = new ArrayList<String>();
        results.add("hot|Hot");
        results.add("trending|Trending");
        return results;
    }
    
    private String extractGagIdFromImageUrl(String url) {
        Pattern p = Pattern.compile("\\/photo\\/([a-zA-Z\\d]*)_");
        Matcher m = p.matcher(url);
        if (m.find()) {
            return m.group(1);
        }
        return null;
    }
    

    public static class Gag {
        public String title, imageUrl, id;
        public String tag;
        
        public Gag(String id, String title, String imageUrl, String tag) {
            this.id = id;
            this.imageUrl = imageUrl;
            this.tag = tag;
            try {
                this.title = URLDecoder.decode(title, "utf-8");
            } catch (UnsupportedEncodingException e) {
                this.title = title;
            }
        }
    }

    // config
    private int getRotateTimeMillis() {
        if (DEBUG) Log.d(TAG, "getRotateTime " + PreferenceHelper.getConfigFreq(this));
        return PreferenceHelper.getConfigFreq(this) * HR_TO_MS;
    }   

    private boolean isConnectedAsPreferred() {
        if(PreferenceHelper.getConfigConnection(this) == PreferenceHelper.CONNECTION_WIFI) {
            return isWifiConnected(this);
        }   
        return true;
    }   

    public static boolean isWifiConnected(Context context) {
        ConnectivityManager connManager = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo mWifi = connManager.getNetworkInfo(ConnectivityManager.TYPE_WIFI);
        return  mWifi.isConnected();
    }   

    
}
