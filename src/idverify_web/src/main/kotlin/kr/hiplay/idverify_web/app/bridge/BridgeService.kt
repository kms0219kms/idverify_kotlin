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
import org.springframework.transaction.annotation.Transactional

@Service
class BridgeService {
    private val dotenv = dotenv()

    private val databaseClient: MongoClient = MongoClients.create(dotenv["MONGODB_URL"])
    private val database: MongoDatabase = databaseClient.getDatabase("cert")

    @Transactional
    fun fetchClientInfo(clientId: String): Document {
        // TODO: DB에서 client값 가져와서 반환하기

        val collection: MongoCollection<Document> = database.getCollection("clients")
        val filter: Bson = Filters.eq("id", clientId)
        val clientInfo: Document = collection.find(filter).first()
            ?: throw ClientInfoException("[CEX-0002] 존재하지 않는 사이트 정보입니다.")

        return clientInfo
    }

}

class ClientInfoException(msg: String) : RuntimeException(msg)