package com.training.microservices.currencyconversionservice.controller;

import java.math.BigDecimal;
import java.util.HashMap;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestTemplate;

import com.training.microservices.currencyconversionservice.bean.CurrencyConversion;
import com.training.microservices.currencyconversionservice.proxy.CurrencyExchangeProxy;

import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;

@RestController
public class CurrencyConversionController {

	@Autowired
	private CurrencyExchangeProxy proxy;

	@GetMapping("/currency-conversion/from/{from}/to/{to}/quantity/{quantity}")
	public CurrencyConversion calculateCurrencyConversion(
			@PathVariable String from,
			@PathVariable String to,
			@PathVariable BigDecimal quantity
			) {
				
		CurrencyConversion currencyConversion = new CurrencyConversion();
		HashMap<String, String> uriVariables = new HashMap<>();
		uriVariables.put("from",from);
		uriVariables.put("to",to);
		ResponseEntity<CurrencyConversion> responseEntity = new RestTemplate().getForEntity
				("http://localhost:8000/currency-exchange/from/{from}/to/{to}", 
						CurrencyConversion.class, uriVariables);
						currencyConversion=responseEntity.getBody();
						return new CurrencyConversion(currencyConversion.getId(), 
								from, to, quantity, 
								currencyConversion.getConversionMultiple(), 
								quantity.multiply(currencyConversion.getConversionMultiple()), 
								currencyConversion.getEnvironment()+ " " + "rest template");
		
	}
	
	@GetMapping("/currency-conversion-feign/from/{from}/to/{to}/quantity/{quantity}")
	@CircuitBreaker(name = "default", fallbackMethod = "hardcodedResponse")
	public CurrencyConversion calculateCurrencyConversionFeign(
			@PathVariable String from,
			@PathVariable String to,
			@PathVariable BigDecimal quantity
			) {
				
		CurrencyConversion currencyConversion = proxy.retrieveExchangeValue(from, to);
		
		return new CurrencyConversion(currencyConversion.getId(), 
				from, to, quantity, 
				currencyConversion.getConversionMultiple(), 
				quantity.multiply(currencyConversion.getConversionMultiple()), 
				currencyConversion.getEnvironment() + " " + "feign");
		
	}
	
	public CurrencyConversion hardcodedResponse(Exception ex) {
		return new CurrencyConversion((long) 0, "from", "to", new BigDecimal(20), new BigDecimal(2), new BigDecimal(10), "not found");
	}

}