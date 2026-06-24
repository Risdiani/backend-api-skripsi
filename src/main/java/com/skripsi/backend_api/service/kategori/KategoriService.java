package com.skripsi.backend_api.service.kategori;

import com.skripsi.backend_api.dto.kategori.request.KategoriReq;
import com.skripsi.backend_api.dto.kategori.response.KategoriRes;
import com.skripsi.backend_api.entity.Kategori;
import com.skripsi.backend_api.repository.KategoriRepository;
import com.skripsi.backend_api.utils.NormalizeUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class KategoriService {
    
    private final KategoriRepository kategoriRepository;
    private final NormalizeUtil normalizeUtil;

    private KategoriRes toResponse(Kategori k) {
        return KategoriRes.builder()
                .id(k.getId())
                .nama(k.getNama())
                .penanda(k.getPenanda())
                .keterangan(k.getKeterangan())
                .createdAt(k.getCreatedAt())
                .build();
    }

    public List<KategoriRes> findAll() {
        return kategoriRepository.findAll().stream().map(this::toResponse).toList();
    }

    public KategoriRes findById(Long id) {
        Kategori k = kategoriRepository.findById(id).orElseThrow(() -> new IllegalArgumentException("Kategori not found"));
        return toResponse(k);
    }

    public KategoriRes create(KategoriReq req) {
        String nama = normalizeUtil.normalizeText(req == null ? null : req.getNama());
        String penanda = normalizeUtil.normalizeText(req == null ? null : req.getPenanda());
        String keterangan = normalizeUtil.normalizeText(req == null ? null : req.getKeterangan());

        if (nama == null || nama.isBlank()) throw new IllegalArgumentException("Nama is required");
        if (penanda == null || penanda.isBlank()) throw new IllegalArgumentException("Penanda is required");
        if (kategoriRepository.existsByNama(nama)) throw new IllegalArgumentException("Nama already used");
        if (kategoriRepository.existsByPenanda(penanda)) throw new IllegalArgumentException("Penanda already used");

        Kategori k = Kategori.builder()
                .nama(nama)
                .penanda(penanda)
                .keterangan(keterangan)
                .build();

        return toResponse(kategoriRepository.save(k));
    }

    public KategoriRes update(Long id, KategoriReq req) {
        String nama = normalizeUtil.normalizeText(req == null ? null : req.getNama());
        String penanda = normalizeUtil.normalizeText(req == null ? null : req.getPenanda());
        String keterangan = normalizeUtil.normalizeText(req == null ? null : req.getKeterangan());

        Kategori k = kategoriRepository.findById(id).orElseThrow(() -> new IllegalArgumentException("Kategori not found"));

        if (nama != null) {
            if (nama.isBlank()) throw new IllegalArgumentException("Nama cannot be blank");
            kategoriRepository.findByNama(nama).ifPresent(existing -> {
                if (!existing.getId().equals(k.getId())) throw new IllegalArgumentException("Nama already used");
            });
            k.setNama(nama);
        }

        if (penanda != null) {
            if (penanda.isBlank()) throw new IllegalArgumentException("Penanda cannot be blank");
            kategoriRepository.findByPenanda(penanda).ifPresent(existing -> {
                if (!existing.getId().equals(k.getId())) throw new IllegalArgumentException("Penanda already used");
            });
            k.setPenanda(penanda);
        }

        if (keterangan != null) k.setKeterangan(keterangan);

        return toResponse(kategoriRepository.save(k));
    }

    public void delete(Long id) {
        if (!kategoriRepository.existsById(id)) throw new IllegalArgumentException("Kategori not found");
        kategoriRepository.deleteById(id);
    }
}
