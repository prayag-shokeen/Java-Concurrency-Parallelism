package com.learnjava.completableFuture;

import com.learnjava.service.HelloWorldService;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static com.learnjava.util.CommonUtil.*;
import static com.learnjava.util.LoggerUtil.log;

public class CompletableFutureHelloWorld {

    private final HelloWorldService hws;

    public CompletableFutureHelloWorld(HelloWorldService hws) {
        this.hws = hws;
    }

    public CompletableFuture<String> helloWorld() {
        return CompletableFuture
                .supplyAsync(hws::helloWorld)
                .thenApply(String::toUpperCase);
    }

    // this method is called from test class.
    // not making changes in main() to call this method, to maintain simplicity
    public String helloWorldMultipleAsyncCallsParallel() {
        startTimer();
        final CompletableFuture<String> helloFuture = CompletableFuture.supplyAsync(hws::hello);
        final CompletableFuture<String> worldFuture = CompletableFuture.supplyAsync(hws::world);

        // example of usage of .thenCombine()
        // which we will use to combine two futures.
        final String resultHelloWorldString = helloFuture
                .thenCombine(worldFuture, (helloFutureResult, worldFutureResult) -> helloFutureResult + worldFutureResult)
                .thenApply(String::toUpperCase)
                .join();
        timeTaken();
        return resultHelloWorldString;
    }

    public String threeAsyncCallsParallel() {
        startTimer();
        final CompletableFuture<String> helloFuture = CompletableFuture.supplyAsync(hws::hello);
        final CompletableFuture<String> worldFuture = CompletableFuture.supplyAsync(hws::world);
        final CompletableFuture<String> thirdFuture = CompletableFuture.supplyAsync(() -> {
            delay(1000);
            log("inside thirdMethod");
            return " Hi from Prayag!";
        });

        // example of usage of .thenCombine()
        // which we will use to combine two futures.
        final String resultHelloWorldString = helloFuture
                .thenCombine(worldFuture, (helloFutureResult, worldFutureResult) -> helloFutureResult + worldFutureResult)
                .thenCombine(thirdFuture, (previousResult, currentResult) -> previousResult + currentResult)
                .thenApply(String::toUpperCase)
                .join();
        timeTaken();
        return resultHelloWorldString;
    }

    // using custom thread pool for CompletableFuture instead of common thread pool.
    // CommonForkJoinPool is shared by parallelStreams & CompletableFuture
    // Verify through logs that the code is executed with custom thread pool
    public String threeAsyncCallsParallel_WithCustomThreadPool() {
        startTimer();
        ExecutorService executorService = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors());
        final CompletableFuture<String> helloFuture = CompletableFuture.supplyAsync(hws::hello, executorService);
        final CompletableFuture<String> worldFuture = CompletableFuture.supplyAsync(hws::world, executorService);
        final CompletableFuture<String> thirdFuture = CompletableFuture.supplyAsync(() -> {
            delay(1000);
            log("inside thirdMethod");
            return " Hi from Prayag!";
        }, executorService);

        // example of usage of .thenCombine()
        // which we will use to combine two futures.
        final String resultHelloWorldString = helloFuture
                .thenCombine(worldFuture, (helloFutureResult, worldFutureResult) -> helloFutureResult + worldFutureResult)
                .thenCombine(thirdFuture, (previousResult, currentResult) -> previousResult + currentResult)
                .thenApply(String::toUpperCase)
                .join();
        timeTaken();
        return resultHelloWorldString;
    }

    // added more logs to demonstrate that all .thenCombine(), .thenApply(), calls are getting executed in
    // forkJoinPool.commonPool
    public String threeAsyncCallsParallel_WithAdditionalLogs() {
        startTimer();
        final CompletableFuture<String> helloFuture = CompletableFuture.supplyAsync(hws::hello);
        final CompletableFuture<String> worldFuture = CompletableFuture.supplyAsync(hws::world);
        final CompletableFuture<String> thirdFuture = CompletableFuture.supplyAsync(() -> {
            delay(1000);
            log("inside thirdMethod");
            return " Hi from Prayag!";
        });

        // example of usage of .thenCombine()
        // which we will use to combine two futures.
        final String resultHelloWorldString = helloFuture
                .thenCombine(worldFuture, (helloFutureResult, worldFutureResult) -> {
                    log("thenCombine of helloFuture & worldFuture");
                    return helloFutureResult + worldFutureResult;
                })
                .thenCombine(thirdFuture, (previousResult, currentResult) -> {
                    log("thenCombine of first 2 future response & 3rd future");
                    return previousResult + currentResult;
                })
                .thenApply(response -> {
                    log("thenApply log");
                    return response.toUpperCase();
                })
                .join();
        timeTaken();
        return resultHelloWorldString;
    }

    public String helloWorldThenCompose() {

        startTimer();
        final CompletableFuture<String> helloFuture = CompletableFuture.supplyAsync(hws::hello);

        // .thenCompose() is used when one future is dependent on the result of other completableFuture
        // like below helloFuture response is getting passed as input to worldFuture method and then used in
        // returned completableFuture.
        // Here latency will be added for both
        final CompletableFuture<String> helloWorldFuture = helloFuture.thenCompose(hws::worldFuture)
                .thenApply(String::toUpperCase);

        final String finalResult = helloWorldFuture.join();
        timeTaken();
        return finalResult;
    }


    public static void main(String[] args) {

        HelloWorldService hws = new HelloWorldService();

        CompletableFutureHelloWorld cfhw = new CompletableFutureHelloWorld(hws);

        CompletableFuture<String> future = cfhw.helloWorld();
        log("job submitted successfully !!!");


        // .join() to make the main thread wait for the response of future, otherwise main thread just continue
        // its execution and finish its task and close, while the fork-join pool thread might still be executing its task

        // .thenApply() is an intermediate function which calls when the result is available or the fork-join thread
        // done with its execution we generally use .thenApply() to transform the data.

        // .thenAccept() is final method called once the result is available and .thenApply finished its execution (if exist)
        future
                .thenApply(String::toUpperCase)
                .thenAccept((result) -> log("Returned response of future is: " + result)).join();

        log("execution finished !!!");
    }
}
