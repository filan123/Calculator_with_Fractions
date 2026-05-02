package ru.fil.calculator

//
//Преобразует инфиксный список токенов в обратную польскую запись (ОПЗ).
//
//Поддерживает:
// - бинарные операторы: +, -, ^, \cdot, \div, \pm
// - скобки: (), {}
// - унарные функции (например sin, sqrt), если за ними идет открывающая скобка
//
fun toRpn(items: MutableList<Item>): MutableList<Item> {
    val output = mutableListOf<Item>()
    val operators = ArrayDeque<Item>()

    fun isOpeningBracket(ch: Char): Boolean = ch == '(' || ch == '{'
    fun isClosingBracket(ch: Char): Boolean = ch == ')' || ch == '}'

    fun precedence(op: String): Int = when (op) {
        "^" -> 4
        "\\cdot", "\\div" -> 3
        "+", "-" -> 2
        else -> 0
    }

    fun isRightAssociative(op: String): Boolean = op == "^"

    fun isFunctionToken(token: Item, index: Int): Boolean {
        if (token !is UnaryOperand) return false
        if (index + 1 >= items.size) return false
        val next = items[index + 1]
        return next is BracketItem && isOpeningBracket(next.bracket)
    }

    items.forEachIndexed { index, token ->
        when (token) {
            is MyFraction -> output.add(token)

            is UnaryOperand -> {
                if (isFunctionToken(token, index)) {
                    operators.addLast(token)
                } else {
                    output.add(token)
                }
            }

            is BinaryOperand -> {
                val currentPrec = precedence(token.value)
                if (currentPrec == 0) {
                    // Неизвестный оператор оставляем как есть.
                    output.add(token)
                    return@forEachIndexed
                }

                while (operators.isNotEmpty()) {
                    val top = operators.last()
                    when (top) {
                        is BinaryOperand -> {
                            val topPrec = precedence(top.value)
                            val shouldPop = if (isRightAssociative(token.value)) {
                                topPrec > currentPrec
                            } else {
                                topPrec >= currentPrec
                            }
                            if (!shouldPop) break
                            output.add(operators.removeLast())
                        }
                        is UnaryOperand -> output.add(operators.removeLast())
                        else -> break
                    }
                }
                operators.addLast(token)
            }

            is BracketItem -> {
                if (isOpeningBracket(token.bracket)) {
                    operators.addLast(token)
                } else if (isClosingBracket(token.bracket)) {
                    while (operators.isNotEmpty()) {
                        val top = operators.last()
                        if (top is BracketItem && isOpeningBracket(top.bracket)) break
                        output.add(operators.removeLast())
                    }

                    if (operators.isNotEmpty() &&
                        operators.last() is BracketItem &&
                        isOpeningBracket((operators.last() as BracketItem).bracket)
                    ) {
                        operators.removeLast()
                    }

                    if (operators.isNotEmpty() && operators.last() is UnaryOperand) {
                        output.add(operators.removeLast())
                    }
                }
            }

            else -> output.add(token)
        }
    }

    while (operators.isNotEmpty()) {
        val top = operators.removeLast()
        if (top !is BracketItem) {
            output.add(top)
        }
    }

    return output
}
