package com.karrot.zeronine.common.extension

import java.util.UUID

/**
 * 문자열 확장 함수들 - Kotlin Idiomatic Programming
 */

fun String.toUUIDOrNull(): UUID? = runCatching { UUID.fromString(this) }.getOrNull()

fun String.toUUID(): UUID = UUID.fromString(this)

fun String?.orEmpty(): String = this ?: ""

fun String?.isNotNullOrBlank(): Boolean = !this.isNullOrBlank()

fun String.maskMiddle(visibleChars: Int = 2): String {
    if (length <= visibleChars * 2) return "*".repeat(length)
    val start = take(visibleChars)
    val end = takeLast(visibleChars)
    val middle = "*".repeat(length - visibleChars * 2)
    return "$start$middle$end"
}

fun String.toSnakeCase(): String =
    replace(Regex("([a-z])([A-Z])"), "$1_$2").lowercase()

fun String.toCamelCase(): String =
    split("_", "-", " ")
        .mapIndexed { index, word ->
            if (index == 0) word.lowercase()
            else word.replaceFirstChar { it.uppercase() }
        }
        .joinToString("")
