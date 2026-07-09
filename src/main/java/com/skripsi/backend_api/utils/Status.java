package com.skripsi.backend_api.utils;

public enum Status {
    PENDING("pending"),
    RUNNING("running"),
    DONE("done"),
    FAILED("failed"),
    PROCESSING("processing"),
    SUCCESS("success"),
    PARTIAL("partial"),
    KELUAR("keluar"),
    MASUK("masuk"),
    REFUND("refund");

    private final String dbValue;

    Status(String dbValue) {
        this.dbValue = dbValue;
    }

    public String getDbValue() {
        return dbValue;
    }

    public static Status fromDbValue(String value) {
        if (value == null) {
            return null;
        }
        for (Status status : values()) {
            if (status.dbValue.equalsIgnoreCase(value)) {
                return status;
            }
        }
        throw new IllegalArgumentException("Unknown status: " + value);
    }
}
