package com.sports.common.util;

import org.springframework.beans.BeanUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

/**
 * Bean转换工具类
 */
public class BeanConvertUtil {

    private BeanConvertUtil() {
    }

    /**
     * 单个对象转换
     *
     * @param source      源对象
     * @param targetClass 目标对象类型
     * @param <S>         源对象类型
     * @param <T>         目标对象类型
     * @return 转换后的目标对象
     */
    public static <S, T> T convert(S source, Class<T> targetClass) {
        if (source == null) {
            return null;
        }
        try {
            T target = targetClass.getDeclaredConstructor().newInstance();
            BeanUtils.copyProperties(source, target);
            return target;
        } catch (Exception e) {
            throw new RuntimeException("Bean转换失败", e);
        }
    }

    /**
     * 单个对象转换（使用Supplier）
     *
     * @param source        源对象
     * @param targetSupplier 目标对象提供者
     * @param <S>           源对象类型
     * @param <T>           目标对象类型
     * @return 转换后的目标对象
     */
    public static <S, T> T convert(S source, Supplier<T> targetSupplier) {
        if (source == null) {
            return null;
        }
        T target = targetSupplier.get();
        BeanUtils.copyProperties(source, target);
        return target;
    }

    /**
     * 列表对象转换
     *
     * @param sourceList  源对象列表
     * @param targetClass 目标对象类型
     * @param <S>         源对象类型
     * @param <T>         目标对象类型
     * @return 转换后的目标对象列表
     */
    public static <S, T> List<T> convertList(List<S> sourceList, Class<T> targetClass) {
        if (sourceList == null || sourceList.isEmpty()) {
            return new ArrayList<>();
        }
        List<T> targetList = new ArrayList<>(sourceList.size());
        for (S source : sourceList) {
            targetList.add(convert(source, targetClass));
        }
        return targetList;
    }

    /**
     * 列表对象转换（使用Supplier）
     *
     * @param sourceList    源对象列表
     * @param targetSupplier 目标对象提供者
     * @param <S>           源对象类型
     * @param <T>           目标对象类型
     * @return 转换后的目标对象列表
     */
    public static <S, T> List<T> convertList(List<S> sourceList, Supplier<T> targetSupplier) {
        if (sourceList == null || sourceList.isEmpty()) {
            return new ArrayList<>();
        }
        List<T> targetList = new ArrayList<>(sourceList.size());
        for (S source : sourceList) {
            targetList.add(convert(source, targetSupplier));
        }
        return targetList;
    }
}
