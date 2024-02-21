package self.me.mp.api.controller;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.ResponseStatus;

import java.io.FileNotFoundException;
import java.net.MalformedURLException;
import java.util.NoSuchElementException;

@ControllerAdvice
public class _GlobalExceptionHandler {

    private static final Logger logger = LogManager.getLogger("GlobalExceptionHandler");

    private static String handleError(@NotNull Throwable e) {
        final String message = e.getMessage();
        logger.error(message);
        return message;
    }

    @ExceptionHandler(FileNotFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    @ResponseBody
    public String handleFileNotFound(@NotNull FileNotFoundException e) {
        return handleError(e);
    }

    @ExceptionHandler(IllegalArgumentException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    @ResponseBody
    public String handleIllegalArg(@NotNull IllegalArgumentException e) {
        return handleError(e);
    }

    @ExceptionHandler(NoSuchElementException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    @ResponseBody
    public String handleNoElement(@NotNull NoSuchElementException e) {
        return handleError(e);
    }

    @ExceptionHandler(MalformedURLException.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    @ResponseBody
    public String handleBadUrl(@NotNull MalformedURLException e) {
        return handleError(e);
    }
}
