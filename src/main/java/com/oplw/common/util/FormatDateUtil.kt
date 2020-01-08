package com.oplw.common.util

import android.text.format.DateUtils
import java.sql.Date
import java.text.SimpleDateFormat
import java.util.*

/**
 *
 *   @author opLW
 *   @date  2019/3/6
 */
/**
 * 需要显示的日期成分
 * Date 代表大体的日期，如2019/7/17
 * Week 代表星期，如 7/15 星期一
 * DetailTime 代表详细的时间，如9:42
 */

enum class Type {
    Date,
    Week,
    DetailTime
}

/**
 * 将日期转换为yyyy年MM月dd日格式
 */
fun dateToFormatDate(date: Date): String {
    val sf = SimpleDateFormat("yyyy年MM月dd日", Locale.CHINA)
    return sf.format(date)
}

/**
 * @param date 当前的时间
 * @param isShowSuitableForm 是否显示亲和的时间格式，如"今天"
 * @param types 需要显示的日期成分，默认只有Date和DetailTime。注意Type的顺序影响内容的顺序。
 */
fun makeDateFormat(
    date: Long,
    isShowSuitableForm: Boolean = true,
    types: Array<Type> = arrayOf(Type.Date, Type.DetailTime)
): String {
    val myTime = Calendar.getInstance().also { it.time = Date(date) }
    val result = StringBuilder()
    types.asSequence()
        .forEach {
            val component = when(it) {
                Type.Date -> getRoughlyDate(date, isShowSuitableForm)
                Type.Week -> getWeekday(myTime)
                else -> getDetailTime(myTime)
            }
            result.append(component)
            result.append(" ")
        }
    val stringResult = result.toString()
    val indexOfLastBlank = stringResult.lastIndexOf(" ")
    return stringResult.substring(0 until indexOfLastBlank)
}

private fun getRoughlyDate(date: Long, isShowSuitableForm: Boolean): String {
    if (isShowSuitableForm) {
        when {
            DateUtils.isToday(date) -> {
                return "今天"
            }
            isYesterday(date) -> {
                return "昨天"
            }
        }
    }
    val myTime = Calendar.getInstance().also { it.time = Date(date) }
    return getDate(myTime)
}

/**
 * 提取日期的大体时间，如"2018/09/03"
 */
private fun getDate(myTime: Calendar): String {
    val systemTime = Calendar.getInstance()

    val sb = StringBuilder()
    //判断年份是否和当前一样，一样则不添加
    if (myTime.get(Calendar.YEAR) != systemTime.get(Calendar.YEAR)) {
        sb.append(myTime.get(Calendar.YEAR))
        sb.append("/")
    }
    sb.append(myTime.get(Calendar.MONTH) + 1)
    sb.append("/")
    sb.append(myTime.get(Calendar.DAY_OF_MONTH))

    return sb.toString()
}

private fun isYesterday(date: Long): Boolean {
    val time = Calendar.getInstance()
    val nowYear = time.get(Calendar.YEAR)
    val nowMonth = time.get(Calendar.MONTH)
    val nowDay = time.get(Calendar.DAY_OF_MONTH)

    time.time = Date(date)
    // 把日期加一天，如果等于现在则表示该日期是昨天
    time.add(Calendar.DAY_OF_MONTH, 1)

    return nowYear == time.get(Calendar.YEAR) &&
            nowMonth == time.get(Calendar.MONTH) &&
            nowDay == time.get(Calendar.DAY_OF_MONTH)
}

/**
 * 提取日期所在的星期，如"星期一"
 */
private fun getWeekday(myTime: Calendar): String {
    val weekAry = arrayOf("星期天", "星期一", "星期二", "星期三", "星期四", "星期五", "星期六")
    return weekAry[myTime.get(Calendar.DAY_OF_WEEK) - 1]
}

/**
 * 获取日期所在的详细时间，如"9:38"
 */
private fun getDetailTime(myTime: Calendar): String {
    val hour = myTime.get(Calendar.HOUR_OF_DAY)
    val minute = myTime.get(Calendar.MINUTE)
    return "$hour:$minute"
}

/**
 * 将如2019:03:07 18:37:02的时间串，
 * 转换成2019年3月7日和28:37两个字符串。
 */
fun getDateAndTime(`when`: String): Array<String> {
    //使用range的时候不用考虑endIndex减一的情况
    val year = `when`.substring(0..3).toInt()
    val month = `when`.substring(5..6).toInt()
    val day = `when`.substring(8..9).toInt()
    val date = String.format("%d年%d月%d日", year, month, day)

    val start = `when`.indexOf(" ", 0) + 1
    val end = `when`.lastIndexOf(":", `when`.lastIndex)
    val time = `when`.substring(start, end)

    return arrayOf(date, time)
}