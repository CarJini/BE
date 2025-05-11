package com.ll.carjini.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@ConfigurationProperties(prefix = "google")
@Getter
@Setter
public class GoogleProperties {
    private List<String> clientIds;
}
