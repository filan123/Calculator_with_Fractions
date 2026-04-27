package ru.fil.calculator


//Лексемы/элементы выражения после парсинга LaTeX-подобной строки.

open class Item


//Униарная операция/команда или идентификатор (например, `\sqrt`, `\sin`, `x`).

data class UnaryOperand(val value: String) : Item()


//Бинарная операция (например, `+`, `-`, `^`, `\cdot`, `\div`).

data class BinaryOperand(val value: String) : Item()


//Скобки: `(` `)` `{` `}`.

data class BracketItem(val bracket: Char) : Item()

