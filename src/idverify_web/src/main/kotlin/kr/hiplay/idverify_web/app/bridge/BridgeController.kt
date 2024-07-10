package kr.hiplay.idverify_web.app.bridge

import io.github.cdimascio.dotenv.dotenv
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import kr.hiplay.idverify_web.common.utils.EncodingUtil
import org.bson.Document
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
@RequestMapping("/bridge")
class BridgeController(var bridgeService: BridgeService) {
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

    //================

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

        try {
            val clientInfo = bridgeService.fetchClientInfo(clientId)
            model["clientId"] = clientId

            // DB에 저장된 Service Name을 사용할 수 있으므로 덮어 쓴다.
            model["serviceName"] =
                encodingUtil.decodeUnicode(clientInfo.getString("name"))

            // 사용가능한 간편인증 리스트를 가져온다.
            model["providersList"] = clientInfo.getList("providers", Document::class.java)
                .map { it.getString("id") }
        } catch (e: ClientInfoException) {
            response.status = HttpServletResponse.SC_BAD_REQUEST
            model["error"] = e.message
            return "identify/error"
        }

        return "bridge/start"
    }

    @GetMapping("/request.html")
    fun request(
        model: Model,
        @RequestParam("client_id", required = false) clientId: String?,
        @RequestParam("provider", required = false) provider: String?
    ): String {
        model["serviceName"] = encodingUtil.decodeUnicode(dotenv["SERVICE_NAME"])
        model["lang"] = localeResolver.resolveLocale(request)

        if (clientId.isNullOrEmpty() || clientId.isBlank()) {
            response.status = HttpServletResponse.SC_BAD_REQUEST
            model["error"] = "[CEX-0001] \"client_id\" 값은 필수입니다."
            return "identify/error"
        }

        if (provider.isNullOrEmpty() || provider.isBlank()) {
            response.status = HttpServletResponse.SC_BAD_REQUEST
            model["error"] = "[CEX-0001] \"provider\" 값은 필수입니다."
            return "identify/error"
        }

        model["provider"] = provider

        try {
            val clientInfo = bridgeService.fetchClientInfo(clientId)
            model["clientId"] = clientId

            // DB에 저장된 Service Name을 사용할 수 있으므로 덮어 쓴다.
            model["serviceName"] =
                encodingUtil.decodeUnicode(clientInfo.getString("name"))

            // 사용가능한 간편인증 리스트에 이 provider가 있는지 확인한다.
            if (
                !clientInfo.getList("providers", Document::class.java).map {
                    it
                        .getString("id")
                }.contains(provider)
            ) {
                response.status = HttpServletResponse.SC_BAD_REQUEST
                model["error"] = "[CEX-0003] 지원하지 않는 인증수단입니다."
                return "identify/error"
            }
        } catch (e: ClientInfoException) {
            response.status = HttpServletResponse.SC_BAD_REQUEST
            model["error"] = e.message
            return "identify/error"
        }

        return "bridge/request"
    }
}
