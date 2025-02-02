package com.common.util;

import com.common.exception.ServiceException;

import java.util.Collection;
import java.util.Map;

public class AssertUtils {


    /**
     * 断言对象为空
     *
     * @param obj     断言的对象
     * @param message 异常信息
     * @throws ServiceException 当断言失败时抛出 ServiceException 异常
     */
    public static void isNull(Object obj, String message) throws ServiceException {
        if (obj != null) {
            throw new ServiceException(message);
        }
    }


    /**
     * 断言对象为空
     *
     * @param obj       断言的对象
     * @param message   异常信息，可包含占位符{}
     * @param arguments 用于填充占位符的可变参数列表
     * @throws ServiceException 当断言失败时抛出 ServiceException 异常
     */
    public static void isNull(Object obj, String message, Object... arguments) throws ServiceException {
        if (obj != null) {
            throw new ServiceException(StringUtil.formatWithPlaceholders(message, arguments));
        }
    }


    /**
     * 断言对象不为空
     *
     * @param obj     断言的对象
     * @param message 异常信息
     * @throws ServiceException 当断言失败时抛出 ServiceException 异常
     */
    public static void isNotNull(Object obj, String message) throws ServiceException {
        if (obj == null) {
            throw new ServiceException(message);
        }
    }


    /**
     * 断言对象不为空
     *
     * @param obj       断言的对象
     * @param message   异常信息，可包含占位符{}
     * @param arguments 用于填充占位符的可变参数列表
     * @throws ServiceException 当断言失败时抛出 ServiceException 异常
     */
    public static void isNotNull(Object obj, String message, Object... arguments) throws ServiceException {
        if (obj == null) {
            throw new ServiceException(StringUtil.formatWithPlaceholders(message, arguments));
        }
    }


    /**
     * 断言条件为真
     *
     * @param condition 断言的条件
     * @param message   异常信息
     * @throws ServiceException 当断言失败时抛出 ServiceException 异常
     */
    public static void isTrue(boolean condition, String message) throws ServiceException {
        if (!condition) {
            throw new ServiceException(message);
        }
    }


    /**
     * 断言条件为真
     *
     * @param condition 断言的条件
     * @param message   异常信息，可包含占位符{}
     * @param arguments 用于填充占位符的可变参数列表
     * @throws ServiceException 当断言失败时抛出 ServiceException 异常
     */
    public static void isTrue(boolean condition, String message, Object... arguments) throws ServiceException {
        if (!condition) {
            throw new ServiceException(StringUtil.formatWithPlaceholders(message, arguments));
        }
    }


    /**
     * 断言条件为假
     *
     * @param condition 断言的条件
     * @param message   异常信息
     * @throws ServiceException 当断言失败时抛出 ServiceException 异常
     */
    public static void isFalse(boolean condition, String message) throws ServiceException {
        if (condition) {
            throw new ServiceException(message);
        }
    }


    /**
     * 断言条件为假
     *
     * @param condition 断言的条件
     * @param message   异常信息，可包含占位符{}
     * @param arguments 用于填充占位符的可变参数列表
     * @throws ServiceException 当断言失败时抛出 ServiceException 异常
     */
    public static void isFalse(boolean condition, String message, Object... arguments) throws ServiceException {
        if (condition) {
            throw new ServiceException(StringUtil.formatWithPlaceholders(message, arguments));
        }
    }


    /**
     * 断言字符串为空
     *
     * @param str     断言的字符串
     * @param message 异常信息，可包含占位符{}
     * @throws ServiceException 当断言失败时抛出 ServiceException 异常
     */
    public static void empty(String str, String message) throws ServiceException {
        if (str != null && !str.isEmpty()) {
            throw new ServiceException(message);
        }
    }


    /**
     * 断言字符串为空
     *
     * @param str       断言的字符串
     * @param message   异常信息，可包含占位符{}
     * @param arguments 用于填充占位符的可变参数列表
     * @throws ServiceException 当断言失败时抛出 ServiceException 异常
     */
    public static void empty(String str, String message, Object... arguments) throws ServiceException {
        if (str != null && !str.isEmpty()) {
            throw new ServiceException(StringUtil.formatWithPlaceholders(message, arguments));
        }
    }


    /**
     * 断言字符串不为空
     *
     * @param str     断言的字符串
     * @param message 异常信息，可包含占位符{}
     * @throws ServiceException 当断言失败时抛出 ServiceException 异常
     */
    public static void notEmpty(String str, String message) throws ServiceException {
        isNotNull(str, message);
        if (str.isEmpty()) {
            throw new ServiceException(message);
        }
    }


    /**
     * 断言字符串不为空
     *
     * @param str       断言的字符串
     * @param message   异常信息，可包含占位符{}
     * @param arguments 用于填充占位符的可变参数列表
     * @throws ServiceException 当断言失败时抛出 ServiceException 异常
     */
    public static void notEmpty(String str, String message, Object... arguments) throws ServiceException {
        isNotNull(str, message);
        if (str.isEmpty()) {
            throw new ServiceException(StringUtil.formatWithPlaceholders(message, arguments));
        }
    }


    /**
     * 断言集合不空
     *
     * @param collection 断言的集合
     * @param message    异常信息
     * @throws ServiceException 当断言失败时抛出 ServiceException 异常
     */
    public static void empty(Collection<?> collection, String message) throws ServiceException {
        if (collection != null && !collection.isEmpty()) {
            throw new ServiceException(message);
        }
    }


    /**
     * 断言集合不空
     *
     * @param collection 断言的集合
     * @param message    异常信息，可包含占位符{}
     * @param arguments  用于填充占位符的可变参数列表
     * @throws ServiceException 当断言失败时抛出 ServiceException 异常
     */
    public static void empty(Collection<?> collection, String message, Object... arguments) throws ServiceException {
        if (collection != null && !collection.isEmpty()) {
            throw new ServiceException(StringUtil.formatWithPlaceholders(message, arguments));
        }
    }


    /**
     * 断言集合不为空
     *
     * @param collection 断言的集合
     * @param message    异常信息
     * @throws ServiceException 当断言失败时抛出 ServiceException 异常
     */
    public static void notEmpty(Collection<?> collection, String message) throws ServiceException {
        if (collection == null || collection.isEmpty()) {
            throw new ServiceException(message);
        }
    }


    /**
     * 断言集合不空
     *
     * @param collection 断言的集合
     * @param message    异常信息，可包含占位符{}
     * @param arguments  用于填充占位符的可变参数列表
     * @throws ServiceException 当断言失败时抛出 ServiceException 异常
     */
    public static void notEmpty(Collection<?> collection, String message, Object... arguments) throws ServiceException {
        if (collection == null || collection.isEmpty()) {
            throw new ServiceException(StringUtil.formatWithPlaceholders(message, arguments));
        }
    }


    /**
     * 断言数组为空
     *
     * @param array   断言的数组
     * @param message 异常信息
     * @throws ServiceException 当断言失败时抛出 ServiceException 异常
     */
    public static void empty(Object[] array, String message) throws ServiceException {
        if (array != null && array.length > 0) {
            throw new ServiceException(message);
        }
    }


    /**
     * 断言数组为空
     *
     * @param array     断言的数组
     * @param message   异常信息，可包含占位符{}
     * @param arguments 用于填充占位符的可变参数列表
     * @throws ServiceException 当断言失败时抛出 ServiceException 异常
     */
    public static void empty(Object[] array, String message, Object... arguments) throws ServiceException {
        if (array != null && array.length > 0) {
            throw new ServiceException(StringUtil.formatWithPlaceholders(message, arguments));
        }
    }


    /**
     * 断言数组不为空
     *
     * @param array   断言的数组
     * @param message 异常信息
     * @throws ServiceException 当断言失败时抛出 ServiceException 异常
     */
    public static void notEmpty(Object[] array, String message) throws ServiceException {
        if (array == null || array.length == 0) {
            throw new ServiceException(message);
        }
    }


    /**
     * 断言数组不为空
     *
     * @param array     断言的数组
     * @param message   异常信息，可包含占位符{}
     * @param arguments 用于填充占位符的可变参数列表
     * @throws ServiceException 当断言失败时抛出 ServiceException 异常
     */
    public static void notEmpty(Object[] array, String message, Object... arguments) throws ServiceException {
        if (array == null || array.length == 0) {
            throw new ServiceException(StringUtil.formatWithPlaceholders(message, arguments));
        }
    }


    /**
     * 断言Map为空
     *
     * @param map     断言的Map
     * @param message 异常信息
     * @throws ServiceException 当断言失败时抛出 ServiceException 异常
     */
    public static void empty(Map<?, ?> map, String message) throws ServiceException {
        if (map != null && !map.isEmpty()) {
            throw new ServiceException(message);
        }
    }


    /**
     * 断言Map为空
     *
     * @param map       断言的Map
     * @param message   异常信息，可包含占位符{}
     * @param arguments 用于填充占位符的可变参数列表
     * @throws ServiceException 当断言失败时抛出 ServiceException 异常
     */
    public static void empty(Map<?, ?> map, String message, Object... arguments) throws ServiceException {
        if (map != null && !map.isEmpty()) {
            throw new ServiceException(StringUtil.formatWithPlaceholders(message, arguments));
        }
    }


    /**
     * 断言Map不为空
     *
     * @param map     断言的Map
     * @param message 异常信息
     * @throws ServiceException 当断言失败时抛出 ServiceException 异常
     */
    public static void notEmpty(Map<?, ?> map, String message) throws ServiceException {
        if (map == null || map.isEmpty()) {
            throw new ServiceException(message);
        }
    }


    /**
     * 断言Map不为空
     *
     * @param map       断言的Map
     * @param message   异常信息，可包含占位符{}
     * @param arguments 用于填充占位符的可变参数列表
     * @throws ServiceException 当断言失败时抛出 ServiceException 异常
     */
    public static void notEmpty(Map<?, ?> map, String message, Object... arguments) throws ServiceException {
        if (map == null || map.isEmpty()) {
            throw new ServiceException(StringUtil.formatWithPlaceholders(message, arguments));
        }
    }
}
