package self.me.mp.api.controller;

import org.jetbrains.annotations.NotNull;
import org.springframework.core.io.UrlResource;
import org.springframework.data.domain.Page;
import org.springframework.hateoas.CollectionModel;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import self.me.mp.api.resource.PictureResource;
import self.me.mp.api.service.PictureService;
import self.me.mp.model.Picture;
import self.me.mp.util.JsonParser;

import java.util.List;
import java.util.UUID;

import static self.me.mp.api.resource.PictureResource.PictureResourceModeller;

@Controller
@RequestMapping("pictures")
public class PictureController {

	private final PictureService pictureService;
	private final PictureResourceModeller modeller;

	public PictureController(PictureService pictureService, PictureResourceModeller modeller) {
		this.pictureService = pictureService;
		this.modeller = modeller;
	}

	@GetMapping({"", "/", "/latest"})
	public String getLatestPictures(
			@RequestParam(name = "page", defaultValue = "0") int page,
			@RequestParam(name = "size", defaultValue = "15") int size,
			@NotNull Model model) {

		Page<Picture> pictures = pictureService.getLatestPictures(page, size);
		setAttributes(model, pictures);
		model.addAttribute("page_title", "Latest pictures");
		return "image/image_list";
	}

	private void setAttributes(@NotNull Model model, @NotNull Page<Picture> pictures) {
		List<PictureResource> resources =
				pictures
						.get()
						.map(modeller::toModel)
						.toList();
		model.addAttribute("images", resources);
		model.addAttribute("current_page", pictures.getNumber() + 1);
		model.addAttribute("total_pages", pictures.getTotalPages());
		if (pictures.hasNext()) {
			model.addAttribute("next_page", pictures.nextPageable().getPageNumber());
		}
		if (pictures.hasPrevious()) {
			model.addAttribute("prev_page", pictures.previousPageable().getPageNumber());
		}
	}

	@GetMapping(value = "/random")
	public ResponseEntity<CollectionModel<PictureResource>> getRandomPictures(
			@RequestParam(name = "size", defaultValue = "15") int size) {
		List<Picture> pictures = pictureService.getRandomPictures(size).getContent();
		return ResponseEntity.ok(modeller.toCollectionModel(pictures));
	}

	@GetMapping(value = "/picture/{picId}", produces = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<PictureResource> getPicture(@PathVariable("picId") UUID picId) {
		return pictureService.getPicture(picId)
				.map(modeller::toModel)
				.map(ResponseEntity::ok)
				.orElseThrow(() -> new IllegalArgumentException("Picture not found: " + picId));
	}

	@GetMapping(value = "/picture/{picId}/data", produces = {MediaType.IMAGE_JPEG_VALUE, MediaType.IMAGE_PNG_VALUE})
	@ResponseBody
	public UrlResource getPictureData(@PathVariable("picId") UUID picId) {
		return pictureService.getPictureData(picId)
				.orElseThrow(() -> new IllegalArgumentException("Picture not found: " + picId));
	}


	@GetMapping(value = "/invalid", produces = MediaType.APPLICATION_JSON_VALUE)
	@ResponseBody
	public String getInvalid() {
		return JsonParser.toJson(pictureService.getInvalidFiles());
	}
}