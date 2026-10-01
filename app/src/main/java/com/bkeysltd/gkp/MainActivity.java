package com.bkeysltd.gkp;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Bundle;
import android.provider.Settings;
import android.text.InputType;
import android.view.Gravity;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;

public class MainActivity extends Activity {
    private static final String VERSION = "v.04.a.00";
    private static final String PREFS = "gkp";
    private static final String KEY_ENDPOINT = "endpoint";
    private static final String KEY_APP_TOKEN = "app_token";
    private static final String KEY_KEEP_TEXT = "keep_visible_text";

    private EditText endpointEdit;
    private EditText tokenEdit;
    private TextView keepPreview;
    private TextView answerView;
    private Button checkButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        buildUi();
    }

    @Override
    protected void onResume() {
        super.onResume();
        refreshKeepPreview();
    }

    private void buildUi() {
        ScrollView scroll = new ScrollView(this);
        scroll.setFillViewport(true);

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(dp(18), dp(18), dp(18), dp(24));
        root.setBackgroundColor(Color.WHITE);
        scroll.addView(root, new ScrollView.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));

        TextView title = new TextView(this);
        title.setText("GKP — SEE KEEP → GPT");
        title.setTextSize(26);
        title.setTypeface(Typeface.DEFAULT_BOLD);
        title.setTextColor(Color.BLACK);
        root.addView(title);

        TextView sub = new TextView(this);
        sub.setText("Open a Keep note. GKP reads only the text visible on that Keep screen.");
        sub.setTextSize(15);
        sub.setTextColor(Color.DKGRAY);
        sub.setPadding(0, dp(6), 0, dp(14));
        root.addView(sub);

        Button enable = new Button(this);
        enable.setText("1. ENABLE KEEP ACCESS");
        enable.setOnClickListener(v -> startActivity(new Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)));
        root.addView(enable);

        Button openKeep = new Button(this);
        openKeep.setText("2. OPEN GOOGLE KEEP");
        openKeep.setOnClickListener(v -> {
            Intent launch = getPackageManager().getLaunchIntentForPackage("com.google.android.keep");
            if (launch == null) {
                Toast.makeText(this, "Google Keep is not installed.", Toast.LENGTH_LONG).show();
            } else {
                startActivity(launch);
            }
        });
        LinearLayout.LayoutParams kp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        kp.setMargins(0, dp(8), 0, dp(12));
        root.addView(openKeep, kp);

        TextView seenLabel = new TextView(this);
        seenLabel.setText("WHAT GKP CAN SEE IN KEEP");
        seenLabel.setTypeface(Typeface.DEFAULT_BOLD);
        seenLabel.setTextColor(Color.BLACK);
        root.addView(seenLabel);

        keepPreview = new TextView(this);
        keepPreview.setText("No visible Keep note captured yet.");
        keepPreview.setTextSize(17);
        keepPreview.setTextColor(Color.BLACK);
        keepPreview.setBackgroundColor(0xFFF3F3F3);
        keepPreview.setPadding(dp(12), dp(12), dp(12), dp(12));
        keepPreview.setTextIsSelectable(true);
        LinearLayout.LayoutParams pp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        pp.setMargins(0, dp(6), 0, dp(12));
        root.addView(keepPreview, pp);

        checkButton = new Button(this);
        checkButton.setText("3. CHECK VISIBLE NOTE WITH GPT");
        checkButton.setOnClickListener(v -> checkVisibleNote());
        root.addView(checkButton);

        TextView answerLabel = new TextView(this);
        answerLabel.setText("GPT ANSWER");
        answerLabel.setTypeface(Typeface.DEFAULT_BOLD);
        answerLabel.setTextColor(Color.BLACK);
        answerLabel.setPadding(0, dp(16), 0, 0);
        root.addView(answerLabel);

        answerView = new TextView(this);
        answerView.setText("The answer will appear here.");
        answerView.setTextSize(17);
        answerView.setTextColor(Color.BLACK);
        answerView.setBackgroundColor(0xFFF7F7F7);
        answerView.setPadding(dp(12), dp(12), dp(12), dp(12));
        answerView.setTextIsSelectable(true);
        LinearLayout.LayoutParams ap = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        ap.setMargins(0, dp(6), 0, dp(18));
        root.addView(answerView, ap);

        TextView settingsTitle = new TextView(this);
        settingsTitle.setText("GPT CONNECTION — ONE-TIME SETUP");
        settingsTitle.setTypeface(Typeface.DEFAULT_BOLD);
        settingsTitle.setTextColor(Color.BLACK);
        root.addView(settingsTitle);

        endpointEdit = new EditText(this);
        endpointEdit.setHint("https://your-server.example.com/ask");
        endpointEdit.setSingleLine(true);
        endpointEdit.setText(load(KEY_ENDPOINT));
        root.addView(endpointEdit);

        tokenEdit = new EditText(this);
        tokenEdit.setHint("GKP app token");
        tokenEdit.setSingleLine(true);
        tokenEdit.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
        tokenEdit.setText(load(KEY_APP_TOKEN));
        root.addView(tokenEdit);

        Button save = new Button(this);
        save.setText("SAVE GPT CONNECTION");
        save.setOnClickListener(v -> {
            SharedPreferences.Editor editor = getSharedPreferences(PREFS, MODE_PRIVATE).edit();
            editor.putString(KEY_ENDPOINT, endpointEdit.getText().toString().trim());
            editor.putString(KEY_APP_TOKEN, tokenEdit.getText().toString());
            editor.apply();
            Toast.makeText(this, "GPT connection saved.", Toast.LENGTH_SHORT).show();
        });
        root.addView(save);

        TextView privacy = new TextView(this);
        privacy.setText("GKP accessibility access is restricted to Google Keep. It stores only the latest text visible in Keep on this phone.");
        privacy.setTextSize(13);
        privacy.setTextColor(Color.GRAY);
        privacy.setPadding(0, dp(10), 0, 0);
        root.addView(privacy);

        TextView version = new TextView(this);
        version.setText("GKP " + VERSION);
        version.setGravity(Gravity.END);
        version.setTextColor(Color.GRAY);
        version.setPadding(0, dp(14), 0, 0);
        root.addView(version);

        setContentView(scroll);
    }

    private void refreshKeepPreview() {
        String text = load(KEY_KEEP_TEXT).trim();
        keepPreview.setText(text.isEmpty() ? "No visible Keep note captured yet." : text);
    }

    private String load(String key) {
        return getSharedPreferences(PREFS, MODE_PRIVATE).getString(key, "");
    }

    private void checkVisibleNote() {
        String text = load(KEY_KEEP_TEXT).trim();
        String endpoint = endpointEdit.getText().toString().trim();
        String token = tokenEdit.getText().toString();

        refreshKeepPreview();

        if (text.isEmpty()) {
            Toast.makeText(this, "Open a note in Google Keep first, then come back to GKP.", Toast.LENGTH_LONG).show();
            return;
        }
        if (endpoint.isEmpty() || token.isEmpty()) {
            Toast.makeText(this, "Set the GPT connection first.", Toast.LENGTH_LONG).show();
            return;
        }

        SharedPreferences.Editor editor = getSharedPreferences(PREFS, MODE_PRIVATE).edit();
        editor.putString(KEY_ENDPOINT, endpoint);
        editor.putString(KEY_APP_TOKEN, token);
        editor.apply();

        checkButton.setEnabled(false);
        checkButton.setText("CHECKING...");
        answerView.setText("Working...");

        new Thread(() -> {
            try {
                String answer = callBackend(endpoint, token, text);
                runOnUiThread(() -> {
                    answerView.setText(answer);
                    checkButton.setEnabled(true);
                    checkButton.setText("3. CHECK VISIBLE NOTE WITH GPT");
                });
            } catch (Exception e) {
                runOnUiThread(() -> {
                    answerView.setText("Connection error: " + e.getMessage());
                    checkButton.setEnabled(true);
                    checkButton.setText("3. CHECK VISIBLE NOTE WITH GPT");
                });
            }
        }).start();
    }

    private String callBackend(String endpoint, String token, String text) throws Exception {
        HttpURLConnection c = (HttpURLConnection) new URL(endpoint).openConnection();
        c.setRequestMethod("POST");
        c.setConnectTimeout(20000);
        c.setReadTimeout(90000);
        c.setDoOutput(true);
        c.setRequestProperty("Content-Type", "application/json; charset=utf-8");
        c.setRequestProperty("Accept", "application/json");
        c.setRequestProperty("Authorization", "Bearer " + token);

        JSONObject req = new JSONObject();
        req.put("text", text);

        byte[] body = req.toString().getBytes(StandardCharsets.UTF_8);
        try (OutputStream out = c.getOutputStream()) {
            out.write(body);
        }

        int code = c.getResponseCode();
        InputStream stream = code >= 200 && code < 300 ? c.getInputStream() : c.getErrorStream();
        StringBuilder response = new StringBuilder();
        if (stream != null) {
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(stream, StandardCharsets.UTF_8))) {
                String line;
                while ((line = reader.readLine()) != null) response.append(line);
            }
        }

        JSONObject obj = response.length() == 0 ? new JSONObject() : new JSONObject(response.toString());
        if (code < 200 || code >= 300) {
            throw new Exception(obj.optString("error", "Server returned HTTP " + code));
        }

        String answer = obj.optString("answer", "");
        if (answer.isEmpty()) throw new Exception("No answer returned.");
        return answer;
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }
}
