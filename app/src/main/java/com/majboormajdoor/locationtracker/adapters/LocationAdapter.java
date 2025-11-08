package com.majboormajdoor.locationtracker.adapters;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.majboormajdoor.locationtracker.R;
import com.majboormajdoor.locationtracker.dto.Location;
import com.majboormajdoor.locationtracker.fragments.CloudFragment;
import com.majboormajdoor.locationtracker.utils.CacheLocations;

import java.util.ArrayList;
import java.util.List;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.stream.Collectors;

/**
 * RecyclerView adapter for displaying location history
 */
public class LocationAdapter extends RecyclerView.Adapter<LocationAdapter.LocationViewHolder> {

    private List<Location> locationList;
    private SimpleDateFormat dateFormat;

    private CacheLocations cacher;

    public LocationAdapter(Context context) {
        this.locationList = new ArrayList<>();
        this.cacher = CacheLocations.getInstance(context);
        this.dateFormat = new SimpleDateFormat("MMM dd, yyyy HH:mm", Locale.getDefault());
    }

    @NonNull
    @Override
    public LocationViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_location, parent, false);
        return new LocationViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull LocationViewHolder holder, int position) {
        Location location = locationList.get(position);
        holder.bind(location);
    }

    @Override
    public int getItemCount() {
        return locationList.size();
    }

    public void updateLocations(List<Location> newLocations, CloudFragment.ShowContentCallback callback) {
        this.cacher.cacheLocations(newLocations);
        this.locationList.clear();
        for(Location loc: this.cacher.getCachedLocations().values()){
            this.locationList.add(loc);
        }
        this.locationList = this.locationList.stream().sorted((l1, l2) -> {
            String t1 = l1.getInsertionTimestamp();
            String t2 = l2.getInsertionTimestamp();
            if(t1 == null) t1 = "";
            if(t2 == null) t2 = "";
            return t2.compareTo(t1);
        }).collect(Collectors.toList());
        if(this.locationList.isEmpty()){
            callback.showEmptyState();
        }
        notifyDataSetChanged();
    }

    public void clearLocations() {
        this.locationList.clear();
        notifyDataSetChanged();
    }

    /**
     * Opens Google Maps with the specified location coordinates
     */
    private void openLocationInGoogleMaps(Context context, double latitude, double longitude) {
        try {
            // Create a URI for Google Maps with the coordinates
            String uri = String.format(Locale.ENGLISH, "geo:%f,%f?q=%f,%f", latitude, longitude, latitude, longitude);
            Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(uri));
            intent.setPackage("com.google.android.apps.maps");

            // Check if Google Maps is installed
            if (intent.resolveActivity(context.getPackageManager()) != null) {
                context.startActivity(intent);
            } else {
                // Fallback to web version of Google Maps
                String webUri = String.format(Locale.ENGLISH, "https://www.google.com/maps?q=%f,%f", latitude, longitude);
                Intent webIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(webUri));
                context.startActivity(webIntent);
            }
        } catch (Exception e) {
            Toast.makeText(context, "Unable to open Google Maps", Toast.LENGTH_SHORT).show();
        }
    }

    class LocationViewHolder extends RecyclerView.ViewHolder {
        private TextView tvLatitude;
        private TextView tvLongitude;
        private TextView tvTimestamp;

        private Button tvButton;
        private TextView tvEmail;

        public LocationViewHolder(@NonNull View itemView) {
            super(itemView);
            tvLatitude = itemView.findViewById(R.id.tv_latitude);
            tvLongitude = itemView.findViewById(R.id.tv_longitude);
            tvTimestamp = itemView.findViewById(R.id.tv_timestamp);
            tvButton = itemView.findViewById(R.id.btn_show_on_map);

        }



        public void bind(Location location) {
            tvLatitude.setText(String.format(Locale.getDefault(), "Lat: %.6f", location.getLatitude()));
            tvLongitude.setText(String.format(Locale.getDefault(), "Lng: %.6f", location.getLongitude()));

            // Format timestamp if available
            if (location.getInsertionTimestamp() != null && !location.getInsertionTimestamp().isEmpty()) {
                try {
                    // Assuming timestamp is in ISO format or epoch time
                    if (location.getInsertionTimestamp().matches("\\d+")) {
                        // Epoch time
                        long timestamp = Long.parseLong(location.getInsertionTimestamp());
                        Date date = new Date(timestamp);
                        tvTimestamp.setText(dateFormat.format(date));
                    } else {
                        // ISO format or other string format
                        tvTimestamp.setText(location.getInsertionTimestamp());
                    }
                } catch (Exception e) {
                    tvTimestamp.setText(location.getInsertionTimestamp());
                }
            } else {
                tvTimestamp.setText("No timestamp");
            }


            // Set up the "Show on Map" button click listener - THIS WAS MISSING!
            tvButton.setOnClickListener(v -> {
                Context context = itemView.getContext();
                if (location.getLatitude() != 0.0 && location.getLongitude() != 0.0) {
                    openLocationInGoogleMaps(context, location.getLatitude(), location.getLongitude());
                } else {
                    Toast.makeText(context, "Invalid coordinates for this location", Toast.LENGTH_SHORT).show();
                }
            });
        }
    }
}
