package kr.hiplay.idverify_web.app.identify.unified

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import io.viascom.nanoid.NanoId
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import kr.hiplay.idverify_web.common.utils.CryptoUtil
import kr.hiplay.idverify_web.common.utils.KISA_SEED_CBC

import org.springframework.stereotype.Service
import java.net.URL
import java.net.URLDecoder

import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.util.*

interface IInitData {
    val mid: String
    val successUrl: String
    val failUrl: String
}

interface IResponseBase {
    val resultCode: String
    val resultMsg: String
}

interface IHashData {
    val orderId: String
    val authHash: String
}

interface IDecryptData : IResponseBase {
    val certKey: String
    val certIv: String
    val authData: String
    val specName: String
    val redirectUrl: String
}

@Service
class UnifiedService {
    private val _mid = "MIIiasTest"
    private val _apiKey = "ZUhPSzQzQUpCN1dLa1I0RFd3Y1VuQT09"

    private val httpClient = HttpClient.newBuilder().build()

    fun createOrder(): String {
        val orderId = NanoId.generate()

        // TODO: 요청 정보 DB처리

        return orderId
    }

    fun getInitialData(): IInitData {
        return object : IInitData {
            override val mid = _mid
            override val successUrl = "http://localhost:8080/unified/success.html"
            override val failUrl = "http://localhost:8080/unified/fail.html"
        }
    }

    fun getHash(orderId: String): IHashData {
        return object : IHashData {
            override val orderId = orderId
            override val authHash = CryptoUtil().SHA256Hash(_mid + orderId + _apiKey)
        }
    }

    fun decryptUserData(
        txId: String,
        inicisSeedKeyRaw: String,
        apiReqUri: String,
        clientService: String
    ): IDecryptData {
        val requestBody = buildJsonObject {
            put("mid", _mid)
            put("txId", txId)
        }

        val request = HttpRequest.newBuilder()
            .uri(URL(apiReqUri).toURI())
            .header("Content-Type", "application/json")
            .POST(HttpRequest.BodyPublishers.ofString(requestBody.toString()))
            .build()

        val response = httpClient.send(request, HttpResponse.BodyHandlers.ofString())
        val responseBody = jacksonObjectMapper().readTree(response.body())

        val specName = "AES/CBC/PKCS5Padding"
        val certKey = ""
        val certIv = ""

        val inicisSeedKey = Base64.getDecoder().decode(inicisSeedKeyRaw.toByteArray())
        val inicisSeedIv = "".toByteArray()

        val userName = Base64.getDecoder().decode(responseBody["userName"].asText())
        val userPhone = Base64.getDecoder().decode(responseBody["userPhone"].asText())
        val userBirth = Base64.getDecoder().decode(responseBody["userBirthday"].asText())
        val userCi = Base64.getDecoder().decode(responseBody["userCi"].asText())

        return object : IDecryptData {
            override val resultCode = responseBody["resultCode"].asText()
            override val resultMsg =
                URLDecoder.decode(responseBody["resultMsg"].asText(), "UTF-8")

            override val authData: String
                get() {
                    val result = buildJsonObject {
                        put("providerId", responseBody["providerDevCd"].asText())

                        put(
                            "userName",
                            KISA_SEED_CBC.SEED_CBC_Decrypt(
                                pbszUserKey = inicisSeedKey,
                                pbszIV = inicisSeedIv,
                                message = userName,
                                message_offset = 0,
                                message_length = userName.size
                            ).toString()
                        )
                        put(
                            "userPhone",
                            KISA_SEED_CBC.SEED_CBC_Decrypt(
                                pbszUserKey = inicisSeedKey,
                                pbszIV = inicisSeedIv,
                                message = userPhone,
                                message_offset = 0,
                                message_length = userPhone.size
                            ).toString()
                        )
                        put(
                            "userBirth",
                            KISA_SEED_CBC.SEED_CBC_Decrypt(
                                pbszUserKey = inicisSeedKey,
                                pbszIV = inicisSeedIv,
                                message = userBirth,
                                message_offset = 0,
                                message_length = userBirth.size
                            ).toString()
                        )
                        put("userGender", "")
                        put("userNationality", "")

                        put(
                            "ci",
                            KISA_SEED_CBC.SEED_CBC_Decrypt(
                                pbszUserKey = inicisSeedKey,
                                pbszIV = inicisSeedIv,
                                message = userCi,
                                message_offset = 0,
                                message_length = userCi.size
                            ).toString()
                        )
                        put("di", "")
                    }

                    return CryptoUtil().AESEncrypt(
                        specName,
                        certKey,
                        certIv,
                        buildJsonObject {
                            put("unified", result)
                        }.toString()
                    )
                }

            override val specName = specName
            override val certKey = certKey
            override val certIv = certIv
            override val redirectUrl: String
                get() = TODO("Not yet implemented")
        }
    }
}
