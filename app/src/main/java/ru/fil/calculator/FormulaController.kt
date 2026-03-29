package ru.fil.calculator

import android.os.Handler
import android.os.Looper


class FormulaController {

    private val handler = Handler(Looper.getMainLooper())

    private var lastFormula = ""
    private var task: Runnable? = null

    fun update(latex: String){

        val normalized = latex.trim()

        if(normalized == lastFormula) return
        lastFormula = normalized

        task?.let { handler.removeCallbacks(it) }

        task = Runnable {
            FormulaEngine.render(normalized)
        }

        handler.postDelayed(task!!, 16)
    }
}