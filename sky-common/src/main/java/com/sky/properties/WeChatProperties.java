package com.sky.properties;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "sky.wechat")
@Data
public class WeChatProperties {
    private String appid;
    private String mchid;
    private String secret;
    private String mchSerialNo;
    private String privateKeyFilePath;
    private String weChatPayCertFilePath;
    private String notifyUrl;
    private String refundNotifyUrl;
    private String apiV3Key;
}