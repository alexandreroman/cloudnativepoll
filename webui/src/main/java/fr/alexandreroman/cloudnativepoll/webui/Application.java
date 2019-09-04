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

package fr.alexandreroman.cloudnativepoll.webui;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.cloud.stream.annotation.EnableBinding;
import org.springframework.cloud.stream.messaging.Source;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static java.util.Collections.emptyMap;

@SpringBootApplication
@EnableFeignClients
@EnableScheduling
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
    private List<String> images;
    private String question;
}

@Controller
@RequiredArgsConstructor
class IndexController {
    private final PollConfig config;

    @GetMapping("/")
    String index(Map<String, Object> view) {
        view.put("question", config.getQuestion());

        final List<Choice> choices = new ArrayList<>(config.getChoices().size());
        for (int i = 0; i < config.getChoices().size(); ++i) {
            final Choice choice = new Choice(config.getChoices().get(i), config.getImages().get(i));
            choices.add(choice);
        }
        view.put("choices", choices);

        return "index";
    }

    @Data
    @RequiredArgsConstructor
    private static class Choice {
        private final String text;
        private final String image;
    }
}

@RestController
@EnableBinding(Source.class)
@RequiredArgsConstructor
@Slf4j
class VotesController {
    private final Counter castedVoteCounter;
    private final VoteCache cache;
    private final Source source;

    @GetMapping("/votes")
    Map<String, Integer> getVotes() {
        return cache.get();
    }

    @PostMapping("/votes")
    void castVote(@RequestBody VoteRequest req) {
        log.info("Casting vote: {}", req.getChoice());
        source.output().send(MessageBuilder.withPayload(req).build());
        castedVoteCounter.increment();
    }
}

@Data
class VoteRequest {
    private String choice;
}

@Component
class VoteCache {
    private final Map<String, Integer> votes = new HashMap<>();

    Map<String, Integer> get() {
        synchronized (votes) {
            return new HashMap<>(votes);
        }
    }

    void update(Map<String, Integer> newVotes) {
        synchronized (votes) {
            votes.clear();
            votes.putAll(newVotes);
        }
    }
}

@FeignClient(name = "cloudnativepoll-backend", fallback = BackendClientServiceFallback.class,
        url = "${poll.backend}")
interface BackendClientService {
    @GetMapping("api/v1/votes")
    Map<String, Integer> getResults();
}

@Component
@Slf4j
class BackendClientServiceFallback implements BackendClientService {
    @Override
    public Map<String, Integer> getResults() {
        log.warn("Failed to get poll results: using values from cache");
        return emptyMap();
    }
}

@Component
@RequiredArgsConstructor
@Slf4j
class VoteCacheUpdater {
    private final BackendClientService backend;
    private final VoteCache cache;

    @Scheduled(fixedRateString = "${poll.refresh}")
    void updateVotes() {
        log.debug("Getting poll results");
        final Map<String, Integer> results = backend.getResults();
        log.debug("Received poll results: {}", results);

        if (!results.isEmpty()) {
            log.debug("Updating vote cache with poll results");
            cache.update(results);
            log.debug("Vote cache updated");
        }
    }
}

@Configuration
class MetricsConfig {
    @Bean
    Counter castedVoteCounter(MeterRegistry reg) {
        return Counter.builder("cloudnativepoll.votes.casted")
                .description("Number of casted votes")
                .register(reg);
    }
}

@Configuration
class SecurityConfig extends WebSecurityConfigurerAdapter {
    @Override
    protected void configure(HttpSecurity http) throws Exception {
        http.csrf().disable()
                .authorizeRequests()
                .anyRequest().permitAll();
    }
}
