package com.sza.s0013probe;

import android.accessibilityservice.AccessibilityService;
import android.graphics.Rect;
import android.os.Build;
import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityNodeInfo;
import android.view.accessibility.AccessibilityWindowInfo;

import java.util.List;

/**
 * S0013 stand instrument, never shipped: reads the window tree the way a screen reader does and writes it
 * in the uiautomator dump shape, without waiting for the UI to go idle. A live screen (the dashboard's
 * compass) never idles, so uiautomator cannot dump it at all.
 */
public class ProbeService extends AccessibilityService {

    static volatile ProbeService instance;

    @Override
    protected void onServiceConnected() {
        instance = this;
    }

    @Override
    public void onAccessibilityEvent(AccessibilityEvent event) {
    }

    @Override
    public void onInterrupt() {
    }

    @Override
    public void onDestroy() {
        instance = null;
        super.onDestroy();
    }

    String dump(boolean allWindows) {
        StringBuilder sb = new StringBuilder(64 * 1024);
        sb.append("<?xml version='1.0' encoding='UTF-8' standalone='yes' ?><hierarchy rotation=\"0\" source=\"s0013probe\">");
        if (allWindows) {
            List<AccessibilityWindowInfo> windows = getWindows();
            for (int i = windows.size() - 1; i >= 0; i--) {
                AccessibilityNodeInfo root = windows.get(i).getRoot();
                if (root != null) append(sb, root, 0);
            }
        } else {
            AccessibilityNodeInfo root = getRootInActiveWindow();
            if (root != null) append(sb, root, 0);
        }
        sb.append("</hierarchy>");
        return sb.toString();
    }

    private static void append(StringBuilder sb, AccessibilityNodeInfo n, int index) {
        Rect r = new Rect();
        n.getBoundsInScreen(r);
        sb.append("<node index=\"").append(index).append('"');
        attr(sb, "text", n.getText());
        attr(sb, "resource-id", n.getViewIdResourceName());
        attr(sb, "class", n.getClassName());
        attr(sb, "package", n.getPackageName());
        attr(sb, "content-desc", n.getContentDescription());
        flag(sb, "checkable", n.isCheckable());
        flag(sb, "checked", n.isChecked());
        flag(sb, "clickable", n.isClickable());
        flag(sb, "enabled", n.isEnabled());
        flag(sb, "focusable", n.isFocusable());
        flag(sb, "focused", n.isFocused());
        flag(sb, "scrollable", n.isScrollable());
        flag(sb, "long-clickable", n.isLongClickable());
        flag(sb, "password", n.isPassword());
        flag(sb, "selected", n.isSelected());
        flag(sb, "visible-to-user", n.isVisibleToUser());
        flag(sb, "accessibility-focused", n.isAccessibilityFocused());
        if (Build.VERSION.SDK_INT >= 30) attr(sb, "state-description", n.getStateDescription());
        if (Build.VERSION.SDK_INT >= 26) attr(sb, "hint", n.getHintText());
        attr(sb, "error", n.getError());
        sb.append(" bounds=\"[").append(r.left).append(',').append(r.top).append("][")
                .append(r.right).append(',').append(r.bottom).append("]\"");
        int count = n.getChildCount();
        if (count == 0) {
            sb.append(" />");
            return;
        }
        sb.append('>');
        for (int i = 0; i < count; i++) {
            AccessibilityNodeInfo c = n.getChild(i);
            if (c != null) append(sb, c, i);
        }
        sb.append("</node>");
    }

    private static void flag(StringBuilder sb, String name, boolean v) {
        sb.append(' ').append(name).append("=\"").append(v).append('"');
    }

    private static void attr(StringBuilder sb, String name, CharSequence v) {
        sb.append(' ').append(name).append("=\"");
        if (v != null) {
            for (int i = 0; i < v.length(); i++) {
                char ch = v.charAt(i);
                switch (ch) {
                    case '&': sb.append("&amp;"); break;
                    case '<': sb.append("&lt;"); break;
                    case '>': sb.append("&gt;"); break;
                    case '"': sb.append("&quot;"); break;
                    case '\n': sb.append("&#10;"); break;
                    case '\r': break;
                    default:
                        if (ch >= 0x20 || ch == '\t') sb.append(ch);
                }
            }
        }
        sb.append('"');
    }
}
