package org.example.controller;

import org.example.service.PriceService;
import org.example.util.Currency;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("v1")
public class PriceController {

    @Autowired
    private PriceService priceService;

    @GetMapping(value ="/{currency}")
    String getPrices(@PathVariable(name = "currency") String currency) {
        return priceService.getPrices(currency);
    }

    @GetMapping("/hello")
    String hello () {
        return "hello!\n";
    }
}
