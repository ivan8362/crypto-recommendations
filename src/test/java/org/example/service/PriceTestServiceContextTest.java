package org.example.service;

import org.example.util.Currency;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Map;

@SpringBootTest
@ExtendWith(MockitoExtension.class)
@ActiveProfiles(profiles = "test")
class PriceTestServiceContextTest {

    @Autowired
    private PriceService priceService;

    @Test
    void readFile() {
        LocalDateTime time1 = Instant.ofEpochMilli(1641009600000L)
            .atZone(ZoneId.systemDefault())
            .toLocalDateTime();
        LocalDateTime time2 = Instant.ofEpochMilli(1641020400000L)
            .atZone(ZoneId.systemDefault())
            .toLocalDateTime();

        Map<LocalDateTime, Double> resultMap = priceService.readFile(Currency.BTC.name());

        Assertions.assertEquals(2, resultMap.size());
        Assertions.assertEquals(46813.21, resultMap.get(time1));
        Assertions.assertEquals(46979.61, resultMap.get(time2));
    }

    @Test
    void readFile_noPrices() {
        Map<LocalDateTime, Double> resultMap = priceService.readFile(Currency.ETH.name());

        Assertions.assertEquals(0, resultMap.size());
    }
}
