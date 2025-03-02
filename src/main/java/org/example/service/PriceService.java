package org.example.service;

import org.example.exception.PriceFileNotFoundException;
import org.example.util.Currency;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.file.FileSystems;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * the service reads the csv files with the names {CRYPTO_MANE}_values.csv
 * and gets statistics about the crypto prices.
 * {CRYPTO_NAME} must be one of the the enum org.example.util.Currency
 */
@Service
public class PriceService {

    private static final Logger LOGGER = LoggerFactory.getLogger(PriceService.class);
    private static final int COLUMN_QUANTITY = 3;
    private static final int TIMESTAMP_INDEX = 0;
    private static final int SYMBOL_INDEX = 1;
    private static final int PRICE_INDEX = 2;
    private static final LocalDateTime EPOCH = LocalDateTime.of(1970, 1, 1, 0, 0);
    private final Map<Currency, Map<LocalDateTime, Double>> cachedPrices = new EnumMap<>(Currency.class);

    @Value("${directory-location}")
    public String directory;

    /**
     * for the given currency reads the prices file and calculates the
     * min, max, oldest, newest prices.
     *
     * @param currency cryptocurrency name
     * @return map: {name of the statistic}: {value in double}
     */
    public Map<String, Double> getPrices(String currency) {
        Currency cryptoName = Currency.valueOf(currency.toUpperCase());
        ensurePricesLoaded(cryptoName);
        Map<String, Double> result = getStat(cachedPrices.get(cryptoName));

        LOGGER.info("returning prices with {} statistics", result.size());
        return result;
    }

    /**
     * for all the currencies reads the prices file and calculates the
     * min, max, oldest, newest prices.
     *
     * @return map: {currency name}: {map: {name of the statistic}:{value in double}}
     */
    public Map<Currency, Map<String, Double>> getPrices() {
        Map<Currency, Map<String, Double>> result = new EnumMap<>(Currency.class);
        for (Currency cryptoName: Currency.values()) {
            ensurePricesLoaded(cryptoName);
            result.put(cryptoName, getStat(cachedPrices.get(cryptoName)));
        }

        LOGGER.info("returning prices for {} currencies", result.size());
        return result;
    }

    /**
     * return a descending sorted list of all the cryptos,
     * comparing the normalized range (i.e. (max-min)/min).
     *
     * @return List of map.Entry {currency}:{normalized}
     */
    public List<Map.Entry<Currency, BigDecimal>> getNormalized() {
        List<Map.Entry<Currency, BigDecimal>> list = new ArrayList<>();
        for (Currency crypto: Currency.values()) {
            ensurePricesLoaded(crypto);
            Map<LocalDateTime, Double> prices = cachedPrices.get(crypto);
            BigDecimal normalized = calculateNormalizedRange(prices).orElse(BigDecimal.ZERO);
            list.add(new AbstractMap.SimpleEntry<>(crypto, normalized));
        }
        list.sort(Map.Entry.<Currency, BigDecimal>comparingByValue().reversed());

        LOGGER.info("returning normalized value for {} currencies", list.size());
        return list;
    }

    private Optional<BigDecimal> calculateNormalizedRange(Map<LocalDateTime, Double> prices) {
        BigDecimal max = BigDecimal.valueOf(getHighestPrice(prices).orElse(0.0));
        BigDecimal min = BigDecimal.valueOf(getLowestPrice(prices).orElse(0.0));

        if (min.compareTo(BigDecimal.ZERO) == 0) {
            LOGGER.error("Cannot calculate normalized range (min price is zero).");
            return Optional.empty();  // Skip if min price is zero
        }
        return Optional.of(max.subtract(min).divide(min, RoundingMode.HALF_UP));
    }

    /**
     * return the crypto with the highest normalized range for a
     * specific day.
     *
     * @param date LocalDate
     * @return currency name
     */
    public String getNormalizedForDay(LocalDate date) {
        Map<Currency, BigDecimal> result = new EnumMap<>(Currency.class);
        for (Currency crypto: Currency.values()) {
            ensurePricesLoaded(crypto);
            Map<LocalDateTime, Double> prices = cachedPrices.get(crypto);
            if (prices.isEmpty()) {
                LOGGER.warn("No price data available for currency: {}", crypto);
                continue;
            }
            double max = getMaxPriceForDay(prices, date)
                .orElse(new AbstractMap.SimpleEntry<>(EPOCH, 0.0))
                .getValue();
            double min = getMinPriceForDay(prices, date)
                .orElse(new AbstractMap.SimpleEntry<>(EPOCH, 0.0))
                .getValue();
            BigDecimal normalized = calculateNormalizedRangeForDay(max, min).orElse(BigDecimal.ZERO);

            result.put(crypto, normalized);
        }
        String currency = result.entrySet().stream()
            .max(Map.Entry.comparingByValue())
            .map(Map.Entry::getKey)
            .map(Currency::name)
            .orElse(null); // Return null if no valid entry
        LOGGER.info("returning the currency with the highest normalized value for a day: {}", currency);
        return currency;
    }

    private Optional<BigDecimal> calculateNormalizedRangeForDay(double max, double min) {
        if (min == 0) {
            return Optional.empty();
        }
        return Optional.of(BigDecimal.valueOf(max - min).divide(BigDecimal.valueOf(min), RoundingMode.HALF_UP));
    }

    private Optional<Map.Entry<LocalDateTime, Double>> getMaxPriceForDay(Map<LocalDateTime, Double> prices, LocalDate date) {
        return prices.entrySet().stream()
            .filter(entry -> entry.getKey().toLocalDate().equals(date))
            .max(Comparator.comparingDouble(Map.Entry::getValue));
    }

    private Optional<Map.Entry<LocalDateTime, Double>> getMinPriceForDay(Map<LocalDateTime, Double> prices, LocalDate date) {
        return prices.entrySet().stream()
            .filter(entry -> entry.getKey().toLocalDate().equals(date))
            .min(Comparator.comparingDouble(Map.Entry::getValue));
    }

    private void ensurePricesLoaded(Currency cryptoName) {
        cachedPrices.computeIfAbsent(cryptoName, k -> readFile(cryptoName.name()));
    }

    /**
     * public modifier for the unit tests.
     *
     * @param currency crypto
     * @return map datetime to price
     */
    public Map<LocalDateTime, Double> readFile(String currency) {
        Map<LocalDateTime, Double> map = new HashMap<>();
        String fileName = directory + FileSystems.getDefault().getSeparator()
            + currency + "_values.csv";
        File file = new File(fileName);
        if (file.exists()) {
            LOGGER.info("Reading file for currency: {}", currency);
        } else {
            throw new PriceFileNotFoundException("File not found: " + fileName);
        }

        try (BufferedReader br = new BufferedReader(new FileReader(fileName))) {
            String line;
            while ((line = br.readLine()) != null) {
                String[] tokens = line.split(",");
                if (isValidPriceData(tokens)) {
                    long timestamp;
                    double price;
                    if (isTimestampValid(tokens[TIMESTAMP_INDEX]) && isPriceValid(tokens[PRICE_INDEX])) {
                        timestamp = Long.parseLong(tokens[TIMESTAMP_INDEX]);
                        price = Double.parseDouble(tokens[PRICE_INDEX]);
                    } else {
                        LOGGER.warn("in the {} price file there is a row with malformed timestamp or price", currency);
                        continue;
                    }
                    LocalDateTime time = convertTimestampToLocalDateTime(timestamp);
                    String symbol = tokens[SYMBOL_INDEX];
                    if (symbol.equals(currency)) {
                        map.put(time, price);
                    }
                } else {
                    LOGGER.warn("the {} price csv file has unknown structure", currency);
                }
            }
        } catch (IOException e) {
            LOGGER.error("error during price file reading {}", fileName, e);
        }

        LOGGER.info("the currency file for {} is successfully read. {} rows", currency, map.size());
        return map;
    }

    private boolean isTimestampValid(String timestamp) {
        try {
            Long.parseLong(timestamp);
            return true;
        } catch (NumberFormatException e) {
            return false;
        }
    }

    private boolean isPriceValid(String price) {
        try {
            Double.parseDouble(price);
            return true;
        } catch (NumberFormatException e) {
            return false;
        }
    }

    private LocalDateTime convertTimestampToLocalDateTime(long timestamp) {
        return Instant.ofEpochMilli(timestamp)
            .atZone(ZoneId.systemDefault())
            .toLocalDateTime();
    }

    private boolean isValidPriceData(String[] tokens) {
        return tokens.length == COLUMN_QUANTITY && !tokens[0].isEmpty()
            && Character.isDigit(tokens[TIMESTAMP_INDEX].charAt(0));
    }

    private Optional<Double> getHighestPrice(Map<LocalDateTime, Double> map) {
        return map.entrySet().stream()
            .max(Comparator.comparingDouble(Map.Entry::getValue))
            .map(Map.Entry::getValue);
    }

    private Optional<Double> getLowestPrice(Map<LocalDateTime, Double> map) {
        return map.entrySet().stream()
            .min(Comparator.comparingDouble(Map.Entry::getValue))
            .map(Map.Entry::getValue);
    }

    private Map<String, Double> getStat(Map<LocalDateTime, Double> map) {
        Map<String, Double> result = new HashMap<>();
        result.put("max", getHighestPrice(map).orElse(null));
        result.put("min", getLowestPrice(map).orElse(null));
        result.put("oldest", getOldestPrice(map).orElse(null));
        result.put("newest", getNewestPrice(map).orElse(null));
        return result;
    }

    private Optional<Double> getOldestPrice(Map<LocalDateTime, Double> map) {
        return map.entrySet().stream().min(Map.Entry.comparingByKey()).map(Map.Entry::getValue);
    }

    private Optional<Double> getNewestPrice(Map<LocalDateTime, Double> map) {
        return map.entrySet().stream().max(Map.Entry.comparingByKey()).map(Map.Entry::getValue);
    }
}
