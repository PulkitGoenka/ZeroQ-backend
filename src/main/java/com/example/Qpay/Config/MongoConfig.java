package com.example.Qpay.Config;

import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.convert.converter.Converter;
import org.springframework.data.mongodb.config.AbstractMongoClientConfiguration;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.convert.DefaultMongoTypeMapper;
import org.springframework.data.mongodb.core.convert.MappingMongoConverter;
import org.springframework.data.mongodb.core.convert.MongoCustomConversions;
import org.springframework.data.mongodb.repository.config.EnableMongoRepositories;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

@Configuration
@EnableMongoRepositories(basePackages = "com.example.Qpay.Repository.mongo")
public class MongoConfig extends AbstractMongoClientConfiguration {

    @Value("${spring.data.mongodb.uri}")
    private String mongoUri;

    @Value("${spring.data.mongodb.database}")
    private String mongoDatabase;

    @Override
    protected String getDatabaseName() {
        return mongoDatabase;
    }

    @Override
    public MongoClient mongoClient() {
        return MongoClients.create(mongoUri);
    }

    /**
     * Date <-> OffsetDateTime conversion — Mongo Date se OffsetDateTime
     * fields (createdAt, updatedAt, PriceHistory.from/to) map karne ke liye.
     */
    @Override
    protected void configureConverters(MongoCustomConversions.MongoConverterConfigurationAdapter adapter) {
        adapter.registerConverter(DateToOffsetDateTimeConverter.INSTANCE);
        adapter.registerConverter(OffsetDateTimeToDateConverter.INSTANCE);
    }

    enum DateToOffsetDateTimeConverter implements Converter<Date, OffsetDateTime> {
        INSTANCE;
        @Override
        public OffsetDateTime convert(Date source) {
            return source == null ? null : source.toInstant().atOffset(ZoneOffset.UTC);
        }
    }

    enum OffsetDateTimeToDateConverter implements Converter<OffsetDateTime, Date> {
        INSTANCE;
        @Override
        public Date convert(OffsetDateTime source) {
            return source == null ? null : Date.from(source.toInstant());
        }
    }

    /**
     * _class field MongoDB mein save nahi hoga — clean documents.
     */
    @Bean
    public MongoTemplate mongoTemplate() throws Exception {
        MongoTemplate template = new MongoTemplate(mongoClient(), getDatabaseName());
        MappingMongoConverter converter = (MappingMongoConverter) template.getConverter();
        converter.setTypeMapper(new DefaultMongoTypeMapper(null));
        return template;
    }
}