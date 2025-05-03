package net.tomasbot.mp.api.service;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.jetbrains.annotations.NotNull;
import org.springframework.stereotype.Component;

import java.time.Instant;

@Component
@Aspect
public class RandomEntityTimer {

    private final static Logger logger = LogManager.getLogger(RandomEntityTimer.class);

    @Around("execution(* net.tomasbot.mp.api.service.ComicBookService.getRandomComics(..))")
    public Object logRandomComicBookRetrieval(@NotNull ProceedingJoinPoint jp) throws Throwable {
        final Instant start = Instant.now();
        final Object result = jp.proceed();
        final Instant end = Instant.now();
        final long duration = end.toEpochMilli() - start.toEpochMilli();

        logger.info("Random Comic Book took: {}ms", duration);
        return result;
    }

    @Around("execution(* net.tomasbot.mp.api.service.PictureService.getRandomPictures(..))")
    public Object logRandomPictureRetrieval(@NotNull ProceedingJoinPoint jp) throws Throwable {
        final Instant start = Instant.now();
        final Object result = jp.proceed();
        final Instant end = Instant.now();
        final long duration = end.toEpochMilli() - start.toEpochMilli();

        logger.info("Random Picture took: {}ms", duration);
        return result;
    }

    @Around("execution(* net.tomasbot.mp.api.service.VideoService.getRandom(..))")
    public Object logRandomVideoRetrieval(@NotNull ProceedingJoinPoint jp) throws Throwable {
        final Instant start = Instant.now();
        final Object result = jp.proceed();
        final Instant end = Instant.now();
        final long duration = end.toEpochMilli() - start.toEpochMilli();

        logger.info("Random Video took: {}ms", duration);
        return result;
    }
}