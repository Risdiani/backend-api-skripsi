package com.skripsi.backend_api.dto.product.request;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AddStokReq {
    private String kodeProduk;
    private String tipe; // contoh: "masuk", "pembelian", "restock", atau default "MASUK"
    private Integer qty;
    private String keterangan;
}
