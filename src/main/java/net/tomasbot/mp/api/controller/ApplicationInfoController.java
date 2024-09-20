package net.tomasbot.mp.api.controller;

import static net.tomasbot.mp.config.ApiConfig.API_ROOT;

import net.tomasbot.mp.api.service.ApplicationInfoService;
import net.tomasbot.mp.api.service.ApplicationInfoService.ApplicationInfo;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class ApplicationInfoController {

  private final ApplicationInfoService infoService;

  public ApplicationInfoController(ApplicationInfoService infoService) {
    this.infoService = infoService;
  }

  @GetMapping(value = API_ROOT + "/app-info", produces = MediaType.APPLICATION_JSON_VALUE)
  @ResponseBody
  public ApplicationInfo getAppInfo() {
    return infoService.getApplicationInfo();
  }
}
