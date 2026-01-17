package com.karrot.zeronine.common.util

import java.util.UUID
import java.util.concurrent.atomic.AtomicLong

/**
 * ID 생성 유틸리티 - Immutability 원칙 준수
 */
object IdGenerator {

    private val sequence = AtomicLong(0)

    fun uuid(): String = UUID.randomUUID().toString()

    fun uuidWithoutDash(): String = uuid().replace("-", "")

    fun shortUuid(): String = uuid().take(8)

    /**
     * Snowflake-like ID 생성 (간소화 버전)
     * 타임스탬프 + 시퀀스 기반
     */
    fun snowflakeId(): Long {
        val timestamp = System.currentTimeMillis()
        val seq = sequence.incrementAndGet() and 0xFFF // 12 bits
        return (timestamp shl 12) or seq
    }

    /**
     * 주문번호 생성 (YYYYMMDD-XXXXXX 형식)
     */
    fun orderNumber(): String {
        val date = java.time.LocalDate.now()
        val dateStr = date.toString().replace("-", "")
        val random = (100000..999999).random()
        return "$dateStr-$random"
    }

    /**
     * 딜 코드 생성 (알파벳 대문자 + 숫자 6자리)
     */
    fun dealCode(): String {
        val chars = ('A'..'Z') + ('0'..'9')
        return (1..6).map { chars.random() }.joinToString("")
    }
}
