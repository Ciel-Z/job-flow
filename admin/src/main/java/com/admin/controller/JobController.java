package com.admin.controller;

import com.admin.entity.Result;
import com.admin.entity.TableInfo;
import com.admin.service.JobDispatchService;
import com.admin.service.JobService;
import com.admin.vo.JobInstanceVO;
import com.admin.vo.JobRequestVO;
import com.common.entity.JobInfo;
import com.common.entity.JobInstance;
import com.common.entity.JobLog;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

@RestController
@RequiredArgsConstructor
@RequestMapping("/job")
public class JobController extends BaseController {

    private final JobService jobService;

    private final JobDispatchService jobDispatchService;


    /**
     * 任务列表
     */
    @GetMapping("/list")
    public Mono<TableInfo<JobInfo>> list(@RequestBody JobRequestVO requestVO) {
        startPage();
        TableInfo<JobInfo> tableInfo = jobService.list(requestVO);
        return Mono.just(tableInfo);
    }


    /**
     * 任务保存
     */
    @PostMapping("/save")
    public Mono<Result<String>> save(@RequestBody JobInfo jobInfo) {
        jobService.save(jobInfo);
        return Mono.just(Result.success("added"));
    }


    /**
     * 任务状态变更
     */
    @GetMapping("/toggle/{jobId}")
    public Mono<Result<String>> toggle(@PathVariable("jobId") Long jobId) {
        jobService.toggle(jobId);
        return Mono.just(Result.success("toggled"));
    }


    /**
     * 删除任务
     */
    @GetMapping("/delete?/{jobId}")
    public Mono<Result<String>> delete(@PathVariable Long jobId) {
        jobService.delete(jobId);
        return Mono.just(Result.success("deleted"));
    }


    /**
     * 任务详情
     *
     * @param jobId
     * @return
     */
    @GetMapping("/detail/{jobId}")
    public Mono<Result<JobInfo>> detail(@PathVariable Long jobId) {
        JobInfo jobInfo = jobService.detail(jobId);
        return Mono.just(Result.success(jobInfo));
    }


    /**
     * 启动任务
     *
     * @param jobId          任务ID
     * @param instanceParams 启动参数 (不传按创建任务时的参数)
     * @param delayMS        延迟启动时间
     */
    @GetMapping("/start/{jobId}")
    public Mono<Result<String>> start(@PathVariable("jobId") Long jobId, @RequestParam(value = "instanceParams", required = false) String instanceParams, @RequestParam(value = "delayMS", required = false, defaultValue = "0") Long delayMS) {
        jobDispatchService.start(jobId, instanceParams, delayMS);
        return Mono.just(Result.success("Published job"));
    }


    /**
     * 任务实例列表
     */
    @GetMapping("/instance/list")
    public Mono<TableInfo<JobInstance>> instanceList(@RequestBody JobInstanceVO requestVO) {
        startPage();
        TableInfo<JobInstance> tableInfo = jobService.instanceList(requestVO);
        return Mono.just(tableInfo);
    }


    /**
     * 停止任务实例
     */
    @PostMapping("/stop")
    public Mono<Result<String>> stop(@RequestBody JobInstanceVO jobInstanceVO) {
        if (jobInstanceVO.getJobId() == null) {
            return Mono.just(Result.error("jobId is null"));
        }
        jobDispatchService.stop(jobInstanceVO);
        return Mono.just(Result.success("Stopped job"));
    }


    /**
     * 任务实例日志
     */
    @GetMapping("/log/{instanceId}")
    public Mono<TableInfo<JobLog>> log(@PathVariable("instanceId") Long instanceId) {
        startPage();
        TableInfo<JobLog> logs = jobDispatchService.log(instanceId);
        return Mono.just(logs);
    }
}
