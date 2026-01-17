package com.karrot.zeronine.api.dto

import java.time.LocalDateTime

/**
 * API 공통 응답 래퍼 - DSL 스타일 빌더 제공
 */
data class ApiResponse<T>(
    val success: Boolean,
    val data: T?,
    val error: ErrorInfo?,
    val timestamp: LocalDateTime = LocalDateTime.now()
) {
    companion object {
        fun <T> success(data: T): ApiResponse<T> = ApiResponse(
            success = true,
            data = data,
            error = null
        )

        fun <T> error(code: String, message: String): ApiResponse<T> = ApiResponse(
            success = false,
            data = null,
            error = ErrorInfo(code, message)
        )

        fun <T> error(errorInfo: ErrorInfo): ApiResponse<T> = ApiResponse(
            success = false,
            data = null,
            error = errorInfo
        )
    }
}

data class ErrorInfo(
    val code: String,
    val message: String,
    val details: Map<String, Any>? = null
)

/**
 * 페이지네이션 응답
 */
data class PageResponse<T>(
    val content: List<T>,
    val page: Int,
    val size: Int,
    val totalElements: Long,
    val totalPages: Int,
    val hasNext: Boolean,
    val hasPrevious: Boolean
)

/**
 * Type-safe Builder 패턴 (DSL)
 * 복잡한 응답 객체 생성을 위한 Kotlin DSL
 */
@DslMarker
annotation class ResponseDsl

@ResponseDsl
class PageResponseBuilder<T> {
    var content: List<T> = emptyList()
    var page: Int = 0
    var size: Int = 20
    var totalElements: Long = 0

    fun build(): PageResponse<T> {
        val totalPages = if (size > 0) ((totalElements + size - 1) / size).toInt() else 0
        return PageResponse(
            content = content,
            page = page,
            size = size,
            totalElements = totalElements,
            totalPages = totalPages,
            hasNext = page < totalPages - 1,
            hasPrevious = page > 0
        )
    }
}

fun <T> pageResponse(block: PageResponseBuilder<T>.() -> Unit): PageResponse<T> =
    PageResponseBuilder<T>().apply(block).build()
