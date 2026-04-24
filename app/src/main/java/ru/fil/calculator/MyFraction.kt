package ru.fil.calculator

import java.math.BigDecimal
import java.math.RoundingMode
import kotlin.math.abs

class MyFraction(var numerator: Long, var denominator: Long) : Item() {
    private fun toDecimalString(maxScale: Int = 6): String {
        val decimalValue = BigDecimal(numerator).divide(BigDecimal(denominator), maxScale, RoundingMode.HALF_UP)
        return decimalValue.stripTrailingZeros().toPlainString()
    }

    init {
        if (denominator<0){
            denominator *= -1L
            numerator *= -1L
        }
        if (denominator == 0L){
            throw DenominatorZeroError()
        }
    }
    fun shorten(){
        if (denominator == 1L){ }
        else {
            val prime_list_num = getPrimeList(numerator)
            val prime_list_den = getPrimeList(denominator)
            var i = 0
            while (i < prime_list_num.size){
                val ai = prime_list_num[i]
                if (ai in prime_list_den){
                    prime_list_den.remove(ai)
                    prime_list_num.remove(ai)
                }
                else{i+=1}
                if (prime_list_den.size == 0){break}
            }
            if (prime_list_den.size==0){prime_list_den.add(1)}
            if (prime_list_num.size==0){prime_list_num.add(1)}
            numerator = multList(prime_list_num)
            denominator = multList(prime_list_den)
        }

    }
    fun change_den(multiplier: Long){
        numerator *= multiplier
        denominator *= multiplier
    }

    fun isDecimal(): Boolean {
        var d = denominator / gcd(abs(numerator), denominator)
        while (d % 2L == 0L) d /= 2L
        while (d % 5L == 0L) d /= 5L
        return d == 1L
    }


    /**
     * @param useStandardFraction true — всегда `\\dfrac{n}{d}` при [denominator] != 1;
     *   false — десятичная запись (в т.ч. для периодических — с ограниченной точностью).
     */
    fun toStringBuilder(useStandardFraction: Boolean): StringBuilder {
        if (denominator == 1L) return StringBuilder(numerator.toString())
        return if (useStandardFraction) {
            StringBuilder("\\dfrac{$numerator}{$denominator}")
        } else {
            StringBuilder(toDecimalString())
        }
    }

//    fun toStringBuilder(): StringBuilder {
//        if (denominator == 1) return StringBuilder(numerator.toString())
//        return if (isDecimal()) {
//            StringBuilder(toDecimalString())
//        } else {
//            StringBuilder("\\dfrac{$numerator}{$denominator}")
//        }
//    }
}