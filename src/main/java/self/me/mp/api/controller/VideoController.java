package self.me.mp.api.controller;

import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.springframework.core.io.UrlResource;
import org.springframework.core.io.support.ResourceRegion;
import org.springframework.data.domain.Page;
import org.springframework.http.*;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;
import self.me.mp.api.resource.VideoResource;
import self.me.mp.api.service.VideoService;

import java.io.IOException;
import java.util.Optional;
import java.util.UUID;

@Controller
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

	@GetMapping("/videos/latest")
	public String getLatestVideos(
			@RequestParam(name = "page", defaultValue = "0") int page,
			@RequestParam(name = "pageSize", defaultValue = "16") int pageSize,
			@NotNull Model model) {
		final Page<VideoResource> videoPage = videoService.fetchLatest(page, pageSize).map(modeller::toModel);
		setVideoAttributes(model, videoPage);
		model.addAttribute("page_title", "Latest Videos");
		return "video_list";
	}

	@GetMapping(value = "/videos/video/{videoId}")
	public VideoResource getVideo(@PathVariable("videoId") UUID videoId) {
		return videoService.fetchById(videoId)
				.map(modeller::toModel)
				.orElseThrow(() -> new IllegalArgumentException("No video with ID: " + videoId));
	}

	@GetMapping("/videos/video/{videoId}/data")
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
}
