package com.example.stock.service;

import com.example.stock.domain.Stock;
import com.example.stock.facade.LettuceLockStockFacade;
import com.example.stock.facade.NamedLockStockFacade;
import com.example.stock.facade.OptimisticLockStockFacade;
import com.example.stock.repository.StockRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
class StockServiceTest {

    @Autowired
    private StockService stockService;
    @Autowired
    private PessimisticLockStockService pessimisticLockStockService;
    @Autowired
    private OptimisticLockStockFacade optimisticLockStockFacade;
    @Autowired
    private NamedLockStockFacade namedLockStockFacade;
    @Autowired
    private LettuceLockStockFacade lettuceLockStockFacade;
    @Autowired
    private StockRepository stockRepository;

    @BeforeEach
    void setUp() {
        Stock stock = new Stock(1L, 100L);
        stockRepository.saveAndFlush(stock);
    }

    @DisplayName("재고 감소")
    @Test
    void decreaseStock() {
        stockService.decrease(1L, 1L);

        Stock stock = stockRepository.findById(1L).orElseThrow();

        assertThat(stock.getQuantity()).isEqualTo(99);
    }

    @DisplayName("동시에 재고 감소 100개 요청 synchronized")
    @Test
    void decreaseStockConcurrency() throws InterruptedException {
        CountDownLatch latch = new CountDownLatch(100);

        동시에_요청(() -> {
            try {
                stockService.decrease(1L, 1L);
            } finally {
                latch.countDown();
            }
        }, latch);
    }

    @DisplayName("동시에 재고 감소 100개 요청 exclusive lock")
    @Test
    void decreaseStockPessimisticLock() throws InterruptedException {
        CountDownLatch latch = new CountDownLatch(100);

        동시에_요청(() -> {
            try {
                pessimisticLockStockService.decrease(1L, 1L);
            } finally {
                latch.countDown();
            }
        }, latch);
    }

    @DisplayName("동시에 재고 감소 100개 요청 optimistic lock")
    @Test
    void decreaseStockOptimisticLock() throws InterruptedException {
        CountDownLatch latch = new CountDownLatch(100);

        동시에_요청(() -> {
            try {
                optimisticLockStockFacade.decrease(1L, 1L);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            } finally {
                latch.countDown();
            }
        }, latch);
    }

    @DisplayName("동시에 재고 감소 100개 요청 named lock")
    @Test
    void decreaseStockNamedLock() throws InterruptedException {
        CountDownLatch latch = new CountDownLatch(100);

        동시에_요청(() -> {
            try {
                namedLockStockFacade.decrease(1L, 1L);
            } finally {
                latch.countDown();
            }
        }, latch);
    }

    @DisplayName("동시에 재고 감소 100개 요청 lettuce lock")
    @Test
    void decreaseStockLettuceLock() throws InterruptedException {
        CountDownLatch latch = new CountDownLatch(100);

        동시에_요청(() -> {
            try {
                lettuceLockStockFacade.decrease(1L, 1L);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            } finally {
                latch.countDown();
            }
        }, latch);
    }

    void 동시에_요청(Runnable runnable, CountDownLatch latch) throws InterruptedException {
        long threadCount = latch.getCount();
        ExecutorService executorService = Executors.newFixedThreadPool(32);

        for (int i = 0; i < threadCount; i++) {
            executorService.submit(runnable);
        }

        latch.await();

        Stock stock = stockRepository.findById(1L).orElseThrow();

        assertThat(stock.getQuantity()).isEqualTo(0);
    }
}
