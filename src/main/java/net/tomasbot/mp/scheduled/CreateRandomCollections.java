package net.tomasbot.mp.scheduled;

import net.tomasbot.mp.api.service.RandomEntityService;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.concurrent.TimeUnit;

@Component
public class CreateRandomCollections {

  private final List<RandomEntityService<?>> randomEntityServices;

  public CreateRandomCollections(List<RandomEntityService<?>> randomEntityServices) {
    this.randomEntityServices = randomEntityServices;
  }

  @Scheduled(fixedRate = 1, timeUnit = TimeUnit.MINUTES)
  public void createRandomCollections() {
    for (RandomEntityService<?> service : randomEntityServices) {
      service.limitCollection();
      service.addRandomCollection();
    }
  }
}
