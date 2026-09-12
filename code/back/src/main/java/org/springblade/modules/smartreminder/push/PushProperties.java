package org.springblade.modules.smartreminder.push;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Component
@ConfigurationProperties(prefix = "smart-reminder.push")
public class PushProperties {
    // Opt-in. A missing private key must not break the existing web application.
    private boolean enabled = false;
    private String teamId = "7U8S8PWU2W";
    private String bundleId = "com.dfyj.xiaoxing";
    private String productionKeyId = "RAR492G6T7";
    private String productionKeyPath = "";
    private String sandboxKeyId = "";
    private String sandboxKeyPath = "";
}
