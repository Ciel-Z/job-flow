package com.admin.controller;

import com.admin.entity.Result;
import com.admin.entity.TableInfo;
import com.admin.service.JobService;
import com.admin.vo.JobRequestVO;
import com.common.entity.JobInfo;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

@RestController
@RequiredArgsConstructor
@RequestMapping("/job")
public class JobController extends BaseController{

    private final JobService jobService;

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

}
