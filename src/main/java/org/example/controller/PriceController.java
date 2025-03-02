package org.example.controller;

import org.example.service.PriceService;
import org.example.util.Currency;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
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
    public String getPrices(@PathVariable(name = "currency") String currency) {
        LOGGER.info("called API /{currency} with an argument {}", currency);
        try {
            Currency.valueOf(currency.toUpperCase()); // Convert to uppercase to match enum values
        } catch (IllegalArgumentException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid currency: " + currency);
        }

        return priceService.getPrices(currency) + "\n";
    }

    @GetMapping("/prices")
    public String getStat() {
        LOGGER.info("called API /prices");
        return priceService.getPrices().toString() + "\n";
    }

    @GetMapping("/normalized")
    public String getNormalizedForAll() {
        LOGGER.info("called API /normalized");
        return priceService.getNormalized().toString() + "\n";
    }

    /**
     * the crypto with the highest normalized range for a
     * specific day.
     * format: YYYY-MM-DD
     */
    @GetMapping("/normalized/{day}")
    public String getNormalizedForDay(@PathVariable(name = "day") LocalDate day) {
        LOGGER.info("called API /normalized/{day} with an argument {}", day);
        return priceService.getNormalizedForDay(day) + "\n";
    }
}
