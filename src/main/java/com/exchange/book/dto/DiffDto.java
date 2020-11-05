package com.exchange.book.dto;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class DiffDto {

    private List<List<String>> a;

    private List<List<String>> b;
}
