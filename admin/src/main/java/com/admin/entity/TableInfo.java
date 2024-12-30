package com.admin.entity;

import com.github.pagehelper.Page;
import lombok.Data;

import java.util.List;

@Data
public class TableInfo<T> {

    private long total;

    private List<T> data;

    public static <T> TableInfo<T> of(Page<T> page) {
        TableInfo<T> info = new TableInfo<>();
        info.setTotal(page.getTotal());
        info.setData(page.getResult());
        return info;
    }

    public static <T> TableInfo<T> of(long total, List<T> data) {
        TableInfo<T> info = new TableInfo<>();
        info.setTotal(total);
        info.setData(data);
        return info;
    }
}
