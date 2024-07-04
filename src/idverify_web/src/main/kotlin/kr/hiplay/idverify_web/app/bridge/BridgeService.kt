package kr.hiplay.idverify_web.app.bridge

import com.mongodb.client.MongoClient
import com.mongodb.client.MongoClients
import com.mongodb.client.MongoCollection
import com.mongodb.client.MongoDatabase
import com.mongodb.client.model.Filters
import io.github.cdimascio.dotenv.dotenv
import org.bson.Document
import org.bson.conversions.Bson
import org.springframework.stereotype.Service

@Service
class BridgeService {
    private val dotenv = dotenv()

    private val databaseClient: MongoClient = MongoClients.create(dotenv["MONGODB_URL"])
    private val database: MongoDatabase = databaseClient.getDatabase("cert")

    fun fetchClientInfo(clientId: String): Document {
        val collection: MongoCollection<Document> = database.getCollection("clients")
        val filter: Bson = Filters.eq("id", clientId)
        val clientInfo: Document = collection.find(filter).first()
            ?: throw ClientInfoException("[CEX-0002] 존재하지 않는 사이트 정보입니다.")

        return clientInfo
    }

    fun fetchConfigs(clientId: String, provider: String): Document {
        return fetchConfigsFromDocument(clientId, fetchClientInfo(clientId), provider)
    }

    fun fetchConfigsFromDocument(clientId: String, document: Document, provider: String): Document {
        val providerInfo = document.getList("providers", Document::class.java).find {
            it.getString("id") == provider
        }
            ?: throw ClientInfoException("[CEX-0003] 지원하지 않는 인증수단입니다.")

        return providerInfo.get("configs", Document::class.java)
    }

}

class ClientInfoException(msg: String) : RuntimeException(msg)