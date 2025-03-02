package org.example.controller;

import org.example.service.PriceService;
import org.example.util.Currency;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;

@RestController
@RequestMapping("v1")
public class PriceController {

    private static final Logger LOGGER = LoggerFactory.getLogger(PriceController.class);
    private final PriceService priceService;

    public PriceController(PriceService priceService) {
        this.priceService = priceService;
    }

    @GetMapping(value = "/{currency}")
    public ResponseEntity<?> getPrices(@PathVariable(name = "currency") String currency) {
        LOGGER.info("called API /{currency} with an argument {}", currency);
        Currency cryptoCurrency = getCurrency(currency);
        if (cryptoCurrency == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid currency: " + currency);
        }
        try {
            return ResponseEntity.ok(priceService.getPrices(currency));
        } catch (Exception e) {
            LOGGER.error("Error fetching prices for currency {}", currency, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Error fetching prices");
        }
    }

    @GetMapping("/prices")
    public ResponseEntity<?> getAllPrices() {
        LOGGER.info("called API /prices");
        try {
            return ResponseEntity.ok(priceService.getPrices());
        } catch (Exception e) {
            LOGGER.error("Error fetching all prices", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Error fetching all prices");
        }
    }

    @GetMapping("/normalized")
    public ResponseEntity<?> getNormalizedForAll() {
        LOGGER.info("called API /normalized");
        try {
            return ResponseEntity.ok(priceService.getNormalized());
        } catch (Exception e) {
            LOGGER.error("Error fetching normalized data for all currencies", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Error fetching normalized data");
        }
    }

    /**
     * the crypto with the highest normalized range for a
     * specific day.
     * format: YYYY-MM-DD
     */
    @GetMapping("/normalized/{day}")
    public ResponseEntity<?> getNormalizedForDay(@PathVariable(name = "day") LocalDate day) {
        LOGGER.info("called API /normalized/{day} with an argument {}", day);
        try {
            String normalizedCurrency = priceService.getNormalizedForDay(day);
            if (normalizedCurrency == null) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body("No data available for the given day");
            }
            return ResponseEntity.ok(normalizedCurrency);
        } catch (Exception e) {
            LOGGER.error("Error fetching normalized data for day {}", day, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Error fetching normalized data for day " + day);
        }
    }

    /**
     * Utility method to validate and retrieve the Currency enum.
     */
    private Currency getCurrency(String currency) {
        try {
            return Currency.valueOf(currency.toUpperCase());
        } catch (IllegalArgumentException e) {
            LOGGER.warn("Invalid currency: {}", currency);
            return null;
        }
    }
}
