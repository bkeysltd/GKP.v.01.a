package com.bkeysltd.gkp;

import android.accessibilityservice.AccessibilityService;
import android.content.SharedPreferences;
import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityNodeInfo;

import java.util.LinkedHashSet;
import java.util.Set;

public class KeepAccessibilityService extends AccessibilityService {
    private static final String KEEP_PACKAGE = "com.google.android.keep";
    private static final String PREFS = "gkp";
    private static final String KEY_KEEP_TEXT = "keep_visible_text";

    @Override
    public void onAccessibilityEvent(AccessibilityEvent event) {
        if (event == null || event.getPackageName() == null) return;
        if (!KEEP_PACKAGE.contentEquals(event.getPackageName())) return;

        AccessibilityNodeInfo root = getRootInActiveWindow();
        if (root == null) return;

        LinkedHashSet<String> parts = new LinkedHashSet<>();
        collectVisibleText(root, parts);

        StringBuilder out = new StringBuilder();
        for (String part : parts) {
            String clean = part.trim();
            if (clean.isEmpty()) continue;
            if (out.length() > 0) out.append("\n");
            out.append(clean);
        }

        String text = out.toString().trim();
        if (!text.isEmpty()) {
            SharedPreferences prefs = getSharedPreferences(PREFS, MODE_PRIVATE);
            prefs.edit().putString(KEY_KEEP_TEXT, text).apply();
        }
    }

    private void collectVisibleText(AccessibilityNodeInfo node, Set<String> parts) {
        if (node == null || !node.isVisibleToUser()) return;

        CharSequence text = node.getText();
        if (text != null) {
            String value = text.toString().trim();
            if (!value.isEmpty()) parts.add(value);
        }

        for (int i = 0; i < node.getChildCount(); i++) {
            AccessibilityNodeInfo child = node.getChild(i);
            if (child != null) {
                collectVisibleText(child, parts);
                child.recycle();
            }
        }
    }

    @Override
    public void onInterrupt() {
    }
}
