package com.one.learn.springbootshutdown;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.web.embedded.tomcat.TomcatServletWebServerFactory;
import org.springframework.boot.web.servlet.server.ConfigurableServletWebServerFactory;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.web.bind.annotation.RestController;

@SpringBootApplication
@RestController
@Slf4j
public class SpringbootShutdownApplication {

    public static void main(String[] args) {
//        SpringApplicationBuilder springApplicationBuilder = new SpringApplicationBuilder(SpringbootShutdownApplication.class);
//        springApplicationBuilder.web(WebApplicationType.SERVLET).build()
//                .addListeners(new ApplicationPidFileWriter("./bin/shutdown.pid"));
//        springApplicationBuilder.run(args);
        ConfigurableApplicationContext run = SpringApplication.run(SpringbootShutdownApplication.class, args);
        Object webServerFactory = run.getBean("webServerFactory");
        log.info(webServerFactory + "");
    }

    @Bean
    public CustomShutdown customShutdown() {
        return new CustomShutdown();
    }

    @Bean
    public ConfigurableServletWebServerFactory webServerFactory(final CustomShutdown customShutdown) {
        TomcatServletWebServerFactory tomcatServletWebServerFactory = new TomcatServletWebServerFactory();
        tomcatServletWebServerFactory.addConnectorCustomizers(customShutdown);
        return tomcatServletWebServerFactory;
    }
}
