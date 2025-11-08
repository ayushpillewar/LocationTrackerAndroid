package com.majboormajdoor.locationtracker.fragments;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.majboormajdoor.locationtracker.R;
import com.majboormajdoor.locationtracker.adapters.LocationAdapter;
import com.majboormajdoor.locationtracker.dto.Location;
import com.majboormajdoor.locationtracker.services.ApiService;
import com.majboormajdoor.locationtracker.utils.CacheLocations;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Fragment for displaying location history from backend
 * Fetches and displays location data in a RecyclerView
 */
public class CloudFragment extends Fragment implements ApiService.LocationHistoryCallback {

    private static final String TAG = "CloudFragment";

    // UI Components
    private RecyclerView recyclerViewLocations;
    private ProgressBar progressBar;
    private LinearLayout emptyStateLayout;
    private LinearLayout errorStateLayout;
    private TextView tvErrorMessage;
    private Button btnRefresh;
    private Button btnRetry;

    // Data and Services
    private LocationAdapter locationAdapter;
    private ApiService apiService;
    private Handler mainHandler;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_cloud, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        initializeViews(view);
        setupRecyclerView();
        setupClickListeners();

        // Initialize services
        apiService = new ApiService(getContext());
        mainHandler = new Handler(Looper.getMainLooper());

        // Load location history on fragment creation
        List<Location> locations = new ArrayList<>(CacheLocations.getInstance(getContext()).getCachedLocations().values());
        onSuccess(locations);
    }

    private void initializeViews(View view) {
        recyclerViewLocations = view.findViewById(R.id.recycler_view_locations);
        progressBar = view.findViewById(R.id.progress_bar);
        emptyStateLayout = view.findViewById(R.id.empty_state_layout);
        errorStateLayout = view.findViewById(R.id.error_state_layout);
        tvErrorMessage = view.findViewById(R.id.tv_error_message);
        btnRefresh = view.findViewById(R.id.btn_refresh);
        btnRetry = view.findViewById(R.id.btn_retry);
    }

    private void setupRecyclerView() {
        locationAdapter = new LocationAdapter(getContext());
        recyclerViewLocations.setLayoutManager(new LinearLayoutManager(getContext()));
        recyclerViewLocations.setAdapter(locationAdapter);
    }

    private void setupClickListeners() {
        btnRefresh.setOnClickListener(v -> {
            Log.d(TAG, "Refresh button clicked");
            loadLocationHistory();
        });

        btnRetry.setOnClickListener(v -> {
            Log.d(TAG, "Retry button clicked");
            loadLocationHistory();
        });
    }

    private void loadLocationHistory() {
        Log.d(TAG, "Loading location history from backend");
        showLoading();

        if (apiService != null) {
            apiService.getLocationHistory(this, getContext());
        } else {
            Log.e(TAG, "ApiService is null");
            showError("Service not available. Please try again.");
        }
    }

    public interface ShowContentCallback {
        void showEmptyState();
    }


    private void showLoading() {
        mainHandler.post(() -> {
            progressBar.setVisibility(View.VISIBLE);
            recyclerViewLocations.setVisibility(View.GONE);
            emptyStateLayout.setVisibility(View.GONE);
            errorStateLayout.setVisibility(View.GONE);
            btnRefresh.setEnabled(false);
        });
    }

    private void showContent(List<Location> locations) {
        mainHandler.post(() -> {
            progressBar.setVisibility(View.GONE);
            btnRefresh.setEnabled(true);

            if (locations != null) {
                Log.d(TAG, "Displaying " + locations.size() + " location records");
                recyclerViewLocations.setVisibility(View.VISIBLE);
                emptyStateLayout.setVisibility(View.GONE);
                errorStateLayout.setVisibility(View.GONE);
                locationAdapter.updateLocations(locations, CloudFragment.this::showEmptyState);
            }

        });
    }

    private void showEmptyState() {
        mainHandler.post(() -> {
            progressBar.setVisibility(View.GONE);
            recyclerViewLocations.setVisibility(View.GONE);
            emptyStateLayout.setVisibility(View.VISIBLE);
            errorStateLayout.setVisibility(View.GONE);
            btnRefresh.setEnabled(true);
        });
    }

    private void showError(String errorMessage) {
        mainHandler.post(() -> {
            progressBar.setVisibility(View.GONE);
            recyclerViewLocations.setVisibility(View.GONE);
            emptyStateLayout.setVisibility(View.GONE);
            errorStateLayout.setVisibility(View.VISIBLE);
            btnRefresh.setEnabled(true);

            if (tvErrorMessage != null) {
                tvErrorMessage.setText(errorMessage);
            }

            // Also show a toast for immediate feedback
            if (getContext() != null) {
                Toast.makeText(getContext(), "Error: " + errorMessage, Toast.LENGTH_SHORT).show();
            }
        });
    }

    // ApiService.LocationHistoryCallback implementation
    @Override
    public void onSuccess(List<Location> locationHistory) {
        Log.d(TAG, "Successfully fetched " + (locationHistory != null ? locationHistory.size() : 0) + " location records");

        showContent(locationHistory);
    }

    @Override
    public void onError(String error) {
        Log.e(TAG, "Error fetching location history: " + error);
        showError("Error fetching location history");
    }

    @Override
    public void onResume() {
        super.onResume();
        // Refresh data when fragment becomes visible
        if (locationAdapter != null && locationAdapter.getItemCount() == 0) {
            List<Location> locations = new ArrayList<>(CacheLocations.getInstance(getContext()).getCachedLocations().values());
            onSuccess(locations);
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        // Clean up resources
        if (locationAdapter != null) {
            locationAdapter.clearLocations();
        }
    }
}
