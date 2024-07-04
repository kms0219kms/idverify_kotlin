package kr.hiplay.identify_cron

import com.mongodb.client.MongoClient
import com.mongodb.client.MongoClients
import com.mongodb.client.MongoCollection
import io.github.cdimascio.dotenv.dotenv
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kr.hiplay.identify_cron.jobs.TossAccessTokenJob
import mu.KotlinLogging
import org.bson.Document
import redis.clients.jedis.JedisPool

class HiplayIdverifyCronjob {
    private val dotenv = dotenv()

    private val dbClient: MongoClient = MongoClients.create(dotenv["MONGODB_URL"])
    private val collection: MongoCollection<Document> = dbClient.getDatabase("cert").getCollection("clients")

    private val jedis = JedisPool(dotenv["REDIS_HOST"], dotenv["REDIS_PORT"].toInt())

    private val logger = KotlinLogging.logger(HiplayIdverifyCronjob::class.java.simpleName)

    fun run() {
        this.logger.info { "Start HiplayIdverifyCronjob" }

        val documents = collection.find().toList()
        this.logger.info { "Fetched client configuration from MongoDB." }
        this.logger.debug { "Documents: $documents" }

        this.logger.info { "Start TOSS AccessToken Job" }
        val tossClients = documents.filter {
            it.getList("providers", Document::class.java).any { provider ->
                provider.getString("id") == "SIGNGATE_TOSS"
            }
        }
        this.logger.info { "Fetched TOSS API Credentials from MongoDB." }
        this.logger.debug { "Credentials: $tossClients" }

        runBlocking {
            launch {
                TossAccessTokenJob().execute(tossClients, jedis)
            }
        }
    }
}

fun main() {
    HiplayIdverifyCronjob().run()
}
