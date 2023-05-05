package self.me.mp.api.controller;

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
import self.me.mp.api.resource.VideoResource;
import self.me.mp.api.service.VideoService;
import self.me.mp.model.Video;

import java.io.IOException;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Controller
@RequestMapping("/videos")
public class VideoController {

	private final VideoService videoService;
	private final VideoResource.VideoResourceModeller modeller;

	public VideoController(VideoService videoService, VideoResource.VideoResourceModeller modeller) {
		this.videoService = videoService;
		this.modeller = modeller;
	}

	private static void setVideoAttributes(@NotNull Model model, @NotNull Page<VideoResource> videoPage) {
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

	@GetMapping({"", "/", "/latest"})
	public String getLatestVideos(
			@RequestParam(name = "page", defaultValue = "0") int page,
			@RequestParam(name = "pageSize", defaultValue = "15") int pageSize,
			@NotNull Model model) {
		final Page<VideoResource> videoPage = videoService.getLatest(page, pageSize).map(modeller::toModel);
		setVideoAttributes(model, videoPage);
		model.addAttribute("page_title", "Latest Videos");
		return "video/video_list";
	}

	@GetMapping(value = "/all", produces = MediaType.APPLICATION_JSON_VALUE)
	public CollectionModel<VideoResource> getAllVideosPaged(
			@RequestParam(name = "page", defaultValue = "0") int page,
			@RequestParam(name = "size", defaultValue = "15") int size) {
		Page<Video> videos = videoService.getAll(page, size);
		return modeller.toCollectionModel(videos);
	}

	@GetMapping(value = "/random", produces = MediaType.APPLICATION_JSON_VALUE)
	public String getRandomVideos(
			@RequestParam(name = "count", defaultValue = "15") int count,
			@NotNull Model model) {
		List<Video> videos = videoService.getRandom(count);
		CollectionModel<VideoResource> resources = modeller.toCollectionModel(videos);
		model.addAttribute("videos", resources);
		model.addAttribute("page_title", "Videos");
		return "video/video_list";
	}

	@GetMapping("/video/{videoId}")
	@ResponseBody
	public VideoResource getVideo(@PathVariable("videoId") UUID videoId) {
		return videoService.getById(videoId)
				.map(modeller::toModel)
				.orElseThrow(() -> new IllegalArgumentException("No video with ID: " + videoId));
	}

	@GetMapping("/video/{videoId}/data")
	public ResponseEntity<ResourceRegion> getVideoData(
			@PathVariable("videoId") UUID videoId,
			@RequestHeader HttpHeaders headers
	) throws IOException {

		UrlResource video = videoService.getVideoData(videoId);
		ResourceRegion region = getResourceRegion(video, headers);
		return ResponseEntity
				.status(HttpStatus.PARTIAL_CONTENT)
				.contentType(
						MediaTypeFactory
								.getMediaType(video)
								.orElse(MediaType.APPLICATION_OCTET_STREAM))
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
		Video video = videoService.toggleVideoFavorite(videoId);
		return modeller.toModel(video);
	}
}
