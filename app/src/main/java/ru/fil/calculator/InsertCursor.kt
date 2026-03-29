package ru.fil.calculator

fun InsertCursor(expr: StringBuilder, posOfpipe: Int): String{

    val res = StringBuilder(expr).insert(posOfpipe, "|").toString()
    return res
}