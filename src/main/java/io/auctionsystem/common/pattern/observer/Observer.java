package io.auctionsystem.common.pattern.observer;

public interface Observer<T> {
    void update(T data);
}