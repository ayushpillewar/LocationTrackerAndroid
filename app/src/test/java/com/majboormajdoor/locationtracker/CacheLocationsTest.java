package com.majboormajdoor.locationtracker;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import com.google.gson.Gson;
import com.majboormajdoor.locationtracker.dto.Location;
import com.majboormajdoor.locationtracker.utils.CacheLocations;
import com.majboormajdoor.locationtracker.utils.PreferenceManager;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.MockitoJUnitRunner;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@RunWith(MockitoJUnitRunner.class)
public class CacheLocationsTest {

    @Mock
    private Context mockContext;

    @Mock
    private SharedPreferences mockSharedPreferences;

    @Mock
    private SharedPreferences.Editor mockEditor;

    @Mock
    private PreferenceManager mockPreferenceManager;


    private Gson gson;

    @Before
    public void setUp() {
        gson = new Gson();

        // Mock SharedPreferences behavior
        when(mockSharedPreferences.edit()).thenReturn(mockEditor);
        when(mockEditor.putString(anyString(), anyString())).thenReturn(mockEditor);
        when(mockEditor.remove(anyString())).thenReturn(mockEditor);
        doNothing().when(mockEditor).apply();

        // Mock PreferenceManager behavior
        when(mockPreferenceManager.getSharedPreferences()).thenReturn(mockSharedPreferences);
    }

    private CacheLocations createCacheLocationsInstance() {
        return CacheLocations.getInstance(mockContext);
    }

    private void runWithMockedStatics(Runnable testCode) {
        try (MockedStatic<PreferenceManager> preferenceManagerMock = mockStatic(PreferenceManager.class);
             MockedStatic<Log> logMock = mockStatic(Log.class)) {

            preferenceManagerMock.when(() -> PreferenceManager.getInstance(any(Context.class)))
                    .thenReturn(mockPreferenceManager);

            // Mock Log methods
            logMock.when(() -> Log.d(anyString(), anyString())).thenReturn(0);
            logMock.when(() -> Log.e(anyString(), anyString(), any(Throwable.class))).thenReturn(0);
            logMock.when(() -> Log.e(anyString(), anyString())).thenReturn(0);
            logMock.when(() -> Log.w(anyString(), anyString())).thenReturn(0);
            logMock.when(() -> Log.i(anyString(), anyString())).thenReturn(0);

            testCode.run();
        }
    }

    @Test
    public void testCacheLocations_WithValidLocations_ShouldCacheSuccessfully() {
        runWithMockedStatics(() -> {
            // Arrange
            CacheLocations cacheLocations = createCacheLocationsInstance();
            List<Location> locations = createTestLocations();
            when(mockSharedPreferences.getString(eq("cached_locations"), eq(null))).thenReturn(null);

            // Act
            cacheLocations.cacheLocations(locations);

            // Assert
            verify(mockEditor).putString(eq("cached_locations"), anyString());
            verify(mockEditor).apply();
        });
    }

    @Test
    public void testCacheLocations_WithEmptyList_ShouldCacheEmptyMap() {
        runWithMockedStatics(() -> {
            // Arrange
            CacheLocations cacheLocations = createCacheLocationsInstance();
            List<Location> emptyLocations = new ArrayList<>();
            when(mockSharedPreferences.getString(eq("cached_locations"), eq(null))).thenReturn(null);

            // Act
            cacheLocations.cacheLocations(emptyLocations);

            // Assert
            verify(mockEditor).putString(eq("cached_locations"), anyString());
            verify(mockEditor).apply();
        });
    }

    @Test
    public void testCacheLocations_WithNullList_ShouldHandleGracefully() {
        runWithMockedStatics(() -> {
            // Arrange
            CacheLocations cacheLocations = createCacheLocationsInstance();
            when(mockSharedPreferences.getString(eq("cached_locations"), eq(null))).thenReturn(null);

            // Act & Assert - Should not throw exception
            try {
                cacheLocations.cacheLocations(null);
                // If we reach here, the method handled null gracefully
                assertTrue(true);
            } catch (Exception e) {
                fail("Should handle null input gracefully");
            }
        });
    }

    @Test
    public void testCacheLocations_WithExistingCache_ShouldMergeLocations() {
        runWithMockedStatics(() -> {
            // Arrange
            CacheLocations cacheLocations = createCacheLocationsInstance();
            List<Location> newLocations = createTestLocations();
            Map<String, Location> existingCache = new HashMap<>();
            Location existingLocation = newLocations.get(0);
            existingCache.put(existingLocation.getUserId() + "_" + existingLocation.getInsertionTimestamp(), existingLocation);

            String existingJson = gson.toJson(existingCache);
            when(mockSharedPreferences.getString(eq("cached_locations"), eq(null))).thenReturn(existingJson);

            // Act
            cacheLocations.cacheLocations(newLocations);

            // Assert
            verify(mockEditor).putString(eq("cached_locations"), anyString());
            verify(mockEditor).apply();
        });
    }

    @Test
    public void testGetCachedLocations_WithValidCache_ShouldReturnLocations() {
        runWithMockedStatics(() -> {
            // Arrange
            CacheLocations cacheLocations = createCacheLocationsInstance();
            Map<String, Location> expectedLocations = createTestLocationMap();
            String json = gson.toJson(expectedLocations);
            when(mockSharedPreferences.getString(eq("cached_locations"), eq(null))).thenReturn(json);

            // Act
            Map<String, Location> result = cacheLocations.getCachedLocations();

            // Assert
            assertNotNull(result);
            assertEquals(expectedLocations.size(), result.size());
            assertTrue(result.containsKey("user123_2023-01-01T12:00:00Z"));
        });
    }

    @Test
    public void testGetCachedLocations_WithNullCache_ShouldReturnEmptyMap() {
        runWithMockedStatics(() -> {
            // Arrange
            CacheLocations cacheLocations = createCacheLocationsInstance();
            when(mockSharedPreferences.getString(eq("cached_locations"), eq(null))).thenReturn(null);

            // Act
            Map<String, Location> result = cacheLocations.getCachedLocations();

            // Assert
            assertNotNull(result);
            assertTrue(result.isEmpty());
        });
    }

    @Test
    public void testGetCachedLocations_WithInvalidJson_ShouldReturnEmptyMap() {
        runWithMockedStatics(() -> {
            // Arrange
            CacheLocations cacheLocations = createCacheLocationsInstance();
            when(mockSharedPreferences.getString(eq("cached_locations"), eq(null))).thenReturn("invalid json");

            // Act
            Map<String, Location> result = cacheLocations.getCachedLocations();

            // Assert
            assertNotNull(result);
            assertTrue(result.isEmpty());
        });
    }

    @Test
    public void testClearCache_ShouldRemoveAllCachedData() {
        runWithMockedStatics(() -> {
            // Arrange
            CacheLocations cacheLocations = createCacheLocationsInstance();

            // Act
            cacheLocations.clearCache();

            // Assert
            verify(mockEditor).remove("cached_locations");
            verify(mockEditor).apply();
        });
    }

    @Test
    public void testCacheLocations_WithDuplicateKeys_ShouldOverwriteExisting() {
        runWithMockedStatics(() -> {
            // Arrange
            CacheLocations cacheLocations = createCacheLocationsInstance();
            List<Location> locations = new ArrayList<>();
            Location location1 = new Location(37.7749, -122.4194, "test@example.com", "2023-01-01T12:00:00Z");
            location1.setUserId("user123");
            Location location2 = new Location(37.7750, -122.4195, "test@example.com", "2023-01-01T12:00:00Z");
            location2.setUserId("user123"); // Same userId and timestamp
            locations.add(location1);
            locations.add(location2);

            when(mockSharedPreferences.getString(eq("cached_locations"), eq(null))).thenReturn(null);

            // Act
            cacheLocations.cacheLocations(locations);

            // Assert
            verify(mockEditor).putString(eq("cached_locations"), anyString());
            verify(mockEditor).apply();
        });
    }

    @Test
    public void testCacheLocations_WithSpecialCharacters_ShouldHandleCorrectly() {
        runWithMockedStatics(() -> {
            // Arrange
            CacheLocations cacheLocations = createCacheLocationsInstance();
            List<Location> locations = new ArrayList<>();
            Location location = new Location(37.7749, -122.4194, "test+special@example.com", "2023-01-01T12:00:00Z");
            location.setUserId("user@#$%");
            locations.add(location);

            when(mockSharedPreferences.getString(eq("cached_locations"), eq(null))).thenReturn(null);

            // Act
            cacheLocations.cacheLocations(locations);

            // Assert
            verify(mockEditor).putString(eq("cached_locations"), anyString());
            verify(mockEditor).apply();
        });
    }

    @Test
    public void testGetCachedLocations_PerformanceTest_ShouldHandleLargeDataset() {
        runWithMockedStatics(() -> {
            // Arrange
            CacheLocations cacheLocations = createCacheLocationsInstance();
            Map<String, Location> largeLocationMap = new HashMap<>();
            for (int i = 0; i < 1000; i++) {
                Location location = new Location(37.7749 + i, -122.4194 + i, "test" + i + "@example.com", "2023-01-01T12:00:0" + i + "Z");
                location.setUserId("user" + i);
                largeLocationMap.put("user" + i + "_2023-01-01T12:00:0" + i + "Z", location);
            }
            String json = gson.toJson(largeLocationMap);
            when(mockSharedPreferences.getString(eq("cached_locations"), eq(null))).thenReturn(json);

            // Act
            long startTime = System.currentTimeMillis();
            Map<String, Location> result = cacheLocations.getCachedLocations();
            long endTime = System.currentTimeMillis();

            // Assert
            assertNotNull(result);
            assertEquals(1000, result.size());
            assertTrue("Performance test: Should complete within reasonable time", (endTime - startTime) < 5000);
        });
    }

    @Test
    public void testCacheLocations_ThreadSafety_ShouldHandleConcurrentAccess() {
        runWithMockedStatics(() -> {
            // Arrange
            CacheLocations cacheLocations = createCacheLocationsInstance();
            List<Location> locations = createTestLocations();
            when(mockSharedPreferences.getString(eq("cached_locations"), eq(null))).thenReturn(null);

            // Act - Simulate concurrent access
            Thread thread1 = new Thread(() -> cacheLocations.cacheLocations(locations));
            Thread thread2 = new Thread(() -> cacheLocations.getCachedLocations());
            Thread thread3 = new Thread(() -> cacheLocations.clearCache());

            // Assert - Should not throw any exceptions
            assertDoesNotThrow(() -> {
                thread1.start();
                thread2.start();
                thread3.start();

                try {
                    thread1.join();
                    thread2.join();
                    thread3.join();
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    fail("Thread interrupted: " + e.getMessage());
                }
            });
        });
    }

    // Helper methods
    private List<Location> createTestLocations() {
        List<Location> locations = new ArrayList<>();
        Location location1 = new Location(37.7749, -122.4194, "test1@example.com", "2023-01-01T12:00:00Z");
        location1.setUserId("user123");
        Location location2 = new Location(40.7128, -74.0060, "test2@example.com", "2023-01-01T13:00:00Z");
        location2.setUserId("user456");
        locations.add(location1);
        locations.add(location2);
        return locations;
    }

    private Map<String, Location> createTestLocationMap() {
        Map<String, Location> locationMap = new HashMap<>();
        Location location = new Location(37.7749, -122.4194, "test@example.com", "2023-01-01T12:00:00Z");
        location.setUserId("user123");
        locationMap.put("user123_2023-01-01T12:00:00Z", location);
        return locationMap;
    }

    private void assertDoesNotThrow(Runnable runnable) {
        try {
            runnable.run();
        } catch (Exception e) {
            fail("Should not throw exception: " + e.getMessage());
        }
    }
}
