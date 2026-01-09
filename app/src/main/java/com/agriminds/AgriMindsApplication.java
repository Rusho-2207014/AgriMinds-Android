package com.agriminds;

import android.app.Application;
import com.google.firebase.FirebaseApp;
import com.google.firebase.analytics.FirebaseAnalytics;
import com.google.firebase.crashlytics.FirebaseCrashlytics;
import com.google.firebase.remoteconfig.FirebaseRemoteConfig;
import com.google.firebase.remoteconfig.FirebaseRemoteConfigSettings;

public class AgriMindsApplication extends Application {

    private FirebaseAnalytics mFirebaseAnalytics;

    @Override
    public void onCreate() {
        super.onCreate();

        // Initialize Firebase
        FirebaseApp.initializeApp(this);

        // Initialize Firebase Analytics (tracks user behavior)
        mFirebaseAnalytics = FirebaseAnalytics.getInstance(this);

        // Initialize Firebase Crashlytics (automatic crash reporting)
        FirebaseCrashlytics crashlytics = FirebaseCrashlytics.getInstance();
        crashlytics.setCrashlyticsCollectionEnabled(true);

        // Initialize Firebase Remote Config (control app settings remotely)
        FirebaseRemoteConfig remoteConfig = FirebaseRemoteConfig.getInstance();
        FirebaseRemoteConfigSettings configSettings = new FirebaseRemoteConfigSettings.Builder()
                .setMinimumFetchIntervalInSeconds(3600) // Fetch every hour
                .build();
        remoteConfig.setConfigSettingsAsync(configSettings);

        // Set default Remote Config values
        remoteConfig.setDefaultsAsync(R.xml.remote_config_defaults);
    }

    public FirebaseAnalytics getAnalytics() {
        return mFirebaseAnalytics;
    }
}
