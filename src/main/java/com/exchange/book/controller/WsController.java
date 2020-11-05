package com.exchange.book.controller;

import com.exchange.book.dto.PoolMessage;
import com.exchange.book.service.BookService;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.stereotype.Controller;

@Controller
@RequiredArgsConstructor
public class WsController {

    private final BookService bookService;

    @MessageMapping("/startPooling")
//    @SendTo("/topic/bookUpdate")
    public void startPooling(PoolMessage message) {
        bookService.initiatePooling(message.getListSize());
    }

    @MessageMapping("/stopPooling")
    public void stopPooling() throws Exception {
        bookService.stopPooling();
    }
}
