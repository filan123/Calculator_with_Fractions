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
        prepareKatexWebView(webView, styleInjectScript(FLEX_LAYOUT_CSS)) { ready = true }
    }

    fun initLastExpressionWebView(webView: WebView) {
        lastExpressionWebView = webView
        prepareKatexWebView(
            lastExpressionWebView,
            styleInjectScript(FLEX_LAYOUT_CSS + LAST_KATEX_CSS)
        ) { lastReady = true }
    }

    private fun prepareKatexWebView(wv: WebView, injectScript: String, onReady: () -> Unit) {
        wv.settings.javaScriptEnabled = true
        wv.settings.domStorageEnabled = true

        wv.isVerticalScrollBarEnabled = false
        wv.isHorizontalScrollBarEnabled = false

        wv.setBackgroundColor(Color.TRANSPARENT)

        wv.webViewClient = object : WebViewClient() {
            override fun onPageFinished(view: WebView?, url: String?) {
                view?.evaluateJavascript(injectScript, null)
                onReady()
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

    private const val FLEX_LAYOUT_CSS =
        "html,body{height:100%;margin:0;padding:0;overflow:hidden;box-sizing:border-box;}" +
            "body{display:flex;align-items:flex-end;justify-content:flex-start;padding-left:8px;padding-bottom:4px;}"

    private const val LAST_KATEX_CSS =
        ".katex,.katex *{color:#A6A6A6!important;}.formula .katex{font-size:0.9em!important;}"

    private fun styleInjectScript(css: String): String =
        "(function(){var s=document.createElement('style');s.textContent='" + css + "';document.head.appendChild(s);})();"
}
