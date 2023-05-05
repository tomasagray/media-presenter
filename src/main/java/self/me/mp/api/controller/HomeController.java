package self.me.mp.api.controller;

import org.jetbrains.annotations.NotNull;
import org.springframework.hateoas.CollectionModel;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import self.me.mp.api.resource.ComicBookResource;
import self.me.mp.api.resource.ComicBookResource.ComicBookModeller;
import self.me.mp.api.resource.PictureResource;
import self.me.mp.api.resource.PictureResource.PictureResourceModeller;
import self.me.mp.api.resource.VideoResource;
import self.me.mp.api.resource.VideoResource.VideoResourceModeller;
import self.me.mp.api.service.ComicBookService;
import self.me.mp.api.service.PictureService;
import self.me.mp.api.service.VideoService;
import self.me.mp.model.ComicBook;
import self.me.mp.model.Picture;
import self.me.mp.model.Video;

import java.util.Collection;
import java.util.List;

@Controller
public class HomeController {

	private static final int DEFAULT_ITEM_COUNT = 6;

	private final VideoService videoService;
	private final VideoResourceModeller videoModeller;
	private final PictureService pictureService;
	private final PictureResourceModeller pictureModeller;
	private final ComicBookService comicBookService;
	private final ComicBookModeller comicModeller;

	public HomeController(
			VideoService videoService,
			VideoResourceModeller videoModeller,
			PictureService pictureService,
			PictureResourceModeller pictureModeller,
			ComicBookService comicBookService,
			ComicBookModeller comicModeller) {
		this.videoService = videoService;
		this.videoModeller = videoModeller;
		this.pictureService = pictureService;
		this.pictureModeller = pictureModeller;
		this.comicBookService = comicBookService;
		this.comicModeller = comicModeller;
	}

	@GetMapping({"/", "/home"})
	public String getHomePage(Model model) {
		List<Video> videos = videoService.getRandom(DEFAULT_ITEM_COUNT);
		CollectionModel<VideoResource> videoResources = videoModeller.toCollectionModel(videos);
		List<Picture> pictures = pictureService.getRandomPictures(DEFAULT_ITEM_COUNT);
		CollectionModel<PictureResource> pictureResources =
				pictureModeller.toCollectionModel(pictures);
		List<ComicBook> comics = comicBookService.getRandomComics(DEFAULT_ITEM_COUNT);
		CollectionModel<ComicBookResource> comicResources =
				comicModeller.toCollectionModel(comics);
		model.addAttribute("videos", videoResources);
		model.addAttribute("pictures", pictureResources);
		model.addAttribute("comics", comicResources);
		model.addAttribute("page_title", "Home");
		return "home";
	}

	@GetMapping("/favorites")
	public String getFavorites(@NotNull Model model) {
		Collection<Video> videos = videoService.getVideoFavorites();
		CollectionModel<VideoResource> videoResources = videoModeller.toCollectionModel(videos);
		Collection<Picture> pictures = pictureService.getFavoritePictures();
		CollectionModel<PictureResource> pictureResources = pictureModeller.toCollectionModel(pictures);
		Collection<ComicBook> comics = comicBookService.getFavoriteComics();
		CollectionModel<ComicBookResource> comicResources = comicModeller.toCollectionModel(comics);
		model.addAttribute("videos", videoResources);
		model.addAttribute("pictures", pictureResources);
		model.addAttribute("comics", comicResources);
		model.addAttribute("page_title", "Favorites");
		return "home";
	}
}
