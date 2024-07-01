package kr.hiplay.idverify_web.common.dto

interface ExceptionDto {
    val code: String
    val status: Int
    val message: String
    val responseAt: String
}
