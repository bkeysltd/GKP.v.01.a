package com.bkeysltd.gkp03;

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
import android.widget.LinearLayout;
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
    private static final String VERSION = "v.00.b.03";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        handleIntent(getIntent());
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);
        handleIntent(intent);
    }

    private void handleIntent(Intent intent) {
        if (intent != null && Intent.ACTION_SEND.equals(intent.getAction())
                && intent.getType() != null && intent.getType().startsWith("text/")) {
            CharSequence body = intent.getCharSequenceExtra(Intent.EXTRA_TEXT);
            CharSequence subject = intent.getCharSequenceExtra(Intent.EXTRA_SUBJECT);

            String text = body == null ? "" : body.toString().trim();
            if (!TextUtils.isEmpty(subject) && !text.startsWith(subject.toString().trim())) {
                text = subject.toString().trim() + "\n\n" + text;
            }

            if (text.isEmpty()) {
                showMessage("No note text received from Google Keep.");
                return;
            }

            String priority = "IMPORTANT";
            if (text.startsWith("!!")) {
                priority = "VERY IMPORTANT";
                text = text.substring(2).trim();
            } else if (text.startsWith("!")) {
                text = text.substring(1).trim();
            }

            try {
                File pdf = makePdf(priority, text);
                sharePdf(pdf);
            } catch (Exception e) {
                showMessage("PDF error: " + e.getMessage());
            }
        } else {
            showReadyScreen();
        }
    }

    private void showReadyScreen() {
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setGravity(Gravity.CENTER);
        root.setPadding(dp(24), dp(24), dp(24), dp(24));
        root.setBackgroundColor(Color.WHITE);

        TextView title = new TextView(this);
        title.setText("GKP 03");
        title.setTextSize(30);
        title.setTypeface(Typeface.DEFAULT_BOLD);
        title.setTextColor(Color.BLACK);
        title.setGravity(Gravity.CENTER);
        root.addView(title);

        TextView instructions = new TextView(this);
        instructions.setText("\nOpen Google Keep\n→ open the note\n→ Share / Send\n→ choose GKP 03\n\nPDF will be created automatically.");
        instructions.setTextSize(20);
        instructions.setTextColor(Color.DKGRAY);
        instructions.setGravity(Gravity.CENTER);
        root.addView(instructions);

        TextView version = new TextView(this);
        version.setText("\n" + VERSION);
        version.setTextColor(Color.GRAY);
        version.setGravity(Gravity.CENTER);
        root.addView(version);

        setContentView(root);
    }

    private void showMessage(String msg) {
        Toast.makeText(this, msg, Toast.LENGTH_LONG).show();
        showReadyScreen();
    }

    private void sharePdf(File pdf) {
        Uri uri = FileProvider.getUriForFile(this, getPackageName() + ".files", pdf);

        Intent send = new Intent(Intent.ACTION_SEND);
        send.setType("application/pdf");
        send.putExtra(Intent.EXTRA_STREAM, uri);
        send.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);

        Intent telegram = new Intent(send);
        telegram.setPackage("org.telegram.messenger");

        try {
            startActivity(telegram);
        } catch (Exception e) {
            startActivity(Intent.createChooser(send, "Send PDF"));
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
        cut.setStrokeWidth(1f);
        canvas.drawLine(0, half, width, half, cut);

        Paint heading = new Paint(Paint.ANTI_ALIAS_FLAG);
        heading.setColor(Color.BLACK);
        heading.setTypeface(Typeface.DEFAULT_BOLD);
        heading.setTextAlign(Paint.Align.CENTER);
        heading.setTextSize(priority.equals("VERY IMPORTANT") ? 28f : 32f);
        canvas.drawText(priority, width / 2f, 72f, heading);

        Paint bodyPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        bodyPaint.setColor(Color.BLACK);

        float left = 52f;
        float maxWidth = width - 104f;
        float top = 120f;
        float maxHeight = half - margin - 24f - top;

        List<String> lines = null;
        float chosenSize = 22f;

        for (float size = 22f; size >= 10f; size -= 1f) {
            bodyPaint.setTextSize(size);
            List<String> candidate = wrap(text, bodyPaint, maxWidth);
            float lineHeight = size * 1.28f;
            if (candidate.size() * lineHeight <= maxHeight) {
                chosenSize = size;
                lines = candidate;
                break;
            }
        }

        if (lines == null) {
            chosenSize = 10f;
            bodyPaint.setTextSize(chosenSize);
            lines = wrap(text, bodyPaint, maxWidth);
        }

        bodyPaint.setTextSize(chosenSize);
        float lineHeight = chosenSize * 1.28f;
        int allowed = Math.max(1, (int) (maxHeight / lineHeight));

        if (lines.size() > allowed) {
            lines = new ArrayList<>(lines.subList(0, allowed));
            int last = lines.size() - 1;
            lines.set(last, lines.get(last) + "…");
        }

        float y = top;
        for (String line : lines) {
            canvas.drawText(line, left, y, bodyPaint);
            y += lineHeight;
        }

        Paint version = new Paint(Paint.ANTI_ALIAS_FLAG);
        version.setTextSize(7f);
        version.setColor(Color.GRAY);
        version.setTextAlign(Paint.Align.RIGHT);
        canvas.drawText("GKP 03 " + VERSION, width - margin, half - 8, version);

        document.finishPage(page);

        File dir = new File(getCacheDir(), "pdf");
        if (!dir.exists() && !dir.mkdirs()) {
            document.close();
            throw new IOException("Cannot create PDF folder");
        }

        String stamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(new Date());
        File out = new File(dir, "GKP03_" + stamp + ".pdf");

        try (FileOutputStream stream = new FileOutputStream(out)) {
            document.writeTo(stream);
        } finally {
            document.close();
        }

        return out;
    }

    private List<String> wrap(String text, Paint paint, float width) {
        List<String> out = new ArrayList<>();
        String[] paragraphs = text.replace("\r\n", "\n").replace('\r', '\n').split("\n", -1);

        for (String p : paragraphs) {
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
