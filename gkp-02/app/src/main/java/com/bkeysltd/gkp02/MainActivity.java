package com.bkeysltd.gkp02;

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
import android.view.ViewGroup;
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
    private static final String VERSION = "v.00.b.02";
    private EditText noteEdit;
    private RadioButton important;
    private RadioButton veryImportant;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        buildScreen();
        loadSharedText(getIntent());
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);
        loadSharedText(intent);
    }

    private void buildScreen() {
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(dp(18), dp(18), dp(18), dp(18));
        root.setBackgroundColor(Color.WHITE);

        TextView title = new TextView(this);
        title.setText("GKP 02");
        title.setTextColor(Color.BLACK);
        title.setTextSize(28);
        title.setTypeface(Typeface.DEFAULT_BOLD);
        root.addView(title);

        TextView flow = new TextView(this);
        flow.setText("Google Keep → Share → GKP 02 → PDF → Telegram");
        flow.setTextColor(Color.DKGRAY);
        flow.setTextSize(15);
        flow.setPadding(0, dp(6), 0, dp(12));
        root.addView(flow);

        RadioGroup priorities = new RadioGroup(this);
        priorities.setOrientation(RadioGroup.HORIZONTAL);

        important = new RadioButton(this);
        important.setText("IMPORTANT");
        important.setChecked(true);
        priorities.addView(important);

        veryImportant = new RadioButton(this);
        veryImportant.setText("VERY IMPORTANT");
        priorities.addView(veryImportant);

        root.addView(priorities);

        noteEdit = new EditText(this);
        noteEdit.setHint("Paste or share a Google Keep note");
        noteEdit.setGravity(Gravity.TOP | Gravity.START);
        noteEdit.setTextSize(18);
        noteEdit.setBackgroundColor(0xFFF3F3F3);
        noteEdit.setPadding(dp(12), dp(12), dp(12), dp(12));

        LinearLayout.LayoutParams noteParams =
                new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f);
        noteParams.setMargins(0, dp(12), 0, dp(12));
        root.addView(noteEdit, noteParams);

        Button create = new Button(this);
        create.setText("CREATE PDF");
        create.setOnClickListener(v -> createAndShare());
        root.addView(create);

        TextView version = new TextView(this);
        version.setText("GKP 02 " + VERSION);
        version.setTextColor(Color.GRAY);
        version.setGravity(Gravity.END);
        version.setPadding(0, dp(10), 0, 0);
        root.addView(version);

        setContentView(root);
    }

    private void loadSharedText(Intent intent) {
        if (intent == null || !Intent.ACTION_SEND.equals(intent.getAction())) return;
        if (intent.getType() == null || !intent.getType().startsWith("text/")) return;

        CharSequence body = intent.getCharSequenceExtra(Intent.EXTRA_TEXT);
        CharSequence subject = intent.getCharSequenceExtra(Intent.EXTRA_SUBJECT);

        String text = body == null ? "" : body.toString().trim();
        if (!TextUtils.isEmpty(subject) && !text.startsWith(subject.toString().trim())) {
            text = subject.toString().trim() + "\n\n" + text;
        }

        if (text.startsWith("!!")) {
            veryImportant.setChecked(true);
            text = text.substring(2).trim();
        } else if (text.startsWith("!")) {
            important.setChecked(true);
            text = text.substring(1).trim();
        }

        noteEdit.setText(text);
        noteEdit.setSelection(noteEdit.length());
    }

    private void createAndShare() {
        String note = noteEdit.getText().toString().trim();
        if (note.isEmpty()) {
            Toast.makeText(this, "Write or share a note first.", Toast.LENGTH_SHORT).show();
            return;
        }

        String priority = veryImportant.isChecked() ? "VERY IMPORTANT" : "IMPORTANT";

        try {
            File pdf = makePdf(priority, note);
            Uri uri = FileProvider.getUriForFile(
                    this,
                    getPackageName() + ".files",
                    pdf
            );

            Intent send = new Intent(Intent.ACTION_SEND);
            send.setType("application/pdf");
            send.putExtra(Intent.EXTRA_STREAM, uri);
            send.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);

            Intent telegram = new Intent(send);
            telegram.setPackage("org.telegram.messenger");

            try {
                startActivity(telegram);
            } catch (Exception noTelegram) {
                startActivity(Intent.createChooser(send, "Send PDF"));
            }
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
        int allowed = Math.max(1, (int)(maxHeight / lineHeight));

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
        canvas.drawText("GKP 02 " + VERSION, width - margin, half - 8, version);

        document.finishPage(page);

        File dir = new File(getCacheDir(), "pdf");
        if (!dir.exists() && !dir.mkdirs()) {
            document.close();
            throw new IOException("Cannot create PDF folder");
        }

        String stamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(new Date());
        File out = new File(dir, "GKP_NEW_" + stamp + ".pdf");

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
