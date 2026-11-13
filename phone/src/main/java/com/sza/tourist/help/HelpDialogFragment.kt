package com.sza.tourist.help

import android.annotation.SuppressLint
import android.app.Dialog
import android.content.ActivityNotFoundException
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.OnBackPressedCallback
import androidx.activity.ComponentDialog
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updatePadding
import androidx.fragment.app.DialogFragment
import com.sza.tourist.R
import com.sza.tourist.databinding.DialogHelpBinding
import timber.log.Timber
import java.io.ByteArrayInputStream

/**
 * The help window: a bundled page shown in a WebView over the screen that asked (CONTEXT-HELP rules 7 and
 * 12). A dialog fragment rather than an Activity, so the screen below keeps its lifecycle - a torch, a
 * siren or a stopwatch behind it is not stopped by someone reading about it.
 *
 * The page makes no request and needs no script: JavaScript is off, network loads are blocked, and
 * anything outside the package's help assets is answered empty. A link that leaves the package goes to the
 * system browser on the user's tap and never loads here.
 */
class HelpDialogFragment : DialogFragment() {

    private var _binding: DialogHelpBinding? = null
    private val binding get() = requireNotNull(_binding) { "Binding is only valid while the view exists" }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setStyle(STYLE_NORMAL, R.style.Theme_Tourist_HelpDialog)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = DialogHelpBinding.inflate(inflater, container, false)
        return binding.root
    }

    @SuppressLint("SetJavaScriptEnabled")
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        ViewCompat.setOnApplyWindowInsetsListener(binding.helpRoot) { v, insets ->
            val bars = insets.getInsets(WindowInsetsCompat.Type.systemBars() or WindowInsetsCompat.Type.displayCutout())
            v.updatePadding(left = bars.left, top = bars.top, right = bars.right, bottom = bars.bottom)
            WindowInsetsCompat.CONSUMED
        }
        binding.helpToolbar.setNavigationOnClickListener { dismiss() }

        val web = binding.helpWebView
        web.settings.apply {
            javaScriptEnabled = false
            allowFileAccess = false // the package's assets stay reachable through file:///android_asset
            allowContentAccess = false
            blockNetworkLoads = true
            domStorageEnabled = false
            setSupportZoom(true)
            builtInZoomControls = true
            displayZoomControls = false
        }
        web.webViewClient = PageClient()

        if (savedInstanceState != null && web.restoreState(savedInstanceState) != null) return
        val path = requireArguments().getString(ARG_PATH).orEmpty()
        val anchor = requireArguments().getString(ARG_ANCHOR)
        val dark = requireArguments().getBoolean(ARG_DARK)
        val query = "?theme=" + if (dark) "dark" else "light"
        val fragment = if (anchor.isNullOrBlank()) "" else "#$anchor"
        web.loadUrl("$ASSET_ROOT/$path$query$fragment")
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val dialog = super.onCreateDialog(savedInstanceState)
        // Back steps back through the pages the reader followed, then closes the window (rule 7).
        (dialog as? ComponentDialog)?.onBackPressedDispatcher?.addCallback(
            this,
            object : OnBackPressedCallback(true) {
                override fun handleOnBackPressed() {
                    val web = _binding?.helpWebView
                    if (web != null && web.canGoBack()) web.goBack() else dismiss()
                }
            },
        )
        return dialog
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        _binding?.helpWebView?.saveState(outState)
    }

    override fun onDestroyView() {
        _binding?.helpWebView?.apply {
            stopLoading()
            webViewClient = WebViewClient()
            destroy()
        }
        _binding = null
        super.onDestroyView()
    }

    private inner class PageClient : WebViewClient() {

        override fun shouldOverrideUrlLoading(view: WebView, request: WebResourceRequest): Boolean {
            val uri = request.url
            if (isHelpAsset(uri)) return false
            openOutside(uri)
            return true
        }

        override fun shouldInterceptRequest(view: WebView, request: WebResourceRequest): WebResourceResponse? =
            if (isHelpAsset(request.url)) {
                null
            } else {
                // Nothing but the package's own help files is ever fetched.
                WebResourceResponse("text/plain", "utf-8", 404, "Not found", emptyMap(), ByteArrayInputStream(ByteArray(0)))
            }

        override fun onPageStarted(view: WebView, url: String, favicon: Bitmap?) {
            Timber.d("help page: %s", url)
        }

        override fun onReceivedError(view: WebView, request: WebResourceRequest, error: WebResourceError) {
            Timber.w("help page error %d %s for %s", error.errorCode, error.description, request.url)
        }
    }

    private fun openOutside(uri: Uri) {
        if (uri.scheme !in EXTERNAL_SCHEMES) return
        try {
            startActivity(Intent(Intent.ACTION_VIEW, uri))
        } catch (e: ActivityNotFoundException) {
            Timber.w(e, "no app for %s", uri.scheme)
        }
    }

    private fun isHelpAsset(uri: Uri): Boolean =
        uri.scheme == "file" && uri.path?.startsWith("/android_asset/help/") == true

    companion object {
        private const val ARG_PATH = "path"
        private const val ARG_ANCHOR = "anchor"
        private const val ARG_DARK = "dark"
        private const val ASSET_ROOT = "file:///android_asset"
        private val EXTERNAL_SCHEMES = setOf("http", "https", "mailto")

        fun newInstance(assetPath: String, anchor: String?, dark: Boolean) = HelpDialogFragment().apply {
            arguments = Bundle().apply {
                putString(ARG_PATH, assetPath)
                putString(ARG_ANCHOR, anchor)
                putBoolean(ARG_DARK, dark)
            }
        }
    }
}
