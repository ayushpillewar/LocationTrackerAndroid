package com.majboormajdoor.locationtracker.utils;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.majboormajdoor.locationtracker.dto.Location;

import java.lang.reflect.Type;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class CacheLocations {
    private static final String TAG = "LocationCacheManager";
    private static final String KEY_LOCATIONS = "cached_locations";

    private static CacheLocations instance;
    private static final Object lock = new Object();

    private SharedPreferences preferences;
    private Gson gson;

    // Private constructor to prevent direct instantiation
    private CacheLocations(Context context) {
        preferences = PreferenceManager.getInstance(context).getSharedPreferences();
        gson = new Gson();
    }

    /**
     * Get singleton instance of CacheLocations
     * Thread-safe implementation using double-checked locking
     */
    public static CacheLocations getInstance(Context context) {
        if (instance == null) {
            synchronized (lock) {
                if (instance == null) {
                    instance = new CacheLocations(context.getApplicationContext());
                }
            }
        }
        return instance;
    }

    public void cacheLocations(List<Location> locations) {
        Map<String,Location> locationMap;
        try {
            locationMap = new ConcurrentHashMap<>();
            Map<String,Location> cachedMap = getCachedLocations();
            if(!cachedMap.isEmpty()) {
                locations.addAll(cachedMap.values());
            }

            for(Location loc : locations) {
                locationMap.put(loc.getUserId() + "_" + loc.getInsertionTimestamp(), loc);
            }
            String json = gson.toJson(locationMap);
            preferences.edit()
                    .putString(KEY_LOCATIONS, json)
                    .apply();
            Log.d(TAG, "Cached " + locations.size() + " locations");
        } catch (Exception e) {
            Log.e(TAG, "Error caching locations", e);
        }
    }

    public Map<String,Location> getCachedLocations() {
        Map<String,Location> locationMap = new ConcurrentHashMap<>();
        try {
            String json = preferences.getString(KEY_LOCATIONS, null);
            if (json != null) {
                Type listType = new TypeToken<Map<String,Location>>(){}.getType();
                locationMap = gson.fromJson(json, listType);
                Log.d(TAG, "Retrieved " + locationMap.size() + " cached locations");
                return locationMap;
            }
        } catch (Exception e) {
            Log.e(TAG, "Error retrieving cached locations", e);
        }
        return locationMap;
    }

    public void clearCache() {
        preferences.edit()
                .remove(KEY_LOCATIONS)
                .apply();
        Log.d(TAG, "Cache cleared");
    }
}