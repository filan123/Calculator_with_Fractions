package ru.fil.calculator

import android.content.Context
import android.graphics.Color
import android.webkit.WebView
import android.webkit.WebViewClient


object FormulaEngine {

    lateinit var webView: WebView
    private var ready = false

    lateinit var lastExpressionWebView: WebView
    private var lastReady = false

    fun init(context: Context) {
        webView = WebView(context)
        configureKatexWebView(webView) { ready = true }
    }

    fun initLastExpressionWebView(webView: WebView) {
        lastExpressionWebView = webView
        configureKatexWebView(lastExpressionWebView) { lastReady = true }
    }

    private fun configureKatexWebView(wv: WebView, onPageReady: () -> Unit) {
        wv.settings.javaScriptEnabled = true
        wv.settings.domStorageEnabled = true

        wv.isVerticalScrollBarEnabled = false
        wv.isHorizontalScrollBarEnabled = false

        wv.setBackgroundColor(Color.TRANSPARENT)

        wv.webViewClient = object : WebViewClient() {
            override fun onPageFinished(view: WebView?, url: String?) {
                onPageReady()
            }
        }

        wv.loadUrl("file:///android_asset/katex_engine.html")
    }

    private fun escapeForJs(latex: String): String =
        latex.replace("\\", "\\\\").replace("'", "\\'")

    fun render(latex: String) {
        if (!ready) return

        val escaped = escapeForJs(latex)

        webView.evaluateJavascript(
            "renderFormula('$escaped');",
            null
        )
    }

    fun renderLastExpression(latex: String) {
        if (!lastReady) return

        val escaped = escapeForJs(latex)

        lastExpressionWebView.evaluateJavascript(
            "renderFormula('$escaped');",
            null
        )
    }
}
