package com.admin.controller;

import com.admin.util.ServletUtils;
import com.github.pagehelper.PageHelper;
import com.admin.text.Convert;

public class BaseController {

    /**
     * 当前记录起始索引
     */
    public static final String PAGE_NUM = "pageNum";

    /**
     * 每页显示记录数
     */
    public static final String PAGE_SIZE = "pageSize";

    /**
     * 分页参数合理化
     */
    public static final String REASONABLE = "reasonable";

    /**
     * 设置请求分页数据
     */
    protected static void startPage() {
        Integer pageNum = Convert.toInt(ServletUtils.getParameter(PAGE_NUM), 1);
        Integer pageSize = Convert.toInt(ServletUtils.getParameter(PAGE_SIZE), 10);
        Boolean reasonable = ServletUtils.getParameterToBool(REASONABLE, true);
        PageHelper.startPage(pageNum, pageSize).setReasonable(reasonable);
    }
}