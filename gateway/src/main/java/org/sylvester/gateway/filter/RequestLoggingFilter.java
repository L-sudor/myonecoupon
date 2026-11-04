package org.sylvester.gateway.filter;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.core.Ordered;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
public class RequestLoggingFilter implements GlobalFilter, Ordered {

    private static final Logger LOG = LoggerFactory.getLogger(RequestLoggingFilter.class);

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        ServerHttpRequest request = exchange.getRequest();
        HttpMethod method = request.getMethod();

        String traceId = UUID.randomUUID().toString();

        long startTime = System.currentTimeMillis();
        MDC.put("traceId", traceId);

        LOG.info("请求URI: {}", request.getURI());
        LOG.info("请求类型: {}", method);
        LOG.info("请求头: {}", request.getHeaders());

        if (method == HttpMethod.GET) {
            LOG.info("请求参数: {}", request.getQueryParams());
        }

        return chain.filter(exchange).then(Mono.fromRunnable(() -> {
            long duration = System.currentTimeMillis() - startTime;
            LOG.info("响应时间：{} ms", duration);
        }));
    }

    @Override
    public int getOrder() {
        return -1;
    }
}