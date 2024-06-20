package kr.hiplay.idverify_web.app.identify.pass.dto

class PassCallbackDto {
    lateinit var res_cd: String
    lateinit var res_msg: String

    lateinit var cert_no: String
    lateinit var ordr_idxx: String
    lateinit var enc_cert_data2: String

    lateinit var up_hash: String
    lateinit var dn_hash: String
}
