package com.scarlxrd.catalog_service.config.metrics;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class CatalogMetrics {

    private final MeterRegistry meterRegistry;


    public void validated() {
        Counter.builder("books_validated_total")
                .description("Total de livros validados.")
                .tag("service", "catalog-service")
                .register(this.meterRegistry)
                .increment();
    }

    public void cancelled(String reason) {
        Counter.builder("books_unavailable_total")
                .description("Total inválido/cancelados de livros")
                .tag("service", "catalog-service")
                .tag("reason", reason)
                .register(this.meterRegistry)
                .increment();
    }

    public void stockSuccess() {

        Counter.builder("stock_decrease_total")
                .description("Total  de redução de estoque")
                .tag("service", "catalog-service")
                .register(this.meterRegistry)
                .increment();
    }


    public void stockError(String reason) {
        Counter.builder("stock_decrease_failed_total")
                .description("total de redução de estoque que deu erro ")
                .tag("service", "catalog-service")
                .tag("reason", reason)
                .register(this.meterRegistry)
                .increment();
    }

}
