package ru.fil.calculator

import java.math.BigDecimal
import kotlin.math.abs

class MyFraction(var numerator: Int, var denominator: Int) : Item() {
    init {
        if (denominator<0){
            denominator *= -1
            numerator *= -1
        }
        if (denominator == 0){
            throw DenominatorZeroError()
        }
    }
    fun shorten(){
        if (denominator == 1){ }
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
    fun change_den(multiplier:Int){
        numerator *= multiplier
        denominator *= multiplier
    }

    fun isDecimal(): Boolean {
        var d = denominator / gcd(abs(numerator), denominator)
        while (d % 2 == 0) d /= 2
        while (d % 5 == 0) d /= 5
        return d == 1
    }

    private fun gcd(a: Int, b: Int): Int {
        var x = a
        var y = b
        while (y != 0) {
            val t = y
            y = x % y
            x = t
        }
        return x
    }

    fun toStringBuilder():StringBuilder {
        if (denominator == 1){return StringBuilder(numerator.toString())}
        else if (isDecimal()){
            val decimalValue = BigDecimal(numerator).divide(BigDecimal(denominator))
            return StringBuilder(decimalValue.stripTrailingZeros().toPlainString())
        }
        else {return StringBuilder("\\dfrac{$numerator}{$denominator}")}
    }
}