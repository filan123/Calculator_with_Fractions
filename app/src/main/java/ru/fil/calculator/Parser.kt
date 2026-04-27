package ru.fil.calculator

import kotlin.math.abs

/**
 * Преобразует LaTeX-подобную строку в список токенов-элементов (`Item`).
 *
 * Поддержка (в рамках текущего ТЗ):
 * - `\dfrac{<num>}{<den>}` -> `MyFraction` (в `{...}` допускается выражение, не только одно число)
 * - числовые литералы (целые и десятичные) -> `MyFraction`
 * - унарные команды вида `\sqrt{...}`, `\sin(x)` -> `UnaryOperand`
 * - бинарные операции: `\cdot`, `\div`, `+`, `-`, `^` -> `BinaryOperand`
 * - скобки: `(` `)` `{` `}` -> `BracketItem`
 */
fun parser(text: StringBuilder): MutableList<Item> {
    val s = text.toString()
    val res = mutableListOf<Item>()

    var i = 0
    // Нужно для различения унарного минуса в числе и бинарного оператора '-'.
    var expectOperand = true

    fun isLatexSpace(pos: Int): Boolean {
        if (pos + 1 >= s.length) return false
        if (s[pos] != '\\') return false
        val next = s[pos + 1]
        return next == ',' || next == ';'
    }

    fun skipLatexSpace() {
        if (isLatexSpace(i)) i += 2
    }

    val numberRegex =
        Regex("^[+-]?(?:\\d+(?:\\.\\d+)?|\\.\\d+)$")

    fun powLong(base: Long, exp: Int): Long {
        var result = 1L
        repeat(exp) { result *= base }
        return result
    }

    fun parseNumberToken(token: String): MyFraction? {
        val trimmed = token.trim()
        if (!numberRegex.matches(trimmed)) return null

        if (!trimmed.contains('.')) {
            val v = trimmed.toLongOrNull() ?: return null
            return MyFraction(v, 1)
        }

        val sign = if (trimmed.startsWith("-")) -1L else 1L
        val absToken = if (sign == -1L) trimmed.substring(1) else trimmed
        val parts = absToken.split('.')
        val intPart = parts.getOrNull(0).orEmpty()
        val fracPart = parts.getOrNull(1).orEmpty()
        val k = fracPart.length

        val intPartNorm = if (intPart.isEmpty()) 0L else intPart.toLong()
        val fracPartNorm = if (fracPart.isEmpty()) 0L else fracPart.toLong()

        val denomLong = powLong(10L, k)
        if (denomLong < 1L) return null

        val numeratorLong = intPartNorm * denomLong + fracPartNorm
        val numeratorSigned = numeratorLong * sign

        return MyFraction(numeratorSigned, denomLong).also { it.shorten() }
    }

    /**
     * Содержимое `{...}` в `\dfrac` / `\frac`: либо одно число, либо полноценное выражение
     * (скобки, операции, вложенные дроби). Во втором случае парсим и считаем рекурсивно.
     */
    fun parseFracGroupToFraction(group: String): MyFraction {
        // Убираем техпробелы `\,` (в т.ч. повторные `\,,\,` в шаблоне `\dfrac{\,\,}{\,\,}`).
        val cleaned = group
            .replace("\\,", "")
            .replace("\\;", "")
            .trim()

        if (cleaned.isEmpty()) return MyFraction(0, 1)

        parseNumberToken(cleaned)?.let { return it }

        val innerItems = parser(StringBuilder(cleaned))
        val rpn = toRpn(innerItems)
        return evaluateRpn(rpn)
    }

    fun parseCurlyGroupStart(pos: Int): Pair<String, Int>? {
        // pos указывает на '{' первого уровня.
        if (pos >= s.length || s[pos] != '{') return null
        var depth = 0
        var j = pos
        val startContent = pos + 1
        while (j < s.length) {
            if (s[j] == '{') depth++
            if (s[j] == '}') depth--
            j++
            if (depth == 0) {
                val content = s.substring(startContent, j - 1)
                return content to j
            }
        }
        return null
    }

    /**
     * Нормализует mixed-запись вида `2\dfrac{1}{3}`:
     * два соседних токена `MyFraction(a,1)` и `MyFraction(b,d)` (d != 1)
     * сворачиваются в один `MyFraction(sign(a) * (abs(a) * d + b), d)`.
     *
     * Защитные правила:
     * - только соседние дробные токены;
     * - дробная часть должна быть положительной (0 < b <= d);
     * - нулевая целая часть не трогается.
     */
    fun normalizeMixedFractions(items: MutableList<Item>) {
        var idx = 0
        while (idx < items.size - 1) {
            val whole = items[idx] as? MyFraction
            val frac = items[idx + 1] as? MyFraction
            if (whole == null || frac == null) {
                idx++
                continue
            }
            
            if (whole.denominator != 1L || frac.denominator == 1L && frac.numerator != 1L) {
                idx++
                continue
            }

            if (whole.numerator == 0L) {
                idx++
                continue
            }

            val b = frac.numerator
            val d = frac.denominator

            if (b <= 0L) {
                idx++
                continue
            }

            val sign = if (whole.numerator < 0) -1L else 1L
            val absWhole = abs(whole.numerator).toLong()
            val denLong = d.toLong()
            val numLong = sign * (absWhole * denLong + b.toLong())

            items[idx] = MyFraction(numLong, d)
            items.removeAt(idx + 1)
        }
    }

    fun canEndOperand(token: Item): Boolean = when (token) {
        is MyFraction -> true
        is UnaryOperand -> true
        is BracketItem -> token.bracket == ')' || token.bracket == '}'
        else -> false
    }

    fun canStartOperand(token: Item): Boolean = when (token) {
        is MyFraction -> true
        is UnaryOperand -> true
        is BracketItem -> token.bracket == '(' || token.bracket == '{'
        else -> false
    }

    fun isMixedPair(left: Item, right: Item): Boolean {
        val whole = left as? MyFraction ?: return false
        val frac = right as? MyFraction ?: return false
        if (whole.denominator != 1L || frac.denominator == 1L) return false
        if (whole.numerator == 0L) return false
        val b = frac.numerator
        val d = frac.denominator
        return b > 0L && abs(b) < d
    }

    fun insertImplicitMultiplication(items: MutableList<Item>): MutableList<Item> {
        if (items.size < 2) return items
        val result = mutableListOf<Item>()
        var idx = 0
        while (idx < items.size) {
            val current = items[idx]
            result.add(current)

            if (idx < items.size - 1) {
                val next = items[idx + 1]
                val unaryFunctionCall =
                    current is UnaryOperand &&
                        next is BracketItem &&
                        (next.bracket == '(' || next.bracket == '{')
                if (
                    canEndOperand(current) &&
                    canStartOperand(next) &&
                    !unaryFunctionCall &&
                    !isMixedPair(current, next)
                ) {
                    result.add(BinaryOperand("\\cdot"))
                }
            }
            idx++
        }
        return result
    }

//     Позиция после цепочки +/- начинает операнд: скобка, LaTeX-команда (`\` + буква), идетнификатор.
    fun operandStartsAt(pos: Int): Boolean {
        if (pos >= s.length) return false
        val c = s[pos]
        if (c == '(' || c == '{') return true
        if (c == '\\') return pos + 1 < s.length && s[pos + 1].isLetter()
        return c.isLetter()
    }

    while (i < s.length) {
        val ch = s[i]

        if (ch.isWhitespace()) {
            i++
            continue
        }

        // Пропуспим \, и \; как "технические" пробелы.
        if (isLatexSpace(i)) {
            skipLatexSpace()
            continue
        }

        // 1) Дроби: \dfrac{...}{...}
        if (s.startsWith("\\dfrac", i) || s.startsWith("\\frac", i)) {
            val cmdLen = if (s.startsWith("\\dfrac", i)) "\\dfrac".length else "\\frac".length
            i += cmdLen

            val numGroup = parseCurlyGroupStart(i)
            if (numGroup == null) continue
            val numStr = numGroup.first
            i = numGroup.second

            val denGroup = parseCurlyGroupStart(i)
            if (denGroup == null) continue
            val denStr = denGroup.first
            i = denGroup.second

            val num = parseFracGroupToFraction(numStr)
            val den = parseFracGroupToFraction(denStr)

            // Представляем \dfrac{a}{b} как рациональное число: a/b.
            val token = if (den.numerator == 0L) {
                // Чтобы парсер не падал на незаполненном шаблоне `\dfrac{\,\,}{\,\,}`.
                MyFraction(0, 1)
            } else {
                MyFraction(
                    num.numerator * den.denominator,
                    num.denominator * den.numerator
                )
            }
            token.shorten()
            res.add(token)
            expectOperand = false
            continue
        }

        // 2) бинарные операторы
        if (s.startsWith("\\cdot", i)) {
            res.add(BinaryOperand("\\cdot"))
            i += "\\cdot".length
            expectOperand = true
            continue
        }
        if (s.startsWith("\\div", i)) {
            res.add(BinaryOperand("\\div"))
            i += "\\div".length
            expectOperand = true
            continue
        }
        if (s.startsWith("\\pm", i)) {
            res.add(BinaryOperand("\\pm"))
            i += "\\pm".length
            expectOperand = true
            continue
        }

        if (ch == '^') {
            res.add(BinaryOperand("^"))
            i++
            expectOperand = true
            continue
        }
        if (!expectOperand && (ch == '+' || ch == '-')) {
            res.add(BinaryOperand(ch.toString()))
            i++
            expectOperand = true
            continue
        }

        // 3) скобки
        if (ch == '(' || ch == ')' || ch == '{' || ch == '}') {
            res.add(BracketItem(ch))
            i++
            expectOperand = (ch == '(' || ch == '{')
            continue
        }

        // 4) Унарная команды: `\sqrt`, `\sin`, `x`, ...
        if (ch == '\\') {
            val start = i + 1
            if (start < s.length && s[start].isLetter()) {
                var j = start
                while (j < s.length && s[j].isLetter()) j++
                val name = s.substring(start, j)
                res.add(UnaryOperand(name))
                i = j
                expectOperand = true
                continue
            }
        }
        if (ch.isLetter()) {
            var j = i
            while (j < s.length && (s[j].isLetterOrDigit() || s[j] == '_')) j++
            res.add(UnaryOperand(s.substring(i, j)))
            i = j
            expectOperand = false
            continue
        }

        // 5) Числа: digits, '.' или литерал со знаком при expectOperand=true.
        // Важно: после бинарного оператора допускается цепочка '+'/'-' (например 5---5 == 5 - (+5)).
        // Унарный +/- перед скобкой, \команда или буква: эквивалент 0 - (...) при минусе.
        if (ch.isDigit() || ch == '.' || (expectOperand && (ch == '-' || ch == '+'))) {
            var j = i
            var sign = 1
            if (expectOperand && j < s.length && (s[j] == '-' || s[j] == '+')) {
                while (j < s.length && (s[j] == '-' || s[j] == '+')) {
                    if (s[j] == '-') sign *= -1
                    j++
                }
                if (j >= s.length || (!s[j].isDigit() && s[j] != '.')) {
                    if (operandStartsAt(j)) {
                        if (sign < 0) {
                            res.add(MyFraction(0, 1))
                            res.add(BinaryOperand("-"))
                        }
                        i = j
                        expectOperand = true
                        continue
                    }
                    j = i
                    sign = 1
                }
            }

            if (j < s.length && (s[j].isDigit() || s[j] == '.')) {
                var dotCount = 0
                var hasDigit = false
                var k = j
                while (k < s.length) {
                    val cj = s[k]
                    if (cj.isDigit()) {
                        hasDigit = true
                        k++
                    } else if (cj == '.') {
                        dotCount++
                        if (dotCount > 1) break
                        k++
                    } else {
                        break
                    }
                }
                if (hasDigit) {
                    val unsigned = s.substring(j, k)
                    val tokenStr = if (sign < 0) "-$unsigned" else unsigned
                    val num = parseNumberToken(tokenStr)
                    if (num != null) {
                        res.add(num)
                        i = k
                        expectOperand = false
                        continue
                    }
                }
            }
        }

        // 6)неизвестный символ — пропускаем.
        i++
    }

    normalizeMixedFractions(res)
    return insertImplicitMultiplication(res)
}

