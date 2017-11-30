package com.soywiz.kpspemu.util

import kotlin.math.PI
import kotlin.math.asin
import kotlin.math.cos
import kotlin.math.sin

fun cosv1(value: Float): Float = cos(value * PI * 0.5).toFloat()
fun sinv1(value: Float): Float = sin(value * PI * 0.5).toFloat()
fun nsinv1(value: Float): Float = (-sin(value * PI * 0.5)).toFloat()
fun asinv1(value: Float): Float = (asin(value) / (PI * 0.5)).toFloat()
