package com.sbs.board.global.config;

import com.sbs.board.auth.LoginUserIdResolver;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.web.config.EnableSpringDataWebSupport;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

import static org.springframework.data.web.config.EnableSpringDataWebSupport.PageSerializationMode.VIA_DTO;

@Slf4j
@Configuration
@EnableSpringDataWebSupport(pageSerializationMode = VIA_DTO)
// @RequiredArgsConstructor
public class WebConfig implements WebMvcConfigurer {
    // private final LoginUserIdResolver loginUserIdResolver;

    private final String uploadDir;

    public WebConfig(@Value("${app.upload.dir}")String uploadDir) {
        this.uploadDir = uploadDir;
    }

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        Path root = Paths.get(uploadDir).toAbsolutePath().normalize();
        String location = root.toUri().toString();
        registry.addResourceHandler("/images/**")
                .addResourceLocations(location);
    }

//    @Override
//    public void addArgumentResolvers(List<HandlerMethodArgumentResolver> resolvers) {
//        log.debug("WebConfig 인스턴스에서 addArgumentResolvers 호출됨");
//
//        resolvers.add(loginUserIdResolver);
//    }
}
