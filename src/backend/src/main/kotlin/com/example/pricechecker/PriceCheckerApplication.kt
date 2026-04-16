package com.example.pricechecker

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication

@SpringBootApplication
class PriceCheckerApplication

fun main(args: Array<String>) {
    runApplication<PriceCheckerApplication>(*args)
}
