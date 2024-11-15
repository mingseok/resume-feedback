package com.jobprep.resume_feedback.util;

import java.util.concurrent.ArrayBlockingQueue;

public class ByteArrayPool {
    private final ArrayBlockingQueue<byte[]> pool;

    public ByteArrayPool(int poolSize, int arraySize) {
        pool = new ArrayBlockingQueue<>(poolSize);
        for (int i = 0; i < poolSize; i++) {
            pool.offer(new byte[arraySize]);
        }
    }

    public byte[] borrowArray() throws InterruptedException {
        return pool.take();
    }

    public void returnArray(byte[] array) {
        pool.offer(array);
    }
}