package com.karrot.zeronine.api

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.autoconfigure.domain.EntityScan
import org.springframework.boot.runApplication
import org.springframework.data.jpa.repository.config.EnableJpaAuditing
import org.springframework.data.jpa.repository.config.EnableJpaRepositories

@SpringBootApplication(scanBasePackages = ["com.karrot.zeronine"])
@EnableJpaAuditing
@EnableJpaRepositories(basePackages = ["com.karrot.zeronine.core.domain"])
@EntityScan(basePackages = ["com.karrot.zeronine.core.domain"])
class ApiApplication

fun main(args: Array<String>) {
    runApplication<ApiApplication>(*args)
}
