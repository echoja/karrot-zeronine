package com.karrot.zeronine.api.config

import io.micrometer.core.instrument.Counter
import io.micrometer.core.instrument.MeterRegistry
import io.micrometer.core.instrument.Timer
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

/**
 * 애플리케이션 메트릭 설정
 * Prometheus/Grafana 연동을 위한 커스텀 메트릭
 */
@Configuration
class MetricsConfig(private val meterRegistry: MeterRegistry) {

    /**
     * 주문 생성 카운터
     */
    @Bean
    fun orderCreatedCounter(): Counter = Counter.builder("karrot.orders.created")
        .description("Number of orders created")
        .register(meterRegistry)

    /**
     * 주문 실패 카운터 (재고 부족 등)
     */
    @Bean
    fun orderFailedCounter(): Counter = Counter.builder("karrot.orders.failed")
        .description("Number of failed order attempts")
        .register(meterRegistry)

    /**
     * 딜 활성화 카운터
     */
    @Bean
    fun dealActivatedCounter(): Counter = Counter.builder("karrot.deals.activated")
        .description("Number of deals activated")
        .register(meterRegistry)

    /**
     * 재고 차감 타이머
     */
    @Bean
    fun stockDecrementTimer(): Timer = Timer.builder("karrot.stock.decrement")
        .description("Time taken to decrement stock")
        .register(meterRegistry)

    /**
     * 분산 락 획득 타이머
     */
    @Bean
    fun lockAcquisitionTimer(): Timer = Timer.builder("karrot.lock.acquisition")
        .description("Time taken to acquire distributed lock")
        .register(meterRegistry)

    /**
     * 캐시 히트율 카운터
     */
    @Bean
    fun cacheHitCounter(): Counter = Counter.builder("karrot.cache.hit")
        .description("Number of cache hits")
        .register(meterRegistry)

    @Bean
    fun cacheMissCounter(): Counter = Counter.builder("karrot.cache.miss")
        .description("Number of cache misses")
        .register(meterRegistry)
}
