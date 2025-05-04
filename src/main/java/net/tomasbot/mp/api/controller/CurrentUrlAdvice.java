package net.tomasbot.mp.api.controller;

import jakarta.servlet.http.HttpServletRequest;
import org.jetbrains.annotations.NotNull;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

@ControllerAdvice("net.tomasbot.mp.api.controller")
public class CurrentUrlAdvice {

    @ModelAttribute("currentUrl")
    public String getCurrentUrl(@NotNull HttpServletRequest request) {
        return request.getRequestURI();
    }
}
