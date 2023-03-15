package com.otis.service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.google.gson.Gson;
import com.otis.exception.ResourceNotFoundException;
import com.otis.model.Company;
import com.otis.model.Product;
import com.otis.model.Response;
import com.otis.repository.CompanyRepository;
import com.otis.repository.ProductRepository;

@Service
public class ProductService {
    private static final Logger logger = LogManager.getLogger();

    private final ProductRepository productRepository;
    private final CompanyRepository companyRepository;

    @Autowired
    public ProductService(ProductRepository productRepository, CompanyRepository companyRepository) {
        this.productRepository = productRepository;
        this.companyRepository = companyRepository;
    }

    public List<Product> findAll() {
        return productRepository.findAll();
    }

    public List<Product> findByNameContaining(String title) {
        return productRepository.findByNameContaining(title);
    }

    public List<Product> findByCompanyName(String companyName) {
        Company company = companyRepository.getCompanyByName(companyName);

        if (null == company) {
            throw new ResourceNotFoundException("Not found Company with name = " + companyName);
        }

        return productRepository.findProductByCompanyID(company.getId());
    }

    public void getReportData() {
        Map<Product, List<Company>> responseMap = new HashMap<>();

        List<Map<String, Object>> results = productRepository.getReportData();

        if (!results.isEmpty()) {
            logger.info("Data is not empty !!!");
            for (Map<String, Object> map : results) {
                Product product = Product.builder()
                        .id(Long.parseLong(map.get("product_id").toString()))
                        .name(map.get("product_name").toString())
                        .build();

                Company company = Company.builder()
                        .id(Long.parseLong(map.get("company_id").toString()))
                        .name(map.get("company_name").toString())
                        .build();

                if (responseMap.containsKey(product)) {
                    List<Company> currentCompanies = responseMap.get(product);

                    if (!currentCompanies.contains(company)) {
                        currentCompanies.add(company);
                    }

                    responseMap.put(product, currentCompanies);
                } else {
                    List<Company> companies = new ArrayList<>();
                    companies.add(company);
                    responseMap.put(product, companies);
                }
            }

            Response response = new Response();
            List<Product> products = new ArrayList<>();

            for (Map.Entry<Product, List<Company>> data : responseMap.entrySet()) {
                Product productKey = data.getKey();
                List<Company> companyValue = data.getValue();
                productKey.setCompanies(companyValue);
                products.add(productKey);
            }

            response.setProducts(products);

            Gson gson = new Gson();
            String json = gson.toJson(response);
            logger.info("Results: {}", json);
        } else {
            logger.info("Data is empty !!!");
        }
    }
}
