package com.example.stock.facade;

import com.example.stock.repository.LockRepository;
import com.example.stock.service.NamedLockStockService;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class NamedLockStockFacade {
    private final LockRepository lockRepository;
    private final NamedLockStockService stockService;

    public NamedLockStockFacade(LockRepository lockRepository, NamedLockStockService service) {
        this.lockRepository = lockRepository;
        this.stockService = service;
    }

    @Transactional
    public void decrease(Long id, Long quantity) {
        try {
            lockRepository.getLock(id.toString());
            stockService.decrease(id, quantity);
        } finally {
            lockRepository.releaseLock(id.toString());
        }
    }
}
