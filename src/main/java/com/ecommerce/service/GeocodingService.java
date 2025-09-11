package com.ecommerce.service;

import com.ecommerce.dto.AddressDto;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.Map;

@Service
@Slf4j
public class GeocodingService {

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    public GeocodingService() {
        this.restTemplate = new RestTemplate();
        this.objectMapper = new ObjectMapper();
    }

    public Map<String, Double> getCoordinates(AddressDto address) {
        log.info("Attempting to geocode address: {} {}, {}, {}",
                address.getStreetAddress(), address.getCity(), address.getState(), address.getCountry());

        // Strategy 1: Try with full address including street, city, country
        Map<String, Double> coords = tryGeocodeWithStrategy(address, "full");
        if (coords != null) {
            return coords;
        }

        // Strategy 2: Try with just city and country (ignore street if it's malformed)
        log.info("Full address geocoding failed, trying city + country only");
        coords = tryGeocodeWithStrategy(address, "city_country");
        if (coords != null) {
            return coords;
        }

        // Strategy 3: Try with just country (fallback)
        log.info("City + country geocoding failed, trying country only");
        coords = tryGeocodeWithStrategy(address, "country");
        if (coords != null) {
            return coords;
        }

        // Strategy 4: Try with a more flexible search
        log.info("Country-only geocoding failed, trying flexible search");
        coords = tryGeocodeWithStrategy(address, "flexible");
        if (coords != null) {
            return coords;
        }

        log.error("All geocoding strategies failed for address: {} {}, {}, {}",
                address.getStreetAddress(), address.getCity(), address.getState(), address.getCountry());
        return null;
    }

    private Map<String, Double> tryGeocodeWithStrategy(AddressDto address, String strategy) {
        try {
            String url;

            switch (strategy) {
                case "full":
                    url = buildGeocodingUrl(address.getStreetAddress(), address.getCity(), address.getCountry());
                    break;
                case "city_country":
                    url = buildGeocodingUrl("", address.getCity(), address.getCountry());
                    break;
                case "country":
                    url = buildGeocodingUrl("", "", address.getCountry());
                    break;
                case "flexible":
                    // Try a more flexible search that might catch malformed addresses
                    String flexibleQuery = String.format("%s %s %s",
                            address.getStreetAddress(), address.getCity(), address.getCountry())
                            .trim().replaceAll("\\s+", "+");
                    url = String.format("https://nominatim.openstreetmap.org/search?q=%s&format=json&limit=1",
                            flexibleQuery);
                    break;
                default:
                    return null;
            }

            log.info("Geocoding strategy '{}' - URL: {}", strategy, url);
            String response = restTemplate.getForObject(url, String.class);
            log.info("Geocoding response: {}", response);

            JsonNode jsonNode = objectMapper.readTree(response);

            if (jsonNode.isArray() && jsonNode.size() > 0) {
                JsonNode firstResult = jsonNode.get(0);
                double lat = firstResult.get("lat").asDouble();
                double lon = firstResult.get("lon").asDouble();

                String displayName = firstResult.has("display_name") ? firstResult.get("display_name").asText()
                        : "Unknown";
                log.info("Strategy '{}' succeeded - Found coordinates for '{}': ({}, {})", strategy, displayName, lat,
                        lon);

                Map<String, Double> coordinates = new HashMap<>();
                coordinates.put("latitude", lat);
                coordinates.put("longitude", lon);
                return coordinates;
            } else {
                log.warn("Strategy '{}' - No coordinates found", strategy);
            }
        } catch (Exception e) {
            log.error("Strategy '{}' failed with error: {}", strategy, e.getMessage());
        }
        return null;
    }

    private String buildGeocodingUrl(String street, String city, String country) {
        StringBuilder url = new StringBuilder("https://nominatim.openstreetmap.org/search?");
        boolean hasParams = false;

        if (street != null && !street.trim().isEmpty()) {
            url.append("street=").append(street.replaceAll("\\s+", "+"));
            hasParams = true;
        }

        if (city != null && !city.trim().isEmpty()) {
            if (hasParams)
                url.append("&");
            url.append("city=").append(city.replaceAll("\\s+", "+"));
            hasParams = true;
        }

        if (country != null && !country.trim().isEmpty()) {
            if (hasParams)
                url.append("&");
            url.append("country=").append(country.replaceAll("\\s+", "+"));
            hasParams = true;
        }

        url.append("&format=json&limit=1");
        return url.toString();
    }
}
