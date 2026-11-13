package com.sza.tourist.help

import android.app.Dialog
import android.content.Context
import android.content.ContextWrapper
import android.content.res.Resources
import android.view.KeyEvent
import android.view.LayoutInflater
import android.view.View
import android.widget.TextView
import androidx.annotation.StringRes
import androidx.fragment.app.FragmentActivity
import com.google.android.material.appbar.MaterialToolbar
import com.sza.tourist.R

/**
 * The view-level seams of in-app help. Everything here asks [HelpResolver]; nothing decides what a
 * surface's help is.
 */
object HelpUi {

    internal fun hostActivity(context: Context): FragmentActivity? {
        var c: Context? = context
        while (c is ContextWrapper) {
            if (c is FragmentActivity) return c
            c = c.baseContext
        }
        return null
    }

    /** The help of [surfaceId] when named, otherwise the help of the host Activity's class. */
    internal fun targetFor(host: FragmentActivity, surfaceId: String?): HelpTarget? {
        val map = HelpMapStore.get(host)
        return if (surfaceId != null) {
            HelpResolver.resolveAsked(map, surfaceId)
        } else {
            HelpResolver.resolveHost(map, host.javaClass.name)
        }
    }

    /**
     * Rule 2, Android: the help action of the top app bar, before the overflow. Added only when the map gives
     * the toolbar's host screen a help, so a screen without help gets no dead control. Existing actions keep
     * their place - the item is appended to the bar's menu.
     */
    fun installToolbarAction(toolbar: MaterialToolbar) {
        val host = hostActivity(toolbar.context) ?: return
        val target = targetFor(host, null) ?: return
        if (toolbar.menu.findItem(R.id.action_help) != null) return
        toolbar.inflateMenu(R.menu.menu_help)
        toolbar.setOnMenuItemClickListener { item ->
            if (item.itemId == R.id.action_help) {
                HelpLauncher.open(host, target)
                true
            } else {
                false
            }
        }
    }

    /**
     * A dialog title with the `?` button in its trailing corner. Use as `setCustomTitle`; the dialog's own
     * title text stays where it was. [surfaceId] is the dialog's row in the help map.
     */
    fun dialogTitle(context: Context, @StringRes title: Int, surfaceId: String): View {
        val view = LayoutInflater.from(context).inflate(R.layout.view_help_dialog_title, null, false)
        view.findViewById<TextView>(R.id.helpDialogTitle).setText(title)
        view.findViewById<HelpButton>(R.id.helpDialogButton).bindSurface(surfaceId)
        return view
    }

    /** F1 for a dialog that is not a fragment (the counter's start-value dialog); fragments are covered by [HelpInstaller]. */
    fun bindDialogKey(dialog: Dialog, context: Context, surfaceId: String) {
        val host = hostActivity(context) ?: return
        val window = dialog.window ?: return
        HelpInstaller.wrapWindow(window) { event ->
            HelpInstaller.handleKey(host, event, dialog.currentFocus, surfaceId = surfaceId)
        }
    }

    internal fun entryName(context: Context, view: View?): String? {
        val id = view?.id ?: return null
        if (id == View.NO_ID) return null
        return try {
            context.resources.getResourceEntryName(id)
        } catch (_: Resources.NotFoundException) {
            null
        }
    }

    internal fun isPlainF1(event: KeyEvent): Boolean =
        event.keyCode == KeyEvent.KEYCODE_F1 &&
            !event.isShiftPressed && !event.isCtrlPressed && !event.isAltPressed && !event.isMetaPressed
}
