package ru.fil.calculator

import android.content.Context
import android.graphics.Color
import android.webkit.WebView
import android.webkit.WebViewClient


object FormulaEngine {

    lateinit var webView: WebView
    private var ready = false

    fun init(context: Context) {

        webView = WebView(context)

        webView.settings.javaScriptEnabled = true
        webView.settings.domStorageEnabled = true

        webView.isVerticalScrollBarEnabled = false
        webView.isHorizontalScrollBarEnabled = false

        webView.setBackgroundColor(Color.TRANSPARENT)

        webView.webViewClient = object : WebViewClient() {

            override fun onPageFinished(view: WebView?, url: String?) {
                ready = true
            }
        }

        webView.loadUrl("file:///android_asset/katex_engine.html")
    }

    fun render(latex: String) {

        if(!ready) return

        val escaped = latex
            .replace("\\","\\\\")
            .replace("'","\\'")

        webView.evaluateJavascript(
            "renderFormula('$escaped');",
            null
        )
    }
}




