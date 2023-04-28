package self.me.mp.api.controller;

import org.jetbrains.annotations.NotNull;
import org.springframework.core.io.UrlResource;
import org.springframework.data.domain.Page;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import self.me.mp.api.resource.ComicBookResource;
import self.me.mp.api.service.ComicBookService;
import self.me.mp.model.ComicBook;

import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static self.me.mp.api.resource.ComicBookResource.ComicBookModeller;

@Controller
@RequestMapping("/comics")
public class ComicBookController {

	private final ComicBookService comicBookService;
	private final ComicBookModeller modeller;

	public ComicBookController(ComicBookService comicBookService, ComicBookModeller modeller) {
		this.comicBookService = comicBookService;
		this.modeller = modeller;
	}

	@GetMapping({"", "/", "/latest"})
	public String getLatestComics(
			@RequestParam(name = "page", defaultValue = "0") int page,
			@RequestParam(name = "size", defaultValue = "15") int size,
			@NotNull Model model) {
		Page<ComicBook> comics = comicBookService.getLatestComics(page, size);
		setAttributes(model, comics);
		model.addAttribute("page_title", "Latest Comics");
		return "image/image_list";
	}

	private void setAttributes(@NotNull Model model, @NotNull Page<ComicBook> page) {
		List<ComicBookResource> resources = page.get().map(modeller::toModel).toList();
		model.addAttribute("images", resources);
		model.addAttribute("current_page", Math.max(page.getNumber(), 1));
		model.addAttribute("total_pages", page.getTotalPages());
		if (page.hasPrevious()) {
			model.addAttribute("previous_page", page.previousPageable().getPageNumber());
		}
		if (page.hasNext()) {
			model.addAttribute("next_page", page.nextPageable().getPageNumber());
		}
	}

	@GetMapping(
			value = "/page/{pageId}/data",
			produces = {MediaType.IMAGE_JPEG_VALUE, MediaType.IMAGE_PNG_VALUE})
	@ResponseBody
	public UrlResource getPageData(@PathVariable("pageId") UUID pageId) {
		return comicBookService.getPageData(pageId)
				.orElseThrow(() -> new IllegalArgumentException("Comic page not found: " + pageId));
	}

	@GetMapping(value = "/invalid", produces = MediaType.APPLICATION_JSON_VALUE)
	@ResponseBody
	public Map<String, Path> getInvalidFiles() {
		return comicBookService.getInvalidFiles();
	}
}
