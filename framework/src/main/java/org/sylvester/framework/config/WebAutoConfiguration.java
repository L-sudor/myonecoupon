package org.sylvester.framework.config;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.context.annotation.Bean;
import org.sylvester.framework.web.GlobalExceptionHandler;

/**
 * Web 组件自动装配
 *
 * <p>由 {@code META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports} 注册，
 * 各业务模块引入 framework 依赖后即可自动生效，无需在启动类上额外扫描。
 */
@AutoConfiguration
@ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.SERVLET)
public class WebAutoConfiguration {

    /**
     * 构建全局异常拦截器组件 Bean
     */
    @Bean
    @ConditionalOnMissingBean(GlobalExceptionHandler.class)
    public GlobalExceptionHandler globalExceptionHandler() {
        return new GlobalExceptionHandler();
    }
}
