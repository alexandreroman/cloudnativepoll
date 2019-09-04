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

import org.json.JSONException;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.cloud.contract.wiremock.AutoConfigureWireMock;
import org.springframework.cloud.stream.annotation.EnableBinding;
import org.springframework.cloud.stream.messaging.Source;
import org.springframework.cloud.stream.test.binder.MessageCollector;
import org.springframework.http.ResponseEntity;
import org.springframework.messaging.Message;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringRunner;

import java.util.Map;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static java.lang.Thread.sleep;
import static org.assertj.core.api.Assertions.assertThat;
import static org.skyscreamer.jsonassert.JSONAssert.assertEquals;

@RunWith(SpringRunner.class)
@ActiveProfiles("test")
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureWireMock(port = 9876)
public class ApplicationTests {
    @Autowired
    private TestRestTemplate restTemplate;
    @Autowired
    private Source source;
    @Autowired
    private MessageCollector messageCollector;

    @Test
    public void contextLoads() {
    }

    @Test
    public void testCastVote() throws JSONException {
        final VoteRequest req = new VoteRequest();
        req.setChoice("Black Panther");
        final ResponseEntity<Object> resp = restTemplate.postForEntity("/votes", req, null);
        assertThat(resp.getStatusCode().is2xxSuccessful()).isTrue();

        final Message<?> msg = messageCollector.forChannel(source.output()).poll();
        assertEquals("{ 'choice': 'Black Panther' }", msg.getPayload().toString(), false);
    }

    @Test
    public void testVoteCache() throws InterruptedException {
        stubFor(get("/api/v1/votes").willReturn(okJson("{ \"The Wasp\": 4 }")));
        sleep(1500);
        Map<String, Integer> results = restTemplate.getForObject("/votes", Map.class);
        assertThat(results).containsEntry("The Wasp", 4);

        stubFor(get("/api/v1/votes").willReturn(serverError()));
        sleep(1500);
        results = restTemplate.getForObject("/votes", Map.class);
        assertThat(results).containsEntry("The Wasp", 4);
    }

    @Test
    public void testMetrics() {
        final String metrics = restTemplate.getForObject("/actuator/prometheus", String.class);
        assertThat(metrics).contains("cloudnativepoll_votes_casted");
    }

    @SpringBootApplication
    @EnableBinding(Source.class)
    public static class MySource {
        @Autowired
        private Source source;
    }
}
