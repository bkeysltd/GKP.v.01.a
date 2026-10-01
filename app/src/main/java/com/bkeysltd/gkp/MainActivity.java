package com.bkeysltd.gkp;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.Gravity;
import android.view.View;
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
    private static final String VERSION = "v.03.a.00";
    private static final String PREFS = "gkp";
    private static final String KEY_ENDPOINT = "endpoint";

    private EditText noteEdit;
    private EditText endpointEdit;
    private TextView answerView;
    private Button askButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        buildUi();
        receiveSharedText(getIntent());
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);
        receiveSharedText(intent);
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
        title.setText("GKP — KEEP → GPT");
        title.setTextSize(26);
        title.setTypeface(Typeface.DEFAULT_BOLD);
        title.setTextColor(Color.BLACK);
        root.addView(title);

        TextView sub = new TextView(this);
        sub.setText("Google Keep → Share → GKP → ASK GPT");
        sub.setTextSize(15);
        sub.setTextColor(Color.DKGRAY);
        sub.setPadding(0, dp(6), 0, dp(14));
        root.addView(sub);

        noteEdit = new EditText(this);
        noteEdit.setHint("Share a Google Keep note here, or type/paste text...");
        noteEdit.setGravity(Gravity.TOP | Gravity.START);
        noteEdit.setTextSize(18);
        noteEdit.setMinLines(7);
        noteEdit.setBackgroundColor(0xFFF3F3F3);
        noteEdit.setPadding(dp(12), dp(12), dp(12), dp(12));
        root.addView(noteEdit, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, dp(240)));

        askButton = new Button(this);
        askButton.setText("ASK GPT");
        LinearLayout.LayoutParams bp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        bp.setMargins(0, dp(12), 0, dp(12));
        root.addView(askButton, bp);
        askButton.setOnClickListener(v -> askGpt());

        TextView answerLabel = new TextView(this);
        answerLabel.setText("GPT ANSWER");
        answerLabel.setTypeface(Typeface.DEFAULT_BOLD);
        answerLabel.setTextColor(Color.BLACK);
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
        settingsTitle.setText("CONNECTION");
        settingsTitle.setTypeface(Typeface.DEFAULT_BOLD);
        settingsTitle.setTextColor(Color.BLACK);
        root.addView(settingsTitle);

        endpointEdit = new EditText(this);
        endpointEdit.setHint("https://your-server.example.com/ask");
        endpointEdit.setSingleLine(true);
        endpointEdit.setText(loadEndpoint());
        root.addView(endpointEdit);

        Button save = new Button(this);
        save.setText("SAVE CONNECTION");
        save.setOnClickListener(v -> {
            String endpoint = endpointEdit.getText().toString().trim();
            getSharedPreferences(PREFS, MODE_PRIVATE).edit().putString(KEY_ENDPOINT, endpoint).apply();
            Toast.makeText(this, "Connection saved.", Toast.LENGTH_SHORT).show();
        });
        root.addView(save);

        TextView note = new TextView(this);
        note.setText("No OpenAI API key is stored in this APK. The key stays on your server.");
        note.setTextSize(13);
        note.setTextColor(Color.GRAY);
        note.setPadding(0, dp(10), 0, 0);
        root.addView(note);

        TextView version = new TextView(this);
        version.setText("GKP " + VERSION);
        version.setGravity(Gravity.END);
        version.setTextColor(Color.GRAY);
        version.setPadding(0, dp(14), 0, 0);
        root.addView(version);

        setContentView(scroll);
    }

    private void receiveSharedText(Intent intent) {
        if (intent == null || !Intent.ACTION_SEND.equals(intent.getAction())) return;
        String type = intent.getType();
        if (type == null || !type.startsWith("text/")) return;

        CharSequence shared = intent.getCharSequenceExtra(Intent.EXTRA_TEXT);
        CharSequence subject = intent.getCharSequenceExtra(Intent.EXTRA_SUBJECT);

        String text = shared == null ? "" : shared.toString().trim();
        if (!TextUtils.isEmpty(subject) && !text.startsWith(subject.toString())) {
            text = subject.toString().trim() + "\n\n" + text;
        }
        noteEdit.setText(text);
        noteEdit.setSelection(noteEdit.getText().length());
    }

    private String loadEndpoint() {
        return getSharedPreferences(PREFS, MODE_PRIVATE).getString(KEY_ENDPOINT, "");
    }

    private void askGpt() {
        String text = noteEdit.getText().toString().trim();
        String endpoint = endpointEdit.getText().toString().trim();

        if (text.isEmpty()) {
            Toast.makeText(this, "There is no text to send.", Toast.LENGTH_SHORT).show();
            return;
        }
        if (endpoint.isEmpty()) {
            Toast.makeText(this, "Set the connection URL first.", Toast.LENGTH_LONG).show();
            endpointEdit.requestFocus();
            return;
        }

        getSharedPreferences(PREFS, MODE_PRIVATE).edit().putString(KEY_ENDPOINT, endpoint).apply();
        askButton.setEnabled(false);
        askButton.setText("SENDING...");
        answerView.setText("Working...");

        new Thread(() -> {
            try {
                String answer = callBackend(endpoint, text);
                runOnUiThread(() -> {
                    answerView.setText(answer);
                    askButton.setEnabled(true);
                    askButton.setText("ASK GPT");
                });
            } catch (Exception e) {
                runOnUiThread(() -> {
                    answerView.setText("Connection error: " + e.getMessage());
                    askButton.setEnabled(true);
                    askButton.setText("ASK GPT");
                });
            }
        }).start();
    }

    private String callBackend(String endpoint, String text) throws Exception {
        HttpURLConnection c = (HttpURLConnection) new URL(endpoint).openConnection();
        c.setRequestMethod("POST");
        c.setConnectTimeout(20000);
        c.setReadTimeout(90000);
        c.setDoOutput(true);
        c.setRequestProperty("Content-Type", "application/json; charset=utf-8");
        c.setRequestProperty("Accept", "application/json");

        JSONObject req = new JSONObject();
        req.put("text", text);

        byte[] body = req.toString().getBytes(StandardCharsets.UTF_8);
        try (OutputStream out = c.getOutputStream()) {
            out.write(body);
        }

        int code = c.getResponseCode();
        InputStream stream = code >= 200 && code < 300 ? c.getInputStream() : c.getErrorStream();
        StringBuilder response = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(stream, StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) response.append(line);
        }

        JSONObject obj = new JSONObject(response.toString());
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
