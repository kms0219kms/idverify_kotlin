package kr.hiplay.idverify_web.app.unified.dto

class UnifiedCallbackDto {
    lateinit var resultCode: String
    lateinit var resultMsg: String
    lateinit var authRequestUrl: String

    lateinit var txId: String
    lateinit var token: String
}
