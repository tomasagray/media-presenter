package net.tomasbot.mp.api.controller;

import static net.tomasbot.mp.api.resource.PictureResource.PictureResourceModeller;
import static net.tomasbot.mp.api.resource.VideoResource.VideoResourceModeller;

import net.tomasbot.mp.api.resource.ComicBookResource;
import net.tomasbot.mp.api.resource.ComicBookResource.ComicBookModeller;
import net.tomasbot.mp.api.resource.PictureResource;
import net.tomasbot.mp.api.resource.VideoResource;
import net.tomasbot.mp.api.service.SearchService;
import net.tomasbot.mp.model.SearchAllResult;
import net.tomasbot.mp.model.SearchResults;
import net.tomasbot.mp.user.UserComicBookView;
import net.tomasbot.mp.user.UserImageView;
import net.tomasbot.mp.user.UserVideoView;
import org.jetbrains.annotations.NotNull;
import org.springframework.data.domain.PageRequest;
import org.springframework.hateoas.CollectionModel;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
@RequestMapping("/search")
public class SearchController {

  private final SearchService searchService;
  private final VideoResourceModeller videoModeller;
  private final PictureResourceModeller pictureModeller;
  private final ComicBookModeller comicModeller;
  private final NavigationLinkModeller navigationLinkModeller;

  public SearchController(
      SearchService searchService,
      VideoResourceModeller videoModeller,
      PictureResourceModeller pictureModeller,
      ComicBookModeller comicModeller,
      NavigationLinkModeller navigationLinkModeller) {
    this.searchService = searchService;
    this.videoModeller = videoModeller;
    this.pictureModeller = pictureModeller;
    this.comicModeller = comicModeller;
    this.navigationLinkModeller = navigationLinkModeller;
  }

  @GetMapping("/all")
  public String searchAllFor(
      @NotNull Model model,
      @RequestParam(value = "q", defaultValue = "") String query,
      @RequestParam(value = "page", defaultValue = "0") int page,
      @RequestParam(value = "limit", defaultValue = "12") int limit) {
    SearchAllResult results = searchService.searchAllFor(query, PageRequest.of(page, limit));
    if (results.isEmpty()) {
      model.addAttribute("query", query);
      return "empty_search";
    }

    SearchResults<UserVideoView> videoResults = results.getVideos();
    SearchResults<UserImageView> pictureResults = results.getPictures();
    SearchResults<UserComicBookView> comicResults = results.getComics();
    CollectionModel<VideoResource> videos =
        videoModeller.toCollectionModel(videoResults.getContent());
    CollectionModel<PictureResource> pictures =
        pictureModeller.toCollectionModel(pictureResults.getContent());
    CollectionModel<ComicBookResource> comics =
        comicModeller.toCollectionModel(comicResults.getContent());

    model.addAttribute("page_title", "Search results: " + query);
    model.addAttribute("videos", videos.getContent());
    model.addAttribute("pictures", pictures.getContent());
    model.addAttribute("comics", comics.getContent());

    if (videoResults.hasNext()) model.addAttribute("more_videos_link", "/search/videos?q=" + query);
    if (pictureResults.hasNext())
      model.addAttribute("more_pictures_link", "/search/pictures?q=" + query);
    if (comicResults.hasNext()) model.addAttribute("more_comics_link", "/search/comics?q=" + query);

    return "home";
  }

  @GetMapping("/videos")
  public String searchVideosFor(
      @NotNull Model model,
      @RequestParam(value = "q", defaultValue = "") String query,
      @RequestParam(value = "page", defaultValue = "0") int page,
      @RequestParam(value = "limit", defaultValue = "12") int limit) {
    SearchAllResult results = searchService.searchVideosFor(query, PageRequest.of(page, limit));
    if (results.isEmpty()) {
      model.addAttribute("query", query);
      return "empty_search";
    }

    SearchResults<UserVideoView> videos = results.getVideos();
    CollectionModel<VideoResource> videoModel =
        videoModeller.toCollectionModel(videos.getContent());

    model.addAttribute("page_title", "Videos: " + query);
    model.addAttribute("videos", videoModel);
    navigationLinkModeller.addPagingAttributes(model, videos);
    navigationLinkModeller.addSortNavLinks(model, VideoController.LINK_PREFIX);

    return "video/video_list";
  }

  @GetMapping("/pictures")
  public String searchPicturesFor(
      @NotNull Model model,
      @RequestParam(value = "q", defaultValue = "") String query,
      @RequestParam(value = "page", defaultValue = "0") int page,
      @RequestParam(value = "limit", defaultValue = "12") int limit) {
    SearchAllResult results = searchService.searchPicturesFor(query, PageRequest.of(page, limit));
    if (results.isEmpty()) {
      model.addAttribute("query", query);
      return "empty_search";
    }

    SearchResults<UserImageView> pictures = results.getPictures();
    CollectionModel<PictureResource> pictureModel =
        pictureModeller.toCollectionModel(pictures.getContent());

    model.addAttribute("page_title", "Pictures: " + query);
    model.addAttribute("images", pictureModel.getContent());
    navigationLinkModeller.addPagingAttributes(model, pictures);
    navigationLinkModeller.addSortNavLinks(model, PictureController.LINK_PREFIX);

    return "image/image_list";
  }

  @GetMapping("/comics")
  public String searchComicsFor(
      @NotNull Model model,
      @RequestParam(value = "q", defaultValue = "") String query,
      @RequestParam(value = "page", defaultValue = "0") int page,
      @RequestParam(value = "limit", defaultValue = "12") int limit) {
    SearchAllResult results = searchService.searchComicsFor(query, PageRequest.of(page, limit));
    if (results.isEmpty()) {
      model.addAttribute("query", query);
      return "empty_search";
    }

    SearchResults<UserComicBookView> comics = results.getComics();
    CollectionModel<ComicBookResource> comicModel =
        comicModeller.toCollectionModel(comics.getContent());

    model.addAttribute("page_title", "Comic Books: " + query);
    model.addAttribute("comics", comicModel.getContent());
    navigationLinkModeller.addPagingAttributes(model, comics);
    navigationLinkModeller.addSortNavLinks(model, ComicBookController.LINK_PREFIX);

    return "image/image_list";
  }
}
