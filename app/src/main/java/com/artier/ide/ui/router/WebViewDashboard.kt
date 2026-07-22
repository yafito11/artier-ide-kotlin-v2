package com.artier.ide.ui.router

import android.annotation.SuppressLint
import android.net.Uri
import android.webkit.WebChromeClient
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView

/**
 * Allowed origins for embedded WebViews (9Router dashboard, DB Studio).
 * Only loopback hosts on known ports.
 */
object WebViewOriginWhitelist {
    private val allowedHosts = setOf("127.0.0.1", "localhost")
    private val allowedPorts = setOf(
        20128, // 9Router dashboard
        8080,  // daemon (if needed)
        54323, // Supabase Studio default
        5050,  // pgAdmin default
        3000,  // generic local studio
    )

    fun isAllowed(url: String?): Boolean {
        if (url.isNullOrBlank()) return false
        return try {
            val uri = Uri.parse(url)
            val scheme = uri.scheme?.lowercase()
            if (scheme != "http" && scheme != "https") return false
            val host = uri.host?.lowercase() ?: return false
            if (host !in allowedHosts) return false
            val port = when {
                uri.port != -1 -> uri.port
                scheme == "https" -> 443
                else -> 80
            }
            // Allow any loopback port in debug if explicitly 127.0.0.1/localhost
            // but prefer known dashboard ports; block non-loopback always.
            port in allowedPorts || (host in allowedHosts && port in 1..65535 && isDashboardPath(uri))
        } catch (_: Exception) {
            false
        }
    }

    private fun isDashboardPath(uri: Uri): Boolean {
        // Only allow root-ish paths for unknown ports on loopback
        val path = uri.path ?: "/"
        return path == "/" || path.startsWith("/v1") || path.startsWith("/api") ||
            path.startsWith("/dashboard") || path.startsWith("/project")
    }
}

@SuppressLint("SetJavaScriptEnabled")
@Composable
fun WebViewDashboard(
    url: String,
    modifier: Modifier = Modifier,
    onLoadComplete: (() -> Unit)? = null
) {
    val context = LocalContext.current

    val webView = remember {
        WebView(context).apply {
            settings.javaScriptEnabled = true
            settings.domStorageEnabled = true
            // Harden: no file/content URI access for remote dashboards
            settings.allowFileAccess = false
            settings.allowContentAccess = false
            settings.allowFileAccessFromFileURLs = false
            settings.allowUniversalAccessFromFileURLs = false

            webViewClient = RouterWebViewClient(onLoadComplete)
            webChromeClient = RouterWebChromeClient()
        }
    }

    DisposableEffect(url) {
        if (WebViewOriginWhitelist.isAllowed(url)) {
            webView.loadUrl(url)
        }
        onDispose {
            webView.stopLoading()
            webView.loadUrl("about:blank")
            webView.destroy()
        }
    }

    AndroidView(
        factory = { webView },
        modifier = modifier,
        update = { view ->
            if (WebViewOriginWhitelist.isAllowed(url) && view.url != url) {
                view.loadUrl(url)
            }
        }
    )
}

class RouterWebViewClient(
    private val onLoadComplete: (() -> Unit)? = null
) : WebViewClient() {

    override fun shouldOverrideUrlLoading(view: WebView?, request: WebResourceRequest?): Boolean {
        val url = request?.url?.toString()
        return if (WebViewOriginWhitelist.isAllowed(url)) {
            false // allow navigation inside WebView
        } else {
            true // block
        }
    }

    @Deprecated("Deprecated in Java")
    override fun shouldOverrideUrlLoading(view: WebView?, url: String?): Boolean {
        return if (WebViewOriginWhitelist.isAllowed(url)) {
            false
        } else {
            true
        }
    }

    override fun onPageFinished(view: WebView?, url: String?) {
        super.onPageFinished(view, url)
        onLoadComplete?.invoke()
    }
}

class RouterWebChromeClient : WebChromeClient()
