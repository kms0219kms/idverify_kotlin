package kr.hiplay.identify_cron.jobs

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import mu.KotlinLogging

import org.bson.Document
import redis.clients.jedis.JedisPool

import java.net.URL
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse


class TossAccessTokenJob {
    private val httpClient = HttpClient.newBuilder().build()
    private lateinit var jedis: JedisPool

    private val logger = KotlinLogging.logger(TossAccessTokenJob::class.java.simpleName)

    fun execute(clients: List<Document>, _jedis: JedisPool) {
        jedis = _jedis

        this.logger.info { "Start fetching token from TOSS API" }

        val clientConfigs = clients.map { client ->
            client.getList("providers", Document::class.java).find {
                it.getString("id") == "SIGNGATE_TOSS"
            }!!.get("configs", Document::class.java)
        }.stream().distinct().toList()

        for (config in clientConfigs) {
            getAccessToken(config)
        }

        this.logger.info { "Finish fetching token from TOSS API" }
    }

    private fun getAccessToken(
        client: Document
    ): HashMap<String, String> {
        val clientId = client.getString("client_id")
        val clientSecret = client.getString("client_secret")

        val requestBody = mapOf(
            "grant_type" to "client_credentials",
            "scope" to "ca",
            "client_id" to clientId,
            "client_secret" to clientSecret
        ).map { (key, value) -> "$key=${java.net.URLEncoder.encode(value, "UTF-8")}" }
            .joinToString("&")

        val request = HttpRequest.newBuilder()
            .uri(URL("https://oauth2.cert.toss.im/token").toURI())
            .header("Content-Type", "application/x-www-form-urlencoded")
            .POST(HttpRequest.BodyPublishers.ofString(requestBody))
            .build()

        val response = httpClient.send(request, HttpResponse.BodyHandlers.ofString())
        val responseBody = jacksonObjectMapper().readTree(response.body())

        this.logger.info { "$clientId : Token fetched" }
        this.logger.debug { "$clientId : $responseBody" }

        val dataMap: HashMap<String, String> = HashMap()
        dataMap["access_token"] = responseBody.get("access_token").asText()
        dataMap["scope"] = responseBody.get("scope").asText()
        dataMap["token_type"] = responseBody.get("token_type").asText()
        dataMap["expires_in"] = responseBody.get("expires_in").asText()

        jedis.resource.use { jedis ->
            val key = "toss-token:$clientId"
            if (jedis.exists(key)) {
                jedis.del(key)
            }

            jedis.hset(key, dataMap)
            jedis.expire(key, responseBody.get("expires_in").asLong())
            this.logger.info { "$key : Token saved" }
        }

        return dataMap
    }
}
