package kr.hiplay.idverify_web.app.identify.nice

import io.github.cdimascio.dotenv.dotenv
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import kr.hiplay.idverify_web.app.identify.nice.dto.NiceCallbackDto
import kr.hiplay.idverify_web.common.utils.EncodingUtil
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.http.MediaType
import org.springframework.stereotype.Controller
import org.springframework.ui.Model
import org.springframework.ui.set
import org.springframework.web.bind.annotation.*
import org.springframework.web.servlet.LocaleResolver


@Controller
@RequestMapping("/identify/nice")
class NiceController(var niceService: NiceService) {
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
        @RequestParam("client_id", required = false) clientId: String?,
        @RequestParam("method", required = false) method: String?
    ): String {
        model["serviceName"] = encodingUtil.decodeUnicode(dotenv["SERVICE_NAME"])
        model["lang"] = localeResolver.resolveLocale(request)

        if (clientId.isNullOrEmpty() || clientId.isBlank()) {
            response.status = HttpServletResponse.SC_BAD_REQUEST
            model["error"] = "[CEX-0001] \"client_id\" 값은 필수입니다."
            return "identify/error"
        }

        if (method.isNullOrEmpty() || method.isBlank()) {
            response.status = HttpServletResponse.SC_BAD_REQUEST
            model["error"] = "[CEX-0001] \"method\" 값은 필수입니다."
            return "identify/error"
        }

        val encData = niceService.getEncData(clientId, method)
        model["nice_encData"] = encData.encData

        val session = request.session
        session.setAttribute("NICE_REQ_SEQ", encData.requestNo)
        session.setAttribute("CLIENT_ID", clientId)
        session.setAttribute("NICE_METHOD", method)

        model["provider"] = "nice"
        model["clientId"] = clientId

        return "identify/start"
    }

    @PostMapping(
        "/success.html",
        consumes = [MediaType.APPLICATION_FORM_URLENCODED_VALUE]
    )
    fun successCallback(
        model: Model,
        @ModelAttribute body: NiceCallbackDto
    ): String {
        val session = request.session

        model["serviceName"] = encodingUtil.decodeUnicode(dotenv["SERVICE_NAME"])
        model["lang"] = localeResolver.resolveLocale(request)

        val decryptedData = niceService.decryptUserData(
            session.getAttribute("CLIENT_ID").toString(),
            session.getAttribute("NICE_METHOD").toString(),
            body.EncodeData,
            session.getAttribute("NICE_REQ_SEQ").toString()
        )

        model["specName"] = decryptedData.specName
        model["authData"] = decryptedData.authData
        model["redirectUrl"] = decryptedData.redirectUrl

        return "identify/callback"
    }
}
