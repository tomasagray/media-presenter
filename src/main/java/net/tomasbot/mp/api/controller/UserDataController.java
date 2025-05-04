package net.tomasbot.mp.api.controller;

import net.tomasbot.mp.api.service.user.UserDataService;
import net.tomasbot.mp.model.UserData;
import org.springframework.web.bind.annotation.*;

import static net.tomasbot.mp.config.ApiConfig.API_ROOT;
import static org.springframework.util.MimeTypeUtils.APPLICATION_JSON_VALUE;

@RestController
public class UserDataController {

  private final UserDataService userDataService;

  public UserDataController(UserDataService userDataService) {
    this.userDataService = userDataService;
  }

  @GetMapping(value = API_ROOT + "/users/user/{username}", produces = APPLICATION_JSON_VALUE)
  @ResponseBody
  public UserData getUserData(@PathVariable String username) {
    return userDataService.getUserData(username);
  }

  @PostMapping(
          value = API_ROOT + "/users/import/userdata",
          consumes = APPLICATION_JSON_VALUE,
          produces = APPLICATION_JSON_VALUE)
  public UserData importUserData(@RequestBody UserData userData) {
    userDataService.importUserData(userData);
    return userData;
  }
}
