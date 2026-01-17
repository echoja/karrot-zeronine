package com.karrot.zeronine.api.config

import com.karrot.zeronine.api.dto.ApiResponse
import com.karrot.zeronine.api.dto.ErrorInfo
import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.RestControllerAdvice

private val logger = KotlinLogging.logger {}

@RestControllerAdvice
class GlobalExceptionHandler {

    @ExceptionHandler(IllegalArgumentException::class)
    fun handleIllegalArgument(ex: IllegalArgumentException): ResponseEntity<ApiResponse<Nothing>> {
        logger.warn { "잘못된 요청: ${ex.message}" }
        return ResponseEntity
            .badRequest()
            .body(ApiResponse.error("BAD_REQUEST", ex.message ?: "잘못된 요청입니다"))
    }

    @ExceptionHandler(IllegalStateException::class)
    fun handleIllegalState(ex: IllegalStateException): ResponseEntity<ApiResponse<Nothing>> {
        logger.warn { "처리 불가 상태: ${ex.message}" }
        return ResponseEntity
            .status(HttpStatus.CONFLICT)
            .body(ApiResponse.error("CONFLICT", ex.message ?: "요청을 처리할 수 없는 상태입니다"))
    }

    @ExceptionHandler(NoSuchElementException::class)
    fun handleNotFound(ex: NoSuchElementException): ResponseEntity<ApiResponse<Nothing>> {
        logger.warn { "리소스 없음: ${ex.message}" }
        return ResponseEntity
            .status(HttpStatus.NOT_FOUND)
            .body(ApiResponse.error("NOT_FOUND", ex.message ?: "요청한 리소스를 찾을 수 없습니다"))
    }

    @ExceptionHandler(Exception::class)
    fun handleException(ex: Exception): ResponseEntity<ApiResponse<Nothing>> {
        logger.error(ex) { "서버 오류 발생" }
        return ResponseEntity
            .status(HttpStatus.INTERNAL_SERVER_ERROR)
            .body(
                ApiResponse.error(
                    ErrorInfo(
                        code = "INTERNAL_ERROR",
                        message = "서버 오류가 발생했습니다",
                        details = mapOf("exception" to ex.javaClass.simpleName)
                    )
                )
            )
    }
}
