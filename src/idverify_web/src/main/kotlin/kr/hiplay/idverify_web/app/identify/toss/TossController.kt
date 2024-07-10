package kr.hiplay.idverify_web.app.identify.toss

import io.github.cdimascio.dotenv.dotenv
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import kr.hiplay.idverify_web.common.utils.EncodingUtil
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.stereotype.Controller
import org.springframework.ui.Model
import org.springframework.ui.set
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.servlet.LocaleResolver


@Controller
@RequestMapping("/identify/signgate_toss")
class TossController(var tossService: TossService) {
    private val dotenv = dotenv {
        ignoreIfMissing = true
        systemProperties = true
    }
    
    private val encodingUtil = EncodingUtil()

    @Autowired
    private lateinit var request: HttpServletRequest

    @Autowired
    private lateinit var response: HttpServletResponse

    @Autowired
    @Qualifier("localeResolver")
    private lateinit var localeResolver: LocaleResolver

    @GetMapping("/start.html")
    fun start(
        model: Model,
        @RequestParam("client_id", required = false) clientId: String?
    ): String {
        model["serviceName"] = encodingUtil.decodeUnicode(dotenv["SERVICE_NAME"])
        model["lang"] = localeResolver.resolveLocale(request)

        if (clientId.isNullOrEmpty() || clientId.isBlank()) {
            response.status = HttpServletResponse.SC_BAD_REQUEST
            model["error"] = "[CEX-0001] \"client_id\" 값은 필수입니다."
            return "identify/error"
        }

        model["provider"] = "signgate_toss"
        model["clientId"] = clientId

        return "identify/start"
    }
}
