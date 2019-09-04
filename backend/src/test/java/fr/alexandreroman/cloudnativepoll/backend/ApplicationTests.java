/*
 * Copyright (c) 2019 Pivotal Software, Inc.
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

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.cloud.stream.annotation.EnableBinding;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.stereotype.Component;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringRunner;
import redis.embedded.RedisServer;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

@RunWith(SpringRunner.class)
@ActiveProfiles("test")
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public class ApplicationTests {
    @Autowired
    private TestRestTemplate restTemplate;
    @Autowired
    private Streams streams;

    @Test
    public void contextLoads() {
    }

    @Test
    public void testGetResults() {
        Map<String, Integer> results = restTemplate.getForObject("/api/v1/votes", Map.class);
        assertThat(results).containsEntry("Iron Man", 0);

        final Vote vote = new Vote();
        vote.setChoice("Iron Man");
        streams.votes().send(MessageBuilder.withPayload(vote).build());

        results = restTemplate.getForObject("/api/v1/votes", Map.class);
        assertThat(results).containsEntry("Iron Man", 1);
    }

    @Test
    public void testInvalidVote() {
        final Vote vote = new Vote();
        vote.setChoice("Ant Man");
        streams.votes().send(MessageBuilder.withPayload(vote).build());

        final Map<String, Integer> results = restTemplate.getForObject("/api/v1/votes", Map.class);
        assertThat(results).doesNotContainKey("Ant Man");
    }

    @Test
    public void testReset() {
        Map<String, Integer> results = restTemplate.getForObject("/api/v1/votes", Map.class);
        assertThat(results).containsEntry("Captain America", 0);

        final Vote vote = new Vote();
        vote.setChoice("Captain America");
        streams.votes().send(MessageBuilder.withPayload(vote).build());

        results = restTemplate.getForObject("/api/v1/votes", Map.class);
        assertThat(results).containsEntry("Captain America", 1);

        streams.reset().send(MessageBuilder.withPayload("Reset").build());

        results = restTemplate.getForObject("/api/v1/votes", Map.class);
        assertThat(results).containsEntry("Captain America", 0);
    }

    @Test
    public void testMetrics() {
        final String metrics = restTemplate.getForObject("/actuator/prometheus", String.class);
        assertThat(metrics).contains("cloudnativepoll_votes_invalid");
        assertThat(metrics).contains("cloudnativepoll_votes_received");
    }

    @SpringBootApplication
    @EnableBinding(Streams.class)
    public static class MyStreams {
        @Autowired
        private Streams streams;
    }
}

@Component
class EmbeddedRedis {
    private RedisServer redisServer;

    @PostConstruct
    public void init() {
        redisServer = new RedisServer();
        redisServer.start();
    }

    @PreDestroy
    public void destroy() {
        redisServer.stop();
        redisServer = null;
    }
}
