package kr.hiplay.idverify_web.app.common

interface IDecryptData {
    val certKey: String
    val certIv: String
    val authData: String
    val specName: String
    val redirectUrl: String
}