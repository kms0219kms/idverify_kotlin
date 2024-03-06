package kr.hiplay.idverify_web.app.unified

import jakarta.servlet.http.HttpServletRequest

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.ResponseBody
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/unified")
class UnifiedApiController(var unifiedService: UnifiedService) {
    @Autowired
    private lateinit var request: HttpServletRequest

    @GetMapping("/getHashData.do")
    @ResponseBody
    fun getHashData(): IHashData {
        val orderId = unifiedService.createOrder()
        val hashData = unifiedService.getHash(orderId)

        return hashData
    }
}