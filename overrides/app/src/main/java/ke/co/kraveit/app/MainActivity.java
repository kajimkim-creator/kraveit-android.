package ke.co.kraveit.app;

import android.Manifest;
import androidx.activity.ComponentActivity;
import androidx.activity.OnBackPressedCallback;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.webkit.GeolocationPermissions;
import androidx.webkit.WebViewCompat;
import androidx.webkit.WebViewFeature;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceRequest;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Button;
import android.widget.ProgressBar;

import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.util.Collections;

public class MainActivity extends ComponentActivity {
    private static final int LOCATION_REQUEST = 1001;
    private static final String HOME_URL = BuildConfig.KRAVEIT_URL;
    private static final String KRAVEIT_HOST = Uri.parse(HOME_URL).getHost();
    private static final String TRACK_URL = Uri.parse(HOME_URL).buildUpon().path("/track.html").build().toString();
    private static final String WHATSAPP_URL = "https://wa.me/254718359797";
    private static final String OFFLINE_URL = "file:///android_asset/offline.html";
    private static final String PROFILE_PREFS = "kraveit_customer_profile";

    private WebView webView;
    private OnBackPressedCallback webBackCallback;
    private ProgressBar pageProgress;
    private GeolocationPermissions.Callback pendingGeoCallback;
    private String pendingGeoOrigin;
    private SharedPreferences profilePrefs;
    private String profileHelperScript = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        WindowCompat.setDecorFitsSystemWindows(getWindow(), false);
        setContentView(R.layout.activity_main);
        configureWindowInsets();

        profilePrefs = getSharedPreferences(PROFILE_PREFS, MODE_PRIVATE);
        profileHelperScript = readAsset("profile-helper.js");

        webView = findViewById(R.id.webView);
        pageProgress = findViewById(R.id.pageProgress);
        Button homeButton = findViewById(R.id.homeButton);
        Button trackButton = findViewById(R.id.trackButton);
        Button whatsAppButton = findViewById(R.id.whatsAppButton);

        webBackCallback = new OnBackPressedCallback(false) {
            @Override
            public void handleOnBackPressed() {
                webView.goBack();
                updateBackNavigation();
            }
        };
        getOnBackPressedDispatcher().addCallback(this, webBackCallback);
        configureWebView();

        homeButton.setOnClickListener(v -> loadInternal(HOME_URL));
        trackButton.setOnClickListener(v -> loadInternal(TRACK_URL));
        whatsAppButton.setOnClickListener(v -> openExternal(Uri.parse(WHATSAPP_URL)));

        Uri incoming = getIntent() != null ? getIntent().getData() : null;
        if (savedInstanceState != null) {
            webView.restoreState(savedInstanceState);
            updateBackNavigation();
        } else if (!isOnline()) {
            webView.loadUrl(OFFLINE_URL);
        } else if (isKraveitHttps(incoming)) {
            webView.loadUrl(incoming.toString());
        } else {
            webView.loadUrl(BuildConfig.KRAVEIT_URL);
        }
    }

    private void configureWindowInsets() {
        View root = ((android.view.ViewGroup) findViewById(android.R.id.content)).getChildAt(0);
        final int left = root.getPaddingLeft();
        final int top = root.getPaddingTop();
        final int right = root.getPaddingRight();
        final int bottom = root.getPaddingBottom();
        ViewCompat.setOnApplyWindowInsetsListener(root, (view, windowInsets) -> {
            Insets safe = windowInsets.getInsets(WindowInsetsCompat.Type.systemBars()
                | WindowInsetsCompat.Type.displayCutout() | WindowInsetsCompat.Type.ime());
            // Always start with original padding: repeated IME/inset events must not accumulate.
            view.setPadding(left + safe.left, top + safe.top,
                right + safe.right, bottom + safe.bottom);
            return WindowInsetsCompat.CONSUMED;
        });
        ViewCompat.requestApplyInsets(root);
    }

    private void updateBackNavigation() {
        if (webBackCallback != null) webBackCallback.setEnabled(webView.canGoBack());
    }

    private void configureWebView() {
        WebSettings s = webView.getSettings();
        s.setJavaScriptEnabled(true);
        s.setDomStorageEnabled(true);
        s.setDatabaseEnabled(true);
        s.setGeolocationEnabled(true);
        s.setLoadsImagesAutomatically(true);
        s.setMixedContentMode(WebSettings.MIXED_CONTENT_NEVER_ALLOW);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            s.setSafeBrowsingEnabled(true);
        }
        s.setAllowFileAccess(true);
        s.setAllowContentAccess(false);
        s.setAllowFileAccessFromFileURLs(false);
        s.setAllowUniversalAccessFromFileURLs(false);
        s.setUserAgentString(s.getUserAgentString() + " KRAVEIT-Android/1.3");

        // Fail closed on old WebViews: checkout works without native remembering.
        if (WebViewFeature.isFeatureSupported(WebViewFeature.WEB_MESSAGE_LISTENER)) {
            ProfileBridge profile = new ProfileBridge();
            WebViewCompat.addWebMessageListener(webView, "KraveitProfileMessages",
                Collections.singleton("https://" + KRAVEIT_HOST),
                (view, message, sourceOrigin, isMainFrame, reply) -> {
                    if (!isMainFrame || !isKraveitHttps(sourceOrigin)) return;
                    try {
                        String raw = message.getData();
                        if (raw == null || raw.length() > 8192) return;
                        JSONObject request = new JSONObject(raw);
                        JSONObject response = new JSONObject();
                        response.put("id", request.getInt("id"));
                        switch (request.optString("action")) {
                            case "get":
                                response.put("profile", new JSONObject(profile.getProfile()));
                                break;
                            case "save":
                                profile.saveProfile(request.getJSONObject("profile").toString());
                                break;
                            case "clear":
                                profile.clearProfile();
                                break;
                            default:
                                return;
                        }
                        response.put("ok", true);
                        reply.postMessage(response.toString());
                    } catch (Exception ignored) {
                        // Malformed messages do not read or change stored details.
                    }
                });
        }

        webView.setWebChromeClient(new WebChromeClient() {
            @Override
            public void onProgressChanged(WebView view, int newProgress) {
                pageProgress.setProgress(newProgress);
                pageProgress.setVisibility(newProgress >= 100 ? View.GONE : View.VISIBLE);
            }

            @Override
            public void onGeolocationPermissionsShowPrompt(String origin, GeolocationPermissions.Callback callback) {
                Uri originUri = Uri.parse(origin);
                if (!isKraveitHttps(originUri)) {
                    callback.invoke(origin, false, false);
                    return;
                }
                if (hasLocationPermission()) {
                    callback.invoke(origin, true, false);
                } else {
                    pendingGeoOrigin = origin;
                    pendingGeoCallback = callback;
                    requestPermissions(
                        new String[]{Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION},
                        LOCATION_REQUEST
                    );
                }
            }
        });

        webView.setWebViewClient(new WebViewClient() {
            @Override
            public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {
                if (!request.isForMainFrame()) return !isKraveitHttps(request.getUrl());
                return handleUri(request.getUrl());
            }

            @Override
            public boolean shouldOverrideUrlLoading(WebView view, String url) {
                return handleUri(Uri.parse(url));
            }

            @Override
            public void onPageStarted(WebView view, String url, Bitmap favicon) {
                super.onPageStarted(view, url, favicon);
                pageProgress.setVisibility(View.VISIBLE);
            }

            @Override
            public void doUpdateVisitedHistory(WebView view, String url, boolean isReload) {
                super.doUpdateVisitedHistory(view, url, isReload);
                updateBackNavigation();
            }

            @Override
            public void onPageFinished(WebView view, String url) {
                super.onPageFinished(view, url);
                pageProgress.setVisibility(View.GONE);
                injectProfileHelper(view, url);
                updateBackNavigation();
            }

            @Override
            public void onReceivedError(WebView view, WebResourceRequest request, android.webkit.WebResourceError error) {
                if (request.isForMainFrame() && !OFFLINE_URL.equals(view.getUrl())) {
                    view.loadUrl(OFFLINE_URL);
                }
            }
        });

        webView.setOverScrollMode(View.OVER_SCROLL_NEVER);
    }

    private void injectProfileHelper(WebView view, String url) {
        if (profileHelperScript.isEmpty()) return;
        Uri uri = Uri.parse(url == null ? "" : url);
        if (!isKraveitHttps(uri)) return;
        view.evaluateJavascript(profileHelperScript, null);
    }

    private String readAsset(String assetName) {
        try (InputStream input = getAssets().open(assetName);
             ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            byte[] buffer = new byte[4096];
            int count;
            while ((count = input.read(buffer)) != -1) {
                output.write(buffer, 0, count);
            }
            return output.toString("UTF-8");
        } catch (Exception ignored) {
            return "";
        }
    }

    private final class ProfileBridge {
        public String getProfile() {
            try {
                JSONObject result = new JSONObject();
                boolean saved = profilePrefs.getBoolean("saved", false);
                result.put("saved", saved);
                if (saved) {
                    result.put("name", profilePrefs.getString("name", ""));
                    result.put("phone", profilePrefs.getString("phone", ""));
                    result.put("distance", profilePrefs.getString("distance", "0"));
                    result.put("notes", profilePrefs.getString("notes", ""));
                }
                return result.toString();
            } catch (Exception ignored) {
                return "{\"saved\":false}";
            }
        }

        public void saveProfile(String json) {
            try {
                JSONObject data = new JSONObject(json == null ? "{}" : json);
                String name = cleanText(data.optString("name", ""), 80);
                String phone = cleanText(data.optString("phone", ""), 30);
                String distance = cleanDistance(data.optString("distance", "0"));
                String notes = cleanText(data.optString("notes", ""), 240);

                profilePrefs.edit()
                    .putBoolean("saved", true)
                    .putString("name", name)
                    .putString("phone", phone)
                    .putString("distance", distance)
                    .putString("notes", notes)
                    .apply();
            } catch (Exception ignored) {
                // Invalid profile data is ignored rather than affecting checkout.
            }
        }

        public void clearProfile() {
            profilePrefs.edit().clear().apply();
        }
    }

    private String cleanText(String value, int maxLength) {
        String cleaned = value == null ? "" : value.trim();
        if (cleaned.length() > maxLength) {
            cleaned = cleaned.substring(0, maxLength);
        }
        return cleaned;
    }

    private String cleanDistance(String value) {
        if ("0".equals(value) || "50".equals(value) || "100".equals(value)
            || "150".equals(value) || "quote".equals(value)) {
            return value;
        }
        return "0";
    }

    private void loadInternal(String url) {
        if (isOnline()) {
            webView.loadUrl(url);
        } else {
            webView.loadUrl(OFFLINE_URL);
        }
    }

    private boolean isOnline() {
        ConnectivityManager cm = (ConnectivityManager) getSystemService(CONNECTIVITY_SERVICE);
        if (cm == null) return false;
        Network network = cm.getActiveNetwork();
        if (network == null) return false;
        NetworkCapabilities caps = cm.getNetworkCapabilities(network);
        return caps != null && caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET);
    }

    private boolean isKraveitHttps(Uri uri) {
        return uri != null
            && "https".equalsIgnoreCase(uri.getScheme())
            && KRAVEIT_HOST.equalsIgnoreCase(uri.getHost())
            && (uri.getPort() == -1 || uri.getPort() == 443)
            && uri.getUserInfo() == null;
    }

    private boolean hasLocationPermission() {
        return checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
            || checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED;
    }

    private boolean handleUri(Uri uri) {
        String scheme = uri.getScheme() == null ? "" : uri.getScheme().toLowerCase();

        if ("file".equals(scheme) && OFFLINE_URL.equals(uri.toString())) {
            return false;
        }

        if (isKraveitHttps(uri)) {
            return false;
        }

        if ("tel".equals(scheme) || "mailto".equals(scheme) || "sms".equals(scheme)
            || "geo".equals(scheme) || "https".equals(scheme) || "whatsapp".equals(scheme)) {
            openExternal(uri);
            return true;
        }
        return true;
    }

    private void openExternal(Uri uri) {
        try {
            startActivity(new Intent(Intent.ACTION_VIEW, uri));
        } catch (ActivityNotFoundException ignored) {
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == LOCATION_REQUEST && pendingGeoCallback != null) {
            boolean granted = hasLocationPermission();
            pendingGeoCallback.invoke(pendingGeoOrigin, granted, false);
            pendingGeoCallback = null;
            pendingGeoOrigin = null;
        }
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);
        Uri uri = intent.getData();
        if (isKraveitHttps(uri)) {
            webView.loadUrl(uri.toString());
        }
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        webView.saveState(outState);
        super.onSaveInstanceState(outState);
    }

}
