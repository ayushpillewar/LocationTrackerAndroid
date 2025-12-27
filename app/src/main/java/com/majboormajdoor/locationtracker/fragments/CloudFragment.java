package com.majboormajdoor.locationtracker.fragments;

import android.app.DatePickerDialog;
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

import com.google.android.material.button.MaterialButton;
import com.majboormajdoor.locationtracker.R;
import com.majboormajdoor.locationtracker.adapters.LocationAdapter;
import com.majboormajdoor.locationtracker.dto.Location;
import com.majboormajdoor.locationtracker.services.ApiService;
import com.majboormajdoor.locationtracker.utils.CacheLocations;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;
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
    private MaterialButton btnSelectDate;
    private MaterialButton btnClearFilter;
    private TextView tvSelectedDate;

    // Data and Services
    private LocationAdapter locationAdapter;
    private ApiService apiService;
    private Handler mainHandler;

    // Filter data
    private List<Location> allLocationsList;
    private String selectedDateFilter;
    private SimpleDateFormat dateFormat;
    private SimpleDateFormat displayDateFormat;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
            @Nullable Bundle savedInstanceState) {
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
        List<Location> locations = new ArrayList<>(
                CacheLocations.getInstance(getContext()).getCachedLocations().values());
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
        btnSelectDate = view.findViewById(R.id.btn_select_date);
        btnClearFilter = view.findViewById(R.id.btn_clear_filter);
        tvSelectedDate = view.findViewById(R.id.tv_selected_date);

        // Initialize date formatters
        dateFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
        displayDateFormat = new SimpleDateFormat("MMM dd, yyyy", Locale.getDefault());
        allLocationsList = new ArrayList<>();
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

        btnSelectDate.setOnClickListener(v -> showDatePicker());

        btnClearFilter.setOnClickListener(v -> clearDateFilter());
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
                // Store all locations for filtering
                allLocationsList = new ArrayList<>(locations);

                // Apply filter if one is selected
                List<Location> displayLocations = locations;
                if (selectedDateFilter != null && !selectedDateFilter.isEmpty()) {
                    displayLocations = filterLocationsByDate(locations, selectedDateFilter);
                } else {
                    displayLocations = allLocationsList;
                }

                Log.d(TAG, "Displaying " + displayLocations.size() + " location records");
                recyclerViewLocations.setVisibility(View.VISIBLE);
                emptyStateLayout.setVisibility(View.GONE);
                errorStateLayout.setVisibility(View.GONE);
                locationAdapter.updateLocations(displayLocations, CloudFragment.this::showEmptyState);
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
        Log.d(TAG,
                "Successfully fetched " + (locationHistory != null ? locationHistory.size() : 0) + " location records");

        showContent(locationHistory);
    }

    @Override
    public void onError(String error) {
        Log.e(TAG, "Error fetching location history: " + error);
        showError("Error fetching location history");
    }

    /**
     * Show date picker dialog
     */
    private void showDatePicker() {
        Calendar calendar = Calendar.getInstance();
        int year = calendar.get(Calendar.YEAR);
        int month = calendar.get(Calendar.MONTH);
        int day = calendar.get(Calendar.DAY_OF_MONTH);

        DatePickerDialog datePickerDialog = new DatePickerDialog(
                requireContext(),
                (view, selectedYear, selectedMonth, selectedDay) -> {
                    Calendar selectedDate = Calendar.getInstance();
                    selectedDate.set(selectedYear, selectedMonth, selectedDay);

                    selectedDateFilter = dateFormat.format(selectedDate.getTime());
                    String displayDate = displayDateFormat.format(selectedDate.getTime());

                    // Update UI
                    tvSelectedDate.setText("Showing: " + displayDate);
                    tvSelectedDate.setVisibility(View.VISIBLE);
                    btnClearFilter.setVisibility(View.VISIBLE);

                    // Apply filter
                    applyDateFilter();
                },
                year, month, day);

        datePickerDialog.show();
    }

    /**
     * Apply the selected date filter
     */
    private void applyDateFilter() {
        if (allLocationsList != null && !allLocationsList.isEmpty()) {
            List<Location> filteredLocations = filterLocationsByDate(allLocationsList, selectedDateFilter);
            Log.d(TAG, "Filtered to " + filteredLocations.size() + " locations for date: " + selectedDateFilter);

            if (filteredLocations.isEmpty()) {
                showEmptyState();
            } else {
                mainHandler.post(() -> {
                    recyclerViewLocations.setVisibility(View.VISIBLE);
                    emptyStateLayout.setVisibility(View.GONE);
                    errorStateLayout.setVisibility(View.GONE);
                    locationAdapter.updateFilteredLocations(filteredLocations, CloudFragment.this::showEmptyState);
                });
            }
        }
    }

    /**
     * Filter locations by date
     */
    private List<Location> filterLocationsByDate(List<Location> locations, String dateFilter) {
        List<Location> filtered = new ArrayList<>();

        for (Location location : locations) {
            String timestamp = location.getInsertionTimestamp();
            if (timestamp != null && !timestamp.isEmpty()) {
                try {
                    // Extract date part from timestamp (assuming format like "2025-12-17 14:30:00")
                    String locationDate;
                    if (timestamp.contains(" ")) {
                        locationDate = timestamp.split(" ")[0];
                    } else if (timestamp.length() >= 10) {
                        locationDate = timestamp.substring(0, 10);
                    } else {
                        continue;
                    }

                    if (locationDate.equals(dateFilter)) {
                        filtered.add(location);
                    }
                } catch (Exception e) {
                    Log.e(TAG, "Error parsing timestamp: " + timestamp, e);
                }
            }
        }

        return filtered;
    }

    /**
     * Clear the date filter and show all records
     */
    private void clearDateFilter() {
        selectedDateFilter = null;
        tvSelectedDate.setVisibility(View.GONE);
        btnClearFilter.setVisibility(View.GONE);

        // Show all locations
        if (allLocationsList != null && !allLocationsList.isEmpty()) {
            Log.d(TAG, "Clearing filter, showing all " + allLocationsList.size() + " locations");
            mainHandler.post(() -> {
                recyclerViewLocations.setVisibility(View.VISIBLE);
                emptyStateLayout.setVisibility(View.GONE);
                errorStateLayout.setVisibility(View.GONE);
                locationAdapter.updateLocations(allLocationsList, CloudFragment.this::showEmptyState);
            });
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        // Refresh data when fragment becomes visible
        if (locationAdapter != null && locationAdapter.getItemCount() == 0) {
            List<Location> locations = new ArrayList<>(
                    CacheLocations.getInstance(getContext()).getCachedLocations().values());
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
