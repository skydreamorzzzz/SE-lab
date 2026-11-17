package edu.market;

import java.time.Instant;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
@Component
public class BargainMaintenance {
    private final BargainRepository bargains;
    private final BargainService service;
    public BargainMaintenance(BargainRepository bargains, BargainService service) {
        this.bargains = bargains;
        this.service = service;
    }
    @Scheduled(fixedDelay = 30000)
    public void sweep() {
        Instant now = Instant.now();
        bargains.findByStatusAndExpiresAtBefore("ACCEPTED", now).forEach(b -> service.expire(b.getId(), now));
    }
}
