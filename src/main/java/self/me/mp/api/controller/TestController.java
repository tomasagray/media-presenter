package self.me.mp.api.controller;

import org.jetbrains.annotations.NotNull;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/testing")
public class TestController {

  @GetMapping("/default")
  public String defaultMsg(@NotNull Model model) {
    model.addAttribute("msg", "Hello world!");
    return "default";
  }
}
