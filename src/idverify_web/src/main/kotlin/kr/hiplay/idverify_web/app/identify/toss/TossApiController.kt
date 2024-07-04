package kr.hiplay.idverify_web.app.identify.toss

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
@RequestMapping("/api/toss")
class TossApiController(var tossService: TossService) {
    @Autowired
    private lateinit var request: HttpServletRequest

    @Autowired
    private lateinit var response: HttpServletResponse

    @PostMapping("/getTxId.do")
    @ResponseBody
    fun getTxId(
        @RequestBody body: PassHashRequestDto
    ): Any {
        val clientId = body.client_id

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

        val txIdResponse = tossService.getTxId(clientId)

        if (txIdResponse.has("error")) {
            val exception = object : ExceptionDto {
                override val code = "TOS-${txIdResponse.getJSONObject("error").getString("errorCode")}"
                override val status = HttpStatus.INTERNAL_SERVER_ERROR.value()

                override val message = txIdResponse.getJSONObject("error").getString("reason")

                override val responseAt = LocalDateTime.now().format(
                    DateTimeFormatter.ISO_DATE_TIME
                )
            }

            response.status = HttpServletResponse.SC_INTERNAL_SERVER_ERROR
            return exception
        }

        val session = request.session
        session.setAttribute("TOSS_TXID", txIdResponse.getJSONObject("success").getString("txId"))

        return txIdResponse.toString()
    }
}
