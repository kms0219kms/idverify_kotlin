package kr.hiplay.idverify.app.test

import jakarta.servlet.http.HttpServletRequest
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.stereotype.Controller
import org.springframework.ui.Model
import org.springframework.ui.set
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.servlet.LocaleResolver


@Controller
class TestController {
    @Autowired
    private lateinit var request: HttpServletRequest

    @Autowired
    @Qualifier("localeResolver")
    private lateinit var localeResolver: LocaleResolver

    @GetMapping("/i18n.html")
    fun i18n(model: Model): String {
        model["lang"] = localeResolver.resolveLocale(request)

        return "i18n"
    }

}