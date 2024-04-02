package com.learnjava.completableFuture;

import com.learnjava.service.HelloWorldService;

import java.util.concurrent.CompletableFuture;

import static com.learnjava.util.LoggerUtil.log;

public class CompletableFutureHelloWorld {

    private final HelloWorldService hws;

    public CompletableFutureHelloWorld(HelloWorldService hws) {
        this.hws = hws;
    }

    // to add unit test created this method to return completable future.
    public CompletableFuture<String> helloWorld() {
        return CompletableFuture
                .supplyAsync(hws::helloWorld)
                .thenApply(String::toUpperCase);
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
