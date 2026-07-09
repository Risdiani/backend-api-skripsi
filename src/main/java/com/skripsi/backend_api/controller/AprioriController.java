package com.skripsi.backend_api.controller;

import com.skripsi.backend_api.dto.BaseResponse;
import com.skripsi.backend_api.dto.apriori.request.AprioriReq;
import com.skripsi.backend_api.service.apriori.AprioriService;
import com.skripsi.backend_api.service.apriori.HistoryAprioriService;

import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/apriori")
@RequiredArgsConstructor
public class AprioriController {

    private final AprioriService aprioriService;
    private final HistoryAprioriService historyAprioriService;

    @PostMapping("/itemset1")
    public ResponseEntity<BaseResponse<Object>> hitungItemset1(@RequestBody AprioriReq req) {
        return ResponseEntity.ok(BaseResponse.ok("Berhasil mengambil Itemset 1", aprioriService.hitungItemset1(req)));
    }

    @PostMapping("/itemset2")
    public ResponseEntity<BaseResponse<Object>> hitungItemset2(@RequestBody AprioriReq req) {
        return ResponseEntity.ok(BaseResponse.ok("Berhasil mengambil Itemset 2", aprioriService.hitungItemset2(req)));
    }

    @PostMapping("/itemset3")
    public ResponseEntity<BaseResponse<Object>> hitungItemset3(@RequestBody AprioriReq req) {
        return ResponseEntity.ok(BaseResponse.ok("Berhasil mengambil Itemset 3", aprioriService.hitungItemset3(req)));
    }

    @PostMapping("/rules")
    public ResponseEntity<BaseResponse<Object>> hitungRules(@RequestBody AprioriReq req) {
        return ResponseEntity
                .ok(BaseResponse.ok("Berhasil membentuk Aturan Asosiasi", aprioriService.hitungAssociationRules(req)));
    }

    @GetMapping("/history")
    public ResponseEntity<BaseResponse<Object>> history() {
        return ResponseEntity.ok(
                BaseResponse.ok("Berhasil mengambil history Apriori", historyAprioriService.getHistory()));
    }

    @GetMapping("/history/{id}")
    public ResponseEntity<BaseResponse<Object>> historyDetail(@PathVariable Long id) {
        return ResponseEntity.ok(
                BaseResponse.ok("Berhasil mengambil detail history Apriori", historyAprioriService.getHistoryDetail(id)));
    }
}
