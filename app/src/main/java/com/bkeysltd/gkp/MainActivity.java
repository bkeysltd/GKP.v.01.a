package com.bkeysltd.gkp;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Typeface;
import android.graphics.pdf.PdfDocument;
import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.Gravity;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.core.content.FileProvider;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class MainActivity extends Activity {

    private static final String VERSION = "v.00.a.00";
    private EditText noteEdit;
    private RadioButton importantButton;
    private RadioButton veryImportantButton;

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
        int pad = dp(18);
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(pad, pad, pad, pad);
        root.setBackgroundColor(Color.WHITE);

        TextView title = new TextView(this);
        title.setText("GKP — GOOGLE KEEP PRINTER");
        title.setTextSize(24);
        title.setTypeface(Typeface.DEFAULT_BOLD);
        title.setTextColor(Color.BLACK);
        root.addView(title);

        TextView sub = new TextView(this);
        sub.setText("Share a Google Keep note here. The app creates an A5 notice on the top half of A4 and sends the PDF to Telegram.");
        sub.setTextSize(15);
        sub.setTextColor(Color.DKGRAY);
        sub.setPadding(0, dp(6), 0, dp(14));
        root.addView(sub);

        RadioGroup group = new RadioGroup(this);
        group.setOrientation(RadioGroup.HORIZONTAL);

        importantButton = new RadioButton(this);
        importantButton.setText("IMPORTANT");
        importantButton.setChecked(true);
        group.addView(importantButton);

        veryImportantButton = new RadioButton(this);
        veryImportantButton.setText("VERY IMPORTANT");
        group.addView(veryImportantButton);
        root.addView(group);

        noteEdit = new EditText(this);
        noteEdit.setHint("Google Keep note...");
        noteEdit.setGravity(Gravity.TOP | Gravity.START);
        noteEdit.setTextSize(18);
        noteEdit.setMinLines(10);
        noteEdit.setBackgroundColor(0xFFF4F4F4);
        noteEdit.setPadding(dp(12), dp(12), dp(12), dp(12));
        LinearLayout.LayoutParams editParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, 0, 1f);
        editParams.setMargins(0, dp(12), 0, dp(12));
        root.addView(noteEdit, editParams);

        Button send = new Button(this);
        send.setText("CREATE PDF & SEND TO TELEGRAM");
        send.setOnClickListener(v -> createAndShare());
        root.addView(send);

        TextView version = new TextView(this);
        version.setText("GKP " + VERSION);
        version.setGravity(Gravity.END);
        version.setTextColor(Color.GRAY);
        version.setPadding(0, dp(12), 0, 0);
        root.addView(version);

        setContentView(root);
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

        applyPriorityMarker(text);
    }

    private void applyPriorityMarker(String raw) {
        String t = raw == null ? "" : raw.trim();
        if (t.startsWith("!!")) {
            veryImportantButton.setChecked(true);
            t = t.substring(2).trim();
        } else if (t.startsWith("!")) {
            importantButton.setChecked(true);
            t = t.substring(1).trim();
        }
        noteEdit.setText(t);
        noteEdit.setSelection(noteEdit.getText().length());
    }

    private void createAndShare() {
        String note = noteEdit.getText().toString().trim();
        if (note.isEmpty()) {
            Toast.makeText(this, "The note is empty.", Toast.LENGTH_SHORT).show();
            return;
        }

        String priority = veryImportantButton.isChecked() ? "VERY IMPORTANT" : "IMPORTANT";

        try {
            File pdf = createPdf(priority, note);
            shareToTelegram(pdf, priority);
        } catch (Exception e) {
            Toast.makeText(this, "Could not create PDF: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    private File createPdf(String priority, String note) throws IOException {
        final int pageW = 595;
        final int pageH = 842;
        final float halfH = pageH / 2f;
        final float margin = 24f;

        PdfDocument doc = new PdfDocument();
        PdfDocument.PageInfo info = new PdfDocument.PageInfo.Builder(pageW, pageH, 1).create();
        PdfDocument.Page page = doc.startPage(info);
        Canvas canvas = page.getCanvas();
        canvas.drawColor(Color.WHITE);

        Paint border = new Paint(Paint.ANTI_ALIAS_FLAG);
        border.setStyle(Paint.Style.STROKE);
        border.setStrokeWidth(2f);
        border.setColor(Color.BLACK);
        canvas.drawRect(margin, margin, pageW - margin, halfH - margin, border);

        Paint guide = new Paint(Paint.ANTI_ALIAS_FLAG);
        guide.setColor(Color.LTGRAY);
        guide.setStrokeWidth(1f);
        canvas.drawLine(0, halfH, pageW, halfH, guide);

        Paint titlePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        titlePaint.setColor(Color.BLACK);
        titlePaint.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.BOLD));
        titlePaint.setTextAlign(Paint.Align.CENTER);
        titlePaint.setTextSize(priority.equals("VERY IMPORTANT") ? 28f : 32f);
        canvas.drawText(priority, pageW / 2f, 72f, titlePaint);

        Paint line = new Paint(Paint.ANTI_ALIAS_FLAG);
        line.setColor(Color.BLACK);
        line.setStrokeWidth(1.5f);
        canvas.drawLine(margin + 28, 88, pageW - margin - 28, 88, line);

        float left = margin + 28;
        float right = pageW - margin - 28;
        float top = 118;
        float bottom = halfH - margin - 16;
        float maxWidth = right - left;
        float maxHeight = bottom - top;

        Paint bodyPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        bodyPaint.setColor(Color.BLACK);
        bodyPaint.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.NORMAL));

        float chosenSize = 22f;
        List<String> lines = null;
        for (float size = 22f; size >= 10f; size -= 1f) {
            bodyPaint.setTextSize(size);
            List<String> test = wrapText(note, bodyPaint, maxWidth);
            float leading = size * 1.28f;
            if (test.size() * leading <= maxHeight) {
                chosenSize = size;
                lines = test;
                break;
            }
        }
        if (lines == null) {
            chosenSize = 10f;
            bodyPaint.setTextSize(chosenSize);
            lines = wrapText(note, bodyPaint, maxWidth);
        }

        bodyPaint.setTextSize(chosenSize);
        float leading = chosenSize * 1.28f;
        int maxLines = Math.max(1, (int) (maxHeight / leading));
        if (lines.size() > maxLines) {
            lines = new ArrayList<>(lines.subList(0, maxLines));
            int last = lines.size() - 1;
            lines.set(last, lines.get(last) + "…");
        }

        float y = top;
        for (String s : lines) {
            canvas.drawText(s, left, y, bodyPaint);
            y += leading;
        }

        Paint versionPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        versionPaint.setColor(Color.GRAY);
        versionPaint.setTextSize(7f);
        versionPaint.setTextAlign(Paint.Align.RIGHT);
        canvas.drawText("GKP " + VERSION, pageW - margin, halfH - 8, versionPaint);

        doc.finishPage(page);

        File dir = new File(getCacheDir(), "pdf");
        if (!dir.exists() && !dir.mkdirs()) throw new IOException("Cannot create PDF folder");
        String stamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(new Date());
        File file = new File(dir, "GKP_" + priority.replace(' ', '_') + "_" + stamp + ".pdf");
        try (FileOutputStream out = new FileOutputStream(file)) {
            doc.writeTo(out);
        } finally {
            doc.close();
        }
        return file;
    }

    private List<String> wrapText(String text, Paint paint, float maxWidth) {
        List<String> result = new ArrayList<>();
        String normalized = text.replace("\r\n", "\n").replace('\r', '\n');
        String[] paragraphs = normalized.split("\n", -1);
        for (String paragraph : paragraphs) {
            if (paragraph.trim().isEmpty()) {
                result.add("");
                continue;
            }
            String[] words = paragraph.trim().split("\\s+");
            String current = "";
            for (String word : words) {
                String trial = current.isEmpty() ? word : current + " " + word;
                if (paint.measureText(trial) <= maxWidth) {
                    current = trial;
                } else {
                    if (!current.isEmpty()) result.add(current);
                    current = word;
                }
            }
            if (!current.isEmpty()) result.add(current);
        }
        return result;
    }

    private void shareToTelegram(File pdf, String priority) {
        Uri uri = FileProvider.getUriForFile(this, getPackageName() + ".fileprovider", pdf);

        Intent share = new Intent(Intent.ACTION_SEND);
        share.setType("application/pdf");
        share.putExtra(Intent.EXTRA_STREAM, uri);
        share.putExtra(Intent.EXTRA_TEXT, priority);
        share.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);

        Intent telegram = new Intent(share);
        telegram.setPackage("org.telegram.messenger");
        try {
            startActivity(telegram);
        } catch (Exception ignored) {
            startActivity(Intent.createChooser(share, "Send PDF"));
        }
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }
}
