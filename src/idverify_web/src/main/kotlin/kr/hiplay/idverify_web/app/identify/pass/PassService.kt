package kr.hiplay.idverify_web.app.identify.pass

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import io.github.cdimascio.dotenv.dotenv
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import kr.co.kcp.CT_CLI
import kr.hiplay.idverify_web.app.bridge.BridgeService
import kr.hiplay.idverify_web.app.bridge.ClientInfoException
import kr.hiplay.idverify_web.app.common.IDecryptData
import kr.hiplay.idverify_web.common.utils.CryptoUtil
import org.bouncycastle.asn1.ASN1Sequence
import org.bouncycastle.asn1.pkcs.EncryptedPrivateKeyInfo
import org.bouncycastle.openssl.jcajce.JcaPEMKeyConverter
import org.bouncycastle.openssl.jcajce.JceOpenSSLPKCS8DecryptorProviderBuilder
import org.bouncycastle.pkcs.PKCS8EncryptedPrivateKeyInfo
import org.bson.Document
import org.springframework.stereotype.Service
import java.net.URL
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.security.PrivateKey
import java.security.Signature
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.*

enum class EConnectionMethod(val libName: String) {
    API_SPL("KCP_SPL_API"),
    SOCKET("KCP_CTCLI_SOCKET")
}

interface IInitData {
    val siteCd: String
    val webSiteId: String
    val callbackUrl: String
    val kcpBaseUrl: String
    val kcpCertLibName: String
}

interface IResponseBase {
    val resCd: String
    val resMsg: String
}

interface IHashData : IResponseBase {
    val upHash: String
    val orderId: String
    val kcpCertLibVer: String
    val kcpCertLibName: String
    val kcpMerchantTime: String?
}

@Service
class PassService {
    private val dotenv = dotenv {
        ignoreIfMissing = true
        systemProperties = true
    }
    
    private val bridgeService = BridgeService()

    private val cc = CT_CLI()
    private val httpClient = HttpClient.newBuilder().build()

    // https://stg-spl.kcp.co.kr - KCP 테스트 서버
    // https://spl.kcp.co.kr - KCP 운영 서버

    private val _kcpApiBaseUrl = if (dotenv["ENVIRONMENT"] == "production") "https://spl.kcp.co.kr"
    else "https://stg-spl.kcp.co.kr"

    private val _kcpApiReqUrl = URL("$_kcpApiBaseUrl/std/certpass") // KCP API 요청 URL

    private fun _getCurrentTime(pattern: String = "YYMMddHHmmss"): String {
        val currentTime = LocalDateTime.now(ZoneId.of("Asia/Seoul"))
        val timeFormatter = DateTimeFormatter.ofPattern(pattern)

        return currentTime.format(timeFormatter)
    }

    private fun _loadSplMctPrivateKeyPKCS8(
        passInfo: Document
    ): PrivateKey {
        val pemKeyConverter = JcaPEMKeyConverter()

        val encPkcs8PriKeyInfo =
            PKCS8EncryptedPrivateKeyInfo(
                EncryptedPrivateKeyInfo.getInstance(
                    ASN1Sequence.getInstance(
                        Base64.getDecoder().decode(
                            passInfo.getString("api_privatekey")
                                .replace("\r", "").replace("\n", "")
                                .replace("-----BEGIN ENCRYPTED PRIVATE KEY-----", "").replace("-----END ENCRYPTED PRIVATE KEY-----", "")
                        )
                    )
                )
            )

        val priKeyInfo = encPkcs8PriKeyInfo.decryptPrivateKeyInfo(
            JceOpenSSLPKCS8DecryptorProviderBuilder().build(passInfo.getString("api_passpharse").toCharArray())
        )

        return pemKeyConverter.getPrivateKey(priKeyInfo)
    }

    private fun _makeSplSignatureData(targetData: String, passInfo: Document): String {
        val priKey: PrivateKey = _loadSplMctPrivateKeyPKCS8(passInfo)
        val btArrTargetData = targetData.toByteArray(Charsets.UTF_8)

        val sign = Signature.getInstance("SHA256WithRSA")
        sign.initSign(priKey)
        sign.update(btArrTargetData)

        val btArrSignData: ByteArray = sign.sign()

        return Base64.getEncoder().encodeToString(btArrSignData)
    }

    fun getInitialData(clientId: String): IInitData {
        val passInfo = bridgeService.fetchConfigs(clientId, "PASS")

        val method: EConnectionMethod = if ((1..2).random() == 2) {
            EConnectionMethod.API_SPL
        } else {
            EConnectionMethod.SOCKET
        }

        return object : IInitData {
            override val siteCd = if (method == EConnectionMethod.API_SPL) passInfo.getString("api_site_cd")
            else passInfo.getString("site_cd")
            override val webSiteId = passInfo.getString("web_siteid")
            override val callbackUrl = "/identify/pass/callback.html"

            // https://testcert.kcp.co.kr - KCP 테스트 서버
            // https://cert.kcp.co.kr - KCP 운영 서버
            override val kcpBaseUrl = if (dotenv["ENVIRONMENT"] == "production") "https://cert.kcp.co.kr"
            else "https://testcert.kcp.co.kr"
            override val kcpCertLibName = method.libName
        }
    }

    fun getHash(clientId: String, orderId: String, kcpCertLibName: String): IHashData {
        val passInfo = bridgeService.fetchConfigs(clientId, "PASS")

        val _webSiteId = passInfo.getString("web_siteid")

        if (kcpCertLibName == EConnectionMethod.API_SPL.libName) {
            val _siteCd = passInfo.getString("api_site_cd")

            val ctType = "HAS"
            val taxNo = "000000"

            val formattedTime = _getCurrentTime()
            val hashData = _makeSplSignatureData("${_siteCd}^${ctType}^${taxNo}^${formattedTime}", passInfo)

            val requestBody = buildJsonObject {
                put("kcp_cert_info", passInfo.getString("api_certificate"))
                put("site_cd", _siteCd)
                put("ordr_idxx", orderId)
                put("ct_type", ctType)
                put("web_siteid", _webSiteId)
                put("tax_no", taxNo)
                put("make_req_dt", formattedTime)
                put("kcp_sign_data", hashData)
            }

            val request = HttpRequest.newBuilder()
                .uri(_kcpApiReqUrl.toURI())
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(requestBody.toString()))
                .build()

            val response = httpClient.send(request, HttpResponse.BodyHandlers.ofString())
            val responseBody = jacksonObjectMapper().readTree(response.body())

            val hashDataReturn = object : IHashData {
                override val orderId = orderId
                override val resCd = responseBody.get("res_cd").asText()
                override val resMsg = responseBody.get("res_msg").asText()
                override val upHash = responseBody.get("up_hash").asText()
                override val kcpCertLibVer = responseBody.get("kcp_cert_lib_ver").asText()
                override val kcpCertLibName = kcpCertLibName
                override val kcpMerchantTime = responseBody.get("kcp_merchant_time").asText()
            }

            return hashDataReturn
        } else {
            val _siteCd = passInfo.getString("site_cd")
            val _encKey = passInfo.getString("enc_key")

            return object : IHashData {
                override val orderId = orderId
                override val resCd = "0000"
                override val resMsg = "정상처리"
                override val upHash = CT_CLI.makeHashData(
                    _encKey,
                    _siteCd + orderId +
                            (if (!_webSiteId.isNullOrEmpty() && _webSiteId.isNotBlank()) _webSiteId else "")
                            + "00" + "00" + "00"
                )
                override val kcpCertLibVer = cc.kcpLibVer
                override val kcpCertLibName = kcpCertLibName
                override val kcpMerchantTime = null // CT_CLI는 kcp_merchant_time을 사용하지 않음
            }
        }
    }

    fun validateHash(
        clientId: String,
        orderId: String,
        certNo: String,
        dnHash: String,
        kcpCertLibName: String
    ): IResponseBase {
        val passInfo = bridgeService.fetchConfigs(clientId, "PASS")

        if (kcpCertLibName == EConnectionMethod.API_SPL.libName) {
            val _siteCd = passInfo.getString("api_site_cd")
            val ctType = "CHK"

            val hashData = _makeSplSignatureData("${_siteCd}^${ctType}^${certNo}^${dnHash}", passInfo)

            val requestBody = buildJsonObject {
                put("kcp_cert_info", passInfo.getString("api_certificate"))
                put("site_cd", _siteCd)
                put("ordr_idxx", orderId)
                put("ct_type", ctType)
                put("dn_hash", dnHash)
                put("cert_no", certNo)
                put("kcp_sign_data", hashData)
            }

            val request = HttpRequest.newBuilder()
                .uri(_kcpApiReqUrl.toURI())
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(requestBody.toString()))
                .build()

            val response = httpClient.send(request, HttpResponse.BodyHandlers.ofString())
            val responseBody = jacksonObjectMapper().readTree(response.body())

            return object : IResponseBase {
                override val resCd = responseBody.get("res_cd").asText()
                override val resMsg = responseBody.get("res_msg").asText()
            }
        } else {
            val _siteCd = passInfo.getString("site_cd")
            val _encKey = passInfo.getString("enc_key")
            val isValid = cc.checkValidHash(_encKey, dnHash, (_siteCd + orderId + certNo))

            return object : IResponseBase {
                override val resCd = if (isValid) "0000" else "FAIL"
                override val resMsg = if (isValid) "정상처리" else "FAIL"
            }
        }
    }

    fun decryptUserData(
        clientId: String,
        orderId: String,
        certNo: String,
        encCertData: String,
        kcpCertLibName: String
    ): IDecryptData {
        val clientInfo = bridgeService.fetchClientInfo(clientId)
        val passInfo = bridgeService.fetchConfigsFromDocument(clientId, clientInfo, "PASS")

        val specName = "AES/CBC/PKCS5Padding"
        val certKey = clientInfo.getString("certKey")
        val certIv = clientInfo.getString("certIv")

        if (kcpCertLibName == EConnectionMethod.API_SPL.libName) {
            val _siteCd = passInfo.getString("api_site_cd")
            val ctType = "DEC"

            val hashData = _makeSplSignatureData("${_siteCd}^${ctType}^${certNo}", passInfo)

            val requestBody = buildJsonObject {
                put("kcp_cert_info", passInfo.getString("api_certificate"))
                put("site_cd", _siteCd)
                put("ordr_idxx", orderId)
                put("ct_type", ctType)
                put("cert_no", certNo)
                put("enc_cert_Data", encCertData)
                put("kcp_sign_data", hashData)
            }

            val request = HttpRequest.newBuilder()
                .uri(_kcpApiReqUrl.toURI())
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(requestBody.toString()))
                .build()

            val response = httpClient.send(request, HttpResponse.BodyHandlers.ofString())
            val responseBody = jacksonObjectMapper().readTree(response.body())

            if (responseBody.get("res_cd").asText() != "0000") {
                throw ClientInfoException(
                    "[KCP-${responseBody.get("res_cd").asText()}] ${
                        responseBody.get("res_msg").asText()
                    }"
                )
            }

            return object : IDecryptData {
                val resCd = responseBody.get("res_cd").asText()
                val resMsg = responseBody.get("res_msg").asText()

                override val authData: String
                    get() {
                        val result = buildJsonObject {
                            put("telecomId", responseBody.get("comm_id").asText())

                            put("userName", responseBody.get("user_name").asText())
                            put("userPhone", responseBody.get("phone_no").asText())
                            put("userBirth", responseBody.get("birth_day").asText())
                            put(
                                "userGender", when {
                                    responseBody.get("sex_code").asText() == "01" -> "M"
                                    responseBody.get("sex_code").asText() == "02" -> "F"
                                    else -> "E"
                                }
                            )
                            put(
                                "userNationality", when {
                                    responseBody.get("local_code").asText() == "01" -> "KR"
                                    else -> "ETC"
                                }
                            )

                            put("ci", responseBody.get("ci_url").asText())
                            put("di", responseBody.get("di_url").asText())
                        }

                        return CryptoUtil().AESEncrypt(
                            specName,
                            certKey,
                            certIv,
                            buildJsonObject {
                                put("pass", result)
                            }.toString()
                        )
                    }

                override val specName = specName
                override val certKey = certKey
                override val certIv = certIv
                override val redirectUrl: String = clientInfo.getString("redirectUrl")
            }
        } else {
            val _siteCd = passInfo.getString("site_cd")
            val _encKey = passInfo.getString("enc_key")

            cc.decryptEncCert(_encKey, _siteCd, certNo, encCertData)

            if (cc.getKeyValue("res_cd") != "0000") {
                throw ClientInfoException("[KCP-${cc.getKeyValue("res_cd")}] ${cc.getKeyValue("res_msg")}")
            }

            return object : IDecryptData {
                val resCd = cc.getKeyValue("res_cd")
                val resMsg = cc.getKeyValue("res_msg")

                override val authData: String
                    get() {
                        val result = buildJsonObject {
                            put("telecomId", cc.getKeyValue("comm_id"))

                            put("userName", cc.getKeyValue("user_name"))
                            put("userPhone", cc.getKeyValue("phone_no"))
                            put("userBirth", cc.getKeyValue("birth_day"))
                            put(
                                "userGender", when {
                                    cc.getKeyValue("sex_code") == "01" -> "M"
                                    cc.getKeyValue("sex_code") == "02" -> "F"
                                    else -> "E"
                                }
                            )
                            put(
                                "userNationality", when {
                                    cc.getKeyValue("local_code") == "01" -> "KR"
                                    else -> "ETC"
                                }
                            )

                            put("ci", cc.getKeyValue("ci_url"))
                            put("di", cc.getKeyValue("di_url"))
                        }

                        return CryptoUtil().AESEncrypt(
                            specName,
                            certKey,
                            certIv,
                            buildJsonObject {
                                put("pass", result)
                            }.toString()
                        )
                    }

                override val specName = specName
                override val certKey = certKey
                override val certIv = certIv
                override val redirectUrl: String = clientInfo.getString("redirectUrl")
            }
        }
    }
}
