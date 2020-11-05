package com.exchange.book.service;

import java.util.List;
import java.util.Map;

import com.exchange.book.dto.DiffDto;

public interface BookService {

    void initiatePooling(Integer listSize);

    void stopPooling();

    Map<Double, Double> getBids();

    Map<Double, Double> getOffers();

    List<Map<Double, Double>> getAll();
}
