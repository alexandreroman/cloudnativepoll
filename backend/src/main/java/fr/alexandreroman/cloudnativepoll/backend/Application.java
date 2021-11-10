/*
 * Copyright (c) 2021 VMware, Inc. or its affiliates
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package fr.alexandreroman.cloudnativepoll.backend;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.event.EventListener;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.GenericToStringSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Supplier;

import static java.util.Objects.requireNonNullElse;

@SpringBootApplication
public class Application {
    public static void main(String[] args) {
        SpringApplication.run(Application.class, args);
    }
}

@Component
@ConfigurationProperties(prefix = "poll")
@Data
class PollConfig {
    private List<String> choices;
}

@RestController
@RequiredArgsConstructor
@Slf4j
class VotesController {
    private final PollConfig config;
    private final RedisTemplate<String, Integer> redis;

    @GetMapping("api/v1/votes")
    Map<String, Integer> getResults() {
        // Get results from the Redis server instance.
        final Map<String, Integer> results = new HashMap<>(config.getChoices().size());
        for (final String choice : config.getChoices()) {
            final Integer counter = redis.opsForValue().get(choice);
            results.put(choice, counter == null ? 0 : counter);
        }
        return results;
    }
}

@Configuration
@RequiredArgsConstructor
@Slf4j
class VoteListener {
    private final Counter invalidVoteCounter;
    private final PollConfig config;
    private final RedisTemplate<String, Integer> redis;

    @Bean
    Consumer<Vote> onVote() {
        return vote -> {
            log.info("Received new vote: {}", vote.getChoice());

            if (!config.getChoices().contains(vote.getChoice())) {
                log.warn("Skip invalid vote: {}", vote.getChoice());
                invalidVoteCounter.increment();
            } else {
                // The Redis server instance is updated as soon as we receive
                // new vote through the RabbitMQ queue.
                redis.opsForValue().increment(vote.getChoice());
            }
        };
    }
}

@Data
class Vote {
    private String choice;
}

@Configuration
class RedisConfig {
    @Bean
    RedisTemplate<String, Integer> redisTemplate(RedisConnectionFactory cf) {
        final RedisTemplate<String, Integer> redis = new RedisTemplate<>();
        redis.setConnectionFactory(cf);
        redis.setKeySerializer(new StringRedisSerializer());
        redis.setValueSerializer(new GenericToStringSerializer<>(Integer.class));
        return redis;
    }
}

@Configuration
@RequiredArgsConstructor
class MetricsConfig {
    private final MeterRegistry reg;
    private final PollConfig config;
    private final RedisTemplate<String, Integer> redisTemplate;

    @Bean
    Counter invalidVotesCounter() {
        return Counter.builder("cloudnativepoll.votes.invalid")
                .description("Number of invalid received votes")
                .register(reg);
    }

    @EventListener(ApplicationReadyEvent.class)
    public void registerReceivedVotesGauge() {
        for (final String choice : config.getChoices()) {
            final Supplier<Number> voteCountSupplier = () -> requireNonNullElse(redisTemplate.opsForValue().get(choice), 0);
            Gauge.builder("cloudnativepoll.votes.received", voteCountSupplier)
                    .description("Number of received votes")
                    .tag("choice", choice)
                    .baseUnit("votes")
                    .register(reg);
        }
    }
}
