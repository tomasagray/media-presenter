package net.tomasbot.mp.api.controller;

import static net.tomasbot.mp.config.ApiConfig.API_ROOT;

import net.tomasbot.mp.api.service.user.UserDataService;
import net.tomasbot.mp.model.UserData;
import org.springframework.web.bind.annotation.*;

@RestController
public class UserDataController {

  private final UserDataService userDataService;

  public UserDataController(UserDataService userDataService) {
    this.userDataService = userDataService;
  }

  @GetMapping(value = API_ROOT + "/users/user/{username}")
  @ResponseBody
  public UserData getUserData(@PathVariable String username) {
    return userDataService.getUserData(username);
  }

  @PostMapping(value = API_ROOT + "/users/user/import")
  public UserData importUserData(@RequestBody UserData userData) {
    userDataService.importUserData(userData);
    return userData;
  }
}
