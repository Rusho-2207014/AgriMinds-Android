package com.agriminds.utils;

import android.util.Log;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Executors;

public class TranslationManager {
    private static final String TAG = "TranslationManager";
    // Use MyMemory Translation API (FREE: 1000 requests/day, 5000/day with email)
    private static final String MYMEMORY_API_URL = "https://api.mymemory.translated.net/get";
    private static final String MYMEMORY_EMAIL = "agriminds@example.com"; // Optional: increases limit to 5000/day
    
    // In-memory cache to avoid re-translating the same text
    private static final Map<String, String> translationCache = new HashMap<>();
    
    // Rate limiting: Be respectful to free API
    private static long lastRequestTime = 0;
    private static final long MIN_REQUEST_INTERVAL = 1000; // 1 second between requests

    public TranslationManager() {
        // No initialization needed for API-based translation
    }

    public void translate(String text, OnTranslationListener listener) {
        if (text == null || text.trim().isEmpty()) {
            listener.onTranslationError("No text to translate");
            return;
        }

        // Clean and prepare text for better translation
        String cleanedText = cleanTextForTranslation(text);

        // Check if text is already in Bengali (contains Bengali Unicode characters)
        if (isBengali(cleanedText)) {
            Log.d(TAG, "Text is already in Bengali, skipping translation");
            listener.onTranslationSuccess(cleanedText);
            return;
        }

        // Check cache first before making API call
        synchronized (translationCache) {
            if (translationCache.containsKey(cleanedText)) {
                Log.d(TAG, "Translation found in cache for: " + cleanedText.substring(0, Math.min(50, cleanedText.length())));
                listener.onTranslationSuccess(translationCache.get(cleanedText));
                return;
            }
        }

        // Use MyMemory Translation API (FREE: 1000-5000 requests/day!)
        Executors.newSingleThreadExecutor().execute(() -> {
            try {
                String translatedText = translateWithMyMemoryAPI(cleanedText);
                
                // Store in cache
                synchronized (translationCache) {
                    translationCache.put(cleanedText, translatedText);
                    
                    // Limit cache size to prevent memory issues
                    if (translationCache.size() > 100) {
                        String firstKey = translationCache.keySet().iterator().next();
                        translationCache.remove(firstKey);
                    }
                }
                
                listener.onTranslationSuccess(translatedText);
            } catch (Exception e) {
                Log.e(TAG, "Translation failed: " + e.getMessage(), e);
                listener.onTranslationError("Translation error: " + e.getMessage());
            }
        });
    }

    /**
     * Synchronous translation method for use in background threads
     * @param text English text to translate
     * @return Bengali translation or original text if already Bengali
     */
    public String translateToBengali(String text) {
        if (text == null || text.trim().isEmpty()) {
            return text;
        }

        // Clean and prepare text for better translation
        String cleanedText = cleanTextForTranslation(text);

        // Check if text is already in Bengali (contains Bengali Unicode characters)
        if (isBengali(cleanedText)) {
            Log.d(TAG, "Text is already in Bengali, skipping translation");
            return cleanedText;
        }

        // Check cache first
        synchronized (translationCache) {
            if (translationCache.containsKey(cleanedText)) {
                Log.d(TAG, "Translation found in cache for: " + cleanedText.substring(0, Math.min(50, cleanedText.length())));
                return translationCache.get(cleanedText);
            }
        }

        try {
            String translatedText = translateWithMyMemoryAPI(cleanedText);
            
            // Store in cache
            synchronized (translationCache) {
                translationCache.put(cleanedText, translatedText);
                
                // Limit cache size to prevent memory issues
                if (translationCache.size() > 100) {
                    String firstKey = translationCache.keySet().iterator().next();
                    translationCache.remove(firstKey);
                }
            }
            
            return translatedText;
        } catch (Exception e) {
            Log.e(TAG, "Translation failed: " + e.getMessage(), e);
            return text; // Return original text on error
        }
    }

    private String translateWithMyMemoryAPI(String text) throws Exception {
        // Rate limiting: wait if needed
        synchronized (TranslationManager.class) {
            long now = System.currentTimeMillis();
            long timeSinceLastRequest = now - lastRequestTime;
            
            if (timeSinceLastRequest < MIN_REQUEST_INTERVAL) {
                long waitTime = MIN_REQUEST_INTERVAL - timeSinceLastRequest;
                try {
                    Thread.sleep(waitTime);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }
            
            lastRequestTime = System.currentTimeMillis();
        }
        
        // URL encode the text
        String encodedText = java.net.URLEncoder.encode(text, "UTF-8");
        
        // Build API URL with optional email for higher quota
        String apiUrl = MYMEMORY_API_URL + "?q=" + encodedText + "&langpair=en|bn";
        if (MYMEMORY_EMAIL != null && !MYMEMORY_EMAIL.isEmpty()) {
            apiUrl += "&de=" + java.net.URLEncoder.encode(MYMEMORY_EMAIL, "UTF-8");
        }
        
        Log.d(TAG, "Sending translation request to MyMemory: " + text);
        
        URL url = new URL(apiUrl);
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        connection.setRequestMethod("GET");
        connection.setConnectTimeout(15000);
        connection.setReadTimeout(15000);

        // Read response
        int responseCode = connection.getResponseCode();
        Log.d(TAG, "Response code: " + responseCode);

        if (responseCode == HttpURLConnection.HTTP_OK) {
            BufferedReader in = new BufferedReader(new InputStreamReader(connection.getInputStream(), "UTF-8"));
            StringBuilder response = new StringBuilder();
            String line;
            while ((line = in.readLine()) != null) {
                response.append(line);
            }
            in.close();

            // Parse JSON response
            JSONObject jsonResponse = new JSONObject(response.toString());
            JSONObject responseData = jsonResponse.getJSONObject("responseData");
            String translatedText = responseData.getString("translatedText").trim();

            Log.d(TAG, "Translation successful: " + text + " -> " + translatedText);
            return translatedText;
        } else {
            // Read error response
            BufferedReader errorReader = new BufferedReader(
                    new InputStreamReader(connection.getErrorStream(), "UTF-8"));
            StringBuilder errorResponse = new StringBuilder();
            String line;
            while ((line = errorReader.readLine()) != null) {
                errorResponse.append(line);
            }
            errorReader.close();

            Log.e(TAG, "API Error Response: " + errorResponse.toString());
            throw new Exception("HTTP error " + responseCode + ": " + errorResponse.toString());
        }
    }

    private boolean isBengali(String text) {
        // Check if text contains Bengali Unicode characters (০-৯, অ-ৰ)
        return text.matches(".*[\\u0980-\\u09FF]+.*");
    }

    private String cleanTextForTranslation(String text) {
        if (text == null)
            return "";

        // Remove extra whitespaces
        String cleaned = text.replaceAll("\\s+", " ").trim();

        // Remove [Voice Answer] placeholder if present
        cleaned = cleaned.replace("[Voice Answer]", "");

        // Ensure proper sentence ending
        if (!cleaned.isEmpty() && !cleaned.matches(".*[.!?]$")) {
            cleaned += ".";
        }

        return cleaned;
    }

    public void close() {
        // No resources to close for API-based translation
    }

    public interface OnTranslationListener {
        void onTranslationSuccess(String translatedText);

        void onTranslationError(String error);
    }
}
