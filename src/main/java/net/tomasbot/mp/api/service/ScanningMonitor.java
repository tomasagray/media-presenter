package net.tomasbot.mp.api.service;

import java.time.Duration;
import java.time.Instant;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.jetbrains.annotations.NotNull;

@Aspect
public class ScanningMonitor {

  // TODO: Do something with this class

  private static final Logger logger = LogManager.getLogger(ScanningMonitor.class);

  @Around("execution(* net.tomasbot.mp.api.service.ComicScanningService.scanFile(..))")
  public Object logScanComicBookPage(@NotNull ProceedingJoinPoint jp) throws Throwable {
    Instant start = Instant.now();
    Object result = jp.proceed();
    Instant end = Instant.now();
    logger.info("Scanning Comic Book page took: {}ms", Duration.between(start, end).toMillis());
    return result;
  }
}
