package com.exchange.book.service;

import java.io.IOException;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import javax.websocket.ContainerProvider;
import javax.websocket.WebSocketContainer;

import com.exchange.book.dto.BookDto;
import com.exchange.book.dto.DiffDto;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.WebSocketMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.client.WebSocketClient;
import org.springframework.web.socket.client.standard.StandardWebSocketClient;
import org.springframework.web.socket.handler.AbstractWebSocketHandler;

@Slf4j
@Service
@RequiredArgsConstructor
public class BookServiceImpl implements BookService {

    public static final int MAX_TEXT_MESSAGE_BUFFER_SIZE = 10 * 8 * 1024;

    public static final int DEFAULT_LIST_SIZE = 10;

    private final SimpMessagingTemplate template;

    private final ObjectMapper objectMapper;

    private WebSocketSession session;

    private Integer listSize;

    private final String WSS_URI = "wss://stream.binance.com:9443/ws/btcusdt@depth";
    // https://www.coinbase.com/
    // https://www.binance.com/en/trade/BTC_USDT
    // https://github.com/binance-exchange/binance-official-api-docs/blob/master/web-socket-streams.md#diff-depth-stream

    private final Map<Double, Double> offerBook = new TreeMap<>();

    private final Map<Double, Double> bidBook = new TreeMap<>(Comparator.reverseOrder());

    @Override
    public synchronized void initiatePooling(Integer listSize) {
        this.listSize = (listSize == null || listSize == 0) ? DEFAULT_LIST_SIZE : listSize;

        WebSocketContainer container = ContainerProvider.getWebSocketContainer();
        container.setDefaultMaxTextMessageBufferSize(MAX_TEXT_MESSAGE_BUFFER_SIZE);

        WebSocketClient wsClient = new StandardWebSocketClient(container);

        WebSocketHandler webSocketHandler = new AbstractWebSocketHandler() {

            @Override
            public void afterConnectionClosed(final WebSocketSession session, final CloseStatus status) throws Exception {
                log.info("Session closed: " + status);
                super.afterConnectionClosed(session, status);
            }

            @Override
            public void afterConnectionEstablished(final WebSocketSession session) throws Exception {
                log.info("Connected");
                super.afterConnectionEstablished(session);
                BookServiceImpl.this.session = session;
            }

            @Override
            public void handleMessage(final WebSocketSession session, final WebSocketMessage<?> message) throws Exception {
                log.info("Got a message. Size: {}", message.getPayloadLength());
                try {
                    DiffDto diffDto = objectMapper.readValue(message.getPayload().toString(), DiffDto.class);
                    if (diffDto != null && !CollectionUtils.isEmpty(diffDto.getA())) {
                        // Offers
                        diffDto.getA()
                                .stream()
                                .filter(strings -> !CollectionUtils.isEmpty(strings))
                                .forEach(strings -> {
                                    updateDataMap(offerBook, strings);
                                });
                    }
                    if (diffDto != null && !CollectionUtils.isEmpty(diffDto.getB())) {
                        // Bids
                        diffDto.getB()
                                .stream()
                                .filter(strings -> !CollectionUtils.isEmpty(strings))
                                .forEach(strings -> {
                                    updateDataMap(bidBook, strings);
                                });
                    }

                    sendUpdate(); // Send update to the WS topic
                } catch (JsonMappingException e) {
                    log.error("TODO...");
                }
                super.handleMessage(session, message);
            }
        };

        wsClient.doHandshake(webSocketHandler, WSS_URI);
    }

    @Override
    public synchronized void stopPooling() {
        log.info("Stop pooling requested");
        try {
            if (session != null && session.isOpen()) {
                session.close();
            }
        } catch (IOException e) {
            // FIXME...
        }
    }

    @Override
    public Map<Double, Double> getBids() {
        return bidBook;
    }

    @Override
    public Map<Double, Double> getOffers() {
        return offerBook;
    }

    @Override
    public List<Map<Double, Double>> getAll() {
        return List.of(getSubset(offerBook, listSize), getSubset(bidBook, listSize));
    }

    private void updateDataMap(Map<Double, Double> map, List<String> strings) {
        double price = Double.parseDouble(strings.get(0));
        double quantity = Double.parseDouble(strings.get(1));
        if (quantity == 0D) {
            map.remove(price);
        } else {
            map.put(price, quantity);
        }
    }

    private Map<Double, Double> getSubset(Map<Double, Double> sourceMap, int maxSize) {
        if (sourceMap.isEmpty()) {
            return Collections.emptyMap();
        }
        Iterator<Map.Entry<Double, Double>> iterator = sourceMap.entrySet().iterator();

        Map<Double, Double> result = new TreeMap<>(Comparator.reverseOrder());
        int i = 0;
        while (iterator.hasNext() && i++ <= maxSize) {
            Map.Entry<Double, Double> next = iterator.next();
            result.put(next.getKey(), next.getValue());
        }
        return result;
    }

    private void sendUpdate() {
        template.convertAndSend("/topic/bookUpdate",
                                new BookDto(getSubset(offerBook, listSize), getSubset(bidBook, listSize)));
    }
}
