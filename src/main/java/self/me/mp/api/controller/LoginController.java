package self.me.mp.api.controller;

import org.jetbrains.annotations.NotNull;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

@Controller
public class LoginController {

	@GetMapping("/login")
	public String getLoginForm(
			@RequestParam(value = "error", defaultValue = "false") boolean isError,
			@NotNull Model model) {
		model.addAttribute("isError", isError);
		return "login/login";
	}

	@PostMapping("/request_login")
	@ResponseBody
	public String requestLogin() {
		return "logging in";
	}
}
