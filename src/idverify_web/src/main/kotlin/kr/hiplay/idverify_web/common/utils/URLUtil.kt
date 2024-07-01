package kr.hiplay.idverify_web.common.utils


class URLUtil {
    fun getQueryMap(query: String): Map<String, String> {
        val params = query.split("&".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
        val map: MutableMap<String, String> = HashMap()

        for (param in params) {
            val name = param.split("=".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()[0]
            val value = param.split("=".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()[1]
            map[name] = value
        }

        return map
    }
}
