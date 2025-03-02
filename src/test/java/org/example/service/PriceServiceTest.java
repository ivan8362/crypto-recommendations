package org.example.service;

import org.example.util.Currency;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.HashMap;
import java.util.Map;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PriceServiceTest {
    private static final LocalTime time = LocalTime.of(12, 0);
    private static final LocalDate date = LocalDate.of(2020, 1, 1);
    @Spy
    private PriceService priceService = new PriceService();

    @Test
    void getPrices() {
        Map<LocalDateTime, Double> map = new HashMap<>();
        map.put(LocalDateTime.of(date, time), 2.0);
        map.put(LocalDateTime.of(date.plusDays(1), time), 4.0);
        map.put(LocalDateTime.of(date.plusDays(2), time), 1.0);
        map.put(LocalDateTime.of(date.plusDays(10), time), 3.0);
        when(priceService.readFile(anyString())).thenReturn(map);

        Map<String, Double> result = priceService.getPrices(Currency.BTC.name());

        Assertions.assertEquals(4.0, result.get("max"));
        Assertions.assertEquals(1.0, result.get("min"));
        Assertions.assertEquals(2.0, result.get("oldest"));
        Assertions.assertEquals(3.0, result.get("newest"));
    }

    @Test
    void getNormalizedForDay() {
        Map<LocalDateTime, Double> mapBtc = new HashMap<>();
        mapBtc.put(LocalDateTime.of(date, time), 2.0);
        mapBtc.put(LocalDateTime.of(date, time.plusHours(3)), 5.0);
        when(priceService.readFile(Currency.BTC.name())).thenReturn(mapBtc);

        Map<LocalDateTime, Double> mapEth = new HashMap<>();
        mapEth.put(LocalDateTime.of(date, time), 2.0);
        mapEth.put(LocalDateTime.of(date, time.plusHours(3)), 4.0);
        when(priceService.readFile(Currency.ETH.name())).thenReturn(mapEth);

        Map<LocalDateTime, Double> mapLt = new HashMap<>();
        mapLt.put(LocalDateTime.of(date, time), 2.0);
        mapLt.put(LocalDateTime.of(date, time.plusHours(3)), 4.0);
        when(priceService.readFile(Currency.LTC.name())).thenReturn(mapLt);

        Map<LocalDateTime, Double> mapDo = new HashMap<>();
        mapDo.put(LocalDateTime.of(date, time), 2.0);
        mapDo.put(LocalDateTime.of(date, time.plusHours(3)), 4.0);
        when(priceService.readFile(Currency.DOGE.name())).thenReturn(mapDo);

        Map<LocalDateTime, Double> mapXrp = new HashMap<>();
        mapXrp.put(LocalDateTime.of(date, time), 2.0);
        mapXrp.put(LocalDateTime.of(date, time.plusHours(3)), 4.0);
        when(priceService.readFile(Currency.XRP.name())).thenReturn(mapXrp);

        String result = priceService.getNormalizedForDay(date);

        Assertions.assertEquals("BTC=1.5", result);
    }

    @Test
    void getNormalizedForDay_maxZero() {
        Map<LocalDateTime, Double> mapBtc = new HashMap<>();
        mapBtc.put(LocalDateTime.of(date, time), 0.0);
        when(priceService.readFile(Currency.BTC.name())).thenReturn(mapBtc);

        Map<LocalDateTime, Double> mapEth = new HashMap<>();
        mapEth.put(LocalDateTime.of(date, time), 0.0);
        when(priceService.readFile(Currency.ETH.name())).thenReturn(mapEth);

        Map<LocalDateTime, Double> mapLt = new HashMap<>();
        mapLt.put(LocalDateTime.of(date, time), 0.0);
        when(priceService.readFile(Currency.LTC.name())).thenReturn(mapLt);

        Map<LocalDateTime, Double> mapDo = new HashMap<>();
        mapDo.put(LocalDateTime.of(date, time), 0.0);
        when(priceService.readFile(Currency.DOGE.name())).thenReturn(mapDo);

        Map<LocalDateTime, Double> mapXrp = new HashMap<>();
        mapXrp.put(LocalDateTime.of(date, time), 0.0);
        when(priceService.readFile(Currency.XRP.name())).thenReturn(mapXrp);

        String result = priceService.getNormalizedForDay(date);

        Assertions.assertNull(result);
    }
}
