package ru.fil.calculator

import kotlin.math.sqrt


fun getPrimeList (numb: Int): MutableList<Int> {
    val lRes = mutableListOf<Int>()
    var n = numb
    if (n<0){
        lRes.add(-1)
        n *= -1
    }
        while(n>1){
            var n2 = sqrt(n.toDouble()).toInt()
            var is_success = false
            for (iDivider in 2..n2+1){
                if (n % iDivider == 0){
                    is_success=true
                    n = n / iDivider
                    lRes.add(iDivider)
                }

            }
            if (!is_success){
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

