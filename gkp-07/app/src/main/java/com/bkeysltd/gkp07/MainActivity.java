package com.bkeysltd.gkp07;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Typeface;
import android.graphics.pdf.PdfDocument;
import android.net.Uri;
import android.os.Bundle;
import android.provider.Settings;
import android.text.TextUtils;
import android.view.Gravity;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.core.content.FileProvider;

import java.io.File;
import java.io.FileOutputStream;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

public class MainActivity extends Activity {
    private TextView status;
    private TextView notesView;
    private Button pdfButton;
    private boolean openedKeep = false;
    private List<String> notes = new ArrayList<>();

    @Override
    protected void onCreate(Bundle state) {
        super.onCreate(state);
        buildUi();
    }

    @Override
    protected void onResume() {
        super.onResume();
        refreshNotes();

        if (!serviceEnabled()) {
            status.setText("ONE-TIME SETUP REQUIRED");
            return;
        }

        if (notes.isEmpty() && !openedKeep) {
            openedKeep = true;
            openKeep();
        }
    }

    private void buildUi() {
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(dp(18), dp(18), dp(18), dp(18));
        root.setBackgroundColor(Color.WHITE);

        TextView title = new TextView(this);
        title.setText("GKP 08");
        title.setTextSize(30);
        title.setTypeface(Typeface.DEFAULT_BOLD);
        title.setTextColor(Color.BLACK);
        root.addView(title);

        status = new TextView(this);
        status.setText("Checking Google Keep...");
        status.setTextSize(17);
        status.setTypeface(Typeface.DEFAULT_BOLD);
        status.setTextColor(Color.DKGRAY);
        status.setPadding(0, dp(12), 0, dp(10));
        root.addView(status);

        Button setup = new Button(this);
        setup.setText("ENABLE ONCE");
        setup.setOnClickListener(v ->
                startActivity(new Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)));
        root.addView(setup);

        Button check = new Button(this);
        check.setText("CHECK GOOGLE KEEP");
        check.setOnClickListener(v -> {
            openedKeep = true;
            openKeep();
        });
        root.addView(check);

        ScrollView scroll = new ScrollView(this);
        LinearLayout.LayoutParams sp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f);
        sp.setMargins(0, dp(12), 0, dp(12));
        root.addView(scroll, sp);

        notesView = new TextView(this);
        notesView.setText("No IMPORTANT / VERY IMPORTANT notes found yet.");
        notesView.setTextSize(18);
        notesView.setTextColor(Color.BLACK);
        notesView.setPadding(dp(12), dp(12), dp(12), dp(12));
        notesView.setBackgroundColor(0xFFF3F3F3);
        scroll.addView(notesView);

        pdfButton = new Button(this);
        pdfButton.setText("GENERATE PDF");
        pdfButton.setTextSize(21);
        pdfButton.setEnabled(false);
        pdfButton.setOnClickListener(v -> generatePdf());
        root.addView(pdfButton, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, dp(68)));

        TextView version = new TextView(this);
        version.setText("GKP 08 v.00.b.08");
        version.setTextColor(Color.GRAY);
        version.setGravity(Gravity.END);
        version.setPadding(0, dp(10), 0, 0);
        root.addView(version);

        setContentView(root);
    }

    private boolean serviceEnabled() {
        String enabled = Settings.Secure.getString(
                getContentResolver(),
                Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
        );
        if (enabled == null) return false;

        ComponentName cn = new ComponentName(this, KeepScanService.class);
        String id = cn.flattenToString();

        for (String item : enabled.split(":")) {
            if (item.equalsIgnoreCase(id)) return true;
        }
        return false;
    }

    private void openKeep() {
        Intent intent = getPackageManager().getLaunchIntentForPackage("com.google.android.keep");
        if (intent == null) {
            Toast.makeText(this, "Google Keep is not installed.", Toast.LENGTH_LONG).show();
            return;
        }
        startActivity(intent);
    }

    private void refreshNotes() {
        SharedPreferences p = getSharedPreferences("gkp07", MODE_PRIVATE);
        Set<String> set = p.getStringSet("notes", new LinkedHashSet<>());
        notes = new ArrayList<>(set);

        if (notes.isEmpty()) {
            notesView.setText("No IMPORTANT / VERY IMPORTANT notes found yet.");
            pdfButton.setEnabled(false);
            if (serviceEnabled()) status.setText("Google Keep scanner ready");
        } else {
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < notes.size(); i++) {
                sb.append(i + 1).append(". ").append(notes.get(i)).append("\n\n");
            }
            notesView.setText(sb.toString().trim());
            status.setText(notes.size() + " note(s) found");
            pdfButton.setEnabled(true);
        }
    }

    private void generatePdf() {
        if (notes.isEmpty()) return;

        try {
            File dir = new File(getCacheDir(), "pdf");
            if (!dir.exists()) dir.mkdirs();
            File out = new File(dir, "GKP07_Important_Notes.pdf");

            PdfDocument doc = new PdfDocument();
            int pageNo = 1;

            for (String raw : notes) {
                String upper = raw.toUpperCase();
                String priority = upper.contains("VERY IMPORTANT")
                        ? "VERY IMPORTANT" : "IMPORTANT";

                String body = raw
                        .replace("VERY IMPORTANT", "")
                        .replace("Important", "")
                        .replace("IMPORTANT", "")
                        .trim();

                PdfDocument.Page page = doc.startPage(
                        new PdfDocument.PageInfo.Builder(595, 842, pageNo++).create()
                );

                Canvas c = page.getCanvas();
                c.drawColor(Color.WHITE);

                Paint border = new Paint(Paint.ANTI_ALIAS_FLAG);
                border.setStyle(Paint.Style.STROKE);
                border.setStrokeWidth(2f);
                border.setColor(Color.BLACK);
                c.drawRect(24, 24, 571, 397, border);

                Paint heading = new Paint(Paint.ANTI_ALIAS_FLAG);
                heading.setColor(Color.BLACK);
                heading.setTypeface(Typeface.DEFAULT_BOLD);
                heading.setTextAlign(Paint.Align.CENTER);
                heading.setTextSize(priority.equals("VERY IMPORTANT") ? 28f : 32f);
                c.drawText(priority, 297.5f, 72, heading);

                Paint bodyPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
                bodyPaint.setColor(Color.BLACK);
                bodyPaint.setTextSize(18f);

                List<String> lines = wrap(body, bodyPaint, 490f);
                float y = 118f;
                for (String line : lines) {
                    if (y > 370f) break;
                    c.drawText(line, 52f, y, bodyPaint);
                    y += 23f;
                }

                Paint cut = new Paint(Paint.ANTI_ALIAS_FLAG);
                cut.setColor(Color.LTGRAY);
                c.drawLine(0, 421, 595, 421, cut);

                doc.finishPage(page);
            }

            try (FileOutputStream stream = new FileOutputStream(out)) {
                doc.writeTo(stream);
            } finally {
                doc.close();
            }

            Uri uri = FileProvider.getUriForFile(
                    this,
                    getPackageName() + ".files",
                    out
            );

            Intent view = new Intent(Intent.ACTION_VIEW);
            view.setDataAndType(uri, "application/pdf");
            view.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            startActivity(view);

        } catch (Exception e) {
            Toast.makeText(this, "PDF error: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    private List<String> wrap(String text, Paint paint, float maxWidth) {
        List<String> out = new ArrayList<>();

        for (String paragraph : text.split("\n", -1)) {
            if (paragraph.trim().isEmpty()) {
                out.add("");
                continue;
            }

            String current = "";
            for (String word : paragraph.trim().split("\\s+")) {
                String test = current.isEmpty() ? word : current + " " + word;
                if (paint.measureText(test) <= maxWidth) {
                    current = test;
                } else {
                    if (!current.isEmpty()) out.add(current);
                    current = word;
                }
            }
            if (!current.isEmpty()) out.add(current);
        }
        return out;
    }

    private int dp(int v) {
        return Math.round(v * getResources().getDisplayMetrics().density);
    }
}
