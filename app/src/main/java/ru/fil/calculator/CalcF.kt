package ru.fil.calculator

import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.tan

private inline fun <T> withOverflowAsExpressionError(block: () -> T): T {
    return try {
        block()
    } catch (_: ArithmeticException) {
        throw ExpressionEvaluationError("Превышена память")
    }
}

private fun powLong(value: Long, exp: Long): Long {
    require(exp >= 0L) { "Exponent must be non-negative in powLong" }

    var result = 1L
    val base = value
    var i = 0L
    while (i < exp) {
        result = Math.multiplyExact(result, base)
        i++
    }
    return result
}

private fun longToLongChecked(value: Long): Long {
    return value
}

private fun powLongChecked(value: Long, exp: Long): Long {
    return withOverflowAsExpressionError {
        val resLong = powLong(value, exp)
        longToLongChecked(resLong)
    }
}

fun Addition(a: MyFraction, b: MyFraction): MyFraction {
    // a/b + c/d = (ad + bc) / bd
    return withOverflowAsExpressionError {
        val leftPart = Math.multiplyExact(a.numerator, b.denominator)
        val rightPart = Math.multiplyExact(b.numerator, a.denominator)
        val num = Math.addExact(leftPart, rightPart)
        val den = Math.multiplyExact(a.denominator, b.denominator)
        shortenIfSafe(MyFraction(longToLongChecked(num), longToLongChecked(den)))
    }
}

fun Subtraction(a: MyFraction, b: MyFraction): MyFraction {
    // a/b - c/d = (ad - bc) / bd
    return withOverflowAsExpressionError {
        val leftPart = Math.multiplyExact(a.numerator, b.denominator)
        val rightPart = Math.multiplyExact(b.numerator, a.denominator)
        val num = Math.subtractExact(leftPart, rightPart)
        val den = Math.multiplyExact(a.denominator, b.denominator)
        shortenIfSafe(MyFraction(longToLongChecked(num), longToLongChecked(den)))
    }
}

fun Multiply(a: MyFraction, b: MyFraction): MyFraction {
    // a/b * c/d = ac / bd
    return withOverflowAsExpressionError {
        val num = Math.multiplyExact(a.numerator, b.numerator)
        val den = Math.multiplyExact(a.denominator, b.denominator)
        shortenIfSafe(MyFraction(longToLongChecked(num), longToLongChecked(den)))
    }
}

fun Divide(a: MyFraction, b: MyFraction): MyFraction {
    // a/b / c/d = ad / bc
    if (b.numerator == 0L) throw DenominatorZeroError()

    return withOverflowAsExpressionError {
        val num = Math.multiplyExact(a.numerator, b.denominator)
        val den = Math.multiplyExact(a.denominator, b.numerator)
        shortenIfSafe(MyFraction(longToLongChecked(num), longToLongChecked(den)))
    }
}


//Возведение рационального числа в степень exp (целую или дробную, как `MyFraction`).

//Если точное значение — рациональное число с умеренными целыми числителем и знаменателем, результат
//вычисляется точно; иначе используется приближение через `Double` и подбор дроби (fractionPowApproximate).
fun Exponentiation(base: MyFraction, exp: MyFraction): MyFraction {
    val (p, q) = reduceFractionPair(exp.numerator, exp.denominator)
    if (p == 0L) return MyFraction(1, 1)

    if (q == 1L) {
        return if (p > 0) {
            val num = powLongChecked(base.numerator, p)
            val den = powLongChecked(base.denominator, p)
            shortenIfSafe(MyFraction(num, den))
        } else {
            if (base.numerator == 0L) throw DenominatorZeroError()
            val absP = abs(p)
            val num = powLongChecked(base.denominator, absP)
            val den = powLongChecked(base.numerator, absP)
            shortenIfSafe(MyFraction(num, den))
        }
    }

    var bn = base.numerator
    var bd = base.denominator
    var pPos = p
    if (pPos < 0) {
        if (bn == 0L) throw DenominatorZeroError()
        bn = bd.also { bd = bn }
        pPos = -pPos
    }

    rationalPowExactOrNull(bn, bd, pPos, q)?.let { return it }

    if (bn < 0 && q % 2L == 0L) {
        throw ArithmeticException(
            "Для отрицательного основания степень с чётным знаменателем не определена"
        )
    }

    return fractionPowApproximate(MyFraction(bn, bd), MyFraction(pPos, q))
}

// Вычисление ОПЗ; internal — для рекурсивного разбора выражений внутри `\dfrac{...}{...}` в parser.
internal fun evaluateRpn(rpnItems: List<Item>): MyFraction {
    if (rpnItems.isEmpty()) {
        throw ExpressionEvaluationError("Пустое выражение: нет операндов и операций")
    }

    val stack = ArrayDeque<MyFraction>()


    rpnItems.forEach { token ->
        when (token) {
            is MyFraction -> {
                stack.addLast(MyFraction(token.numerator, token.denominator))
            }

            is BinaryOperand -> {
                if (stack.size < 2) {
                    throw ExpressionEvaluationError(
                        "Не хватает операнда для операции «${token.value}»"
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
//                    "\\pm" -> Addition(left, right)
                    else -> throw ExpressionEvaluationError("Неподдерживаемая операция: ${token.value}")
                }
                stack.addLast(result)
            }

            is UnaryOperand -> {
                when (token.value) {
                    "pi" -> {
                        stack.addLast(MyFraction(104348L,33215L))
                    }

                    "sqrt" -> {
                        if (stack.size < 1) {
                            throw ExpressionEvaluationError(
                                "Не хватает операнда для квадратного корня"
                            )
                        }
                        val arg = stack.removeLast()
                        if (arg.numerator < 0) {
                            throw ArithmeticException(
                                "Квадратный корень из отрицательного числа не определён"
                            )
                        }
                        val result = rationalPowExactOrNull(
                            arg.numerator,
                            arg.denominator,
                            1,
                            2
                        ) ?: fractionPowApproximate(arg, MyFraction(1, 2))
                        stack.addLast(result)
                    }

                    "sin", "cos", "tan", "cot" -> {
                        if (stack.size < 1) {
                            throw ExpressionEvaluationError(
                                "Не хватает операнда для функции «${token.value}»"
                            )
                        }
                        val arg = stack.removeLast()
                        val argAsDouble = arg.numerator.toDouble() / arg.denominator.toDouble()
                        val result = when (token.value) {
                            "sin" -> sin(argAsDouble)
                            "cos" -> cos(argAsDouble)
                            "tan" -> {
                                val tanValue = tan(argAsDouble)
                                if (abs(tanValue) > 79) {
                                    throw ArithmeticException("Тангенс не определён для этого значения")
                                }
                                tanValue
                            }

                            "cot" -> {
                                val tanValue = tan(argAsDouble)
                                if (abs(tanValue) > 79) {
                                    throw ArithmeticException("Котангенс не определён для этого значения")
                                }
                                1.0 / tanValue
                            }

                            else -> throw ExpressionEvaluationError(
                                "Неподдерживаемый унарный операнд: ${token.value}"
                            )
                        }
                        stack.addLast(doubleToApproxFraction(result))
                    }

                    else -> throw ExpressionEvaluationError(
                        "Неподдерживаемый унарный операнд: ${token.value}"
                    )
                }
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

