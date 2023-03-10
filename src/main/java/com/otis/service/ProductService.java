package com.otis.service;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.otis.exception.ResourceNotFoundException;
import com.otis.model.Company;
import com.otis.model.Product;
import com.otis.repository.CompanyRepository;
import com.otis.repository.ProductRepository;

@Service
public class ProductService {
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
}
