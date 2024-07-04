package kr.hiplay.idverify_web.app.identify.toss

import io.github.cdimascio.dotenv.dotenv
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import kr.hiplay.idverify_web.app.bridge.BridgeService
import org.json.JSONObject
import org.springframework.stereotype.Service
import redis.clients.jedis.JedisPooled
import java.net.URL
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse

@Service
class TossService {
    private val dotenv = dotenv()
    private val bridgeService = BridgeService()

    private val httpClient = HttpClient.newBuilder().build()
    private val jedis = JedisPooled(dotenv["REDIS_HOST"], dotenv["REDIS_PORT"].toInt())

    private val _tossApiBaseUrl = "https://cert.toss.im/api/v2/"

    fun getTxId(clientId: String): JSONObject {
        val tossInfo = bridgeService.fetchConfigs(clientId, "SIGNGATE_TOSS")

        val requestBody = buildJsonObject {
            put("requestType", "USER_NONE")
        }

        val request = HttpRequest.newBuilder()
            .uri(URL("$_tossApiBaseUrl/sign/user/auth/id/request").toURI())
            .header("Content-Type", "application/json")
            .header("Authorization", "Bearer ${jedis.get("toss-token:${tossInfo.getString("client_id")}")}")
            .POST(HttpRequest.BodyPublishers.ofString(requestBody.toString()))
            .build()

        val response = httpClient.send(request, HttpResponse.BodyHandlers.ofString())
        val responseBody = JSONObject(response.body())

        println(responseBody)
        return responseBody
    }
}
