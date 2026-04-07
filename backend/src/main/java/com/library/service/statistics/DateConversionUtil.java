package com.library.service.statistics;

import java.sql.Date;
import java.time.LocalDate;

/**
 * 日期转换工具类
 * 用于安全地将JPA查询返回的日期对象转换为LocalDate
 * 兼容不同JPA Provider可能返回的不同类型
 */
public class DateConversionUtil {

    /**
     * 将Object转换为LocalDate
     * 支持java.sql.Date和java.time.LocalDate
     */
    public static LocalDate convertToLocalDate(Object dateObj) {
        if (dateObj == null) {
            return LocalDate.now();
        }

        if (dateObj instanceof LocalDate) {
            return (LocalDate) dateObj;
        } else if (dateObj instanceof Date) {
            return ((Date) dateObj).toLocalDate();
        } else if (dateObj instanceof java.util.Date) {
            return new Date(((java.util.Date) dateObj).getTime()).toLocalDate();
        } else {
            // 尝试解析字符串
            return LocalDate.parse(dateObj.toString());
        }
    }
}
