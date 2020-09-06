package com.github.gobars.l2cache.web.config;

import com.github.gobars.l2cache.web.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

@Configuration
public class WebMvcConfig implements WebMvcConfigurer {
  @Autowired private UserService userService;

  @Bean
  public WebMvcConfig getMyWebMvcConfig() {
    return new WebMvcConfig() {
      @Override
      public void addInterceptors(InterceptorRegistry registry) {
        registry
            .addInterceptor(new LoginInterceptor())
            .addPathPatterns("/**")
            .excludePathPatterns(
                "/demo/**",
                "/error",
                "/login**",
                "/user/submit-login",
                "/toLogin",
                "/redis/redis-config",
                "/css/**",
                "/js/**",
                "/fonts/**",
                "/i/**");
      }
    };
  }

  public class LoginInterceptor implements HandlerInterceptor {
    @Override
    public boolean preHandle(
        HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
      String token = request.getParameter("token");
      boolean ok = userService.checkLogin(token);
      if (!ok) {
        response.sendRedirect("/toLogin");
      }

      return ok;
    }
  }
}
