package com.karrot.zeronine.common.extension

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import java.util.UUID

/**
 * String 확장 함수 테스트
 * Kotest FunSpec 스타일 사용
 */
class StringExtensionsTest : FunSpec({

    test("toUUIDOrNull - 유효한 UUID 문자열") {
        val uuidString = "550e8400-e29b-41d4-a716-446655440000"

        val result = uuidString.toUUIDOrNull()

        result shouldNotBe null
        result?.toString() shouldBe uuidString
    }

    test("toUUIDOrNull - 잘못된 UUID 문자열") {
        val invalidString = "not-a-uuid"

        val result = invalidString.toUUIDOrNull()

        result shouldBe null
    }

    test("toUUID - 유효한 UUID 문자열") {
        val uuidString = "550e8400-e29b-41d4-a716-446655440000"

        val result = uuidString.toUUID()

        result.toString() shouldBe uuidString
    }

    test("orEmpty - null인 경우") {
        val nullString: String? = null

        val result = nullString.orEmpty()

        result shouldBe ""
    }

    test("orEmpty - 값이 있는 경우") {
        val string: String? = "hello"

        val result = string.orEmpty()

        result shouldBe "hello"
    }

    test("isNotNullOrBlank - null인 경우") {
        val nullString: String? = null

        nullString.isNotNullOrBlank() shouldBe false
    }

    test("isNotNullOrBlank - 빈 문자열인 경우") {
        val emptyString: String? = ""

        emptyString.isNotNullOrBlank() shouldBe false
    }

    test("isNotNullOrBlank - 공백 문자열인 경우") {
        val blankString: String? = "   "

        blankString.isNotNullOrBlank() shouldBe false
    }

    test("isNotNullOrBlank - 값이 있는 경우") {
        val string: String? = "hello"

        string.isNotNullOrBlank() shouldBe true
    }

    test("maskMiddle - 기본 마스킹") {
        val string = "01012345678"

        val result = string.maskMiddle()

        result shouldBe "01*******78"
    }

    test("maskMiddle - 커스텀 visible chars") {
        val string = "abcdefgh"

        val result = string.maskMiddle(visibleChars = 1)

        result shouldBe "a******h"
    }

    test("maskMiddle - 짧은 문자열") {
        val string = "abc"

        val result = string.maskMiddle()

        result shouldBe "***"
    }

    test("toSnakeCase") {
        "camelCaseString".toSnakeCase() shouldBe "camel_case_string"
        "PascalCaseString".toSnakeCase() shouldBe "pascal_case_string"
        "already_snake_case".toSnakeCase() shouldBe "already_snake_case"
    }

    test("toCamelCase") {
        "snake_case_string".toCamelCase() shouldBe "snakeCaseString"
        "kebab-case-string".toCamelCase() shouldBe "kebabCaseString"
        "space separated string".toCamelCase() shouldBe "spaceSeparatedString"
    }
})
