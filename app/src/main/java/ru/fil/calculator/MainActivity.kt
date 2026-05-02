package ru.fil.calculator

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.Gravity
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.webkit.WebView
import android.widget.FrameLayout
import android.widget.GridLayout
import android.widget.TextView
import android.widget.Toast
import androidx.core.content.ContextCompat
import com.judemanutd.katexview.KatexView
import java.lang.StringBuilder


class MainActivity : AppCompatActivity() {
    val fractionLatex = "\\dfrac{\\,\\,}{\\,\\,}"
    val list_numb = listOf("0","1","2","3","4","5","6","7","8","9","." ,fractionLatex, "\\pi")
    val list_oper = listOf("\\cdot", "+", "-", "\\div", "(", ")","^{2}","^{3}","^{\\,\\,}", "\\sqrt{\\,\\,}",  "\\sin()", "\\cos()", "\\tan()","\\cot()")

    // создаем тэги для кнопок в стиле KaTeX
    val list_btn = listOf("\\leftarrow","\\rightarrow",fractionLatex,"\\cdot", "7","8","9","+", "4","5","6","\\div", "1","2","3","-", ".","0","S \\Leftrightarrow D","eval")
    val list_add_btn = listOf("delete","AC","\\sqrt{\\,\\,}", "(", ")", "^{\\,\\,}", "^{2}","\\pi",  "\\sin()", "\\cos()", "\\tan()","\\cot()")

    lateinit var controller: FormulaController

    // KatexView template (katex.chtml) registers only $$…$$ for auto-render, not $…$
    private fun displayLatexForTag(tag: String): String = when (tag) {
        "delete" -> "$$\\text{Del}$$"
        "AC" -> "$$\\text{AC}$$"
        "eval" -> "$$=$$"
        fractionLatex->"$$\\dfrac{x}{y}$$"
        "^{2}" -> "\$\$x^{2}$$"
        "^{3}" -> "\$\$x^{3}$$"
        "\\sin()" -> "$$\\sin(x)$$"
        "\\cos()" -> "$$\\cos(x)$$"
        "\\tan()" -> "$$\\tan(x)$$"
        "\\cot()" -> "$$\\cot(x)$$"
        "\\cdot" -> "$$\\times$$"
        "S \\Leftrightarrow D" -> "\$\$S \\leftrightarrow D\$\$"
        "^{\\,\\,}" -> "\$\$x^{y}$$"
        "\\sqrt{\\,\\,}" -> "$$\\sqrt{x}$$"
        else -> "$$" + tag + "$$"
    }

    private fun createKatexKey(tag: String, listener: View.OnClickListener): FrameLayout {
        val minHeightPx = (48 * resources.displayMetrics.density).toInt()
        val katex = KatexView(this).apply {
            setText(displayLatexForTag(tag))
            setTextColor(ContextCompat.getColor(this@MainActivity, R.color.black))
            layoutParams = FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT,
                Gravity.CENTER
            )
        }
        return object : FrameLayout(this) {
            override fun onInterceptTouchEvent(ev: MotionEvent): Boolean = true

            override fun onTouchEvent(event: MotionEvent): Boolean {
                return super.onTouchEvent(event)
            }
        }.apply {
            id = View.generateViewId()
            this.tag = tag
            setBackgroundResource(R.drawable.button_border)
            isClickable = true
            isFocusable = true
            minimumHeight = minHeightPx
            layoutParams = GridLayout.LayoutParams().apply {
                columnSpec = GridLayout.spec(GridLayout.UNDEFINED, 1f)
                rowSpec = GridLayout.spec(GridLayout.UNDEFINED, 1f)
                width = 0
                height = minHeightPx
            }
            addView(katex)
            setOnClickListener(listener)
        }
    }

    private fun showErrorToast(message: String) {
        val toastView = LayoutInflater.from(this).inflate(R.layout.toast_error, null)
        val toastText = toastView.findViewById<TextView>(R.id.toastErrorText)
        toastText.text = message

        Toast(this).apply {
            duration = Toast.LENGTH_SHORT
            setGravity(Gravity.CENTER, 0, 0)
            view = toastView
            show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        val context = ExprContext(StringBuilder(), 0)


        //создаем кнопки
        val container1 = findViewById<GridLayout>(R.id.GridLayout1)
        val container2 = findViewById<GridLayout>(R.id.GridLayout2)


        controller = FormulaController()
        val listener = View.OnClickListener { view ->
            val char = view.tag.toString()

            when (char){
                in list_numb -> context.insertToken(char)
                /*else {if(expr.last() in list_oper){expr.add(char)} else{expr[expr.size-1]+=char}}*/
                in list_oper -> context.insertToken(char)



                "delete" -> {
                    if(context.posOfpipe != 0) context.funcRemove()
                }
                "AC" -> {
                    context.text.clear()
                    FormulaEngine.renderLastExpression("")
                    context.posOfpipe = 0
                }
                "\\leftarrow"-> {
                    if (context.posOfpipe > 0) {
                        context.posOfpipe -= 1
                        context.findNextSpace(false)
                    }
                }
                "\\rightarrow"-> {
                    if (context.posOfpipe < context.text.length ) {
                        context.posOfpipe += 1
                        context.findNextSpace(true)
                    }
                }
                "S \\Leftrightarrow D" -> {
                    context.useStandardFractionOutput = !context.useStandardFractionOutput
                    tryParseSingleRationalValue(context.text)?.let { value ->
                        context.text.clear()
                        context.text.append(value.toStringBuilder(context.useStandardFractionOutput))
                        context.posOfpipe = context.text.length
                    }
                }
                "eval" -> {
                    try {
                        val expressionBeforeEval = context.text
                        val result = EVAL(context.text)
                        context.text = result.toStringBuilder(context.useStandardFractionOutput)
                        context.posOfpipe = context.text.length
                        FormulaEngine.renderLastExpression(expressionBeforeEval.toString())
                    } catch (e: ExpressionEvaluationError) {
                        showErrorToast(e.message ?: "Ошибка вычисления выражения")
                    } catch (e: DenominatorZeroError) {
                        showErrorToast(e.message ?: "Деление на ноль")
                    } catch (e: ArithmeticException) {
                        showErrorToast(e.message ?: "Алгебраическая ошибка")

                    }
                }
            }

            context.cleanExpr()
            controller.update(InsertCursor(context.text, context.posOfpipe))
        }

        for (i in 0..19) {
            container2.addView(createKatexKey(list_btn[i], listener))
        }
        for (i in 0..11) {
            container1.addView(createKatexKey(list_add_btn[i], listener))
        }

        FormulaEngine.init(this)
        FormulaEngine.initLastExpressionWebView(findViewById(R.id.Last_Expression))

        val UserExpr = findViewById<WebView>(R.id.Expression)

        UserExpr.addView(
            FormulaEngine.webView,
            ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
        )
    }
    //direction: True ->; False <-



}
