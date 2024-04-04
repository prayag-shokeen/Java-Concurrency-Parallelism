package com.learnjava.completableFuture;

import com.learnjava.service.HelloWorldService;
import org.junit.jupiter.api.Test;

import java.util.concurrent.CompletableFuture;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class CompletableFutureHelloWorldTest {
    private final HelloWorldService hws = new HelloWorldService();
    private final CompletableFutureHelloWorld completableFutureHelloWorld = new CompletableFutureHelloWorld(hws);

    @Test
    public void testHelloWorld() {
        CompletableFuture<String> completableFuture = completableFutureHelloWorld.helloWorld();

        completableFuture.thenAccept((response) -> {
            assertEquals("HELLO WORLD", response);
        }).join();
    }

    @Test
    public void testHelloWorldMultipleAsyncCallsParallel() {
        // can see the time taken log to verify that both calls were parallel
        assertEquals("HELLO WORLD!", completableFutureHelloWorld.helloWorldMultipleAsyncCallsParallel());
    }
}
