package self.me.mp.api.controller;

import java.util.Optional;
import java.util.UUID;
import org.jetbrains.annotations.NotNull;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import self.me.mp.api.service.VideoService;
import self.me.mp.model.Video;

@Controller
public class VideoController {

  private final VideoService videoService;

  public VideoController(VideoService videoService) {
    this.videoService = videoService;
  }

  private static void setVideoAttributes(@NotNull Model model, @NotNull Page<Video> videoPage) {
    model.addAttribute("videos", videoPage.getContent());
    model.addAttribute("current_page", videoPage.getNumber() + 1);
    model.addAttribute("total_pages", videoPage.getTotalPages());
    if (videoPage.hasPrevious()) {
      model.addAttribute("prev_page", videoPage.previousPageable().getPageNumber());
    }
    if (videoPage.hasNext()) {
      model.addAttribute("next_page", videoPage.nextPageable().getPageNumber());
    }
  }

  @GetMapping("/videos/latest")
  public String getLatestVideos(
      @RequestParam(name = "page", defaultValue = "0") int page,
      @RequestParam(name = "pageSize", defaultValue = "16") int pageSize,
      @NotNull Model model) {

    final Page<Video> videoPage = videoService.fetchLatest(page, pageSize);
    setVideoAttributes(model, videoPage);
    model.addAttribute("page_title", "Latest Videos");
    return "latest";
  }

  @GetMapping(value = "/videos/video/{videoId}")
  public Video getVideo(@PathVariable("videoId") UUID videoId) {
    final Optional<Video> videoOptional = videoService.fetchById(videoId);
    if (videoOptional.isPresent()) {
      return videoOptional.get();
    } else {
      throw new IllegalArgumentException("No video with ID: " + videoId);
    }
  }
}
