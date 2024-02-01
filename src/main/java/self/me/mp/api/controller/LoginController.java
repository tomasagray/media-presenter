package self.me.mp.api.controller;

import org.jetbrains.annotations.NotNull;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

@Controller
public class LoginController {

	@RequestMapping(value = "/login", method = {RequestMethod.GET, RequestMethod.POST})
	public String getLoginForm(
			@RequestParam(value = "error", defaultValue = "false") boolean isError,
			@NotNull Model model) {
		model.addAttribute("isError", isError);
		model.addAttribute("page_title", "MP: Login");
		return "login/login";
	}

	@PostMapping("/request_login")
	@ResponseBody
	public String requestLogin() {
		return "logging in...";
	}

	@GetMapping("/logout_success")
	public String logout(@NotNull Model model) {
		model.addAttribute("page_title", "Logged out");
		model.addAttribute("login_url", "/login");
		return "login/logout";
	}
}
