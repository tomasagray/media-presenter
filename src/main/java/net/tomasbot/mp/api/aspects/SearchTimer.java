package net.tomasbot.mp.api.aspects;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.jetbrains.annotations.NotNull;
import org.springframework.stereotype.Component;

@Aspect
@Component
public class SearchTimer {

    private static final Logger logger = LogManager.getLogger(SearchTimer.class);

    @Around("execution(* net.tomasbot.mp.api.service.SearchService.search*(..))")
    public Object logSearchAll(@NotNull ProceedingJoinPoint jp) throws  Throwable {
        final long start = System.currentTimeMillis();
        final Object result = jp.proceed();
        final long end = System.currentTimeMillis();
        final long duration = end - start;

        logger.info("Search took: {}ms", duration);
        return result;
    }
}
