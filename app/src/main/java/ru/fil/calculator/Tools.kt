package ru.fil.calculator

import java.lang.StringBuilder
import kotlin.math.abs
import kotlin.math.pow
import kotlin.math.sqrt

/**
 * Если непосредственно перед [endExclusive] в [text] стоит LaTeX-оператор
 * `\cdot`, `\div` или `\pm`, возвращает его длину; иначе 0.
 */
fun latexBinaryOpLenBefore(text: CharSequence, endExclusive: Int): Int {
    if (endExclusive <= 0) return 0
    for (cmd in arrayOf("\\cdot", "\\div", "\\pm")) {
        val start = endExclusive - cmd.length
        if (start >= 0 && text.startsWith(cmd, start)) return cmd.length
    }
    return 0
}

fun getPrimeList(numb: Long): MutableList<Long> {
    val lRes = mutableListOf<Long>()
    var n = numb
    if (n < 0) {
        lRes.add(-1L)
        n *= -1L
    }
    while (n > 1L) {
        val n2 = sqrt(n.toDouble()).toLong()
        var is_success = false
        var iDivider = 2L
        while (iDivider <= n2 + 1L) {
            if (n % iDivider == 0L) {
                is_success = true
                n /= iDivider
                lRes.add(iDivider)
                iDivider = 2L
                continue
            }
            iDivider++
        }

        if (!is_success) {
            lRes.add(n)
            break
        }
    }
    if (n == 0L) {
        lRes.add(n)
    }
    return lRes
}

fun multList(listN: MutableList<Long>): Long {
    var res = 1L
    for (v in listN) {
        res *= v
    }
    return res
}


fun gcd(a: Long, b: Long): Long {
    var x = abs(a)
    var y = abs(b)
    while (y != 0L) {
        val t = y
        y = x % y
        x = t
    }
    return x
}

/** Несократимая пара (числитель, знаменатель), знаменатель > 0. */
internal fun reduceFractionPair(num: Long, den: Long): Pair<Long, Long> {
    if (den < 0) return reduceFractionPair(-num, -den)
    val g = gcd(num, den)
    return num / g to den / g
}

/**
 * Показатели простых в разложении [|value|]; для |value| ≤ 1 возвращается пустая карта.
 */
internal fun primeExponentMap(value: Long): MutableMap<Long, Int> {
    val m = mutableMapOf<Long, Int>()
    var n = abs(value)
    if (n <= 1L) return m
    var d = 2L
    while (d * d <= n) {
        while (n % d == 0L) {
            m[d] = (m[d] ?: 0) + 1
            n /= d
        }
        d++
    }
    if (n > 1L) m[n] = (m[n] ?: 0) + 1
    return m
}

/** Произведение ∏ p^e по карте; при переполнении Long — [ArithmeticException]. */
internal fun longFromPrimeExponentMap(map: Map<Long, Int>): Long {
    var r = 1L
    for ((p, e) in map) {
        repeat(e) {
            r *= p
            if (r < 0L) throw ArithmeticException("Long overflow")
        }
    }
    return r
}

/**
 * В текущей реализации [MyFraction.shorten] знак числителя учитывается некорректно,
 * поэтому упрощаем только при неотрицательном числителе.
 */
internal fun shortenIfSafe(fr: MyFraction): MyFraction {
    if (fr.numerator >= 0) fr.shorten()
    return fr
}

/**
 * Приближение вещественного числа рациональной дробью (медианты Штерна–Броко).
 */
internal fun doubleToApproxFraction(value: Double, maxDen: Long = 1_000_000_000): MyFraction {
    if (!value.isFinite()) throw ArithmeticException("Некорректный результат степени")
    val sign = if (value < 0) -1L else 1L
    val x = abs(value)
    if (x == 0.0) return MyFraction(0, 1)

    var lN = 0L
    var lD = 1L
    var rN = 1L
    var rD = 0L
    var bestN = 1L
    var bestD = 1L
    var bestDiff = abs(x - 1.0)

    repeat(80) {
        val mN = lN + rN
        val mD = lD + rD
        if (mD > maxDen || mN < 0L) return@repeat
        val med = mN.toDouble() / mD
        val diff = abs(med - x)
        if (diff < bestDiff) {
            bestDiff = diff
            bestN = mN
            bestD = mD
        }
        if (diff < 1e-14) {
            return shortenIfSafe(MyFraction(sign * mN, mD))
        }
        if (med < x) {
            lN = mN
            lD = mD
        } else {
            rN = mN
            rD = mD
        }
    }
    return shortenIfSafe(MyFraction(sign * bestN, bestD))
}

/**
 * Точное (a/b)^(p/q), если все (e·p)/q целые; иначе `null`.
 * Знаменатель основания > 0; p > 0; q > 1.
 */
internal fun rationalPowExactOrNull(baseNum: Long, baseDen: Long, p: Long, q: Long): MyFraction? {
    val (a, b) = reduceFractionPair(baseNum, baseDen)
    if (a == 0L) return MyFraction(0, 1)

    var signNegative = false
    var aa = a
    if (aa < 0) {
        if (q % 2L == 0L) return null
        if (p % 2L != 0L) signNegative = true
        aa = -aa
    }

    val numMap = primeExponentMap(aa)
    val denMap = primeExponentMap(b)
    val primes = numMap.keys union denMap.keys
    val outNum = mutableMapOf<Long, Int>()
    val outDen = mutableMapOf<Long, Int>()
    for (prime in primes) {
        val e = (numMap[prime] ?: 0) - (denMap[prime] ?: 0)
        if (e == 0) continue
        val scaled = e.toLong() * p
        if (scaled % q != 0L) return null
        val k = (scaled / q).toInt()
        when {
            k > 0 -> outNum[prime] = (outNum[prime] ?: 0) + k
            k < 0 -> outDen[prime] = (outDen[prime] ?: 0) - k
        }
    }
    val n = longFromPrimeExponentMap(outNum)
    val d = longFromPrimeExponentMap(outDen)
    val fr = if (signNegative) MyFraction(-n, d) else MyFraction(n, d)
    return shortenIfSafe(fr)
}

/**
 * Если [text] — одно рациональное значение без бинарных операций и без унарных функций/идентификаторов
 * на верхнем уровне (например только `\\dfrac{1}{2}`, число или скобки вокруг дроби), возвращает его;
 * иначе `null`.
 */
fun tryParseSingleRationalValue(text: StringBuilder): MyFraction? {
    return try {
        val items = parser(StringBuilder(text.toString()))
        if (items.any { it is BinaryOperand }) return null
        if (items.any { it is UnaryOperand }) return null
        val rpn = toRpn(items)
        if (rpn.any { it is BinaryOperand || it is UnaryOperand }) return null
        evaluateRpn(rpn)
    } catch (_: Exception) {
        null
    }
}

/** Степень через [Double] и [doubleToApproxFraction]. */
internal fun fractionPowApproximate(base: MyFraction, exp: MyFraction): MyFraction {
    val baseD = base.numerator.toDouble() / base.denominator.toDouble()
    val expD = exp.numerator.toDouble() / exp.denominator.toDouble()
    val r = baseD.pow(expD)
    if (r.isNaN() || r.isInfinite()) {
        throw ArithmeticException("Степень не определена в действительных числах")
    }
    return doubleToApproxFraction(r)
}
