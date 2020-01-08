package com.oplw.common.util

import java.security.MessageDigest

private val hexDigIts =
    arrayOf("0", "1", "2", "3", "4", "5", "6", "7", "8", "9", "a", "b", "c", "d", "e", "f")

/**
 * MD5加密
 * @param origin 字符
 * @return MD5加密之后的字符串
 */
fun encodeMd5(origin: String): String? {
    return  try {
        val md = MessageDigest.getInstance("MD5")
        md.update(origin.toByteArray())
        byteArrayToHexString(md.digest())
    } catch (e: Exception) {
        origin.hashCode().toString()
    }
}

private fun byteArrayToHexString(b: ByteArray): String {
    val resultSb = StringBuilder()
    for (i in b.indices) {
        val hex = Integer.toHexString(0xFF and(b[i].toInt()))
        if (hex.length == 1) {
            resultSb.append('0')
        }
        resultSb.append(hex)
    }
    return resultSb.toString()
}