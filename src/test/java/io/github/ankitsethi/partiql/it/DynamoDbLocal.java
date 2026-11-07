package io.github.ankitsethi.partiql.it;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.http.urlconnection.UrlConnectionHttpClient;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;
import software.amazon.awssdk.services.dynamodb.model.ExecuteStatementRequest;
import software.amazon.dynamodb.services.local.main.ServerRunner;
import software.amazon.dynamodb.services.local.server.DynamoDBProxyServer;

/**
 * A single, shared in-process instance of DynamoDB Local for the integration tests.
 *
 * <p>The server runs entirely in-memory over a loopback HTTP port (no Docker, no on-disk state)
 * and is started lazily on first use, then torn down by a JVM shutdown hook. Integration tests
 * use {@link #client()} to talk to it and {@link #execute(String)} to run the PartiQL strings
 * produced by the builders.
 */
public final class DynamoDbLocal {

    private static DynamoDbClient client;

    private DynamoDbLocal() {
    }

    public static synchronized DynamoDbClient client() {
        if (client == null) {
            start();
        }
        return client;
    }

    /** Runs a PartiQL statement and returns all result items (following pagination). */
    public static List<Map<String, AttributeValue>> execute(String statement) {
        List<Map<String, AttributeValue>> items = new ArrayList<>();
        String nextToken = null;
        do {
            var response = client().executeStatement(ExecuteStatementRequest.builder()
                    .statement(statement)
                    .nextToken(nextToken)
                    .build());
            items.addAll(response.items());
            nextToken = response.nextToken();
        } while (nextToken != null);
        return items;
    }

    private static void start() {
        try {
            int port = freePort();
            DynamoDBProxyServer server = ServerRunner.createServerFromCommandLineArgs(
                    new String[] {"-inMemory", "-port", Integer.toString(port)});
            server.start();
            Runtime.getRuntime().addShutdownHook(new Thread(() -> {
                try {
                    server.stop();
                } catch (Exception ignored) {
                    // best-effort shutdown
                }
            }));
            client = DynamoDbClient.builder()
                    .endpointOverride(URI.create("http://localhost:" + port))
                    .httpClient(UrlConnectionHttpClient.builder().build())
                    .region(Region.US_WEST_2)
                    .credentialsProvider(StaticCredentialsProvider.create(
                            AwsBasicCredentials.create("dummyKey", "dummySecret")))
                    .build();
        } catch (Exception e) {
            throw new IllegalStateException("Failed to start DynamoDB Local", e);
        }
    }

    private static int freePort() throws IOException {
        try (ServerSocket socket = new ServerSocket(0)) {
            return socket.getLocalPort();
        }
    }
}
