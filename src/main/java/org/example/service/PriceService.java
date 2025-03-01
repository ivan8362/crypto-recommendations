package org.example.service;

import org.example.util.Currency;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@Service
public class PriceService {

    private static final Logger LOGGER = LoggerFactory.getLogger(PriceService.class);

    @Value("${directory-location}")
    public String directory;

    public String getPrices(String currency) {
        Currency cryptoName = Currency.valueOf(currency.toUpperCase());
        Map<LocalDateTime, Double> map = readFile(cryptoName.name());
        Map.Entry<LocalDateTime, Double> entry = getMaxPrice(map);
        return "Date: " + entry.getKey() + ", Price: " + entry.getValue();
    }

    Map<LocalDateTime, Double> readFile(String currency) {
        ZoneId zoneId = ZoneId.systemDefault();
        Map<LocalDateTime, Double> map = new HashMap<>();
        String fileName = directory + "/" + currency + "_values.csv";
        try (BufferedReader br = new BufferedReader(new FileReader(fileName))) {
            String line;
            while ((line = br.readLine()) != null) {
                String[] tokens = line.split(",");
                if (tokens.length == 3 && !tokens[0].isEmpty() && Character.isDigit(tokens[0].charAt(0))) {
                    long timestamp = Long.parseLong(tokens[0]);
                    LocalDateTime time = Instant.ofEpochMilli(timestamp).atZone(zoneId).toLocalDateTime();
                    String symbol = tokens[1];
                    double price = Double.parseDouble(tokens[2]);
                    if (symbol.equals(currency)) {
                        map.put(time, price);
                    }
                }
            }
        } catch (IOException e) {
            LOGGER.error("error during price file reading {}", fileName, e);
        }
        return map;
    }

    Map.Entry<LocalDateTime, Double> getMaxPrice(Map<LocalDateTime, Double> map) {
        Optional<Map.Entry<LocalDateTime, Double>> max = map.entrySet().stream()
            .max(Comparator.comparingDouble(Map.Entry::getValue));
        return max.orElseThrow();
    }
}
