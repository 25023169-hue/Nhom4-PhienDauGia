package io.auctionsystem.common.pattern.observer;

public interface Subject {
    void registerObserver(Observer<?> o);
    void removeObserver(Observer<?> o);
    void notifyObservers(Object arg);
}