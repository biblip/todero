package com.social100.todero.common.observer;

import java.util.ArrayList;
import java.util.List;

public class PublisherManager implements Subject {
    private List<Observer> observers = new ArrayList<>();

    @Override
    public void addObserver(Observer observer) {
        observers.add(observer);
    }

    @Override
    public void removeObserver(Observer observer) {
        observers.remove(observer);
    }

    @Override
    public void notifyObservers(String message) {
        for (Observer observer : observers) {
            observer.update(message);
        }
    }

    public void publish(String message) {
        System.out.println("Publishing message: " + message);
        notifyObservers(message);
    }
}
