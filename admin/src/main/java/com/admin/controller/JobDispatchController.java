package com.admin.controller;

import com.admin.entity.Result;
import com.admin.service.JobDispatchService;
import com.admin.vo.JobInstanceVO;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

@RestController
@RequiredArgsConstructor
@RequestMapping("/job/dispatch")
public class JobDispatchController extends BaseController{

    private final JobDispatchService jobDispatchService;


    @GetMapping("/start/{jobId}")
    public Mono<Result<String>> start(@PathVariable("jobId") Long jobId, @RequestParam(value = "instanceParams", required = false, defaultValue = "#{null}") String instanceParams
            , @RequestParam(value = "delayMS", required = false, defaultValue = "0") Long delayMS) {
        jobDispatchService.start(jobId, instanceParams, delayMS);
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
