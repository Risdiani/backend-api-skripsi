package com.skripsi.backend_api.utils;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

@Converter
public class KorelasiConverter implements AttributeConverter<Korelasi, String> {
    @Override
    public String convertToDatabaseColumn(Korelasi attribute) {
        return attribute == null ? null : attribute.getDbValue();
    }

    @Override
    public Korelasi convertToEntityAttribute(String dbData) {
        return Korelasi.fromDbValue(dbData);
    }
}
