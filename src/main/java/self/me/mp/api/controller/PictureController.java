package self.me.mp.api.controller;

import org.jetbrains.annotations.NotNull;
import org.springframework.core.io.UrlResource;
import org.springframework.data.domain.Page;
import org.springframework.hateoas.CollectionModel;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import self.me.mp.api.resource.PictureResource;
import self.me.mp.api.service.user.UserPictureService;
import self.me.mp.user.UserImageView;

import java.util.List;
import java.util.UUID;

import static self.me.mp.api.resource.PictureResource.PictureResourceModeller;

@Controller
@RequestMapping("pictures")
public class PictureController {

	private final UserPictureService pictureService;
	private final PictureResourceModeller modeller;

	public PictureController(UserPictureService pictureService, PictureResourceModeller modeller) {
		this.pictureService = pictureService;
		this.modeller = modeller;
	}

	@GetMapping({"", "/", "/latest"})
	public String getLatestPictures(
			@RequestParam(name = "page", defaultValue = "0") int page,
			@RequestParam(name = "size", defaultValue = "15") int size,
			@NotNull Model model) {

		Page<UserImageView> pictures = pictureService.getLatestUserPictures(page, size);
		setAttributes(model, pictures);
		model.addAttribute("page_title", "Latest pictures");
		return "image/image_list";
	}

	private void setAttributes(@NotNull Model model, @NotNull Page<UserImageView> pictures) {
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
	public String getRandomPictures(
			@RequestParam(name = "size", defaultValue = "15") int size,
			@NotNull Model model) {
		List<UserImageView> pictures = pictureService.getRandomUserPictures(size);
		CollectionModel<PictureResource> resources = modeller.toCollectionModel(pictures);
		model.addAttribute("images", resources);
		model.addAttribute("page_title", "Pictures");
		return "image/image_list";
	}

	@GetMapping(value = "/picture/{picId}", produces = MediaType.APPLICATION_JSON_VALUE)
	public PictureResource getPicture(@PathVariable("picId") UUID picId) {
		return pictureService.getUserPicture(picId)
				.map(modeller::toModel)
				.orElseThrow(() -> new IllegalArgumentException("Picture not found: " + picId));
	}

	@GetMapping(value = "/picture/{picId}/data", produces = {MediaType.IMAGE_JPEG_VALUE, MediaType.IMAGE_PNG_VALUE})
	@ResponseBody
	public UrlResource getPictureData(@PathVariable("picId") UUID picId) {
		return pictureService.getPictureData(picId)
				.orElseThrow(() -> new IllegalArgumentException("Picture not found: " + picId));
	}


	@PatchMapping(value = "/picture/{picId}/favorite", produces = MediaType.APPLICATION_JSON_VALUE)
	@ResponseBody
	public PictureResource togglePictureFavorite(@PathVariable UUID picId) {
		UserImageView picture = pictureService.toggleIsPictureFavorite(picId);
		return modeller.toModel(picture);
	}
}
