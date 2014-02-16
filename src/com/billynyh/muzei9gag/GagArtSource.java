package com.billynyh.muzei9gag;


import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.Random;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import android.content.Intent;
import android.net.Uri;

import com.billynyh.muzei9gag.util.HttpRequest;
import com.google.android.apps.muzei.api.Artwork;
import com.google.android.apps.muzei.api.RemoteMuzeiArtSource;

public class GagArtSource extends RemoteMuzeiArtSource {
    private static final String TAG = "GagArtSource";
    
    private static final int UPDATE_INTERVAL = 1 * 60 * 1000; // 1min
    Random mRandom;
    
    public GagArtSource() {
        super("billynyh-9gag");
    }


    @Override
    public void onCreate() {
        super.onCreate();
        setUserCommands(BUILTIN_COMMAND_ID_NEXT_ARTWORK);
        mRandom = new Random();
    }
    
    @Override
    protected void onTryUpdate(int reason) throws RetryException {
        String url = "http://9gag.com/";
        HttpRequest request =  HttpRequest.get(url);
        
        String body = request.body();
        ArrayList<Gag> list = parsePage(body); 
        
        int i = mRandom.nextInt(list.size());
        Gag gag = list.get(i);
        
        publishArtwork(new Artwork.Builder()
            .title(gag.title)
            .imageUri(Uri.parse(gag.imageUrl))
            .token(gag.id)
            .viewIntent(new Intent(Intent.ACTION_VIEW,
                Uri.parse("http://9gag.com/gag/" + gag.id)))
            .build());
     
        scheduleUpdate(System.currentTimeMillis() + UPDATE_INTERVAL);
    }

    private ArrayList<Gag> parsePage(String body) {
        ArrayList<Gag> list = new ArrayList<Gag>();
        
        String patternString = "data-title=\"([^\"]*)\"\\s*data-img=\"([^\"]*)\"";
        Pattern p = Pattern.compile(patternString, Pattern.MULTILINE);
        Matcher m = p.matcher(body);
        while (m.find()) { 
            String title = m.group(1);
            String imageUrl = m.group(2);
            String id = extractGagIdFromImageUrl(imageUrl);
            
            Gag g = new Gag(id, title, imageUrl);
            list.add(g);
        }
        return list;
    }
    
    private String extractGagIdFromImageUrl(String url) {
        Pattern p = Pattern.compile("\\/photo\\/([\\w\\d]*)_700b.jpg");
        Matcher m = p.matcher(url);
        if (m.find()) {
            return m.group(1);
        }
        return null;
    }
    

    public static class Gag {
        public String title, imageUrl, id;
        
        public Gag(String id, String title, String imageUrl) {
            this.id = id;
            this.imageUrl = imageUrl;
            try {
                this.title = URLDecoder.decode(title, "utf-8");
            } catch (UnsupportedEncodingException e) {
                this.title = title;
            }
        }
    }
}
