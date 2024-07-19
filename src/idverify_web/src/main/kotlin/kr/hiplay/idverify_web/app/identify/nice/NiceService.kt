package kr.hiplay.idverify_web.app.identify.nice

import NiceID.Check.CPClient
import io.github.cdimascio.dotenv.dotenv
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import kr.hiplay.idverify_web.app.bridge.BridgeService
import kr.hiplay.idverify_web.app.bridge.ClientInfoException
import kr.hiplay.idverify_web.app.common.IDecryptData
import kr.hiplay.idverify_web.common.utils.CryptoUtil
import org.springframework.stereotype.Service
import java.net.URLDecoder
import java.net.URLEncoder

interface IInitData {
    val siteCode: String
    val sitePasswd: String

    val authType: Char

    val successUrl: String
    val failUrl: String
}

interface IEncData {
    val encData: String
    val requestNo: String
}

@Service
class NiceService {
    private val dotenv = dotenv {
        ignoreIfMissing = true
        systemProperties = true
    }

    private val bridgeService = BridgeService()

    private val niceCheck = CPClient()

    fun getInitialData(clientId: String, method: String): IInitData {
        val niceConfigInfo = bridgeService.fetchConfigs(clientId, method.uppercase())

        if (niceConfigInfo.getString("provider") != "nice" && method != "pki") {
            throw ClientInfoException("[CEX-0002] 지원하지 않는 인증수단입니다.")
        }

        return object : IInitData {
            override val siteCode = niceConfigInfo.getString("site_code")
            override val sitePasswd = niceConfigInfo.getString("site_passwd")

            override val authType: Char = when (method) {
                "pass" -> 'M'
                "pki" -> 'U'
                "yeskey" -> 'F'
                "pass_cert" -> 'S'
                "card" -> 'C'
                else -> throw ClientInfoException("[CEX-0003] 지원하지 않는 인증수단입니다.")
            }

            override val successUrl = "/identify/nice/callback.html?client_id=$clientId&method=$method"
            override val failUrl = "/identify/nice/fail.html?client_id=$clientId&method=$method"
        }
    }

    fun getEncData(clientId: String, method: String): IEncData {
        val initialData = getInitialData(clientId, method)
        val requestNo = niceCheck.getRequestNO(initialData.siteCode)

        val sPlainData = "7:REQ_SEQ${requestNo.toByteArray().size}:$requestNo" +
                "8:SITECODE${initialData.siteCode.toByteArray().size}:${initialData.siteCode}" +
                "9:AUTH_TYPE${initialData.authType.toString().toByteArray().size}:${initialData.authType}" +
                "7:RTN_URL${initialData.successUrl.toByteArray().size}:${initialData.successUrl}" +
                "7:ERR_URL${initialData.failUrl.toByteArray().size}:${initialData.failUrl}" +
                "9:CUSTOMIZE0:"

        val iReturn = niceCheck.fnEncode(initialData.siteCode, initialData.sitePasswd, sPlainData)

        if (iReturn == 0) return object : IEncData {
            override val encData = niceCheck.cipherData
            override val requestNo = requestNo
        }

        if (iReturn == -1) throw ClientInfoException("[VNO-CP01] 암호화 시스템 에러입니다.")
        if (iReturn == -2) throw ClientInfoException("[VNO-CP02] 암호화 처리오류입니다.")
        if (iReturn == -3) throw ClientInfoException("[VNO-CP03] 암호화 데이터 오류입니다.")
        if (iReturn == -9) throw ClientInfoException("[VNO-CP09] 입력 데이터 오류입니다.")
        throw ClientInfoException("[VNO-CP99] 알수 없는 에러 입니다. ($iReturn)")
    }

    fun decryptUserData(clientId: String, method: String, encodedData: String, requestNo: String): IDecryptData {
        val clientInfo = bridgeService.fetchClientInfo(clientId)
        val niceConfigInfo = bridgeService.fetchConfigsFromDocument(clientId, clientInfo, method.uppercase())

        if (niceConfigInfo.getString("provider") != "nice" && method != "pki") {
            throw ClientInfoException("[CEX-0002] 지원하지 않는 인증수단입니다.")
        }

        val specName = "AES/CBC/PKCS5Padding"
        val certKey = clientInfo.getString("certKey")
        val certIv = clientInfo.getString("certIv")

        val iReturn = niceCheck.fnDecode(
            niceConfigInfo.getString("site_code"),
            niceConfigInfo.getString("site_passwd"),
            encodedData
        )

        if (iReturn == -1) throw ClientInfoException("[VNO-CP01] 복호화 시스템 오류입니다.")
        if (iReturn == -4) throw ClientInfoException("[VNO-CP04] 복호화 처리 오류입니다.")
        if (iReturn == -5) throw ClientInfoException("[VNO-CP05] 복호화 해쉬 오류입니다.")
        if (iReturn == -6) throw ClientInfoException("[VNO-CP06] 복호화 데이터 오류입니다.")
        if (iReturn == -9) throw ClientInfoException("[VNO-CP09] 입력 데이터 오류입니다.")
        if (iReturn == -12) throw ClientInfoException("[VNO-CP12] 사이트 패스워드 오류입니다.")
        if (iReturn < 0) throw ClientInfoException("[VNO-CP99] 알수 없는 에러 입니다. ($iReturn)")

        val sMapResult = niceCheck.fnParse(niceCheck.plainData)

        if (sMapResult["REQ_SEQ"].toString() != requestNo) {
            throw ClientInfoException("[VNO-FAIL] 세션값 불일치 오류입니다.")
        }

        if (sMapResult["ERR_CODE"].toString().isNotEmpty() && sMapResult["ERR_CODE"].toString().isNotBlank()) {
            throw ClientInfoException("[VNO-${sMapResult["ERR_CODE"]}] 본인 인증에 실패했습니다. 관리자에게 문의하십시오.")
        }

        return object : IDecryptData {
            override val authData: String
                get() {
                    val result = buildJsonObject {
                        put("telecomId", sMapResult["MOBILE_CO"].toString())

                        put("userName", URLDecoder.decode(sMapResult["UTF8_NAME"].toString(), "UTF-8"))
                        put("userPhone", sMapResult["MOBILE_NO"].toString())
                        put("userBirth", sMapResult["BIRTHDATE"].toString())
                        put(
                            "userGender", when {
                                sMapResult["GENDER"].toString() == "0" -> "F"
                                sMapResult["GENDER"].toString() == "1" -> "M"
                                else -> "E"
                            }
                        )
                        put(
                            "userNationality", when {
                                sMapResult["NATIONAINFO"].toString() == "0" -> "KR"
                                else -> "ETC"
                            }
                        )

                        put("ci", URLEncoder.encode(sMapResult["CI"].toString(), "UTF-8"))
                        put("di", URLEncoder.encode(sMapResult["DI"].toString(), "UTF-8"))
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
