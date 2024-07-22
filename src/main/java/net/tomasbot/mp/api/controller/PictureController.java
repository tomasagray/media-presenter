package net.tomasbot.mp.api.controller;

import static net.tomasbot.mp.api.resource.PictureResource.PictureResourceModeller;

import java.util.Collection;
import java.util.List;
import java.util.UUID;
import net.tomasbot.mp.api.resource.PictureResource;
import net.tomasbot.mp.api.service.user.UserPictureService;
import net.tomasbot.mp.user.UserImageView;
import org.jetbrains.annotations.NotNull;
import org.springframework.core.io.UrlResource;
import org.springframework.data.domain.Page;
import org.springframework.hateoas.CollectionModel;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

@Controller
@RequestMapping("pictures")
public class PictureController {

  private final UserPictureService pictureService;
  private final PictureResourceModeller modeller;

  public PictureController(UserPictureService pictureService, PictureResourceModeller modeller) {
    this.pictureService = pictureService;
    this.modeller = modeller;
  }

  private static void addSortLinks(@NotNull Model model) {
    model.addAttribute("latest_link", "/pictures/latest");
    model.addAttribute("random_link", "/pictures/random");
    model.addAttribute("fav_link", "/pictures/favorites");
  }

  @GetMapping({"", "/", "/latest"})
  public String getLatestPictures(
      @RequestParam(name = "page", defaultValue = "0") int page,
      @RequestParam(name = "size", defaultValue = "15") int size,
      @NotNull Model model) {
    Page<UserImageView> pictures = pictureService.getLatestUserPictures(page, size);
    model.addAttribute("page_title", "Latest pictures");
    setAttributes(model, pictures);
    addSortLinks(model);
    return "image/image_list";
  }

  private void setAttributes(@NotNull Model model, @NotNull Page<UserImageView> pictures) {
    List<PictureResource> resources = pictures.get().map(modeller::toModel).toList();
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
      @RequestParam(name = "size", defaultValue = "15") int size, @NotNull Model model) {
    List<UserImageView> pictures = pictureService.getRandomUserPictures(size);
    CollectionModel<PictureResource> resources = modeller.toCollectionModel(pictures);
    model.addAttribute("images", resources.getContent());
    model.addAttribute("page_title", "Pictures");
    addSortLinks(model);
    return "image/image_list";
  }

  @GetMapping("/favorites")
  public String getFavoritePictures(@NotNull Model model) {
    Collection<UserImageView> favorites = pictureService.getFavoritePictures();
    CollectionModel<PictureResource> resources = modeller.toCollectionModel(favorites);
    model.addAttribute("images", resources.getContent());
    model.addAttribute("page_title", "Favorite Pictures");
    addSortLinks(model);
    return "image/image_list";
  }

  @GetMapping(value = "/picture/{picId}", produces = MediaType.APPLICATION_JSON_VALUE)
  public PictureResource getPicture(@PathVariable("picId") UUID picId) {
    return pictureService
        .getUserPicture(picId)
        .map(modeller::toModel)
        .orElseThrow(() -> new IllegalArgumentException("Picture not found: " + picId));
  }

  @GetMapping(
      value = "/picture/{picId}/data",
      produces = {MediaType.IMAGE_JPEG_VALUE, MediaType.IMAGE_PNG_VALUE})
  @ResponseBody
  public UrlResource getPictureData(@PathVariable("picId") UUID picId) {
    return pictureService
        .getPictureData(picId)
        .orElseThrow(() -> new IllegalArgumentException("Picture not found: " + picId));
  }

  @PatchMapping(value = "/picture/{picId}/favorite", produces = MediaType.APPLICATION_JSON_VALUE)
  @ResponseBody
  public PictureResource togglePictureFavorite(@PathVariable UUID picId) {
    UserImageView picture = pictureService.toggleIsPictureFavorite(picId);
    return modeller.toModel(picture);
  }
}
