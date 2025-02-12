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


    @GetMapping("/list")
    public Mono<TableInfo<JobInfo>> list(@RequestBody JobRequestVO requestVO) {
        startPage();
        TableInfo<JobInfo> tableInfo = jobService.list(requestVO);
        return Mono.just(tableInfo);
    }

    @PostMapping("/save")
    public Mono<Result<String>> save(@RequestBody JobInfo jobInfo) {
        jobService.save(jobInfo);
        return Mono.just(Result.success("added"));
    }

    @GetMapping("/toggle/{jobId}")
    public Mono<Result<String>> toggle(@PathVariable("jobId") Long jobId) {
        jobService.toggle(jobId);
        return Mono.just(Result.success("toggled"));
    }

    @GetMapping("/delete?/{jobId}")
    public Mono<Result<String>> delete(@PathVariable Long jobId) {
        jobService.delete(jobId);
        return Mono.just(Result.success("deleted"));
    }

    @GetMapping("/detail/{jobId}")
    public Mono<Result<JobInfo>> detail(@PathVariable Long jobId) {
        JobInfo jobInfo = jobService.detail(jobId);
        return Mono.just(Result.success(jobInfo));
    }


    @GetMapping("/start/{jobId}")
    public Mono<Result<String>> start(@PathVariable("jobId") Long jobId, @RequestParam(value = "instanceParams", required = false) String instanceParams, @RequestParam(value = "delayMS", required = false, defaultValue = "0") Long delayMS) {
        jobDispatchService.start(jobId, instanceParams, delayMS);
        return Mono.just(Result.success("Published job"));
    }

    @GetMapping("/instance/list")
    public Mono<TableInfo<JobInstance>> instanceList(@RequestBody JobInstanceVO requestVO) {
        startPage();
        TableInfo<JobInstance> tableInfo = jobService.instanceList(requestVO);
        return Mono.just(tableInfo);
    }

    @PostMapping("/stop")
    public Mono<Result<String>> stop(@RequestBody JobInstanceVO jobInstanceVO) {
        if (jobInstanceVO.getJobId() == null) {
            return Mono.just(Result.error("jobId is null"));
        }
        jobDispatchService.stop(jobInstanceVO);
        return Mono.just(Result.success("Stopped job"));
    }

    @GetMapping("/log/{instanceId}")
    public Mono<TableInfo<JobLog>> log(@PathVariable("instanceId") Long instanceId) {
        startPage();
        TableInfo<JobLog> logs = jobDispatchService.log(instanceId);
        return Mono.just(logs);
    }
}
