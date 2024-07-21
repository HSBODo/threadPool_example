package com.example.thread;

import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.config.Task;
import org.springframework.util.StopWatch;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.*;

import static org.junit.jupiter.api.Assertions.*;

class threadTest {


    private static final Logger log = LoggerFactory.getLogger(threadTest.class);

    @Test
    void 쓰레드풀_예제_1() throws InterruptedException {
        // 테스트할 쓰레드 풀 크기 배열
        int[] poolSizes = {1, 2, 4, 8, 16, 32};

        // 각 쓰레드 풀 크기에 대해 테스트 실행
        for (int poolSize : poolSizes) {
            // 쓰레드 풀 생성
            ExecutorService executorService = Executors.newFixedThreadPool(poolSize);

            // 작업 수
            int taskCount = 100;

            // 작업 실행 시간 측정
            long startTime = System.currentTimeMillis();

            // 작업 제출
            for (int i = 0; i < taskCount; i++) {
                executorService.submit(new Task());
            }

            // 작업 완료 후 종료
            executorService.shutdown();
            executorService.awaitTermination(1, TimeUnit.HOURS);

            long endTime = System.currentTimeMillis();
            log.info("Pool size:{} totalTime: {} ms",poolSize,(endTime - startTime));
        }
    }

    static class Task implements Runnable {
        @Override
        public void run() {
            // 예시 작업: 100ms 동안 대기
            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
    }

    @Test
    void AbortPolicy() throws InterruptedException {
        Runnable task = () -> {
            try {
                String start = Thread.currentThread().getName();

                Thread.sleep(2000); // Simulate work

                log.info("Task started {} Task finished{}",start,Thread.currentThread().getName());
            } catch (InterruptedException e) {
                System.out.println("Task interrupted: " + Thread.currentThread().getName());
                Thread.currentThread().interrupt(); // Restore interrupted status
            }
        };



        // Test different policies
        testPolicy("AbortPolicy", new ThreadPoolExecutor(
                2, // core pool size
                2, // maximum pool size
                60L, TimeUnit.SECONDS, // keep-alive time
                new LinkedBlockingQueue<>(2), // work queue
                new ThreadPoolExecutor.AbortPolicy()
        ), task);

        //
//        testPolicy("CallerRunsPolicy", new ThreadPoolExecutor(
//                2, // core pool size
//                2, // maximum pool size
//                60L, TimeUnit.SECONDS, // keep-alive time
//                new LinkedBlockingQueue<>(2), // work queue
//                new ThreadPoolExecutor.CallerRunsPolicy()
//        ), task);
////

//

    }

    private static void testPolicy(String policyName, ThreadPoolExecutor executor, Runnable task) {
        log.info("Testing policy {}", policyName);
        // Submit tasks
        for (int i = 0; i < 8; i++) {
            final int taskId = i;
            executor.submit(() -> {
                System.out.println("Submitting task: " + taskId);
                task.run();
            });
        }

        // Shutdown the executor
        executor.shutdown();
        try {
            executor.awaitTermination(1, TimeUnit.MINUTES);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    // 공유 자원
    private static int memberCounter = 0;
    // 카운터를 증가시키는 메서드
    private static void incrementCounterMember() {
        memberCounter++;
    }
    // 카운터를 증가시키는 메서드
    private static synchronized void incrementCounterMemberSynchronized () {
        memberCounter++;
    }

    @Test
    void synchronizedTest() {
        // 스레드 풀 생성
        ExecutorService executorService = Executors.newFixedThreadPool(10);
        StopWatch stopWatch = new StopWatch();
        stopWatch.start();
        //100개의 잡을 실행
        for (int i = 0; i < 1000; i++) {
            executorService.submit(() -> {
                // 공유 자원에 접근하는 작업
                incrementCounter();
            });
        }

        // 스레드 풀 종료
        executorService.shutdown();

        // 모든 작업이 완료된 후 결과 출력
        try {
            // 모든 스레드가 완료될 때까지 대기
            if (executorService.awaitTermination(1, java.util.concurrent.TimeUnit.MINUTES)) {
                stopWatch.stop();
                double totalTime = stopWatch.getTotalTime(TimeUnit.MILLISECONDS);
                log.info("value {} totalTime: {} ms",counter,totalTime);
            }
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

    }
    private static int counter = 0; // 공유 자원
    private static synchronized void incrementCounter() {
        // 공유 자원에 대한 작업
        int temp = counter;
        temp++;
        // 임의로 짧은 지연 추가 (경쟁 조건을 더 잘 드러내기 위해)
        try {
            Thread.sleep(1);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        counter = temp;
    }


    private static final Object lock1 = new Object();
    private static final Object lock2 = new Object();
    @Test
    void 스레드_교착상태_이슈() {
        // 스레드 1: lock1 -> lock2
        Runnable task1 = () -> {
            synchronized (lock1) {
                System.out.println("Thread 1: rock1 얻음");

                try {
                    Thread.sleep(100);
                } catch (InterruptedException e) {

                }

                System.out.println("Thread 1: rock2를 얻기 위해 대기중");
                synchronized (lock2) {
                    System.out.println("Thread 1: rock2 얻음");
                }
            }
        };

        // 스레드 2: lock2 -> lock1
        Runnable task2 = () -> {
            synchronized (lock2) {
                System.out.println("Thread 2: rock2 얻음");

                try {
                    Thread.sleep(100);
                } catch (InterruptedException e) {

                }

                System.out.println("Thread 2: rock1를 얻기 위해 대기중");
                synchronized (lock1) {
                    System.out.println("Thread 2: rock1 얻음");
                }
            }
        };

        // 스레드 실행
        Thread thread1 = new Thread(task1);
        Thread thread2 = new Thread(task2);

        thread1.start();
        thread2.start();

        try {
            thread1.join();
            thread2.join();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }


}