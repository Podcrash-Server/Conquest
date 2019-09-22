package me.raindance.champions;

import me.raindance.champions.redis.Communicator;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class RedisTest {
    @Test
    @DisplayName("Redis")
    public void redis() {
        CompletableFuture.allOf(Communicator.setup(Executors.newSingleThreadExecutor()));

        Assertions.assertNotNull(Communicator.getCode(), "The lobby code is null! Expected not!");

        Communicator.getMap().put("TEST", "TEST123");
        Assertions.assertEquals("TEST123", Communicator.getMap().get("TEST"), "redis failed");

        Communicator.shutdown();
    }
}