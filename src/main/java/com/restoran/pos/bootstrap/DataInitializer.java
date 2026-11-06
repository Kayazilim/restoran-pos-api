package com.restoran.pos.bootstrap;

import com.restoran.pos.service.DataSeedService;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

/**
 * Uygulama ayağa kalktığında örnek verilerin yüklenmesini tetikler.
 */
@Component
public class DataInitializer implements ApplicationRunner {

    private final DataSeedService dataSeedService;

    public DataInitializer(DataSeedService dataSeedService) {
        this.dataSeedService = dataSeedService;
    }

    @Override
    public void run(ApplicationArguments args) {
        dataSeedService.seed();
    }
}
