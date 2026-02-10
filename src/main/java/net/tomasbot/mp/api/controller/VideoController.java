package net.tomasbot.mp.api.controller;

import java.io.IOException;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import net.tomasbot.mp.api.resource.VideoResource;
import net.tomasbot.mp.api.service.user.UserVideoService;
import net.tomasbot.mp.user.UserVideoView;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.springframework.core.io.UrlResource;
import org.springframework.core.io.support.ResourceRegion;
import org.springframework.data.domain.Page;
import org.springframework.hateoas.CollectionModel;
import org.springframework.http.*;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

@Controller
@RequestMapping("/videos")
public class VideoController {

  static final String LINK_PREFIX = "/videos";

  private final UserVideoService videoService;
  private final VideoResource.VideoResourceModeller modeller;
  private final NavigationLinkModeller navigationLinkModeller;

  public VideoController(
      UserVideoService videoService,
      VideoResource.VideoResourceModeller modeller,
      NavigationLinkModeller navigationLinkModeller) {
    this.videoService = videoService;
    this.modeller = modeller;
    this.navigationLinkModeller = navigationLinkModeller;
  }

  @GetMapping({"", "/", "/latest"})
  public String getLatestVideos(
      @RequestParam(name = "page", defaultValue = "0") int page,
      @RequestParam(name = "pageSize", defaultValue = "15") int pageSize,
      @NotNull Model model) {
    final Page<VideoResource> videoPage =
        videoService.getLatestUserVideos(page, pageSize).map(modeller::toModel);

    model.addAttribute("page_title", "Latest Videos");
    model.addAttribute("videos", videoPage.getContent());
    navigationLinkModeller.addPagingAttributes(model, videoPage);
    navigationLinkModeller.addSortNavLinks(model, LINK_PREFIX);

    return "video/video_list";
  }

  @GetMapping(value = "/all", produces = MediaType.APPLICATION_JSON_VALUE)
  public CollectionModel<VideoResource> getAllVideosPaged(
      @RequestParam(name = "page", defaultValue = "0") int page,
      @RequestParam(name = "size", defaultValue = "15") int size) {
    Page<UserVideoView> videos = videoService.getAllUserVideos(page, size);
    return modeller.toCollectionModel(videos);
  }

  @GetMapping(value = "/random")
  public String getRandomVideos(
      @RequestParam(name = "count", defaultValue = "15") int count, @NotNull Model model) {
    List<UserVideoView> videos = videoService.getRandomUserVideos();
    CollectionModel<VideoResource> resources = modeller.toCollectionModel(videos);

    model.addAttribute("page_title", "Videos");
    model.addAttribute("videos", resources);
    navigationLinkModeller.addSortNavLinks(model, LINK_PREFIX);
    model.addAttribute("more_link", LINK_PREFIX + "/random");

    return "video/video_list";
  }

  @GetMapping(value = "/favorites")
  public String getFavoriteVideos(@NotNull Model model) {
    Collection<UserVideoView> favorites = videoService.getVideoFavorites();
    CollectionModel<VideoResource> resources = modeller.toCollectionModel(favorites);

    model.addAttribute("page_title", "Favorite Videos");
    model.addAttribute("videos", resources);
    navigationLinkModeller.addSortNavLinks(model, LINK_PREFIX);

    return "video/video_list";
  }

  @GetMapping("/video/{videoId}")
  @ResponseBody
  public VideoResource getVideo(@PathVariable("videoId") UUID videoId) {
    return videoService
        .getUserVideo(videoId)
        .map(modeller::toModel)
        .orElseThrow(() -> new IllegalArgumentException("No video with ID: " + videoId));
  }

  @GetMapping("/video/{videoId}/data")
  public ResponseEntity<ResourceRegion> getVideoData(
      @PathVariable("videoId") UUID videoId, @RequestHeader HttpHeaders headers)
      throws IOException {
    UrlResource video = videoService.getVideoData(videoId);
    ResourceRegion region = getResourceRegion(video, headers);
    return ResponseEntity.status(HttpStatus.PARTIAL_CONTENT)
        .contentType(
            MediaTypeFactory.getMediaType(video).orElse(MediaType.APPLICATION_OCTET_STREAM))
        .body(region);
  }

  @Contract("_, _ -> new")
  private @NotNull ResourceRegion getResourceRegion(
      @NotNull UrlResource video, @NotNull HttpHeaders headers) throws IOException {
    long length = video.contentLength();
    Optional<HttpRange> rangeOptional = headers.getRange().stream().findFirst();
    if (rangeOptional.isPresent()) {
      HttpRange range = rangeOptional.get();
      long start = range.getRangeStart(length);
      long end = range.getRangeEnd(length);
      return new ResourceRegion(video, start, calculateRange(end - start + 1));
    } else {
      return new ResourceRegion(video, 0, calculateRange(length));
    }
  }

  private long calculateRange(long contentLength) {
    return Math.min(1024 * 1024, contentLength);
  }

  @GetMapping(value = "/video/{videoId}/thumb/{thumbId}", produces = MediaType.IMAGE_JPEG_VALUE)
  public ResponseEntity<UrlResource> getThumbnail(
      @PathVariable("videoId") UUID videoId, @PathVariable("thumbId") UUID thumbId) {
    UrlResource thumb = videoService.getVideoThumb(videoId, thumbId);
    return ResponseEntity.ok(thumb);
  }

  @PatchMapping(value = "/video/{videoId}/favorite", produces = MediaType.APPLICATION_JSON_VALUE)
  @ResponseBody
  public VideoResource toggleVideoFavorite(@PathVariable UUID videoId) {
    UserVideoView video = videoService.toggleVideoFavorite(videoId);
    return modeller.toModel(video);
  }

  @PatchMapping("/video/update")
  @ResponseBody
  public VideoResource updateVideo(@RequestBody VideoResource videoResource) {
    UserVideoView videoView = modeller.fromModel(videoResource);
    UserVideoView updatedView = videoService.updateVideo(videoView);
    return modeller.toModel(updatedView);
  }
}
