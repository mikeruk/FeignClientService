package dating.configExcludedFromComponentScan;

import feign.Client;
import feign.Request;
import feign.Response;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

public class LoggingFeignClient implements Client {

    private static final Logger logger = LoggerFactory.getLogger(LoggingFeignClient.class);

    private final Client delegate;

    public LoggingFeignClient(Client delegate) {
        this.delegate = delegate;
    }

    @Override
    public Response execute(Request request, Request.Options options) throws IOException {
        logger.info("Feign Request: {} {}", request.httpMethod(), request.url());

        Response response = delegate.execute(request, options);

        logger.info("Feign Response: Status={} for URL={}", response.status(), request.url());

        return response;
    }
}

