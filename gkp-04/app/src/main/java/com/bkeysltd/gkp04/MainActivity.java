package com.bkeysltd.gkp04;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Typeface;
import android.graphics.pdf.PdfDocument;
import android.os.Bundle;
import android.os.CancellationSignal;
import android.os.ParcelFileDescriptor;
import android.print.PageRange;
import android.print.PrintAttributes;
import android.print.PrintDocumentAdapter;
import android.print.PrintDocumentInfo;
import android.print.PrintManager;
import android.view.Gravity;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class MainActivity extends Activity {
    private static final String VERSION = "v.00.b.04";
    private File currentPdf;
    private Button printButton;
    private TextView status;

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
        root.setGravity(Gravity.CENTER);
        root.setPadding(dp(28), dp(28), dp(28), dp(28));
        root.setBackgroundColor(Color.WHITE);

        TextView title = new TextView(this);
        title.setText("GKP 04");
        title.setTextSize(32);
        title.setTypeface(Typeface.DEFAULT_BOLD);
        title.setTextColor(Color.BLACK);
        title.setGravity(Gravity.CENTER);
        root.addView(title);

        status = new TextView(this);
        status.setText("Send a note from Google Keep");
        status.setTextSize(18);
        status.setTextColor(Color.DKGRAY);
        status.setGravity(Gravity.CENTER);
        status.setPadding(0, dp(20), 0, dp(28));
        root.addView(status);

        printButton = new Button(this);
        printButton.setText("PRINT NOW");
        printButton.setTextSize(22);
        printButton.setEnabled(false);
        printButton.setOnClickListener(v -> printNow());
        LinearLayout.LayoutParams bp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, dp(72));
        root.addView(printButton, bp);

        TextView version = new TextView(this);
        version.setText("\n" + VERSION);
        version.setTextColor(Color.GRAY);
        version.setGravity(Gravity.CENTER);
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
        if (subject != null && subject.length() > 0) {
            String s = subject.toString().trim();
            if (!s.isEmpty() && !text.startsWith(s)) {
                text = s + "\n\n" + text;
            }
        }

        if (text.isEmpty()) {
            Toast.makeText(this, "No note text received.", Toast.LENGTH_LONG).show();
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
            currentPdf = makePdf(priority, text);
            status.setText("A4 PDF READY");
            printButton.setEnabled(true);
        } catch (Exception e) {
            currentPdf = null;
            printButton.setEnabled(false);
            status.setText("PDF ERROR");
            Toast.makeText(this, e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    private void printNow() {
        if (currentPdf == null || !currentPdf.exists()) {
            Toast.makeText(this, "Send a note from Google Keep first.", Toast.LENGTH_SHORT).show();
            return;
        }

        PrintManager manager = (PrintManager) getSystemService(Context.PRINT_SERVICE);
        PrintAttributes attributes = new PrintAttributes.Builder()
                .setMediaSize(PrintAttributes.MediaSize.ISO_A4)
                .setColorMode(PrintAttributes.COLOR_MODE_MONOCHROME)
                .build();

        manager.print("GKP_A4_NOTICE", new PdfPrintAdapter(currentPdf), attributes);
    }

    private static class PdfPrintAdapter extends PrintDocumentAdapter {
        private final File file;

        PdfPrintAdapter(File file) {
            this.file = file;
        }

        @Override
        public void onLayout(PrintAttributes oldAttributes, PrintAttributes newAttributes,
                             CancellationSignal cancellationSignal,
                             LayoutResultCallback callback, Bundle extras) {
            if (cancellationSignal.isCanceled()) {
                callback.onLayoutCancelled();
                return;
            }

            PrintDocumentInfo info = new PrintDocumentInfo.Builder(file.getName())
                    .setContentType(PrintDocumentInfo.CONTENT_TYPE_DOCUMENT)
                    .setPageCount(1)
                    .build();

            callback.onLayoutFinished(info, true);
        }

        @Override
        public void onWrite(PageRange[] pages, ParcelFileDescriptor destination,
                            CancellationSignal cancellationSignal,
                            WriteResultCallback callback) {
            try (FileInputStream in = new FileInputStream(file);
                 FileOutputStream out = new FileOutputStream(destination.getFileDescriptor())) {
                byte[] buffer = new byte[8192];
                int len;
                while ((len = in.read(buffer)) > 0) {
                    if (cancellationSignal.isCanceled()) {
                        callback.onWriteCancelled();
                        return;
                    }
                    out.write(buffer, 0, len);
                }
                callback.onWriteFinished(new PageRange[]{PageRange.ALL_PAGES});
            } catch (Exception e) {
                callback.onWriteFailed(e.getMessage());
            }
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
        canvas.drawText("GKP 04 " + VERSION, width - margin, half - 8, version);

        document.finishPage(page);

        File dir = new File(getCacheDir(), "pdf");
        if (!dir.exists() && !dir.mkdirs()) {
            document.close();
            throw new IOException("Cannot create PDF folder");
        }

        String stamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(new Date());
        File out = new File(dir, "GKP04_" + stamp + ".pdf");

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
