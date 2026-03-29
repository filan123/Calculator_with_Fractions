package ru.fil.calculator

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.widget.TextView
import android.widget.Button
import android.view.View
import android.webkit.WebView
import android.widget.GridLayout
import android.widget.Toast
import com.judemanutd.katexview.KatexView
import java.lang.StringBuilder


class MainActivity : AppCompatActivity() {
    val list_numb = listOf("0","1","2","3","4","5","6","7","8","9","." ,"\\pm", "\\dfrac{\\,\\,}{\\,\\,}")
    val list_oper = listOf("\\cdot", "+", "-", "\\div", "(", ")",)

    // создаем тэги для кнопок в стиле KaTeX
    val list_btn = listOf("delete","AC","\\dfrac{\\,\\,}{\\,\\,}","\\cdot", "7","8","9","+", "4","5","6","-", "1","2","3","\\div", ".","0","\\pm","eval")
    val list_add_btn = listOf("\\leftarrow","\\rightarrow","\\sqrt{}", "(", ")", "log", "ln", "x^2", "\\sin(x)", "\\cos(x)", "\\tan(x)","\\cot(x)")

    lateinit var controller: FormulaController
    val buttons = mutableListOf<Int>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        val context = ExprContext(StringBuilder(), 0)


        //создаем кнопки
        val container1 = findViewById<GridLayout>(R.id.GridLayout1)
        val container2 = findViewById<GridLayout>(R.id.GridLayout2)


        for (i in 0..19){
            val button = Button(this).apply{
                text= list_btn[i] // что отображаем в последствии заменить на картинки
                id = View.generateViewId()
                tag = list_btn[i] // тэг
                layoutParams = GridLayout.LayoutParams().apply {
                    columnSpec = GridLayout.spec(GridLayout.UNDEFINED, 1f)
                    rowSpec = GridLayout.spec(GridLayout.UNDEFINED, 1f)
                    width = 0
                    height = GridLayout.LayoutParams.WRAP_CONTENT
                }
            }
            container2.addView(button)
            buttons.add(button.id)
        }
        for (i in 0..11){
            val button = Button(this).apply{
                text= list_add_btn[i] // что отображаем в последствии заменить на картинки
                id = View.generateViewId()
                tag = list_add_btn[i] // тэг
                layoutParams = GridLayout.LayoutParams().apply {
                    columnSpec = GridLayout.spec(GridLayout.UNDEFINED, 1f)
                    rowSpec = GridLayout.spec(GridLayout.UNDEFINED, 1f)
                    width = 0
                    height = GridLayout.LayoutParams.WRAP_CONTENT
                }
            }
            container1.addView(button)
            buttons.add(button.id)
        }

        FormulaEngine.init(this)

        val UserExpr = findViewById<WebView>(R.id.Expression)

        UserExpr.addView(FormulaEngine.webView)

        controller = FormulaController()
        // слушаем кнопки
        val listener = View.OnClickListener{ view ->
            val char = view.tag.toString()

            when (char){
                in list_numb -> {
                    context.text.insert(context.posOfpipe,char)
                    context.posOfpipe += char.length
                }
                /*else {if(expr.last() in list_oper){expr.add(char)} else{expr[expr.size-1]+=char}}*/
                in list_oper -> {
                    context.text.insert(context.posOfpipe, char)
                    context.posOfpipe += char.length
                }


                "delete" -> {
                    if(context.posOfpipe != 0) context.funcRemove()
                }
                "AC" -> {
                    context.text.clear()
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
                "eval" -> {
                    try {
                        val result = EVAL(context.text)
                        context.text = result.toStringBuilder()
                        context.posOfpipe = context.text.length
                    } catch (e: ExpressionEvaluationError) {
                        Toast.makeText(
                            this@MainActivity,
                            e.message ?: "Ошибка вычисления выражения",
                            Toast.LENGTH_LONG
                        ).show()
                    } catch (e: DenominatorZeroError) {
                        Toast.makeText(
                            this@MainActivity,
                            e.message ?: "Деление на ноль",
                            Toast.LENGTH_LONG
                        ).show()
                    }
                }
            }
//            Log.d("posOfpipe: ",posOfpipe.toString())
//            Log.d("expr: ", expr.length.toString())
//            UserExpr.renderFormula(expr, posOfpipe)
            context.cleanExpr()
            controller.update(InsertCursor(context.text, context.posOfpipe))
        }




        buttons.forEach { id -> findViewById<Button>(id).setOnClickListener(listener)}

    }
    //direction: True ->; False <-



}
