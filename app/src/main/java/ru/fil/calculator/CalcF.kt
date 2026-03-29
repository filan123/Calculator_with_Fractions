package ru.fil.calculator

import kotlin.math.abs

private fun powIntToLong(value: Int, exp: Int): Long {
    require(exp >= 0) { "Exponent must be non-negative in powIntToLong" }

    var result = 1L
    var base = value.toLong()
    repeat(exp) {
        result *= base
    }
    return result
}

private fun longToIntChecked(value: Long): Int {
    if (value < Int.MIN_VALUE || value > Int.MAX_VALUE) {
        throw ArithmeticException("Int overflow: $value")
    }
    return value.toInt()
}

private fun powIntChecked(value: Int, exp: Int): Int {
    val resLong = powIntToLong(value, exp)
    return longToIntChecked(resLong)
}

private fun shortenIfSafe(fr: MyFraction): MyFraction {
    // В текущей реализации `MyFraction.shorten()` знак числителя учитывается некорректно,
    // поэтому чтобы не портить результат, упрощаем только неотрицательный числитель.
    if (fr.numerator >= 0) fr.shorten()
    return fr
}

fun Addition(a: MyFraction, b: MyFraction): MyFraction {
    // a/b + c/d = (ad + bc) / bd
    val num = a.numerator.toLong() * b.denominator.toLong() + b.numerator.toLong() * a.denominator.toLong()
    val den = a.denominator.toLong() * b.denominator.toLong()
    return shortenIfSafe(MyFraction(longToIntChecked(num), longToIntChecked(den)))
}

fun Subtraction(a: MyFraction, b: MyFraction): MyFraction {
    // a/b - c/d = (ad - bc) / bd
    val num = a.numerator.toLong() * b.denominator.toLong() - b.numerator.toLong() * a.denominator.toLong()
    val den = a.denominator.toLong() * b.denominator.toLong()
    return shortenIfSafe(MyFraction(longToIntChecked(num), longToIntChecked(den)))
}

fun Multiply(a: MyFraction, b: MyFraction): MyFraction {
    // a/b * c/d = ac / bd
    val num = a.numerator.toLong() * b.numerator.toLong()
    val den = a.denominator.toLong() * b.denominator.toLong()
    return shortenIfSafe(MyFraction(longToIntChecked(num), longToIntChecked(den)))
}

fun Divide(a: MyFraction, b: MyFraction): MyFraction {
    // a/b / c/d = ad / bc
    if (b.numerator == 0) throw DenominatorZeroError()

    val num = a.numerator.toLong() * b.denominator.toLong()
    val den = a.denominator.toLong() * b.numerator.toLong()
    return shortenIfSafe(MyFraction(longToIntChecked(num), longToIntChecked(den)))
}

/**
 * Возведение рационального числа в целую степень.
 *
 * Ожидается, что `exp` - целое число, представленное как `MyFraction` (то есть exp.denominator == 1).
 */
fun Exponentiation(base: MyFraction, exp: MyFraction): MyFraction {
    if (exp.denominator != 1) {
        throw IllegalArgumentException("Exponent must be an integer fraction (denominator == 1)")
    }

    val n = exp.numerator
    if (n == 0) return MyFraction(1, 1)

    return if (n > 0) {
        val num = powIntChecked(base.numerator, n)
        val den = powIntChecked(base.denominator, n)
        shortenIfSafe(MyFraction(num, den))
    } else {
        // base^(-n) = (den/num)^(n)
        if (base.numerator == 0) throw DenominatorZeroError()

        val absN = abs(n)
        val num = powIntChecked(base.denominator, absN)
        val den = powIntChecked(base.numerator, absN)
        shortenIfSafe(MyFraction(num, den))
    }
}

/** Вычисление ОПЗ; [internal] — для рекурсивного разбора выражений внутри `\dfrac{...}{...}` в [parser]. */
internal fun evaluateRpn(rpnItems: List<Item>): MyFraction {
    if (rpnItems.isEmpty()) {
        throw ExpressionEvaluationError("Пустое выражение: нет операндов и операций")
    }

    val stack = ArrayDeque<MyFraction>()

    rpnItems.forEach { token ->
        when (token) {
            is MyFraction -> {
                // Keep stack values isolated from source tokens.
                stack.addLast(MyFraction(token.numerator, token.denominator))
            }

            is BinaryOperand -> {
                if (stack.size < 2) {
                    throw ExpressionEvaluationError(
                        "Не хватает операнда для операции «${token.value}»: в стеке ${stack.size}, нужно 2"
                    )
                }

                val right = stack.removeLast()
                val left = stack.removeLast()

                val result = when (token.value) {
                    "+" -> Addition(left, right)
                    "-" -> Subtraction(left, right)
                    "\\cdot" -> Multiply(left, right)
                    "\\div" -> Divide(left, right)
                    "^" -> Exponentiation(left, right)
                    "\\pm" -> Addition(left, right)
                    else -> throw ExpressionEvaluationError("Неподдерживаемая операция: ${token.value}")
                }
                stack.addLast(result)
            }

            is UnaryOperand -> {
                throw ExpressionEvaluationError("Неподдерживаемый унарный операнд: ${token.value}")
            }

            is BracketItem -> {
                throw ExpressionEvaluationError("Лишняя скобка в обратной польской записи: ${token.bracket}")
            }
        }
    }

    when {
        stack.isEmpty() ->
            throw ExpressionEvaluationError("Не хватает операнда: после вычисления стек пуст")
        stack.size > 1 ->
            throw ExpressionEvaluationError(
                "Не хватает операции: осталось ${stack.size} операндов без оператора между ними"
            )
    }

    return stack.last()
}

fun EVAL(expression: StringBuilder): MyFraction {
    val items = parser(expression)
    val rpnItems = toRpn(items)
    return evaluateRpn(rpnItems)
}

fun EVAL(expression: String): MyFraction {
    return EVAL(StringBuilder(expression))
}

