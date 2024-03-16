package com.learnjava.forkjoin;

import com.learnjava.util.DataSet;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.ForkJoinTask;
import java.util.concurrent.RecursiveTask;

import static com.learnjava.util.CommonUtil.delay;
import static com.learnjava.util.CommonUtil.stopWatch;
import static com.learnjava.util.LoggerUtil.log;

public class ForkJoinUsingRecursion extends RecursiveTask<List<String>> {
    List<String> inputList;

    public ForkJoinUsingRecursion(final List<String> inputList) {
        this.inputList = inputList;
    }

    @Override
    protected List<String> compute() {

        // base case
        if(inputList.size() <= 1) {
            List<String> resultList = new ArrayList<>();
            inputList.forEach(name -> resultList.add(addNameLengthTransform(name)));
            return resultList;
        }

        int midPoint = inputList.size() / 2;
        // dividing the task, and create a new task so that it can be picked by some other fork
        // to achieve parallelism
        ForkJoinTask<List<String>> leftHalfResult = new ForkJoinUsingRecursion(inputList.subList(0, midPoint)).fork();

        // update input to right half, as left half will taken care by some other fork
        inputList = inputList.subList(midPoint, inputList.size());

        // call recursion
        List<String> rightHalfResultList = compute();

        // join to get the result list of left Half
        List<String> leftHalfResultList = leftHalfResult.join();
        leftHalfResultList.addAll(rightHalfResultList);
        return leftHalfResultList;
    }

    public static void main(String[] args) {

        stopWatch.start();
        final List<String> names = DataSet.namesList();
        // to create forkJoin pool, with parallelism equal to Runtime.availableProcessors
        final ForkJoinPool forkJoinPool = new ForkJoinPool();
        // created the task for forkJoinPool
        final ForkJoinUsingRecursion forkJoinUsingRecursion = new ForkJoinUsingRecursion(names);
        // below statement adds the task to WorkerQueue
        final List<String> resultList = forkJoinPool.invoke(forkJoinUsingRecursion);
        stopWatch.stop();
        log("Final Result : "+ resultList);
        log("Total Time Taken : "+ stopWatch.getTime());
    }

    private static String addNameLengthTransform(String name) {
        delay(500);
        return name.length()+" - "+name ;
    }
}
