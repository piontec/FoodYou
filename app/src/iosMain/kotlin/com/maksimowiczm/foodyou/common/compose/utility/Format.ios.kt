package com.maksimowiczm.foodyou.common.compose.utility

import kotlin.math.pow
import kotlin.math.round

actual fun Float.formatClipZeros(format: String): String {
    if (this % 1f == 0f) return toInt().toString()
    val decimals = format.substringAfter('.').substringBefore('f').toIntOrNull() ?: 2
    val factor = 10.0.pow(decimals)
    val rounded = round(this.toDouble() * factor) / factor
    val s = rounded.toString()
    return if ('.' in s) s.trimEnd('0').trimEnd('.') else s
}

actual fun Double.formatClipZeros(format: String): String {
    if (this % 1.0 == 0.0) return toInt().toString()
    val decimals = format.substringAfter('.').substringBefore('f').toIntOrNull() ?: 2
    val factor = 10.0.pow(decimals)
    val rounded = round(this * factor) / factor
    val s = rounded.toString()
    return if ('.' in s) s.trimEnd('0').trimEnd('.') else s
}
