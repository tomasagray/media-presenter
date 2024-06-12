package net.tomasbot.mp.api.controller;

import static net.tomasbot.mp.api.resource.PictureResource.PictureResourceModeller;
import static net.tomasbot.mp.api.resource.VideoResource.VideoResourceModeller;

import net.tomasbot.mp.api.resource.ComicBookResource;
import net.tomasbot.mp.api.resource.ComicBookResource.ComicBookModeller;
import net.tomasbot.mp.api.resource.PictureResource;
import net.tomasbot.mp.api.resource.VideoResource;
import net.tomasbot.mp.api.service.SearchService;
import net.tomasbot.mp.model.SearchAllResult;
import org.jetbrains.annotations.NotNull;
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

  public SearchController(
      SearchService searchService,
      VideoResourceModeller videoModeller,
      PictureResourceModeller pictureModeller,
      ComicBookModeller comicModeller) {
    this.searchService = searchService;
    this.videoModeller = videoModeller;
    this.pictureModeller = pictureModeller;
    this.comicModeller = comicModeller;
  }

  @GetMapping("/all")
  public String searchFor(
      @NotNull Model model,
      @RequestParam(value = "q", defaultValue = "") String query,
      @RequestParam(value = "offset", defaultValue = "0") int offset,
      @RequestParam(value = "limit", defaultValue = "12") int limit) {

    SearchAllResult results = searchService.searchFor(query, offset, limit);
    if (results.isEmpty()) {
      model.addAttribute("query", query);
      return "empty_search";
    }

    CollectionModel<VideoResource> videos = videoModeller.toCollectionModel(results.getVideos());
    CollectionModel<PictureResource> pictures =
        pictureModeller.toCollectionModel(results.getPictures());
    CollectionModel<ComicBookResource> comics =
        comicModeller.toCollectionModel(results.getComics());

    model.addAttribute("videos", videos);
    model.addAttribute("pictures", pictures);
    model.addAttribute("comics", comics);
    model.addAttribute("page_title", "Search results: " + query);
    model.addAttribute("total_pages", results.getTotalResults());
    return "home";
  }
}
