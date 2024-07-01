package kr.hiplay.idverify_web.common.utils

class DecodeUnicodeUtil {
    /**
     * \uXXXX 로 된 아스키코드 변경
     * i+2 to i+6 을 16진수의 int 계산 후 char 타입으로 변환
     * 코드 출처: https://cheonjoosung.github.io/blog/ko-unicode
     */
    fun convert(data: String): String {
        val sb = StringBuilder()
        var i = 0

        while (i < data.length) {

            if (data[i] == '\\' && data[i + 1] == 'u') {

                val word = data.substring(i + 2, i + 6).toInt(16).toChar()
                sb.append(word)
                i += 5
            } else {
                sb.append(data[i])
            }

            i++
        }

        return sb.toString()
    }
}