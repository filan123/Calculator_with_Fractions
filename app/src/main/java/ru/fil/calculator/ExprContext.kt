package ru.fil.calculator

import java.lang.StringBuilder

/**
 * Контекст редактирования выражения:
 * - `text` хранит формулу
 * - `posOfpipe` хранит позицию курсора (всегда ограничена 0..text.length)
 * - методы объединяют логику работы с выражением
 */
class ExprContext(
    initialText: StringBuilder,
    initialPosOfpipe: Int = 0
) {
    var text: StringBuilder = initialText
        set(value) {
            field = value
            // Если строка стала короче/длиннее, курсор нужно пересчитать.
            posOfpipe = posOfpipe
        }
    var posOfpipe: Int = 0
        set(value) {
            // Гарантируем, что курсор всегда валиден.
            field = value.coerceIn(0, text.length)
        }

    init {
        posOfpipe = initialPosOfpipe
    }

    /** true — режим S (вывод как `\\dfrac{n}{d}`), false — режим D (десятичная запись). */
    var useStandardFractionOutput: Boolean = true

    // direction: True -> ; false <-
    fun findNextSpace(direction: Boolean): Int {
        posOfpipe = findNextSpaceInternal(posOfpipe, text.toString(), direction)
        return posOfpipe
    }

    /**
     * Вставляет `token` в `text` в позицию курсора и выставляет `posOfpipe` в "место ввода".
     *
     * Примеры:
     * - `\dfrac{\,,\,}{\,,\,}` -> курсор в начало числителя: `\dfrac{|\,\,}{\,\,}`
     * - `^{2}` -> курсор внутри фигурных скобок степени: `^{|2}`
     * - `\sqrt{}` -> курсор внутри `{}`: `\sqrt{|}`
     * - `\sin(x)` -> курсор после `(`: `\sin(|x)`
     */
    fun insertToken(token: String) {
        val insertPos = posOfpipe
        text.insert(insertPos, token)

        posOfpipe = when {
            // \dfrac{...}{...}
            token.startsWith("\\dfrac") -> {
                val firstBrace = token.indexOf('{')
                if (firstBrace >= 0) insertPos + firstBrace + 1 else insertPos + token.length
            }

            // ^{...}
            token.startsWith("^{") -> {
                val firstBrace = token.indexOf('{')
                if (firstBrace >= 0) insertPos + firstBrace + 1 else insertPos + token.length
            }
            token.startsWith("^{2") -> {
                val firstBrace = token.indexOf('{')
                if (firstBrace >= 0) insertPos + firstBrace + 2 else insertPos + token.length
            }
            token.startsWith("^{3") -> {
                val firstBrace = token.indexOf('{')
                if (firstBrace >= 0) insertPos + firstBrace + 2 else insertPos + token.length
            }


            // \sqrt{...}
            token.startsWith("\\sqrt") -> {
                val firstBrace = token.indexOf('{')
                if (firstBrace >= 0) insertPos + firstBrace + 1 else insertPos + token.length
            }

            // \sin(x), \cos(x), \tan(x), \cot(x) и т.п.
            token.contains('(') -> {
                val lastOpenParen = token.lastIndexOf('(')
                if (lastOpenParen >= 0) insertPos + lastOpenParen + 1 else insertPos + token.length
            }

            else -> insertPos + token.length
        }.coerceIn(0, text.length)
    }

    /**
     * Удаляет символ/команду слева от курсора (аналог старой funcRemove).
     * Обновляет `text` и `posOfpipe`.
     */
    fun funcRemove(): Int {
        if (posOfpipe <= 0 || text.isEmpty()) return posOfpipe

        // Удаление обычной цифры или точки
        if (text[posOfpipe - 1] in listOf('0','1','2','3','4','5','6','7','8','9','.','-','+',')','(')) {
            text.delete(posOfpipe - 1, posOfpipe)
            posOfpipe -= 1
            return posOfpipe
        }
        // Удаление технического пробела
        if (text[posOfpipe-1] in listOf(',')) {
            text.delete(posOfpipe-2, posOfpipe)
            posOfpipe -= 2
            return posOfpipe
        }
        // Если перед курсором закрывающая скобка, возможно это конец \frac{...}{...}
        val start = posOfpipe - 1

        // Попробуем найти начало команды \frac
        val fracStart = findFracStart(text, start)
        if (fracStart != -1) {
            val fracEnd = findFracEnd(text, fracStart)
            if (fracEnd != -1 && posOfpipe <= fracEnd) {
                text.delete(fracStart, fracEnd)
                posOfpipe = fracStart
                return posOfpipe
            }
        }

        // Целиком убираем бинарные LaTeX-операторы \cdot, \div, \pm
        val latexOpLen = latexBinaryOpLenBefore(text, posOfpipe)
        if (latexOpLen > 0) {
            text.delete(posOfpipe - latexOpLen, posOfpipe)
            posOfpipe -= latexOpLen
            return posOfpipe
        }

        // Обычное удаление LaTeX-команды типа \sin, \cos и т.п.
        var i = posOfpipe - 1
        while (i >= 0 && text[i] != '\\') {
            i--
        }

        if (i >= 0) {
            text.delete(i, posOfpipe)
            posOfpipe = i
            return posOfpipe
        }

        // fallback: удалить один символ
        text.delete(posOfpipe - 1, posOfpipe)
        posOfpipe -= 1
        return posOfpipe
    }

    //Чистит выражение от пробелов
    fun cleanExpr(): StringBuilder {
        fun adjustInsert(insertPos: Int, len: Int) {
            if (len <= 0) return
            if (insertPos <= posOfpipe) posOfpipe += len
        }

        fun adjustDelete(start: Int, end: Int) {
            if (end <= start) return
            val del = end - start
            if (posOfpipe > end) posOfpipe -= del
            else if (posOfpipe > start) posOfpipe = start
        }

        fun isDoubleCommaAt(from: Int): Boolean {
            return from + 3 < text.length &&
                text[from] == '\\' &&
                text[from + 1] == ',' &&
                text[from + 2] == '\\' &&
                text[from + 3] == ','
        }

        fun isLeadingDoubleCommaAfterOpen(openIdx: Int): Boolean {
            return openIdx + 5 < text.length &&
                text[openIdx] == '{' &&
                isDoubleCommaAt(openIdx + 1)
        }

        fun findMatchingOpenBrace(closeIdx: Int): Int {
            var depth = 0
            var j = closeIdx - 1
            while (j >= 0) {
                when (text[j]) {
                    '}' -> depth++
                    '{' -> {
                        if (depth == 0) return j
                        depth--
                    }
                }
                j--
            }
            return -1
        }

        var i = 0
        while (i < text.length ) {
            // случай: {} -> {\,\,} (два техпробела, как в шаблоне \dfrac{\,\,}{\,\,})
            if (text[i] == '{' && text[i + 1] == '}') {
                text.insert(i + 1, "\\,\\,")
                adjustInsert(i + 1, 4)
                i += 6 // { + \,\, + }
                continue
            }

            // случай: {\,\,X...} -> {X...} (X не сразу `}`)
            if (isLeadingDoubleCommaAfterOpen(i) && text[i + 5] != '}') {
                text.delete(i + 1, i + 5)
                adjustDelete(i + 1, i + 5)
                continue
            }

            // случай: {...\,\,} с непустым содержимым -> убрать хвостовые \,\,
            if (text[i] == '}' && i >= 4 && isDoubleCommaAt(i - 4)) {
                val open = findMatchingOpenBrace(i)
                if (open != -1) {
                    val innerStart = open + 1
                    val innerEnd = i - 4
                    if (innerEnd > innerStart) {
                        val inner = text.substring(innerStart, innerEnd)
                        val stripped = inner.replace("\\,", "").replace("\\;", "").trim()
                        if (stripped.isNotEmpty()) {
                            text.delete(i - 4, i)
                            adjustDelete(i - 4, i)
                            continue
                        }
                    }
                }
            }

            // legacy: одиночный \, после {, если дальше не `}` и это не начало пары \,\,
            if (
                i + 3 < text.length &&
                text[i] == '{' &&
                text[i + 1] == '\\' &&
                text[i + 2] == ','
            ) {
                if (text[i + 3] != '}' && text[i + 3] != '\\') {
                    text.delete(i + 1, i + 3)
                    adjustDelete(i + 1, i + 3)
                    continue
                }
            }

            i++
        }

        posOfpipe = posOfpipe.coerceIn(0, text.length)
        return text
    }

    private fun findNextSpaceInternal(position: Int, text: String, direction: Boolean): Int {
        val len = text.length
        val start = position.coerceIn(0, len)

        fun isBadCursorPos(pos: Int): Boolean {
            if (pos !in 0..len) return true


            //1)Если курсор стоит }|{
            if (pos < len && text[pos] == '{' && text[pos-1]=='}'){ return true}

            // 2) Позиция внутри имени команды: \f|rac, \frac|
            // Ищем начало "слова" слева от pos
            var i = pos - 1
            while (i >= 0 && text[i].isLetter() && (text[i+1] == '{' || text[i+1].isLetter())) i--

            // Если перед буквами стоит '\', то это имя команды
            if (i >= 0 && text[i] == '\\') {
                var end = i + 1
                while (end < len && text[end].isLetter()) end++
                // Внутренние позиции имени + позиция сразу после имени считаем "плохими"
                if (pos in (i + 1)..end) return true
            }


            return false
        }

        return if (direction) {
            // Вперёд
            for (p in start..len) {
                if (!isBadCursorPos(p)) return p
            }
            len
        } else {
            // Назад
            for (p in start downTo 0) {
                if (!isBadCursorPos(p)) return p
            }
            0
        }
    }

    private fun findFracStart(text: StringBuilder, from: Int): Int {
        var i = from

        while (i >= 0) {
            if (text[i] == '\\') {
                val cmd = "\\dfrac"
                if (i + cmd.length <= text.length && text.substring(i, i + cmd.length) == cmd) {
                    return i
                }
            }
            i--
        }

        return -1
    }

    private fun findFracEnd(text: StringBuilder, fracStart: Int): Int {
        var i = fracStart

        // пропускаем "\frac"
        i += 6
        if (i >= text.length) return -1

        // первая группа {...}
        i = skipGroup(text, i)
        if (i == -1) return -1

        // вторая группа {...}
        i = skipGroup(text, i)
        if (i == -1) return -1

        return i
    }

    private fun skipGroup(text: StringBuilder, start: Int): Int {
        var i = start
        if (i >= text.length || text[i] != '{') return -1

        var balance = 0
        while (i < text.length) {
            if (text[i] == '{') balance++
            if (text[i] == '}') balance--

            i++

            if (balance == 0) {
                return i
            }
        }

        return -1
    }
}

