package net.tomasbot.mp.api.controller;

import java.util.List;
import net.tomasbot.mp.api.service.TagManagementService;
import net.tomasbot.mp.model.Tag;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

@Controller
@RequestMapping("/tags")
public class TagController {

  private final TagManagementService tagManagementService;

  public TagController(TagManagementService tagManagementService) {
    this.tagManagementService = tagManagementService;
  }

  @GetMapping("/search")
  @ResponseBody
  public List<Tag> findTagsMatching(@RequestParam(value = "q", defaultValue = "") String q) {
      return tagManagementService.findTagsMatching(q);
  }
}
