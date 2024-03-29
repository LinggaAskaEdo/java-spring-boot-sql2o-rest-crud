package com.otis.controller;

import java.util.ArrayList;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.otis.model.Product;
import com.otis.model.Response;
import com.otis.service.ProductService;

@CrossOrigin(origins = "*")
@RestController
@RequestMapping("/api")
public class ProductController {
    private final ProductService service;

    @Autowired
    public ProductController(ProductService service) {
        this.service = service;
    }

    @GetMapping("/products")
    public ResponseEntity<List<Product>> getAllProducts(
            @RequestParam(required = false) String name) {
        List<Product> products = new ArrayList<>();

        if (name == null)
            products.addAll(service.findAll());
        else
            products.addAll(service.findByNameContaining(name));

        if (products.isEmpty()) {
            return new ResponseEntity<>(HttpStatus.NO_CONTENT);
        }

        return new ResponseEntity<>(products, HttpStatus.OK);
    }

    @GetMapping("/products/company")
    public ResponseEntity<List<Product>> getProductByCompanyName(@RequestParam String companyName) {
        List<Product> products = new ArrayList<>(service.findByCompanyName(companyName));

        if (products.isEmpty()) {
            return new ResponseEntity<>(HttpStatus.NO_CONTENT);
        }

        return new ResponseEntity<>(products, HttpStatus.OK);
    }

    @GetMapping("/products/report")
    public ResponseEntity<Response> getReportData() {

        Response response = service.getReportData();

        if (response.getProducts().isEmpty()) {
            return new ResponseEntity<>(HttpStatus.NO_CONTENT);
        }

        return new ResponseEntity<>(response, HttpStatus.OK);
    }
}
