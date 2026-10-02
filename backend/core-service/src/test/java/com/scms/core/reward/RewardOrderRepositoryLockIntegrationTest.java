package com.scms.core.reward;

import com.scms.core.reward.domain.RewardOrderEntity;
import com.scms.core.reward.domain.RewardOrderStatus;
import com.scms.core.reward.repository.RewardOrderRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Testcontainers(disabledWithoutDocker = true)
class RewardOrderRepositoryLockIntegrationTest {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine")
            .withDatabaseName("scms")
            .withUsername("scms")
            .withPassword("scms");

    @DynamicPropertySource
    static void datasourceProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
        registry.add("spring.jpa.hibernate.ddl-auto", () -> "create-drop");
        registry.add("spring.flyway.enabled", () -> "false");
    }

    @Autowired
    private RewardOrderRepository rewardOrderRepository;

    @Autowired
    private PlatformTransactionManager transactionManager;

    private final ExecutorService executor = Executors.newFixedThreadPool(2);

    @AfterEach
    void tearDown() {
        executor.shutdownNow();
    }

    @Test
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    void findByIdForUpdateShouldBlockConcurrentReadersUntilTransactionCompletes() throws Exception {
        RewardOrderEntity order = new RewardOrderEntity();
        order.setRewardId(3L);
        order.setUserId(4L);
        order.setScoreCost(10);
        order.setStatus(RewardOrderStatus.PENDING);
        RewardOrderEntity saved = rewardOrderRepository.saveAndFlush(order);

        TransactionTemplate transactionTemplate = new TransactionTemplate(transactionManager);
        transactionTemplate.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);

        CountDownLatch firstLockAcquired = new CountDownLatch(1);
        CountDownLatch releaseFirstTransaction = new CountDownLatch(1);

        Future<Long> firstTransaction = executor.submit(() ->
                transactionTemplate.execute(status -> {
                    RewardOrderEntity locked = rewardOrderRepository.findByIdForUpdate(saved.getId()).orElseThrow();
                    firstLockAcquired.countDown();
                    awaitLatch(releaseFirstTransaction);
                    return locked.getId();
                })
        );

        assertTrue(firstLockAcquired.await(5, TimeUnit.SECONDS));

        Future<Long> secondTransaction = executor.submit(() ->
                transactionTemplate.execute(status ->
                        rewardOrderRepository.findByIdForUpdate(saved.getId()).orElseThrow().getId()
                )
        );

        assertThrows(TimeoutException.class, () -> secondTransaction.get(500, TimeUnit.MILLISECONDS));

        releaseFirstTransaction.countDown();

        assertEquals(saved.getId(), firstTransaction.get(5, TimeUnit.SECONDS));
        assertEquals(saved.getId(), secondTransaction.get(5, TimeUnit.SECONDS));
    }

    private void awaitLatch(CountDownLatch latch) {
        try {
            assertTrue(latch.await(5, TimeUnit.SECONDS));
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException(exception);
        }
    }
}
