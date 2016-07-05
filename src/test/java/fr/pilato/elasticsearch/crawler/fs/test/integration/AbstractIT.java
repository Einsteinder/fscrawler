/*
 * Licensed to David Pilato (the "Author") under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership. Author licenses this
 * file to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package fr.pilato.elasticsearch.crawler.fs.test.integration;

import fr.pilato.elasticsearch.crawler.fs.test.AbstractFSCrawlerTest;
import fr.pilato.elasticsearch.crawler.fs.client.ElasticsearchClient;
import fr.pilato.elasticsearch.crawler.fs.meta.settings.Elasticsearch;
import org.elasticsearch.common.Strings;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;

import java.io.IOException;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.junit.Assert.fail;
import static org.junit.Assume.assumeThat;

/**
 * All integration tests assume that an elasticsearch cluster is already running on
 * the machine and one of the nodes is available at 127.0.0.1:9400.
 *
 * You can run one by launching:
 * bin/elasticsearch -Des.http.port=9400
 *
 * The node can be run manually or when using maven, it's automatically started as
 * during the pre-integration phase and stopped after the tests.
 *
 * Note that all existing data in this cluster might be removed
 */
public abstract class AbstractIT extends AbstractFSCrawlerTest {

    protected final static int HTTP_TEST_PORT = 9400;

    protected static ElasticsearchClient elasticsearchClient;

    @BeforeClass
    public static void startRestClient() throws IOException {
        elasticsearchClient = ElasticsearchClient.builder().build();
        elasticsearchClient.addNode(Elasticsearch.Node.builder().setHost("127.0.0.1").setPort(HTTP_TEST_PORT).build());

        try {
            String version = elasticsearchClient.findVersion();
            staticLogger.info("Starting integration tests against an external cluster running elasticsearch [{}]", version);
        } catch (IOException e) {
            // If we have an exception here, let's ignore the test
            staticLogger.warn("Integration tests are skipped: [{}]", e.getMessage());
            assumeThat("Integration tests are skipped", e.getMessage(), not(containsString("no active node found")));
            staticLogger.error("Full error is", e);
            fail("Something wrong is happening. REST Client seemed to raise an exception.");
        }
    }

    @AfterClass
    public static void stopRestClient() {
        elasticsearchClient = null;
        staticLogger.info("Stopping integration tests against an external cluster");
    }

    @Before
    public void cleanExistingIndex() throws IOException {
        logger.info(" -> Removing existing index [{}*]", getCrawlerName());
        elasticsearchClient.deleteIndex(getCrawlerName() + "*");
    }

    private static final String testCrawlerPrefix = "fscrawler_";

    protected String getCrawlerName() {
        String testName = testCrawlerPrefix.concat(getCurrentTestName());
        return testName.contains(" ") ? Strings.split(testName, " ")[0] : testName;
    }
}