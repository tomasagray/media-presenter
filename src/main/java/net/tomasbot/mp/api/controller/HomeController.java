package net.tomasbot.mp.api.controller;

import java.util.Collection;
import java.util.List;
import net.tomasbot.mp.api.resource.ComicBookResource;
import net.tomasbot.mp.api.resource.ComicBookResource.ComicBookModeller;
import net.tomasbot.mp.api.resource.PictureResource;
import net.tomasbot.mp.api.resource.PictureResource.PictureResourceModeller;
import net.tomasbot.mp.api.resource.VideoResource;
import net.tomasbot.mp.api.resource.VideoResource.VideoResourceModeller;
import net.tomasbot.mp.api.service.user.UserComicService;
import net.tomasbot.mp.api.service.user.UserPictureService;
import net.tomasbot.mp.api.service.user.UserVideoService;
import net.tomasbot.mp.user.UserComicBookView;
import net.tomasbot.mp.user.UserImageView;
import net.tomasbot.mp.user.UserVideoView;
import org.jetbrains.annotations.NotNull;
import org.springframework.hateoas.CollectionModel;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class HomeController {

  private static final int DEFAULT_ITEM_COUNT = 6;

  private final UserVideoService videoService;
  private final VideoResourceModeller videoModeller;
  private final UserPictureService pictureService;
  private final PictureResourceModeller pictureModeller;
  private final UserComicService comicBookService;
  private final ComicBookModeller comicModeller;

  public HomeController(
      UserVideoService videoService,
      VideoResourceModeller videoModeller,
      UserPictureService pictureService,
      PictureResourceModeller pictureModeller,
      UserComicService comicBookService,
      ComicBookModeller comicModeller) {
    this.videoService = videoService;
    this.videoModeller = videoModeller;
    this.pictureService = pictureService;
    this.pictureModeller = pictureModeller;
    this.comicBookService = comicBookService;
    this.comicModeller = comicModeller;
  }

  @GetMapping({"/", "/home"})
  public String getHomePage(@NotNull Model model) {
    List<UserVideoView> videos = videoService.getRandomUserVideos(DEFAULT_ITEM_COUNT);
    CollectionModel<VideoResource> videoResources = videoModeller.toCollectionModel(videos);
    List<UserImageView> pictures = pictureService.getRandomUserPictures(DEFAULT_ITEM_COUNT);
    CollectionModel<PictureResource> pictureResources = pictureModeller.toCollectionModel(pictures);
    List<UserComicBookView> comics = comicBookService.getRandomUserComics(DEFAULT_ITEM_COUNT);
    CollectionModel<ComicBookResource> comicResources = comicModeller.toCollectionModel(comics);
    model.addAttribute("videos", videoResources.getContent());
    model.addAttribute("pictures", pictureResources.getContent());
    model.addAttribute("comics", comicResources.getContent());
    model.addAttribute("page_title", "Home");
    return "home";
  }

  @GetMapping("/favorites")
  public String getFavorites(@NotNull Model model) {
    Collection<UserVideoView> videos = videoService.getVideoFavorites();
    CollectionModel<VideoResource> videoResources = videoModeller.toCollectionModel(videos);
    Collection<UserImageView> pictures = pictureService.getFavoritePictures();
    CollectionModel<PictureResource> pictureResources = pictureModeller.toCollectionModel(pictures);
    Collection<UserComicBookView> comics = comicBookService.getFavoriteComics();
    CollectionModel<ComicBookResource> comicResources = comicModeller.toCollectionModel(comics);
    model.addAttribute("videos", videoResources.getContent());
    model.addAttribute("pictures", pictureResources.getContent());
    model.addAttribute("comics", comicResources.getContent());
    model.addAttribute("page_title", "Favorites");
    return "home";
  }
}
