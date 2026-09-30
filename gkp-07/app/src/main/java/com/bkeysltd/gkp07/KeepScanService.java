package com.bkeysltd.gkp07;

import android.accessibilityservice.AccessibilityService;
import android.content.SharedPreferences;
import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityNodeInfo;

import java.util.LinkedHashSet;
import java.util.Set;

public class KeepScanService extends AccessibilityService {
    private final Set<String> notes = new LinkedHashSet<>();

    @Override
    public void onAccessibilityEvent(AccessibilityEvent event) {
        AccessibilityNodeInfo root = getRootInActiveWindow();
        if (root == null) return;

        scan(root);
        saveNotes();

        AccessibilityNodeInfo scrollable = findScrollable(root);
        if (scrollable != null) {
            scrollable.performAction(AccessibilityNodeInfo.ACTION_SCROLL_FORWARD);
        }
    }

    @Override
    public void onInterrupt() {}

    private void scan(AccessibilityNodeInfo node) {
        if (node == null) return;

        String subtree = collectText(node).trim();
        String upper = subtree.toUpperCase();

        if ((upper.contains("VERY IMPORTANT") || upper.contains("IMPORTANT"))
                && subtree.length() > 5 && subtree.length() < 4000) {
            notes.add(clean(subtree));
        }

        for (int i = 0; i < node.getChildCount(); i++) {
            scan(node.getChild(i));
        }
    }

    private String collectText(AccessibilityNodeInfo node) {
        if (node == null) return "";
        StringBuilder sb = new StringBuilder();

        if (node.getText() != null) {
            sb.append(node.getText()).append("\n");
        }
        if (node.getContentDescription() != null) {
            sb.append(node.getContentDescription()).append("\n");
        }

        for (int i = 0; i < node.getChildCount(); i++) {
            sb.append(collectText(node.getChild(i)));
        }
        return sb.toString();
    }

    private String clean(String s) {
        return s.replaceAll("(?m)^[\\s]*$", "")
                .replaceAll("\n{3,}", "\n\n")
                .trim();
    }

    private AccessibilityNodeInfo findScrollable(AccessibilityNodeInfo node) {
        if (node == null) return null;
        if (node.isScrollable()) return node;

        for (int i = 0; i < node.getChildCount(); i++) {
            AccessibilityNodeInfo found = findScrollable(node.getChild(i));
            if (found != null) return found;
        }
        return null;
    }

    private void saveNotes() {
        SharedPreferences p = getSharedPreferences("gkp07", MODE_PRIVATE);
        p.edit().putStringSet("notes", new LinkedHashSet<>(notes)).apply();
    }
}
