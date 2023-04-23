package self.me.mp.api.controller;

import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;
import self.me.mp.api.service.ApplicationInfoService;
import self.me.mp.api.service.ApplicationInfoService.ApplicationInfo;

@RestController
public class ApplicationInfoController {

	private final ApplicationInfoService infoService;

	public ApplicationInfoController(ApplicationInfoService infoService) {
		this.infoService = infoService;
	}

	@GetMapping(value = "/application/info", produces = MediaType.APPLICATION_JSON_VALUE)
	@ResponseBody
	public ApplicationInfo getAppInfo() {
		return infoService.getApplicationInfo();
	}
}
