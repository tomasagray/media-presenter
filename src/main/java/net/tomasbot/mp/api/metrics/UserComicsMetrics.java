package net.tomasbot.mp.api.metrics;

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
public class UserComicsMetrics {

  private static final Logger logger = LogManager.getLogger(UserComicsMetrics.class);

  @Around("execution(* net.tomasbot.mp.api.service.user.UserComicService.recycleComic(..))")
  public Object logTimeRecycleComicBook(@NotNull ProceedingJoinPoint jp) throws Throwable {
    Instant start = Instant.now();
    Object result = jp.proceed();
    Instant end = Instant.now();

    long duration = Duration.between(start, end).toMillis();
    logger.info("Time taken to recycle Comic Book {} : {}ms", jp.getArgs(), duration);
    return result;
  }

  @Around("execution(* net.tomasbot.mp.api.service.user.UserComicService.unfavoriteForAllUsers(..))")
  public Object logTimeUnfavoriteComic(@NotNull ProceedingJoinPoint jp) throws Throwable {
    Instant start = Instant.now();
    Object result = jp.proceed();
    Instant end = Instant.now();

    long duration = Duration.between(start, end).toMillis();
    logger.info("Time taken to unfavorite Comic Book {} : {}ms", jp.getArgs(), duration);
    return result;
  }

  @Around("execution(* net.tomasbot.mp.api.service.RandomComicService.deleteContaining(..))")
  public Object logTimeRemovePictureFromRandomCollection(@NotNull ProceedingJoinPoint jp) throws Throwable {
    Instant start = Instant.now();
    Object result = jp.proceed();
    Instant end = Instant.now();

    long duration = Duration.between(start, end).toMillis();
    logger.info("Time taken to remove Comic Book {} from random collection: {}ms", jp.getArgs(), duration);
    return result;
  }
}
