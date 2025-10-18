package com.ecommerce.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Service for validating if coordinates are on or near a road using Google Maps Roads API
 */
@Service
@Slf4j
public class RoadValidationService {

    // Maximum acceptable distance from road in meters
    private static final double MAX_DISTANCE_FROM_ROAD_METERS = 15.0;

    @Value("${google.maps.api.key:}")
    private String googleMapsApiKey;

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;
    
    public RoadValidationService() {
        this.restTemplate = new RestTemplate();
        this.objectMapper = new ObjectMapper();
    }

    /**
     * Validates if the given coordinates are on or near a road
     * Uses Google Maps Roads API - Snap to Roads feature
     * 
     * @param latitude The latitude coordinate
     * @param longitude The longitude coordinate
     * @throws IllegalArgumentException if coordinates are not on/near a road
     */
    public void validateRoadLocation(Double latitude, Double longitude) {
        log.info("Road validation started for coordinates: lat={}, lng={}", latitude, longitude);
        
        if (latitude == null || longitude == null) {
            throw new IllegalArgumentException("Latitude and longitude are required");
        }
        log.info("The google maps key is" + googleMapsApiKey);
        log.info("Google Maps API key status: {}", 
                (googleMapsApiKey != null && !googleMapsApiKey.trim().isEmpty()) ? "CONFIGURED" : "NOT CONFIGURED");
        
        if (googleMapsApiKey != null && googleMapsApiKey.length() > 10) {
            log.info("API Key (first 10 chars): {}...", googleMapsApiKey.substring(0, 10));
        }

        if (googleMapsApiKey == null || googleMapsApiKey.trim().isEmpty()) {
            log.warn("Google Maps API key not configured. Skipped the road validation.");
            return;
        }

        try {
            log.info("Calling Google Maps Roads API...");
            
            String url = UriComponentsBuilder
                    .fromHttpUrl("https://roads.googleapis.com/v1/snapToRoads")
                    .queryParam("path", latitude + "," + longitude)
                    .queryParam("interpolate", "false")
                    .queryParam("key", googleMapsApiKey)
                    .toUriString();

            log.info("API URL: {}", url.replace(googleMapsApiKey, "***KEY***"));

            String response = restTemplate.getForObject(url, String.class);
            
            if (response == null || response.trim().isEmpty()) {
                log.warn("Empty response from Google Maps Roads API. Trying Geocoding API fallback...");
                validateWithGeocodingAPI(latitude, longitude);
                return;
            }

            log.info("Received response from Google Maps Roads API: {}", response);
            JsonNode jsonResponse = objectMapper.readTree(response);
            
            // Check for API errors
            JsonNode error = jsonResponse.get("error");
            if (error != null) {
                String errorMessage = error.get("message").asText();
                log.warn("Roads API returned error: {}. Trying Geocoding API fallback...", errorMessage);
                validateWithGeocodingAPI(latitude, longitude);
                return;
            }
            
            JsonNode snappedPoints = jsonResponse.get("snappedPoints");
            
            if (snappedPoints == null || snappedPoints.isEmpty()) {
                log.warn("No road found near coordinates via Roads API: lat={}, lng={}. Trying Geocoding API fallback...", 
                        latitude, longitude);
                validateWithGeocodingAPI(latitude, longitude);
                return;
            }

            JsonNode firstPoint = snappedPoints.get(0);
            JsonNode location = firstPoint.get("location");
            
            if (location == null) {
                log.warn("Invalid response structure from Roads API. Trying Geocoding API fallback...");
                validateWithGeocodingAPI(latitude, longitude);
                return;
            }

            double snappedLat = location.get("latitude").asDouble();
            double snappedLng = location.get("longitude").asDouble();

            double distance = calculateDistance(latitude, longitude, snappedLat, snappedLng);

            log.info("Distance to nearest road: {} meters", distance);

            if (distance > MAX_DISTANCE_FROM_ROAD_METERS) {
                String errorMsg = String.format(
                    "Your pickup location is %.1f meters away from the nearest road (maximum allowed: %.1f meters). " +
                    "Please select a location closer to a road or street for pickup.",
                    distance, MAX_DISTANCE_FROM_ROAD_METERS
                );
                log.error("Road validation failed: {}", errorMsg);
                throw new IllegalArgumentException(errorMsg);
            }

            log.info("Road validation successful via Roads API! Distance to road: {} meters", distance);

        } catch (IllegalArgumentException e) {
            throw e;
        } catch (Exception e) {
            log.error("Error validating road location: {}", e.getMessage(), e);
            log.warn("Road validation failed due to technical error. Trying Geocoding API fallback...");
            try {
                validateWithGeocodingAPI(latitude, longitude);
            } catch (IllegalArgumentException iae) {
                throw iae;
            } catch (Exception fallbackException) {
                log.error("Geocoding API fallback also failed: {}", fallbackException.getMessage());
                // Allow request to proceed only if both APIs fail due to technical issues
                log.warn("Allowing request to proceed due to technical errors in validation services.");
            }
        }
    }

    /**
     * Calculate distance between two coordinates using Haversine formula
     * Returns distance in meters
     */
    private double calculateDistance(double lat1, double lon1, double lat2, double lon2) {
        final int EARTH_RADIUS = 6371000; // meters

        double dLat = Math.toRadians(lat2 - lat1);
        double dLon = Math.toRadians(lon2 - lon1);

        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                * Math.sin(dLon / 2) * Math.sin(dLon / 2);

        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));

        return EARTH_RADIUS * c;
    }

    /**
     * Fallback validation using Google Maps Geocoding API
     * Checks if location is accessible and validates distance to nearest road
     */
    private void validateWithGeocodingAPI(Double latitude, Double longitude) {
        try {
            log.info("Using Geocoding API fallback for validation...");

            String url = UriComponentsBuilder
                    .fromHttpUrl("https://maps.googleapis.com/maps/api/geocode/json")
                    .queryParam("latlng", latitude + "," + longitude)
                    .queryParam("result_type", "street_address|route|premise")
                    .queryParam("key", googleMapsApiKey)
                    .toUriString();

            log.info("Geocoding API URL: {}", url.replace(googleMapsApiKey, "***KEY***"));

            String response = restTemplate.getForObject(url, String.class);
            
            if (response == null || response.trim().isEmpty()) {
                log.warn("Empty response from Geocoding API. Cannot validate location.");
                throw new IllegalArgumentException(
                    "Unable to validate your location. Please ensure you're selecting a location on or near a road."
                );
            }

            log.info("Received response from Geocoding API");
            JsonNode jsonResponse = objectMapper.readTree(response);
            
            // Check API status
            String status = jsonResponse.get("status").asText();
            log.info("Geocoding API status: {}", status);
            
            if ("ZERO_RESULTS".equals(status)) {
                log.error("No address found at coordinates. Location is not accessible.");
                throw new IllegalArgumentException(
                    "Your selected location is not accessible by road. Please choose a pickup point on or near a street."
                );
            }
            
            if (!"OK".equals(status)) {
                log.warn("Geocoding API returned status: {}. Cannot validate location.", status);
                throw new IllegalArgumentException(
                    "Unable to validate your location. Please try selecting a different pickup point on a road."
                );
            }
            
            JsonNode results = jsonResponse.get("results");

            if (results == null || results.isEmpty()) {
                log.error("No geocoding results found for coordinates.");
                throw new IllegalArgumentException(
                    "Your location could not be verified. Please select a pickup point on or near a road."
                );
            }

            // Get the first (most relevant) result
            JsonNode firstResult = results.get(0);
            JsonNode types = firstResult.get("types");
            
            // Check if location has road-related types
            boolean isRoadRelated = false;
            if (types != null) {
                StringBuilder typesList = new StringBuilder();
                for (JsonNode type : types) {
                    String typeStr = type.asText();
                    typesList.append(typeStr).append(", ");
                    
                    // Check for road-related types
                    if (typeStr.equals("street_address") || 
                        typeStr.equals("route") || 
                        typeStr.equals("premise") ||
                        typeStr.equals("intersection")) {
                        isRoadRelated = true;
                    }
                }
                log.info("Location types: {}", typesList.toString());
            }
            
            JsonNode geometry = firstResult.get("geometry");
            if (geometry != null) {
                JsonNode location = geometry.get("location");
                if (location != null) {
                    double geocodedLat = location.get("lat").asDouble();
                    double geocodedLng = location.get("lng").asDouble();
                    
                    double distance = calculateDistance(latitude, longitude, geocodedLat, geocodedLng);
                    log.info("Distance from geocoded address to selected point: {} meters", distance);
                    
                    if (distance > MAX_DISTANCE_FROM_ROAD_METERS) {
                        String errorMsg = String.format(
                            "Your pickup location is approximately %.1f meters away from the nearest accessible road " +
                            "(maximum allowed: %.1f meters). Please change your pickup point to a location on or closer to a road.",
                            distance, MAX_DISTANCE_FROM_ROAD_METERS
                        );
                        log.error("Geocoding validation failed: {}", errorMsg);
                        throw new IllegalArgumentException(errorMsg);
                    }
                }
            }
            
            // Additional check for address components
            JsonNode addressComponents = firstResult.get("address_components");
            if (addressComponents == null || addressComponents.isEmpty()) {
                log.error("Location has no address components - not accessible.");
                throw new IllegalArgumentException(
                    "Your selected location does not appear to be accessible by road. Please choose a different pickup point."
                );
            }
            
            log.info("Location is addressable with {} address components.", addressComponents.size());
            
            if (!isRoadRelated) {
                log.warn("Location types don't clearly indicate road access, but within acceptable distance.");
            }
            
            log.info("Geocoding validation successful! Location is accessible and within acceptable distance from road.");

        } catch (IllegalArgumentException e) {
            throw e;
        } catch (Exception e) {
            log.error("Error in Geocoding API fallback: {}", e.getMessage(), e);
            throw new IllegalArgumentException(
                "Unable to validate your location due to a technical error. Please try again or select a different pickup point near the main road."
            );
        }
    }
}