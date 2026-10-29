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
public class UserPictureMetrics {

  private static final Logger logger = LogManager.getLogger(UserPictureMetrics.class);

  @Around("execution(* net.tomasbot.mp.api.service.user.UserPictureService.recyclePicture(..))")
  public Object logTimeRecyclePicture(@NotNull ProceedingJoinPoint jp) throws Throwable {
    Instant start = Instant.now();
    Object result = jp.proceed();
    Instant end = Instant.now();

    long duration = Duration.between(start, end).toMillis();
    logger.info("Time to recycle picture {}: {}ms", jp.getArgs(), duration);
    return result;
  }

  @Around("execution(* net.tomasbot.mp.api.service.user.UserPictureService.unfavoriteForAllUsers(..))")
  public Object logTimeUnfavoritePicture(@NotNull ProceedingJoinPoint jp) throws Throwable {
    Instant start = Instant.now();
    Object result = jp.proceed();
    Instant end = Instant.now();

    long duration = Duration.between(start, end).toMillis();
    logger.info("Time to unfavorite picture {}: {}ms", jp.getArgs(), duration);
    return result;
  }

  @Around("execution(* net.tomasbot.mp.api.service.RandomPictureService.deleteContaining(..))")
  public Object logTimeRemovePictureFromRandomCollection(@NotNull ProceedingJoinPoint jp) throws Throwable {
    Instant start = Instant.now();
    Object result = jp.proceed();
    Instant end = Instant.now();

    long duration = Duration.between(start, end).toMillis();
    logger.info("Time to remove Picture {} from random collections: {}ms", jp.getArgs(), duration);
    return result;
  }
}
