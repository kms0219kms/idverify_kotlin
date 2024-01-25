package kr.hiplay.idverify.app.pass

import jakarta.servlet.http.HttpServletRequest
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.ResponseBody
import org.springframework.web.bind.annotation.RestController


@RestController
@RequestMapping("/api/pass")
class PassApiController(var passService: PassService) {
    @Autowired
    private lateinit var request: HttpServletRequest

    @GetMapping("/getHashData.do")
    @ResponseBody
    fun getHashData(): IHashData {
        val orderId = passService.createOrder()
        val hashData = passService.getHash(orderId)

        return hashData
    }
}
