package ru.fil.calculator

import org.junit.Test
import org.junit.Assert.*

/**
 * Example local unit test, which will execute on the development machine (host).
 *
 * See [testing documentation](http://d.android.com/tools/testing).
 */
class ExampleUnitTest {
    @Test
    fun addition_isCorrect() {
        assertEquals(4, 2 + 2)
    }

    @Test
    fun parse_mixedFraction_positive() {
        val items = parser(StringBuilder("2\\dfrac{1}{3}"))
        assertEquals(1, items.size)
        val token = items[0] as MyFraction
        assertEquals(7, token.numerator)
        assertEquals(3, token.denominator)
    }

    @Test
    fun parse_mixedFraction_negativeWhole() {
        val items = parser(StringBuilder("-2\\dfrac{1}{3}"))
        assertEquals(1, items.size)
        val token = items[0] as MyFraction
        assertEquals(-7, token.numerator)
        assertEquals(3, token.denominator)
    }

    @Test
    fun eval_sumOfMixedFractions() {
        val result = EVAL("1\\dfrac{1}{2}+2\\dfrac{2}{3}")
        assertEquals(25, result.numerator)
        assertEquals(6, result.denominator)
    }

    @Test
    fun parse_regularFraction_unchanged() {
        val items = parser(StringBuilder("\\dfrac{5}{7}"))
        assertEquals(1, items.size)
        val token = items[0] as MyFraction
        assertEquals(5, token.numerator)
        assertEquals(7, token.denominator)
    }

    @Test
    fun parse_placeholderFraction_unchangedSafety() {
        val items = parser(StringBuilder("\\dfrac{\\,}{\\,}"))
        assertEquals(1, items.size)
        val token = items[0] as MyFraction
        assertEquals(0, token.numerator)
        assertEquals(1, token.denominator)
    }

    @Test
    fun eval_implicitMultiplication_numberBracket() {
        val result = EVAL("2(3+4)")
        assertEquals(14, result.numerator)
        assertEquals(1, result.denominator)
    }

    @Test
    fun eval_implicitMultiplication_bracketNumber() {
        val result = EVAL("(1+2)3")
        assertEquals(9, result.numerator)
        assertEquals(1, result.denominator)
    }

    @Test
    fun eval_implicitMultiplication_bracketBracket() {
        val result = EVAL("(1+2)(3+4)")
        assertEquals(21, result.numerator)
        assertEquals(1, result.denominator)
    }

    @Test
    fun parse_implicitMultiplication_fractionBracket() {
        val items = parser(StringBuilder("\\dfrac{1}{2}(1+1)"))
        assertEquals(7, items.size)
        assertTrue(items[0] is MyFraction)
        assertEquals("\\cdot", (items[1] as BinaryOperand).value)
        assertEquals('(', (items[2] as BracketItem).bracket)
    }

    @Test
    fun parse_implicitMultiplication_numberIdentifierAndFunction() {
        val varItems = parser(StringBuilder("2x"))
        assertEquals(3, varItems.size)
        assertTrue(varItems[0] is MyFraction)
        assertEquals("\\cdot", (varItems[1] as BinaryOperand).value)
        assertEquals("x", (varItems[2] as UnaryOperand).value)

        val fnItems = parser(StringBuilder("2\\sin(x)"))
        assertEquals(6, fnItems.size)
        assertTrue(fnItems[0] is MyFraction)
        assertEquals("\\cdot", (fnItems[1] as BinaryOperand).value)
        assertEquals("sin", (fnItems[2] as UnaryOperand).value)
        assertEquals('(', (fnItems[3] as BracketItem).bracket)
        // Важно: между sin и '(' не должно появляться неявное умножение.
        assertTrue(fnItems[3] !is BinaryOperand)
    }

    @Test
    fun eval_sqrt_integer() {
        val result = EVAL("\\sqrt{4}")
        assertEquals(2, result.numerator)
        assertEquals(1, result.denominator)
    }

    @Test
    fun eval_sqrt_fraction_exact() {
        val result = EVAL("\\sqrt{\\dfrac{9}{4}}")
        assertEquals(3, result.numerator)
        assertEquals(2, result.denominator)
    }

    @Test(expected = ArithmeticException::class)
    fun eval_sqrt_negative_throws() {
        EVAL("\\sqrt{-1}")
    }

    @Test
    fun fraction_stringBuilder_standard_oneHalf() {
        val f = MyFraction(1, 2)
        assertEquals("\\dfrac{1}{2}", f.toStringBuilder(true).toString())
    }

    @Test
    fun fraction_stringBuilder_decimal_oneHalf() {
        val f = MyFraction(1, 2)
        assertEquals("0.5", f.toStringBuilder(false).toString())
    }

    @Test
    fun tryParseSingleRationalValue_rejects_expression_with_plus() {
        assertNull(tryParseSingleRationalValue(StringBuilder("1+2")))
    }

    @Test
    fun tryParseSingleRationalValue_accepts_frac() {
        val v = tryParseSingleRationalValue(StringBuilder("\\dfrac{1}{2}"))
        assertNotNull(v)
        assertEquals(1, v!!.numerator)
        assertEquals(2, v.denominator)
    }
}
