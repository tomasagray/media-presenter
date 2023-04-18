package self.me.mp.api.controller;

import org.springframework.core.io.UrlResource;
import org.springframework.hateoas.CollectionModel;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import self.me.mp.api.resource.PictureResource;
import self.me.mp.api.service.PictureService;
import self.me.mp.model.Picture;
import self.me.mp.util.JsonParser;

import java.util.List;
import java.util.UUID;

import static self.me.mp.api.resource.PictureResource.PictureResourceModeller;

@RestController
@RequestMapping("pictures")
public class PictureController {

	private final PictureService pictureService;
	private final PictureResourceModeller modeller;

	public PictureController(PictureService pictureService, PictureResourceModeller modeller) {
		this.pictureService = pictureService;
		this.modeller = modeller;
	}

	@GetMapping(value = "/latest", produces = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<CollectionModel<PictureResource>> getLatestPictures(
			@RequestParam(name = "page", defaultValue = "0") int page,
			@RequestParam(name = "size", defaultValue = "16") int size) {
		List<Picture> pictures = pictureService.getLatestPictures(page, size).getContent();
		return ResponseEntity.ok(modeller.toCollectionModel(pictures));
	}

	@GetMapping(value = "/random")
	public ResponseEntity<CollectionModel<PictureResource>> getRandomPictures(
			@RequestParam(name = "size", defaultValue = "16") int size) {
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
