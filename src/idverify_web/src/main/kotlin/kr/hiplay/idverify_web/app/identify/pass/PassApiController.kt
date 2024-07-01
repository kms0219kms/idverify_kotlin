package kr.hiplay.idverify_web.app.identify.pass

import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import kr.hiplay.idverify_web.app.identify.pass.dto.PassHashRequestDto
import kr.hiplay.idverify_web.common.dto.ExceptionDto
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.*
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter


@RestController
@RequestMapping("/api/pass")
class PassApiController(var passService: PassService) {
    @Autowired
    private lateinit var request: HttpServletRequest

    @Autowired
    private lateinit var response: HttpServletResponse

    @PostMapping("/getHashData.do")
    @ResponseBody
    fun getHashData(
        @RequestBody body: PassHashRequestDto
    ): Any {
        val clientId = body.client_id
        val kcpCertLibName = body.kcp_cert_lib_name

        if (clientId.isNullOrEmpty()) {
            val exception = object : ExceptionDto {
                override val code = "CEX-0001"
                override val status = HttpStatus.BAD_REQUEST.value()

                override val message = "\"client_id\" 값은 필수입니다."

                override val responseAt = LocalDateTime.now().format(
                    DateTimeFormatter.ISO_DATE_TIME
                )
            }

            response.status = HttpServletResponse.SC_BAD_REQUEST
            return exception
        }

        if (kcpCertLibName.isNullOrEmpty()) {
            val exception = object : ExceptionDto {
                override val code = "CEX-0001"
                override val status = HttpStatus.BAD_REQUEST.value()

                override val message = "\"kcp_cert_lib_name\" 값은 필수입니다."

                override val responseAt = LocalDateTime.now().format(
                    DateTimeFormatter.ISO_DATE_TIME
                )
            }

            response.status = HttpServletResponse.SC_BAD_REQUEST
            return exception
        }

        val orderId = passService.createOrder()
        val hashData = passService.getHash(clientId, orderId, kcpCertLibName)

        return hashData
    }
}
