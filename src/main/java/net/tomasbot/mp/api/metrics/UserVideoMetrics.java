package net.tomasbot.mp.api.metrics;

import net.tomasbot.mp.model.Video;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.jetbrains.annotations.NotNull;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;

@Aspect
@Component
public class UserVideoMetrics {

  private static final Logger logger = LogManager.getLogger(UserVideoMetrics.class);

  @Around("execution(* net.tomasbot.mp.api.service.user.UserVideoService.recycleVideo(..))")
  public Object timeRecycleVideo(@NotNull ProceedingJoinPoint jp) throws Throwable {
    Instant start = Instant.now();
    Object result = jp.proceed();
    Instant end = Instant.now();

    logger.info("Time to recycle video{}: {}ms", jp.getArgs(), Duration.between(start, end).toMillis());
    return result;
  }

  @Around("execution(* net.tomasbot.mp.api.service.user.UserVideoService.unfavoriteForAllUsers(..))")
  public Object timeUnfavoriteVideo(@NotNull ProceedingJoinPoint jp) throws Throwable {
    Instant start = Instant.now();
    Object result = jp.proceed();
    Instant end = Instant.now();

    Object[] args = jp.getArgs();
    if (args != null && args.length > 0 && args[0] instanceof Video video)
      logger.info("Time to unfavorite video{}: {}ms", video.getId(), Duration.between(start, end).toMillis());

    return result;
  }

  @Around("execution(* net.tomasbot.mp.api.service.RandomVideoService.deleteContaining(..))")
  public Object timeToRemoveVideoFromRandomCollection(@NotNull ProceedingJoinPoint jp) throws Throwable {
    Instant start = Instant.now();
    Object result = jp.proceed();
    Instant end = Instant.now();

    long duration = Duration.between(start, end).toMillis();
    logger.info("Time to remove video {} from random collection: {}ms", jp.getArgs(), duration);
    return result;
  }
}
