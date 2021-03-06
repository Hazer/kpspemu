package com.soywiz.kpspemu.util

fun Int.setBits(bits: Int, set: Boolean = true): Int {
    if (set) {
        return this or bits
    } else {
        return this and bits.inv()
    }
}

// Useful function
fun Int.asNull(nullValue: Int = 0): Int? = if (this == nullValue) null else this
