package kr.hiplay.idverify_web.app.pass

import jakarta.servlet.http.HttpServletRequest
import kr.hiplay.idverify_web.app.pass.dto.PassCallbackDto
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.http.MediaType
import org.springframework.stereotype.Controller
import org.springframework.ui.Model
import org.springframework.ui.set
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.servlet.LocaleResolver


@Controller
@RequestMapping("/pass")
class PassController(var passService: PassService) {
    @Autowired
    private lateinit var request: HttpServletRequest

    @Autowired
    @Qualifier("localeResolver")
    private lateinit var localeResolver: LocaleResolver

    @GetMapping("/start.html")
    fun start(model: Model): String {
        model["provider"] = "pass"
        model["lang"] = localeResolver.resolveLocale(request)

        return "identify/start"
    }

    @GetMapping("/request.html")
    fun openRequest(model: Model): String {
        val initialData = passService.getInitialData()

        model["pass_siteCd"] = initialData.siteCd
        model["pass_webSiteId"] = initialData.webSiteId
        model["pass_webSiteIdHashing"] = initialData.webSiteIdHashing
        model["pass_callbackUrl"] = initialData.callbackUrl
        model["pass_kcpBaseUrl"] = initialData.kcpBaseUrl

        return "identify/pass/request"
    }

    @PostMapping("/callback.html", consumes = [MediaType.APPLICATION_FORM_URLENCODED_VALUE])
    fun callback(
        model: Model,
        @RequestBody body: PassCallbackDto
    ): String {
        if (body.res_cd !== "0000") {
            model["error"] = body.res_msg
            return "identify/error"
        }

        val validationResult = passService.validateHash(
            orderId = body.ordr_idxx,
            certNo = body.cert_no,
            dnHash = body.dn_hash
        )

        if (!validationResult.resCd.equals("0000")) {
            model["error"] = "You sent a malformed request."
            return "identify/error"
        }

        // TODO: clientService DB에서 가져오기

        val decryptedData = passService.decryptUserData(
            orderId = body.ordr_idxx,
            certNo = body.cert_no,
            encCertData = body.enc_cert_data2,
            clientService = "nguard"
        )

        // TODO: 요청 정보 DB처리

        model["authData"] = decryptedData
        return "identify/callback"
    }
}
