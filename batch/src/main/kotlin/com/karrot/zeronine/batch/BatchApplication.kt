package com.karrot.zeronine.batch

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.autoconfigure.domain.EntityScan
import org.springframework.boot.runApplication
import org.springframework.data.jpa.repository.config.EnableJpaAuditing
import org.springframework.data.jpa.repository.config.EnableJpaRepositories
import org.springframework.scheduling.annotation.EnableScheduling

@SpringBootApplication(scanBasePackages = ["com.karrot.zeronine"])
@EnableJpaAuditing
@EnableJpaRepositories(basePackages = ["com.karrot.zeronine.core.domain"])
@EntityScan(basePackages = ["com.karrot.zeronine.core.domain"])
@EnableScheduling
class BatchApplication

fun main(args: Array<String>) {
    runApplication<BatchApplication>(*args)
}
