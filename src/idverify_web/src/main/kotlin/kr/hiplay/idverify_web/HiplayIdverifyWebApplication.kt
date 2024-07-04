package kr.hiplay.idverify_web

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.builder.SpringApplicationBuilder
import org.springframework.boot.runApplication
import org.springframework.boot.web.servlet.support.SpringBootServletInitializer

@SpringBootApplication
class HiplayIdverifyWebApplication : SpringBootServletInitializer() {
    override fun configure(application: SpringApplicationBuilder): SpringApplicationBuilder {
        return application.sources(HiplayIdverifyWebApplication::class.java)
    }
}

fun main(args: Array<String>) {
    runApplication<HiplayIdverifyWebApplication>(*args)
}
