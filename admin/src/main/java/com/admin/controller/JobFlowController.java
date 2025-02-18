package com.admin.controller;

import com.admin.entity.Result;
import com.admin.entity.TableInfo;
import com.admin.service.JobFlowDispatchService;
import com.admin.service.JobFlowService;
import com.admin.vo.JobFlowVO;
import com.common.entity.JobFlow;
import com.common.entity.JobFlowInstance;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

@RestController
@RequiredArgsConstructor
@RequestMapping("/job/flow")
public class JobFlowController extends BaseController {

    private final JobFlowService jobFlowService;

    private final JobFlowDispatchService jobFlowDispatchService;


    /**
     * 工作流保存
     */
    @PostMapping("/save")
    public Mono<Result<String>> save(@RequestBody JobFlowVO jobFlowVO) {
        jobFlowService.save(jobFlowVO);
        return Mono.just(Result.success("saved"));
    }


    /**
     * 删除工作流
     */
    @GetMapping("/delete/{jobFlowId}")
    public Mono<Result<String>> delete(@PathVariable("jobFlowId") Long jobFlowId) {
        jobFlowService.delete(jobFlowId);
        return Mono.just(Result.success("deleted"));
    }


    /**
     * 工作流列表
     */
    @GetMapping("/list")
    public Mono<TableInfo<JobFlow>> list(@RequestBody JobFlowVO jobFlowVO) {
        startPage();
        TableInfo<JobFlow> tableInfo = jobFlowService.list(jobFlowVO);
        return Mono.just(tableInfo);
    }


    /**
     * 工作流详情
     */
    @GetMapping("/detail/{jobFlowId}")
    public Mono<Result<JobFlowVO>> detail(@PathVariable("jobFlowId") Long jobFlowId) {
        JobFlowVO jobFlow = jobFlowService.detail(jobFlowId);
        return Mono.just(Result.success(jobFlow));
    }


    /**
     * 启动工作流 停止动作在 jobInstance 中
     *
     * @param jobFlowId 工作流ID
     */
    @GetMapping("/start/{jobFlowId}")
    public Mono<Result<Long>> start(@PathVariable("jobFlowId") Long jobFlowId) {
        Long flowInstanceId = jobFlowDispatchService.start(jobFlowId);
        return Mono.just(Result.success(flowInstanceId, "Published job"));
    }


    /**
     * 停止工作流
     *
     * @param instanceId 工作流实例ID
     */
    @GetMapping("/stop/{instanceId}")
    public Mono<Result<String>> stop(@PathVariable("instanceId") Long instanceId) {
        jobFlowDispatchService.stop(instanceId);
        return Mono.just(Result.success("Stopped job"));
    }


    /**
     * 从某节点重试, 工作流运行状态为失败时使用. 这样整个 dag 只要每个节点至少运行一次就不会失败
     *
     * @param instanceId 工作流实例ID
     */
    @GetMapping("/retry/{instanceId}/{nodeId}")
    public Mono<Result<String>> retry(@PathVariable("instanceId") Long instanceId, @PathVariable("nodeId") Long nodeId) {
        jobFlowDispatchService.retry(instanceId, nodeId);
        return Mono.just(Result.success("Published job"));
    }


    /**
     * 工作流实例列表
     *
     * @param jobFlowVO 工作流实例查询条件
     */
    public Mono<TableInfo<JobFlowInstance>> instanceList(@RequestBody JobFlowVO jobFlowVO) {
        startPage();
        TableInfo<JobFlowInstance> tableInfo = jobFlowService.instanceList(jobFlowVO);
        return Mono.just(tableInfo);
    }


    /**
     * 工作流进度
     *
     * @param flowInstanceId 工作流实例 ID
     */
    @GetMapping("/progress/{flowInstanceId}")
    public Mono<Result<JobFlowVO>> progress(@PathVariable("flowInstanceId") Long flowInstanceId) {
        JobFlowVO flowVO = jobFlowService.progress(flowInstanceId);
        return Mono.just(Result.success(flowVO));
    }
}
