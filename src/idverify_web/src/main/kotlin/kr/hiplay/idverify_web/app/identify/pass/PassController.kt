package kr.hiplay.idverify_web.app.identify.pass

import io.github.cdimascio.dotenv.dotenv
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import kr.hiplay.idverify_web.app.bridge.ClientInfoException
import kr.hiplay.idverify_web.app.identify.pass.dto.PassCallbackDto
import kr.hiplay.idverify_web.common.utils.DecodeUnicodeUtil
import kr.hiplay.idverify_web.common.utils.URLUtil
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.http.MediaType
import org.springframework.stereotype.Controller
import org.springframework.ui.Model
import org.springframework.ui.set
import org.springframework.web.bind.annotation.*
import org.springframework.web.servlet.LocaleResolver
import java.net.URLDecoder


@Controller
@RequestMapping("/identify/pass")
class PassController(var passService: PassService) {
    private val dotenv = dotenv()

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
        model["serviceName"] = DecodeUnicodeUtil().convert(dotenv["SERVICE_NAME"])
        model["lang"] = localeResolver.resolveLocale(request)

        if (clientId.isNullOrEmpty() || clientId.isBlank()) {
            response.status = HttpServletResponse.SC_BAD_REQUEST
            model["error"] = "[CEX-0001] \"client_id\" 값은 필수입니다."
            return "identify/error"
        }

        model["provider"] = "pass"
        model["clientId"] = clientId

        return "identify/start"
    }

    @GetMapping("/request.html")
    fun openRequest(
        model: Model,
        @RequestParam("client_id", required = false) clientId: String?
    ): String {
        try {
            model["serviceName"] = DecodeUnicodeUtil().convert(dotenv["SERVICE_NAME"])
            model["lang"] = localeResolver.resolveLocale(request)

            if (clientId.isNullOrEmpty() || clientId.isBlank()) {
                response.status = HttpServletResponse.SC_BAD_REQUEST
                model["error"] = "[CEX-0001] \"client_id\" 값은 필수입니다."
                return "identify/error"
            }

            val initialData = passService.getInitialData(clientId)

            model["clientId"] = clientId
            model["pass_siteCd"] = initialData.siteCd
            model["pass_webSiteId"] = initialData.webSiteId
            model["pass_callbackUrl"] = initialData.callbackUrl
            model["pass_kcpBaseUrl"] = initialData.kcpBaseUrl
            model["pass_kcpCertLibName"] = initialData.kcpCertLibName

            return "identify/pass/request"
        } catch (e: ClientInfoException) {
            response.status = HttpServletResponse.SC_BAD_REQUEST
            model["error"] = e.message
            return "identify/error"
        }
    }

    @PostMapping(
        "/callback.html",
        consumes = [MediaType.APPLICATION_FORM_URLENCODED_VALUE]
    )
    fun callback(
        model: Model,
        @ModelAttribute body: PassCallbackDto
    ): String {
        model["serviceName"] = DecodeUnicodeUtil().convert(dotenv["SERVICE_NAME"])
        model["lang"] = localeResolver.resolveLocale(request)

        // URL 인코딩된 응답값을 디코딩한다.
        for (field in body::class.java.declaredFields) {
            field.isAccessible = true
            val value = field.get(body)
            if (value is String) {
                field.set(body, URLDecoder.decode(value, "EUC-KR"))
            }
        }

        if (body.res_cd != "0000") {
            println("ERROR on form... [KCP-${body.res_cd}] ${body.res_msg}")
            model["error"] = "[KCP-${body.res_cd}] ${body.res_msg}"
            return "identify/error"
        }

        val clientId = URLUtil().getQueryMap(body.param_opt_1).get("client_id")
        val kcpCertLibName = URLUtil().getQueryMap(body.param_opt_2).get("kcp_cert_lib_name")

        if (clientId.isNullOrEmpty() || clientId.isBlank()) {
            response.status = HttpServletResponse.SC_BAD_REQUEST
            model["error"] = "[CEX-0004] 인증 정보 수신에 문제가 발생했습니다. 관리자에게 문의하십시오. (\"client_id\")"
            return "identify/error"
        }

        if (kcpCertLibName.isNullOrEmpty() || kcpCertLibName.isBlank()) {
            response.status = HttpServletResponse.SC_BAD_REQUEST
            model["error"] = "[CEX-0004] 인증 정보 수신에 문제가 발생했습니다. 관리자에게 문의하십시오. (\"kcp_cert_lib_name\")"
            return "identify/error"
        }

        val validationResult = passService.validateHash(
            clientId = clientId,
            orderId = body.ordr_idxx,
            certNo = body.cert_no,
            dnHash = body.dn_hash,
            kcpCertLibName = kcpCertLibName
        )

        if (!validationResult.resCd.equals("0000")) {
            println("ERROR on Validation... ${kcpCertLibName} [KCP-${validationResult.resCd}] ${validationResult.resMsg}")
            model["error"] = "[KCP-${validationResult.resCd}] ${validationResult.resMsg}"
            return "identify/error"
        }

        val decryptedData = passService.decryptUserData(
            clientId = clientId,
            orderId = body.ordr_idxx,
            certNo = body.cert_no,
            encCertData = body.enc_cert_data2,
            kcpCertLibName = kcpCertLibName
        )

        if (!decryptedData.resCd.equals("0000")) {
            println("ERROR on Decrypt... ${kcpCertLibName} [KCP-${decryptedData.resCd}] ${decryptedData.resMsg}")
            model["error"] = "[KCP-${decryptedData.resCd}] ${decryptedData.resMsg}"
            return "identify/error"
        }

        model["specName"] = decryptedData.specName
        model["authData"] = decryptedData.authData
        model["redirectUrl"] = decryptedData.redirectUrl
        return "identify/callback"
    }
}
