package com.jobprep.resume_feedback.util;

import java.util.concurrent.ArrayBlockingQueue;

public class ByteArrayPool {
    private final ArrayBlockingQueue<byte[]> pool;

    // 객체 풀 초기화 - 풀의 크기와 각 byte[]의 크기를 설정
    public ByteArrayPool(int poolSize, int arraySize) {
        pool = new ArrayBlockingQueue<>(poolSize);
        for (int i = 0; i < poolSize; i++) {
            pool.offer(new byte[arraySize]); // 미리 byte[] 객체를 생성하여 풀에 넣음
        }
    }

    // 객체 풀에서 byte[] 객체를 가져오는 메서드
    public byte[] getByteArray() throws InterruptedException {
        return pool.take(); // 풀에 객체가 없으면 대기
    }

    // 사용한 byte[] 객체를 풀에 반환하는 메서드
    public void returnByteArray(byte[] array) {
        pool.offer(array); // 객체를 풀에 반환
    }
}