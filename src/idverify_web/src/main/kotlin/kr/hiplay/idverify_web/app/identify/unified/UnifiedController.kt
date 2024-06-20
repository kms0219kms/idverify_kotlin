package kr.hiplay.idverify_web.app.identify.unified

import jakarta.servlet.http.HttpServletRequest
import kr.hiplay.idverify_web.app.identify.unified.dto.UnifiedCallbackDto
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.http.MediaType
import org.springframework.stereotype.Controller
import org.springframework.ui.Model
import org.springframework.ui.set
import org.springframework.web.bind.annotation.*
import org.springframework.web.servlet.LocaleResolver


@Controller
@RequestMapping("/identify/unified")
class UnifiedController(var unifiedService: UnifiedService) {
    @Autowired
    private lateinit var request: HttpServletRequest

    @Autowired
    @Qualifier("localeResolver")
    private lateinit var localeResolver: LocaleResolver

    @GetMapping("/start.html")
    fun start(model: Model, @RequestParam direct: String?): String {
        model["provider"] = "unified"
        model["lang"] = localeResolver.resolveLocale(request)

        val initialData = unifiedService.getInitialData()

        model["unified_mid"] = initialData.mid
        model["unified_directAgency"] = direct ?: ""

        model["unified_successUrl"] = initialData.successUrl
        model["unified_failUrl"] = initialData.failUrl

        return "identify/start"
    }

    @PostMapping(
        "/success.html",
        consumes = [MediaType.APPLICATION_FORM_URLENCODED_VALUE]
    )
    fun successCallback(
        model: Model,
        @RequestBody body: UnifiedCallbackDto
    ): String {
        if (body.resultCode !== "0000") {
            model["error"] = body.resultMsg
            return "identify/error"
        }

        val decryptedData = unifiedService.decryptUserData(
            txId = body.txId,
            inicisSeedKeyRaw = body.token,
            apiReqUri = body.authRequestUrl,
            clientService = "nguard"
        )

        // TODO: 요청 정보 DB처리

        model["authData"] = decryptedData

        return "identify/success"
    }
}
