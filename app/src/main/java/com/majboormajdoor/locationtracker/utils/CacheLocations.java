package com.majboormajdoor.locationtracker.utils;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.majboormajdoor.locationtracker.dto.Location;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;

public class CacheLocations {
    private static final String TAG = "LocationCacheManager";
    private static final String KEY_LOCATIONS = "cached_locations";

    private SharedPreferences preferences;
    private Gson gson;

    CacheLocations(Context context) {
        preferences = PreferenceManager.getInstance(context).getSharedPreferences();
        gson = new Gson();
    }

    public void cacheLocations(List<Location> locations) {
        try {
            locations.addAll(getCachedLocations());
            String json = gson.toJson(locations);
            preferences.edit()
                    .putString(KEY_LOCATIONS, json)
                    .apply();
            Log.d(TAG, "Cached " + locations.size() + " locations");
        } catch (Exception e) {
            Log.e(TAG, "Error caching locations", e);
        }
    }

    public List<Location> getCachedLocations() {
        try {
            String json = preferences.getString(KEY_LOCATIONS, null);
            if (json != null) {
                Type listType = new TypeToken<List<Location>>(){}.getType();
                List<Location> locations = gson.fromJson(json, listType);
                Log.d(TAG, "Retrieved " + locations.size() + " cached locations");
                return locations;
            }
        } catch (Exception e) {
            Log.e(TAG, "Error retrieving cached locations", e);
        }
        return new ArrayList<>();
    }

    public void clearCache() {
        preferences.edit()
                .remove(KEY_LOCATIONS)
                .apply();
        Log.d(TAG, "Cache cleared");
    }
}