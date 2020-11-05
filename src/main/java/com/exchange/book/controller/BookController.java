package com.exchange.book.controller;

import java.util.List;
import java.util.Map;

import com.exchange.book.service.BookService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/book")
@RequiredArgsConstructor
public class BookController {

    private final BookService bookService;

    @GetMapping("/pool")
    public void startPooling() {
        bookService.initiatePooling(10);
    }

    @GetMapping("/stop")
    public void stopPooling() {
        bookService.stopPooling();
    }

    @GetMapping("/bids")
    public Map<Double, Double> getBids() {
        return bookService.getBids();
    }

    @GetMapping("/offers")
    public Map<Double, Double> getOffers() {
        return bookService.getOffers();
    }

    @GetMapping("/all")
    public List<Map<Double, Double>> getAll() {
        return bookService.getAll();
    }
}
