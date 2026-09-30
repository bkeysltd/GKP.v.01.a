package com.bkeysltd.gkp05;

import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Typeface;
import android.graphics.pdf.PdfDocument;
import android.net.Uri;
import android.os.Bundle;
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
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class MainActivity extends Activity {
    private static final String VERSION = "v.00.b.05";

    private String noteText = "";
    private String priority = "IMPORTANT";
    private TextView priorityView;
    private TextView noteView;
    private TextView statusView;
    private Button generateButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        buildScreen();
        handleIntent(getIntent());
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);
        handleIntent(intent);
    }

    private void buildScreen() {
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(dp(18), dp(18), dp(18), dp(18));
        root.setBackgroundColor(Color.WHITE);

        TextView title = new TextView(this);
        title.setText("GKP 05");
        title.setTextSize(30);
        title.setTypeface(Typeface.DEFAULT_BOLD);
        title.setTextColor(Color.BLACK);
        root.addView(title);

        priorityView = new TextView(this);
        priorityView.setText("WAITING FOR GOOGLE KEEP NOTE");
        priorityView.setTextSize(18);
        priorityView.setTypeface(Typeface.DEFAULT_BOLD);
        priorityView.setTextColor(Color.DKGRAY);
        priorityView.setPadding(0, dp(12), 0, dp(8));
        root.addView(priorityView);

        ScrollView scroll = new ScrollView(this);
        scroll.setBackgroundColor(0xFFF3F3F3);
        LinearLayout.LayoutParams sp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f);
        sp.setMargins(0, dp(8), 0, dp(12));
        root.addView(scroll, sp);

        noteView = new TextView(this);
        noteView.setText("Open Google Keep → note → Send → GKP 05");
        noteView.setTextSize(19);
        noteView.setTextColor(Color.BLACK);
        noteView.setPadding(dp(14), dp(14), dp(14), dp(14));
        noteView.setTextIsSelectable(true);
        scroll.addView(noteView);

        statusView = new TextView(this);
        statusView.setText("Check the note before generating the PDF.");
        statusView.setTextSize(15);
        statusView.setTextColor(Color.GRAY);
        statusView.setGravity(Gravity.CENTER);
        statusView.setPadding(0, 0, 0, dp(10));
        root.addView(statusView);

        generateButton = new Button(this);
        generateButton.setText("GENERATE PDF");
        generateButton.setTextSize(21);
        generateButton.setEnabled(false);
        generateButton.setOnClickListener(v -> generatePdf());
        root.addView(generateButton, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, dp(68)));

        TextView version = new TextView(this);
        version.setText("GKP 05 " + VERSION);
        version.setTextColor(Color.GRAY);
        version.setGravity(Gravity.END);
        version.setPadding(0, dp(10), 0, 0);
        root.addView(version);

        setContentView(root);
    }

    private void handleIntent(Intent intent) {
        if (intent == null || !Intent.ACTION_SEND.equals(intent.getAction())
                || intent.getType() == null || !intent.getType().startsWith("text/")) {
            return;
        }

        CharSequence body = intent.getCharSequenceExtra(Intent.EXTRA_TEXT);
        CharSequence subject = intent.getCharSequenceExtra(Intent.EXTRA_SUBJECT);

        String text = body == null ? "" : body.toString().trim();

        if (subject != null) {
            String s = subject.toString().trim();
            if (!s.isEmpty() && !text.startsWith(s)) {
                text = s + "\n\n" + text;
            }
        }

        if (text.isEmpty()) {
            Toast.makeText(this, "No note text received from Google Keep.", Toast.LENGTH_LONG).show();
            return;
        }

        priority = "IMPORTANT";

        if (text.startsWith("!!")) {
            priority = "VERY IMPORTANT";
            text = text.substring(2).trim();
        } else if (text.startsWith("!")) {
            priority = "IMPORTANT";
            text = text.substring(1).trim();
        }

        noteText = text;
        priorityView.setText(priority);
        noteView.setText(noteText);
        statusView.setText("Check the note. If correct, press GENERATE PDF.");
        generateButton.setEnabled(true);
    }

    private void generatePdf() {
        if (noteText.trim().isEmpty()) {
            Toast.makeText(this, "No Google Keep note loaded.", Toast.LENGTH_SHORT).show();
            return;
        }

        try {
            File pdf = makePdf(priority, noteText);
            statusView.setText("A4 PDF READY");
            openPdf(pdf);
        } catch (Exception e) {
            statusView.setText("PDF ERROR");
            Toast.makeText(this, "PDF error: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    private void openPdf(File pdf) {
        Uri uri = FileProvider.getUriForFile(
                this,
                getPackageName() + ".files",
                pdf
        );

        Intent view = new Intent(Intent.ACTION_VIEW);
        view.setDataAndType(uri, "application/pdf");
        view.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);

        try {
            startActivity(view);
        } catch (ActivityNotFoundException e) {
            Intent chooser = new Intent(Intent.ACTION_SEND);
            chooser.setType("application/pdf");
            chooser.putExtra(Intent.EXTRA_STREAM, uri);
            chooser.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            startActivity(Intent.createChooser(chooser, "Open or save PDF"));
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
        canvas.drawText("GKP 05 " + VERSION, width - margin, half - 8, version);

        document.finishPage(page);

        File dir = new File(getCacheDir(), "pdf");
        if (!dir.exists() && !dir.mkdirs()) {
            document.close();
            throw new IOException("Cannot create PDF folder");
        }

        String stamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(new Date());
        File out = new File(dir, "GKP05_" + stamp + ".pdf");

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
