// File: src/main/java/com/haris/MechanicApp/Service/GcsConfig.java
package com.haris.MechanicApp.Service;

import com.google.cloud.storage.Storage;
import com.google.cloud.storage.StorageOptions;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import java.io.IOException;

@Configuration
public class GcsConfig {
    @Bean
    public Storage storage() throws IOException {
        // Yeh khud hi Google Cloud environment se credentials utha lega.
        return StorageOptions.getDefaultInstance().getService();
    }
}
