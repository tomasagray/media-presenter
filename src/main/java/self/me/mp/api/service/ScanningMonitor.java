package self.me.mp.api.service;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.jetbrains.annotations.NotNull;

import java.time.Duration;
import java.time.Instant;

@Aspect
public class ScanningMonitor {

	private static final Logger logger = LogManager.getLogger(ScanningMonitor.class);

	@Around("execution(* self.me.mp.api.service.ComicScanningService.scanFile(..))")
	public Object logScanComicBookPage(@NotNull ProceedingJoinPoint jp) throws Throwable {
		Instant start = Instant.now();
		Object result = jp.proceed();
		Instant end = Instant.now();
		logger.info("Scanning Comic Book page took: {}ms", Duration.between(start, end).toMillis());
		return result;
	}
}
