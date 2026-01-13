package com.agriminds.ui.scancrop;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Base64;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.agriminds.R;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.concurrent.Executors;

public class ScanCropActivity extends AppCompatActivity {

    private static final int REQUEST_CAMERA = 100;
    private static final int REQUEST_IMAGE_CAPTURE = 101;
    private static final int REQUEST_GALLERY = 102;

    // API key - In production, move to BuildConfig or secure backend
    private String getApiKey() {
        // Rotate between multiple keys to avoid quota limits
        String[] keys = {
                "AIzaSyC90368I-saYiOPOd9DH5Ean3NNS1K8RQo",
                "AIzaSyAx-IxNj1EbbOAKIiD8PiTF6t6EhQml4zY",
                "AIzaSyC90368I-saYiOPOd9DH5Ean3NNS1K8RQo"
        };
        return keys[(int) (System.currentTimeMillis() / 1000) % keys.length];
    }

    private ImageView imageCrop;
    private Button btnTakePhoto, btnSelectGallery, btnAnalyze;
    private TextView textResult;
    private boolean hasImage = false;
    private Bitmap currentBitmap;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_scan_crop);

        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle("Scan Crop");
        }

        imageCrop = findViewById(R.id.image_crop);
        btnTakePhoto = findViewById(R.id.btn_take_photo);
        btnSelectGallery = findViewById(R.id.btn_select_gallery);
        btnAnalyze = findViewById(R.id.btn_analyze_crop);
        textResult = findViewById(R.id.text_crop_result);

        btnTakePhoto.setOnClickListener(v -> checkCameraPermission());
        btnSelectGallery.setOnClickListener(v -> openGallery());
        btnAnalyze.setOnClickListener(v -> analyzeCropWithGemini());
    }

    private void checkCameraPermission() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[] { Manifest.permission.CAMERA }, REQUEST_CAMERA);
        } else {
            openCamera();
        }
    }

    private void openCamera() {
        Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        try {
            startActivityForResult(takePictureIntent, REQUEST_IMAGE_CAPTURE);
        } catch (Exception e) {
            Toast.makeText(this, "Camera not available. Make sure camera app is installed.", Toast.LENGTH_LONG).show();
        }
    }

    private void openGallery() {
        Intent pickPhotoIntent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        pickPhotoIntent.setType("image/*");
        try {
            startActivityForResult(pickPhotoIntent, REQUEST_GALLERY);
        } catch (Exception e) {
            Toast.makeText(this, "Gallery not available", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
            @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_CAMERA) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                openCamera();
            } else {
                Toast.makeText(this, "Camera permission denied", Toast.LENGTH_SHORT).show();
            }
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == RESULT_OK) {
            if (requestCode == REQUEST_IMAGE_CAPTURE && data != null) {
                Bundle extras = data.getExtras();
                Bitmap imageBitmap = (Bitmap) extras.get("data");
                imageCrop.setImageBitmap(imageBitmap);
                currentBitmap = imageBitmap;
                hasImage = true;
                btnAnalyze.setEnabled(true);
                textResult.setVisibility(View.GONE);
            } else if (requestCode == REQUEST_GALLERY && data != null) {
                try {
                    android.net.Uri selectedImage = data.getData();
                    Bitmap bitmap = MediaStore.Images.Media.getBitmap(this.getContentResolver(), selectedImage);
                    imageCrop.setImageBitmap(bitmap);
                    currentBitmap = bitmap;
                    hasImage = true;
                    btnAnalyze.setEnabled(true);
                    textResult.setVisibility(View.GONE);
                } catch (Exception e) {
                    Toast.makeText(this, "Failed to load image", Toast.LENGTH_SHORT).show();
                }
            }
        }
    }

    private void analyzeCropWithGemini() {
        if (!hasImage || currentBitmap == null) {
            Toast.makeText(this, "Please take a photo first", Toast.LENGTH_SHORT).show();
            return;
        }

        textResult.setText("Analyzing image patterns...\nConnecting to AgriMinds AI Database...");
        textResult.setVisibility(View.VISIBLE);
        btnAnalyze.setEnabled(false);

        Executors.newSingleThreadExecutor().execute(() -> {
            try {
                android.util.Log.d("GeminiAI", "Starting AI analysis...");

                // Convert bitmap to base64
                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                currentBitmap.compress(Bitmap.CompressFormat.JPEG, 70, baos);
                byte[] imageBytes = baos.toByteArray();
                String base64Image = Base64.encodeToString(imageBytes, Base64.NO_WRAP);

                android.util.Log.d("GeminiAI", "Image encoded, size: " + imageBytes.length + " bytes");

                // Create JSON request
                JSONObject requestJson = new JSONObject();
                JSONArray contents = new JSONArray();
                JSONObject content = new JSONObject();
                JSONArray parts = new JSONArray();

                JSONObject textPart = new JSONObject();
                textPart.put("text",
                        "You are an agricultural expert. Analyze this crop/plant image and provide a structured response:\n"
                                +
                                "1. Disease Name\n" +
                                "2. Confidence Level\n" +
                                "3. Symptoms Observed\n" +
                                "4. Recommended Treatment\n" +
                                "5. Prevention");

                JSONObject imagePart = new JSONObject();
                JSONObject inlineData = new JSONObject();
                inlineData.put("mime_type", "image/jpeg");
                inlineData.put("data", base64Image);
                imagePart.put("inline_data", inlineData);

                parts.put(textPart);
                parts.put(imagePart);
                content.put("parts", parts);
                contents.put(content);
                requestJson.put("contents", contents);

                android.util.Log.d("GeminiAI", "Request JSON created");

                // Make API call
                URL url = new URL(
                        "https://generativelanguage.googleapis.com/v1beta/models/gemini-2.5-flash:generateContent?key="
                                + getApiKey());
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("POST");
                conn.setRequestProperty("Content-Type", "application/json");
                conn.setDoOutput(true);
                conn.setConnectTimeout(30000);
                conn.setReadTimeout(30000);

                android.util.Log.d("GeminiAI", "Sending request to Gemini API...");

                OutputStream os = conn.getOutputStream();
                os.write(requestJson.toString().getBytes());
                os.flush();
                os.close();

                int responseCode = conn.getResponseCode();
                android.util.Log.d("GeminiAI", "Response code: " + responseCode);

                if (responseCode == 200) {
                    android.util.Log.d("GeminiAI", "Success! Reading response...");
                    BufferedReader br = new BufferedReader(new InputStreamReader(conn.getInputStream()));
                    StringBuilder response = new StringBuilder();
                    String line;
                    while ((line = br.readLine()) != null) {
                        response.append(line);
                    }
                    br.close();

                    android.util.Log.d("GeminiAI", "Response: " + response.toString());

                    JSONObject responseJson = new JSONObject(response.toString());
                    String result = responseJson.getJSONArray("candidates")
                            .getJSONObject(0)
                            .getJSONObject("content")
                            .getJSONArray("parts")
                            .getJSONObject(0)
                            .getString("text");

                    android.util.Log.d("GeminiAI", "Parsed result successfully");

                    runOnUiThread(() -> {
                        textResult.setText(result);
                        textResult.setVisibility(View.VISIBLE);
                        btnAnalyze.setEnabled(true);
                    });
                } else {
                    // API Failed -> Show Error
                    android.util.Log.e("GeminiAI", "API returned error code: " + responseCode);
                    String errorMsg = "";
                    try {
                        BufferedReader errorBr = new BufferedReader(new InputStreamReader(conn.getErrorStream()));
                        StringBuilder errorResponse = new StringBuilder();
                        String errorLine;
                        while ((errorLine = errorBr.readLine()) != null) {
                            errorResponse.append(errorLine);
                        }
                        errorBr.close();
                        errorMsg = errorResponse.toString();
                        android.util.Log.e("GeminiAI", "Error response: " + errorMsg);
                    } catch (Exception ex) {
                        errorMsg = "Unable to read error message";
                        android.util.Log.e("GeminiAI", "Failed to read error stream", ex);
                    }

                    final String finalErrorMsg = errorMsg;
                    runOnUiThread(() -> {
                        textResult.setText("API ERROR (" + responseCode + "):\n" + finalErrorMsg);
                        textResult.setVisibility(View.VISIBLE);
                        btnAnalyze.setEnabled(true);
                    });
                }

            } catch (Exception e) {
                android.util.Log.e("GeminiAI", "Error calling API", e);
                runOnUiThread(() -> {
                    textResult.setText("CONNECTION ERROR:\n" + e.getClass().getSimpleName() + ": " + e.getMessage());
                    textResult.setVisibility(View.VISIBLE);
                    btnAnalyze.setEnabled(true);
                });
            }
        });
    }

    private void performDemoSimulation() {
        // Simulate a delay for "Analysis"
        new android.os.Handler().postDelayed(() -> {
            String demoResult = "DEMO ANALYSIS RESULT\n" +
                    "-----------------------------------\n" +
                    "Detected Crop: Rice (Oryza sativa)\n" +
                    "Condition: Rice Blast (Magnaporthe oryzae)\n" +
                    "Confidence: 94%\n\n" +
                    "SYMPTOMS:\n" +
                    "• Diamond-shaped white centered lesions on leaves\n" +
                    "• Brown borders around lesions\n\n" +
                    "RECOMMENDED TREATMENT:\n" +
                    "• Spray Tricyclazole 75 WP @ 0.6g/L water\n" +
                    "• Maintain standing water in the field\n\n" +
                    "PREVENTION:\n" +
                    "• Use resistant varieties\n" +
                    "• Avoid excessive nitrogen fertilizer";

            textResult.setText(demoResult);
            textResult.setVisibility(View.VISIBLE);
            btnAnalyze.setEnabled(true);
        }, 1500); // 1.5 second delay
    }

    @Override
    public boolean onSupportNavigateUp() {
        finish();
        return true;
    }
}
