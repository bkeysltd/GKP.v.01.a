package com.bkeysltd.gkp06;

import android.app.Activity;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Typeface;
import android.graphics.pdf.PdfDocument;
import android.os.Bundle;
import android.view.Gravity;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class MainActivity extends Activity {
    private static final String VERSION = "v.00.b.06";
    private String noteText = "";
    private String priority = "IMPORTANT";
    private TextView noteView;
    private TextView priorityView;
    private Button generateButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        buildScreen();
    }

    private void buildScreen() {
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(dp(18), dp(18), dp(18), dp(18));
        root.setBackgroundColor(Color.WHITE);

        TextView title = new TextView(this);
        title.setText("GKP 06");
        title.setTextSize(30);
        title.setTypeface(Typeface.DEFAULT_BOLD);
        title.setTextColor(Color.BLACK);
        root.addView(title);

        priorityView = new TextView(this);
        priorityView.setText("COPY NOTE FROM GOOGLE KEEP");
        priorityView.setTextSize(18);
        priorityView.setTypeface(Typeface.DEFAULT_BOLD);
        priorityView.setTextColor(Color.DKGRAY);
        priorityView.setPadding(0, dp(12), 0, dp(8));
        root.addView(priorityView);

        Button pasteButton = new Button(this);
        pasteButton.setText("PASTE NOTE");
        pasteButton.setTextSize(19);
        pasteButton.setOnClickListener(v -> pasteFromClipboard());
        root.addView(pasteButton);

        ScrollView scroll = new ScrollView(this);
        scroll.setBackgroundColor(0xFFF3F3F3);
        LinearLayout.LayoutParams sp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f);
        sp.setMargins(0, dp(12), 0, dp(12));
        root.addView(scroll, sp);

        noteView = new TextView(this);
        noteView.setText("Copied Google Keep note will appear here.");
        noteView.setTextSize(19);
        noteView.setTextColor(Color.BLACK);
        noteView.setPadding(dp(14), dp(14), dp(14), dp(14));
        noteView.setTextIsSelectable(true);
        scroll.addView(noteView);

        generateButton = new Button(this);
        generateButton.setText("GENERATE PDF");
        generateButton.setTextSize(21);
        generateButton.setEnabled(false);
        generateButton.setOnClickListener(v -> generatePdf());
        root.addView(generateButton, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, dp(68)));

        TextView version = new TextView(this);
        version.setText("GKP 06 " + VERSION);
        version.setTextColor(Color.GRAY);
        version.setGravity(Gravity.END);
        version.setPadding(0, dp(10), 0, 0);
        root.addView(version);

        setContentView(root);
    }

    private void pasteFromClipboard() {
        ClipboardManager clipboard =
                (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);

        if (clipboard == null || !clipboard.hasPrimaryClip()) {
            Toast.makeText(this, "Clipboard is empty.", Toast.LENGTH_SHORT).show();
            return;
        }

        ClipData clip = clipboard.getPrimaryClip();
        if (clip == null || clip.getItemCount() == 0) return;

        CharSequence value = clip.getItemAt(0).coerceToText(this);
        if (value == null) return;

        String text = value.toString().trim();
        if (text.isEmpty()) return;

        priority = "IMPORTANT";
        if (text.startsWith("!!")) {
            priority = "VERY IMPORTANT";
            text = text.substring(2).trim();
        } else if (text.startsWith("!")) {
            text = text.substring(1).trim();
        }

        noteText = text;
        priorityView.setText(priority);
        noteView.setText(noteText);
        generateButton.setEnabled(true);
    }

    private void generatePdf() {
        if (noteText.isEmpty()) return;
        try {
            File pdf = makePdf(priority, noteText);
            Toast.makeText(this, "PDF ready: " + pdf.getName(), Toast.LENGTH_LONG).show();
        } catch (Exception e) {
            Toast.makeText(this, "PDF error: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    private File makePdf(String priority, String text) throws IOException {
        final int width = 595;
        final int height = 842;
        final float half = height / 2f;
        final float margin = 24f;

        PdfDocument document = new PdfDocument();
        PdfDocument.Page page = document.startPage(
                new PdfDocument.PageInfo.Builder(width, height, 1).create()
        );

        Canvas canvas = page.getCanvas();
        canvas.drawColor(Color.WHITE);

        Paint border = new Paint(Paint.ANTI_ALIAS_FLAG);
        border.setColor(Color.BLACK);
        border.setStyle(Paint.Style.STROKE);
        border.setStrokeWidth(2f);
        canvas.drawRect(margin, margin, width - margin, half - margin, border);

        Paint cut = new Paint(Paint.ANTI_ALIAS_FLAG);
        cut.setColor(Color.LTGRAY);
        canvas.drawLine(0, half, width, half, cut);

        Paint heading = new Paint(Paint.ANTI_ALIAS_FLAG);
        heading.setColor(Color.BLACK);
        heading.setTypeface(Typeface.DEFAULT_BOLD);
        heading.setTextAlign(Paint.Align.CENTER);
        heading.setTextSize(priority.equals("VERY IMPORTANT") ? 28f : 32f);
        canvas.drawText(priority, width / 2f, 72f, heading);

        Paint body = new Paint(Paint.ANTI_ALIAS_FLAG);
        body.setColor(Color.BLACK);

        float left = 52f;
        float maxWidth = width - 104f;
        float top = 120f;
        float maxHeight = half - margin - 24f - top;

        List<String> lines = null;
        float chosen = 22f;

        for (float size = 22f; size >= 10f; size -= 1f) {
            body.setTextSize(size);
            List<String> candidate = wrap(text, body, maxWidth);
            float lh = size * 1.28f;
            if (candidate.size() * lh <= maxHeight) {
                chosen = size;
                lines = candidate;
                break;
            }
        }

        if (lines == null) {
            chosen = 10f;
            body.setTextSize(chosen);
            lines = wrap(text, body, maxWidth);
        }

        body.setTextSize(chosen);
        float lineHeight = chosen * 1.28f;
        float y = top;

        for (String line : lines) {
            if (y > half - margin - 20f) break;
            canvas.drawText(line, left, y, body);
            y += lineHeight;
        }

        document.finishPage(page);

        File dir = new File(getCacheDir(), "pdf");
        if (!dir.exists()) dir.mkdirs();

        String stamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(new Date());
        File out = new File(dir, "GKP06_" + stamp + ".pdf");

        try (FileOutputStream stream = new FileOutputStream(out)) {
            document.writeTo(stream);
        } finally {
            document.close();
        }

        return out;
    }

    private List<String> wrap(String text, Paint paint, float width) {
        List<String> out = new ArrayList<>();
        for (String p : text.split("\n", -1)) {
            if (p.trim().isEmpty()) {
                out.add("");
                continue;
            }

            String current = "";
            for (String word : p.trim().split("\\s+")) {
                String trial = current.isEmpty() ? word : current + " " + word;
                if (paint.measureText(trial) <= width) {
                    current = trial;
                } else {
                    if (!current.isEmpty()) out.add(current);
                    current = word;
                }
            }
            if (!current.isEmpty()) out.add(current);
        }
        return out;
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }
}
