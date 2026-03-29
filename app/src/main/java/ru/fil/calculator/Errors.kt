package ru.fil.calculator

class ExpressionEvaluationError(message: String) : RuntimeException(message)

class DenominatorZeroError : RuntimeException("Деление на ноль")