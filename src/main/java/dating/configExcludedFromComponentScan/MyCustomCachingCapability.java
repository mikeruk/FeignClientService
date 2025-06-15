package dating.configExcludedFromComponentScan;

import feign.Capability;
import feign.Client;
import feign.Request;
import feign.Response;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;

public class MyCustomCachingCapability implements Capability {

    @Override
    public Client enrich(Client client) {
        // Wrap the existing client with caching logic
        return new CachingClient(client);
    }

    // Internal CachingClient that intercepts requests and caches them
    private static class CachingClient implements Client {
        private static final Logger logger = LoggerFactory.getLogger(CachingClient.class);
        private static final Map<String, Response> CACHE = new ConcurrentHashMap<>();

        private final Client delegate;

        public CachingClient(Client delegate) {
            this.delegate = delegate;
        }

        @Override
        public Response execute(Request request, Request.Options options) throws IOException {
            String key = generateCacheKey(request);
            logger.info("Feign request to URL: {}", key);

            // Check cache
            Response cachedResponse = CACHE.get(key);
            if (cachedResponse != null) {
                logger.info("Cache HIT for key: {}", key);
                return cloneResponse(cachedResponse); // Return a fresh clone
            }

            logger.info("Cache MISS for key: {}", key);
            // Execute the request
            Response response = null;
            try {
                response = delegate.execute(request, options);
                // Read and cache the body, preserving the original response
                byte[] bodyData = readBody(response);
                Response clonedResponse = Response.builder()
                        .status(response.status())
                        .reason(response.reason())
                        .headers(response.headers())
                        .request(response.request())
                        .body(bodyData)
                        .build();
                CACHE.put(key, clonedResponse);
                // Return a new response with the same body to avoid stream issues
                return Response.builder()
                        .status(response.status())
                        .reason(response.reason())
                        .headers(response.headers())
                        .request(response.request())
                        .body(bodyData)
                        .build();
            } catch (IOException e) {
                logger.error("Failed to execute request for key: {}", key, e);
                throw e;
            } finally {
                // Close the original response body if it exists
                if (response != null && response.body() != null) {
                    try {
                        response.body().close();
                    } catch (IOException e) {
                        logger.warn("Failed to close response body for key: {}", key, e);
                    }
                }
            }
        }

        private String generateCacheKey(Request request) {
            // Include method to avoid collisions between GET/POST
            return request.httpMethod().name() + ":" + request.url();
        }

        private byte[] readBody(Response response) throws IOException {
            if (response.body() == null) {
                return new byte[0];
            }
            try (InputStream inputStream = response.body().asInputStream();
                 ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
                byte[] buffer = new byte[1024];
                int bytesRead;
                while ((bytesRead = inputStream.read(buffer)) != -1) {
                    baos.write(buffer, 0, bytesRead);
                }
                return baos.toByteArray();
            } catch (IOException e) {
                logger.error("Failed to read response body", e);
                throw e;
            }
        }

        private Response cloneResponse(Response original) throws IOException {
            // For cached responses, the body is already a byte array
            byte[] bodyData = readBody(original);
            return Response.builder()
                    .status(original.status())
                    .reason(original.reason())
                    .headers(original.headers())
                    .request(original.request())
                    .body(bodyData)
                    .build();
        }
    }
}


