package com.admin.controller;

import com.admin.entity.Result;
import com.admin.service.JobDispatchService;
import com.admin.vo.JobInstanceVO;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

@RestController
@RequiredArgsConstructor
@RequestMapping("/job/dispatch")
public class JobDispatchController extends BaseController{

    private final JobDispatchService jobDispatchService;

    // Long jobId, String instanceParams, long delayMS

    @PostMapping("/start")
    public Mono<Result<String>> start(@RequestBody JobInstanceVO jobInstanceVO) {
        if (jobInstanceVO.getJobId() == null) {
            return Mono.just(Result.error("jobId is null"));
        }
        jobDispatchService.start(jobInstanceVO);
        return Mono.just(Result.success("Published job"));
    }

    @PostMapping("/stop")
    public Mono<Result<String>> stop(@RequestBody JobInstanceVO jobInstanceVO) {
        if (jobInstanceVO.getJobId() == null) {
            return Mono.just(Result.error("jobId is null"));
        }
        jobDispatchService.stop(jobInstanceVO);
        return Mono.just(Result.success("Stopped job"));
    }


//    @RequestMapping("/log")
//    public String log() {
//        return "log";
//    }

}
