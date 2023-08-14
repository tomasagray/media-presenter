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
import self.me.mp.api.service.user.UserComicService;
import self.me.mp.api.service.user.UserPictureService;
import self.me.mp.api.service.user.UserVideoService;
import self.me.mp.user.UserComicBookView;
import self.me.mp.user.UserImageView;
import self.me.mp.user.UserVideoView;

import java.util.Collection;
import java.util.List;

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
		CollectionModel<PictureResource> pictureResources =
				pictureModeller.toCollectionModel(pictures);
		List<UserComicBookView> comics = comicBookService.getRandomUserComics(DEFAULT_ITEM_COUNT);
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
		Collection<UserVideoView> videos = videoService.getVideoFavorites();
		CollectionModel<VideoResource> videoResources = videoModeller.toCollectionModel(videos);
		Collection<UserImageView> pictures = pictureService.getFavoritePictures();
		CollectionModel<PictureResource> pictureResources = pictureModeller.toCollectionModel(pictures);
		Collection<UserComicBookView> comics = comicBookService.getFavoriteComics();
		CollectionModel<ComicBookResource> comicResources = comicModeller.toCollectionModel(comics);
		model.addAttribute("videos", videoResources);
		model.addAttribute("pictures", pictureResources);
		model.addAttribute("comics", comicResources);
		model.addAttribute("page_title", "Favorites");
		return "home";
	}
}
