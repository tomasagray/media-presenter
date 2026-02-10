package net.tomasbot.mp.api.controller;

import net.tomasbot.mp.api.resource.ComicBookResource;
import net.tomasbot.mp.api.service.user.UserComicService;
import net.tomasbot.mp.user.UserComicBookView;
import org.jetbrains.annotations.NotNull;
import org.springframework.core.io.UrlResource;
import org.springframework.data.domain.Page;
import org.springframework.hateoas.CollectionModel;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

import static net.tomasbot.mp.api.resource.ComicBookResource.ComicBookModeller;

@Controller
@RequestMapping("/comics")
public class ComicBookController {

  static final String LINK_PREFIX = "/comics";

  private final UserComicService comicBookService;
  private final ComicBookModeller modeller;
  private final NavigationLinkModeller navigationLinkModeller;

  public ComicBookController(
      UserComicService comicBookService,
      ComicBookModeller modeller,
      NavigationLinkModeller navigationLinkModeller) {
    this.comicBookService = comicBookService;
    this.modeller = modeller;
    this.navigationLinkModeller = navigationLinkModeller;
  }

  @GetMapping({"", "/", "/latest"})
  public String getLatestComics(
      @RequestParam(name = "page", defaultValue = "0") int page,
      @RequestParam(name = "size", defaultValue = "15") int size,
      @NotNull Model model) {
    Page<UserComicBookView> comics = comicBookService.getLatestUserComics(page, size);
    List<ComicBookResource> resources = comics.get().map(modeller::toModel).toList();

    model.addAttribute("page_title", "Latest Comics");
    model.addAttribute("comics", resources);
    navigationLinkModeller.addPagingAttributes(model, comics);
    navigationLinkModeller.addSortNavLinks(model, LINK_PREFIX);

    return "image/comic_list";
  }
  
  @GetMapping(value = "/all/paged", produces = MediaType.APPLICATION_JSON_VALUE)
  public CollectionModel<ComicBookResource> getAllComicsPaged(
      @RequestParam(name = "page", defaultValue = "0") int page,
      @RequestParam(name = "size", defaultValue = "15") int size) {
    Page<UserComicBookView> comics = comicBookService.getAllUserComics(page, size);
    return modeller.toCollectionModel(comics);
  }

  @GetMapping(value = "/random", produces = MediaType.APPLICATION_JSON_VALUE)
  public String getRandomComics(
      @RequestParam(name = "count", defaultValue = "15") int count, @NotNull Model model) {
    List<UserComicBookView> comics = comicBookService.getRandomUserComics().stream().limit(count).toList();
    CollectionModel<ComicBookResource> resources = modeller.toCollectionModel(comics);

    model.addAttribute("page_title", "Comic Books");
    model.addAttribute("comics", resources.getContent());
    navigationLinkModeller.addSortNavLinks(model, LINK_PREFIX);
    model.addAttribute("more_link", LINK_PREFIX + "/random");

    return "image/comic_list";
  }

  @GetMapping("/favorites")
  public String getFavoriteComics(@NotNull Model model) {
    Collection<UserComicBookView> favorites = comicBookService.getFavoriteComics();
    CollectionModel<ComicBookResource> resources = modeller.toCollectionModel(favorites);

    model.addAttribute("page_title", "Favorite Comic Books");
    model.addAttribute("comics", resources.getContent());
    navigationLinkModeller.addSortNavLinks(model, LINK_PREFIX);

    return "image/comic_list";
  }

  @GetMapping(value = "/comic/{comicId}", produces = MediaType.APPLICATION_JSON_VALUE)
  public ComicBookResource getComic(@PathVariable UUID comicId) {
    return comicBookService
        .getUserComicBook(comicId)
        .map(modeller::toModel)
        .orElseThrow(() -> new IllegalArgumentException("No Comic Book with ID: " + comicId));
  }

  @GetMapping(
      value = "/comic/page/{pageId}/data",
      produces = {MediaType.IMAGE_JPEG_VALUE, MediaType.IMAGE_PNG_VALUE})
  @ResponseBody
  public UrlResource getPageData(@PathVariable UUID pageId) {
    return comicBookService
        .getPageData(pageId)
        .orElseThrow(() -> new IllegalArgumentException("Comic page not found: " + pageId));
  }

  @PatchMapping(value = "/comic/{comicId}/favorite", produces = MediaType.APPLICATION_JSON_VALUE)
  @ResponseBody
  public ComicBookResource toggleIsComicBookFavorite(@PathVariable UUID comicId) {
    UserComicBookView comic = comicBookService.toggleIsComicFavorite(comicId);
    return modeller.toModel(comic);
  }

  @PatchMapping("/comic/update")
  @ResponseBody
  public ComicBookResource updateComicBook(@RequestBody ComicBookResource comicResource){
    UserComicBookView comicView = modeller.fromModel(comicResource);
    UserComicBookView updated = comicBookService.updateComic(comicView);
    return modeller.toModel(updated);
  }
}
