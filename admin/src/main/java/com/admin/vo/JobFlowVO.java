package com.admin.vo;

import com.common.dag.NodeEdgeDAG;
import lombok.Data;

@Data
public class JobFlowVO {

    private Long flowId;

    private String name;

    private Integer status;

    private String params;

    private NodeEdgeDAG dag;

}
