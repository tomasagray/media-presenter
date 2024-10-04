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

  private static final String LINK_PREFIX = "/pictures";

  private final UserPictureService pictureService;
  private final PictureResourceModeller modeller;
  private final NavigationLinkModeller navigationLinkModeller;

  public PictureController(
      UserPictureService pictureService,
      PictureResourceModeller modeller,
      NavigationLinkModeller navigationLinkModeller) {
    this.pictureService = pictureService;
    this.modeller = modeller;
    this.navigationLinkModeller = navigationLinkModeller;
  }

  @GetMapping({"", "/", "/latest"})
  public String getLatestPictures(
      @RequestParam(name = "page", defaultValue = "0") int page,
      @RequestParam(name = "size", defaultValue = "15") int size,
      @NotNull Model model) {
    Page<UserImageView> pictures = pictureService.getLatestUserPictures(page, size);
    List<PictureResource> pictureResources = pictures.get().map(modeller::toModel).toList();

    model.addAttribute("page_title", "Latest Pictures");
    model.addAttribute("images", pictureResources);
    navigationLinkModeller.addPagingAttributes(model, pictures);
    navigationLinkModeller.addSortNavLinks(model, LINK_PREFIX);

    return "image/image_list";
  }

  @GetMapping(value = "/random")
  public String getRandomPictures(
      @RequestParam(name = "size", defaultValue = "15") int size, @NotNull Model model) {
    List<UserImageView> pictures = pictureService.getRandomUserPictures(size);
    CollectionModel<PictureResource> resources = modeller.toCollectionModel(pictures);

    model.addAttribute("page_title", "Pictures");
    model.addAttribute("images", resources.getContent());
    navigationLinkModeller.addSortNavLinks(model, LINK_PREFIX);
    model.addAttribute("more_link", LINK_PREFIX + "/random");

    return "image/image_list";
  }

  @GetMapping("/favorites")
  public String getFavoritePictures(@NotNull Model model) {
    Collection<UserImageView> favorites = pictureService.getFavoritePictures();
    CollectionModel<PictureResource> resources = modeller.toCollectionModel(favorites);

    model.addAttribute("page_title", "Favorite Pictures");
    model.addAttribute("images", resources.getContent());
    navigationLinkModeller.addSortNavLinks(model, LINK_PREFIX);

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
