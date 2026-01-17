package com.karrot.zeronine.api.integration

import com.karrot.zeronine.api.dto.CreateDealRequest
import com.karrot.zeronine.core.domain.deal.DealRepository
import com.karrot.zeronine.core.domain.deal.DealStatus
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.web.client.TestRestTemplate
import org.springframework.boot.test.web.server.LocalServerPort
import org.springframework.http.HttpEntity
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.DynamicPropertyRegistry
import org.springframework.test.context.DynamicPropertySource
import org.testcontainers.containers.GenericContainer
import org.testcontainers.containers.PostgreSQLContainer
import org.testcontainers.utility.DockerImageName
import java.math.BigDecimal
import java.time.LocalDateTime

/**
 * Testcontainers를 활용한 통합 테스트
 * 실제 PostgreSQL, Redis와 연동하여 테스트
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
class DealIntegrationTest(
    @LocalServerPort private val port: Int,
    private val restTemplate: TestRestTemplate,
    private val dealRepository: DealRepository
) : DescribeSpec({

    describe("딜 API 통합 테스트") {

        context("POST /api/v1/deals - 딜 생성") {
            it("유효한 요청으로 딜을 생성할 수 있다") {
                val request = CreateDealRequest(
                    title = "통합 테스트 딜",
                    description = "통합 테스트용 딜입니다",
                    originalPrice = BigDecimal(10000),
                    dealPrice = BigDecimal(7000),
                    totalStock = 100,
                    startTime = LocalDateTime.now().plusHours(1),
                    endTime = LocalDateTime.now().plusDays(1),
                    regionCode = "SEOUL-01"
                )

                val headers = HttpHeaders().apply {
                    contentType = MediaType.APPLICATION_JSON
                    set("X-User-Id", "1")
                }

                val response = restTemplate.postForEntity(
                    "/api/v1/deals",
                    HttpEntity(request, headers),
                    Map::class.java
                )

                response.statusCode shouldBe HttpStatus.CREATED
                response.body?.get("success") shouldBe true
                (response.body?.get("data") as? Map<*, *>)?.get("title") shouldBe "통합 테스트 딜"
            }
        }

        context("GET /api/v1/deals/{uuid} - 딜 조회") {
            it("생성된 딜을 조회할 수 있다") {
                // Given: 딜 생성
                val createRequest = CreateDealRequest(
                    title = "조회 테스트 딜",
                    description = "조회 테스트용",
                    originalPrice = BigDecimal(20000),
                    dealPrice = BigDecimal(15000),
                    totalStock = 50,
                    startTime = LocalDateTime.now().plusHours(1),
                    endTime = LocalDateTime.now().plusDays(1),
                    regionCode = "SEOUL-02"
                )

                val createHeaders = HttpHeaders().apply {
                    contentType = MediaType.APPLICATION_JSON
                    set("X-User-Id", "1")
                }

                val createResponse = restTemplate.postForEntity(
                    "/api/v1/deals",
                    HttpEntity(createRequest, createHeaders),
                    Map::class.java
                )

                val createdDeal = createResponse.body?.get("data") as Map<*, *>
                val dealUuid = createdDeal["uuid"] as String

                // When: 딜 조회
                val getResponse = restTemplate.getForEntity(
                    "/api/v1/deals/$dealUuid",
                    Map::class.java
                )

                // Then
                getResponse.statusCode shouldBe HttpStatus.OK
                val data = getResponse.body?.get("data") as Map<*, *>
                data["uuid"] shouldBe dealUuid
                data["title"] shouldBe "조회 테스트 딜"
            }
        }

        context("GET /api/v1/deals/region/{regionCode} - 지역별 딜 조회") {
            it("특정 지역의 활성 딜 목록을 조회할 수 있다") {
                val response = restTemplate.getForEntity(
                    "/api/v1/deals/region/SEOUL-01",
                    Map::class.java
                )

                response.statusCode shouldBe HttpStatus.OK
                response.body?.get("success") shouldBe true
            }
        }
    }
}) {
    companion object {
        private val postgres = PostgreSQLContainer(DockerImageName.parse("postgres:17-alpine"))
            .apply {
                withDatabaseName("karrot_test")
                withUsername("test")
                withPassword("test")
            }

        private val redis = GenericContainer(DockerImageName.parse("redis:7-alpine"))
            .apply {
                withExposedPorts(6379)
            }

        init {
            postgres.start()
            redis.start()
        }

        @JvmStatic
        @DynamicPropertySource
        fun properties(registry: DynamicPropertyRegistry) {
            registry.add("spring.datasource.url", postgres::getJdbcUrl)
            registry.add("spring.datasource.username", postgres::getUsername)
            registry.add("spring.datasource.password", postgres::getPassword)
            registry.add("spring.jpa.hibernate.ddl-auto") { "create-drop" }
            registry.add("spring.data.redis.host", redis::getHost)
            registry.add("spring.data.redis.port") { redis.getMappedPort(6379) }
            registry.add("redisson.single-server-config.address") {
                "redis://${redis.host}:${redis.getMappedPort(6379)}"
            }
        }
    }
}
