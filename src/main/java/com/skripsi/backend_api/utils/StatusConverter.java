package com.skripsi.backend_api.utils;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

@Converter
public class StatusConverter implements AttributeConverter<Status, String> {
    @Override
    public String convertToDatabaseColumn(Status attribute) {
        return attribute == null ? null : attribute.getDbValue();
    }

    @Override
    public Status convertToEntityAttribute(String dbData) {
        return Status.fromDbValue(dbData);
    }
}
