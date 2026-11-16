package com.example.blueprint.infrastructure.bootstrap;

import com.example.blueprint.application.product.ProductCommand;
import com.example.blueprint.application.product.ProductService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.List;

/**
 * Au démarrage :
 * <ul>
 *   <li>si le catalogue est vide et que les données de démo sont activées → insère quelques produits ;</li>
 *   <li>sinon → reconstruit l'index vectoriel EN MÉMOIRE à partir de MongoDB.</li>
 * </ul>
 * Tout est asynchrone et tolérant aux pannes : l'application démarre même sans MongoDB ni Ollama.
 */
@Component
@ConditionalOnProperty(name = "blueprint.bootstrap.enabled", havingValue = "true", matchIfMissing = true)
public class CatalogBootstrap {

    private static final Logger log = LoggerFactory.getLogger(CatalogBootstrap.class);

    static final List<ProductCommand> DEMO_PRODUCTS = List.of(
            new ProductCommand("DESK-ERGO-01", "Bureau assis-debout électrique",
                    "Bureau réglable en hauteur, idéal pour le télétravail : alterner position assise et debout réduit les douleurs de dos lors des longues journées à la maison."),
            new ProductCommand("CHAIR-ERGO-02", "Fauteuil ergonomique",
                    "Fauteuil de bureau avec soutien lombaire réglable et accoudoirs 4D, pensé pour travailler confortablement huit heures par jour."),
            new ProductCommand("HEADSET-ANC-01", "Casque à réduction de bruit",
                    "Casque Bluetooth à réduction active de bruit avec micro antibruit, parfait pour les visioconférences et se concentrer en open space ou à la maison."),
            new ProductCommand("SHOE-RUN-RAIN", "Chaussures de trail imperméables",
                    "Chaussures de course à pied avec membrane imperméable et semelle crantée : courir sous la pluie et sur terrain boueux sans avoir les pieds mouillés."),
            new ProductCommand("JACKET-RAIN-01", "Veste coupe-pluie respirante",
                    "Veste légère imperméable et respirante, compressible dans sa poche, pour le running et la randonnée par temps humide."));

    private final ProductService productService;
    private final boolean demoDataEnabled;

    public CatalogBootstrap(ProductService productService,
                            @Value("${blueprint.demo-data.enabled:true}") boolean demoDataEnabled) {
        this.productService = productService;
        this.demoDataEnabled = demoDataEnabled;
    }

    @EventListener(ApplicationReadyEvent.class)
    public void onReady() {
        productService.list().hasElements()
                .flatMap(hasProducts -> {
                    if (!hasProducts && demoDataEnabled) {
                        return Flux.fromIterable(DEMO_PRODUCTS)
                                .concatMap(productService::create)
                                .count()
                                .doOnNext(n -> log.info("🌱 {} produits de démonstration insérés et indexés", n));
                    }
                    return productService.reindexAll()
                            .doOnNext(n -> log.info("🧠 Index sémantique reconstruit : {} produits", n));
                })
                .onErrorResume(e -> {
                    log.warn("Initialisation du catalogue ignorée (MongoDB ou Ollama indisponible ?) : {}", e.getMessage());
                    return Mono.empty();
                })
                .subscribe();
    }
}
