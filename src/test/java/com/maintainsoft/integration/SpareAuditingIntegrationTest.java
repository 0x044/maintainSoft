package com.maintainsoft.integration;

import com.maintainsoft.entity.Spare;
import com.maintainsoft.repository.SpareRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Transactional
class SpareAuditingIntegrationTest {

    @Autowired
    private SpareRepository spareRepository;

    @PersistenceContext
    private EntityManager entityManager;

    @Test
    void updatingSpareDoesNotChangeLastPurchaseDate() {
        Instant purchaseDate = Instant.parse("2026-01-15T10:00:00Z");
        Spare spare = new Spare();
        spare.setPartNumber("AUDIT-" + UUID.randomUUID());
        spare.setName("Original name");
        spare.setStock(2);
        spare.setLastPurchaseDate(purchaseDate);
        spareRepository.saveAndFlush(spare);

        UUID id = spare.getId();
        entityManager.clear();
        Spare loaded = spareRepository.findById(id).orElseThrow();
        loaded.setName("Updated name");
        spareRepository.saveAndFlush(loaded);

        entityManager.clear();
        assertThat(spareRepository.findById(id).orElseThrow().getLastPurchaseDate())
                .isEqualTo(purchaseDate);
    }
}
