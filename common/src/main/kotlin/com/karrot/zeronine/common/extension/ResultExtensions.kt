package com.karrot.zeronine.common.extension

/**
 * Result 확장 함수들 - Kotlin Null Safety & Error Handling
 */

inline fun <T, R> Result<T>.flatMap(transform: (T) -> Result<R>): Result<R> =
    fold(
        onSuccess = { transform(it) },
        onFailure = { Result.failure(it) }
    )

inline fun <T> Result<T>.recover(transform: (Throwable) -> T): Result<T> =
    fold(
        onSuccess = { Result.success(it) },
        onFailure = { Result.success(transform(it)) }
    )

inline fun <T> Result<T>.recoverIf(
    predicate: (Throwable) -> Boolean,
    transform: (Throwable) -> T
): Result<T> = fold(
    onSuccess = { Result.success(it) },
    onFailure = {
        if (predicate(it)) Result.success(transform(it))
        else Result.failure(it)
    }
)

fun <T> Result<T>.getOrThrow(lazyMessage: (Throwable) -> String): T =
    getOrElse { throw IllegalStateException(lazyMessage(it), it) }

inline fun <T> Result<T>.onFailureLog(
    log: (Throwable) -> Unit
): Result<T> = onFailure { log(it) }

fun <T> T?.toResult(errorMessage: String = "Value is null"): Result<T> =
    this?.let { Result.success(it) } ?: Result.failure(NullPointerException(errorMessage))

inline fun <T> runCatchingWithContext(
    context: String,
    block: () -> T
): Result<T> = runCatching(block).mapFailure {
    IllegalStateException("Error in $context: ${it.message}", it)
}

inline fun <T> Result<T>.mapFailure(transform: (Throwable) -> Throwable): Result<T> =
    fold(
        onSuccess = { Result.success(it) },
        onFailure = { Result.failure(transform(it)) }
    )
