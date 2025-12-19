package com.titan.ledger.adapter.in.web.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import zipkin2.reporter.BytesMessageSender;
import zipkin2.reporter.urlconnection.URLConnectionSender;

@Configuration
public class ZipkinConfig {
    

    @Bean
    public BytesMessageSender sender(){
        return URLConnectionSender.create("http://localhost:9411/api/v2/spans");
    }
}
