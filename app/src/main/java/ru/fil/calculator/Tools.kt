package ru.fil.calculator

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

fun getPrimeList(numb: Int): MutableList<Int> {
    val lRes = mutableListOf<Int>()
    var n = numb
    if (n < 0) {
        lRes.add(-1)
        n *= -1
    }
    while (n > 1) {
        val n2 = sqrt(n.toDouble()).toInt()
        var is_success = false
        var iDivider = 2
        while (iDivider <= n2 + 1) {
            if (n % iDivider == 0) {
                is_success = true
                n /= iDivider
                lRes.add(iDivider)
                iDivider = 2
                continue
            }
            iDivider++
        }

        if (!is_success) {
            lRes.add(n)
            break
        }
    }
    if (n == 0) {
        lRes.add(n)
    }
    return lRes
}

fun multList(listN: MutableList<Int>): Int {
    var res = 1
    for (v in listN) {
        res *= v
    }
    return res
}

/** НОД для целых; аргументы по модулю (устойчиво к отрицательным). */
fun gcd(a: Int, b: Int): Int {
    var x = abs(a)
    var y = abs(b)
    while (y != 0) {
        val t = y
        y = x % y
        x = t
    }
    return x
}

/** Несократимая пара (числитель, знаменатель), знаменатель > 0. */
internal fun reduceFractionPair(num: Int, den: Int): Pair<Int, Int> {
    if (den < 0) return reduceFractionPair(-num, -den)
    val g = gcd(num, den)
    return num / g to den / g
}

/**
 * Показатели простых в разложении [|value|]; для |value| ≤ 1 возвращается пустая карта.
 */
internal fun primeExponentMap(value: Int): MutableMap<Int, Int> {
    val m = mutableMapOf<Int, Int>()
    var n = abs(value)
    if (n <= 1) return m
    var d = 2
    while (d * d <= n) {
        while (n % d == 0) {
            m[d] = (m[d] ?: 0) + 1
            n /= d
        }
        d++
    }
    if (n > 1) m[n] = (m[n] ?: 0) + 1
    return m
}

/** Произведение ∏ p^e по карте; при переполнении Int — [ArithmeticException]. */
internal fun intFromPrimeExponentMap(map: Map<Int, Int>): Int {
    var r = 1L
    for ((p, e) in map) {
        repeat(e) {
            r *= p
            if (r > Int.MAX_VALUE) throw ArithmeticException("Int overflow")
        }
    }
    return r.toInt()
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
internal fun doubleToApproxFraction(value: Double, maxDen: Int = 1_000_000): MyFraction {
    if (!value.isFinite()) throw ArithmeticException("Некорректный результат степени")
    val sign = if (value < 0) -1 else 1
    val x = abs(value)
    if (x == 0.0) return MyFraction(0, 1)

    var lN = 0
    var lD = 1
    var rN = 1
    var rD = 0
    var bestN = 1
    var bestD = 1
    var bestDiff = abs(x - 1.0)

    repeat(80) {
        val mN = lN + rN
        val mD = lD + rD
        if (mD > maxDen || mN > Int.MAX_VALUE / 2) return@repeat
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
internal fun rationalPowExactOrNull(baseNum: Int, baseDen: Int, p: Int, q: Int): MyFraction? {
    val (a, b) = reduceFractionPair(baseNum, baseDen)
    if (a == 0) return MyFraction(0, 1)

    var signNegative = false
    var aa = a
    if (aa < 0) {
        if (q % 2 == 0) return null
        if (p % 2 != 0) signNegative = true
        aa = -aa
    }

    val numMap = primeExponentMap(aa)
    val denMap = primeExponentMap(b)
    val primes = numMap.keys union denMap.keys
    val outNum = mutableMapOf<Int, Int>()
    val outDen = mutableMapOf<Int, Int>()
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
    val n = intFromPrimeExponentMap(outNum)
    val d = intFromPrimeExponentMap(outDen)
    val fr = if (signNegative) MyFraction(-n, d) else MyFraction(n, d)
    return shortenIfSafe(fr)
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
