package kr.hiplay.idverify_web.app.bridge

import jakarta.servlet.http.HttpServletRequest
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.ResponseBody
import org.springframework.web.bind.annotation.RestController

import java.security.KeyFactory
import java.security.KeyPairGenerator
import java.security.spec.RSAPublicKeySpec


@RestController
@RequestMapping("/api/bridge")
class BridgeApiController {
    @Autowired
    private lateinit var request: HttpServletRequest

    @GetMapping("/getRsaPublicKey.do", produces = ["text/plain;charset=utf-8"])
    @ResponseBody
    fun getRsaPublicKey(): String {
        val generator = KeyPairGenerator.getInstance("RSA")
        generator.initialize(2048)

        val keyPair = generator.generateKeyPair()
        val keyFactory = KeyFactory.getInstance("RSA")

        val publicSpec: RSAPublicKeySpec = keyFactory.getKeySpec(
            keyPair.public,
            RSAPublicKeySpec::class.java
        )

        val publicKeyModulus = publicSpec.modulus.toString(16)
        val publicKeyExponent = publicSpec.publicExponent.toString(16).padStart(6, '0')

        request.session.setAttribute("RSA-PrivateKey", keyPair.private)
        return listOf(publicKeyModulus, publicKeyExponent).joinToString(",")
    }
}
