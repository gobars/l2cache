package com.github.gobars.l2cache.starter.config;

import org.springframework.context.annotation.Import;

import java.lang.annotation.*;

@Target({ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Import({L2CacheAutoConfig.class})
public @interface EnableL2Cache {}
