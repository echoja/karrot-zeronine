package com.karrot.zeronine.common.extension

/**
 * 컬렉션 확장 함수들 - Kotlin Idiomatic Programming
 */

inline fun <T> List<T>.ifNotEmpty(block: (List<T>) -> Unit) {
    if (isNotEmpty()) block(this)
}

fun <T> List<T>.second(): T = this[1]

fun <T> List<T>.secondOrNull(): T? = getOrNull(1)

fun <T> List<T>.third(): T = this[2]

fun <T> List<T>.thirdOrNull(): T? = getOrNull(2)

fun <K, V> Map<K, V>.getOrThrow(key: K, lazyMessage: () -> String = { "Key $key not found" }): V =
    get(key) ?: throw NoSuchElementException(lazyMessage())

inline fun <T, R : Comparable<R>> Iterable<T>.maxByOrThrow(
    selector: (T) -> R,
    lazyMessage: () -> String = { "Collection is empty" }
): T = maxByOrNull(selector) ?: throw NoSuchElementException(lazyMessage())

inline fun <T, R : Comparable<R>> Iterable<T>.minByOrThrow(
    selector: (T) -> R,
    lazyMessage: () -> String = { "Collection is empty" }
): T = minByOrNull(selector) ?: throw NoSuchElementException(lazyMessage())

fun <T> Iterable<T>.partitionIndexed(predicate: (index: Int, T) -> Boolean): Pair<List<T>, List<T>> {
    val first = mutableListOf<T>()
    val second = mutableListOf<T>()
    forEachIndexed { index, element ->
        if (predicate(index, element)) first.add(element)
        else second.add(element)
    }
    return Pair(first, second)
}

fun <T> List<T>.chunkedBySize(maxSize: Int): List<List<T>> {
    require(maxSize > 0) { "maxSize must be positive" }
    return chunked(maxSize)
}
