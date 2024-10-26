package net.tomasbot.mp.api.controller;

import static net.tomasbot.mp.config.ApiConfig.API_ROOT;

import java.nio.file.Path;
import net.tomasbot.mp.api.service.InvalidFilesService;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Controller;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.*;

@Controller
@RequestMapping(API_ROOT + "/diagnostics/invalid")
public class InvalidFileController {

  public record AllInvalidFiles(
      MultiValueMap<String, Path> invalidVideos,
      MultiValueMap<String, Path> invalidPictures,
      MultiValueMap<String, Path> invalidComics) {}

  private final InvalidFilesService invalidFilesService;

  public InvalidFileController(InvalidFilesService invalidFilesService) {
    this.invalidFilesService = invalidFilesService;
  }

  @GetMapping(value = "/all", produces = MediaType.APPLICATION_JSON_VALUE)
  @ResponseBody
  public AllInvalidFiles getAllInvalidFiles() {
    MultiValueMap<String, Path> invalidVideos = invalidFilesService.getInvalidVideoFiles();
    MultiValueMap<String, Path> invalidPictures = invalidFilesService.getInvalidPictureFiles();
    MultiValueMap<String, Path> invalidComics = invalidFilesService.getInvalidComicBookFiles();

    return new AllInvalidFiles(invalidVideos, invalidPictures, invalidComics);
  }

  @GetMapping(value = "/video", produces = MediaType.APPLICATION_JSON_VALUE)
  @ResponseBody
  public MultiValueMap<String, Path> getInvalidVideoFiles() {
    return invalidFilesService.getInvalidVideoFiles();
  }

  @GetMapping(value = "/picture", produces = MediaType.APPLICATION_JSON_VALUE)
  @ResponseBody
  public MultiValueMap<String, Path> getInvalidPictureFiles() {
    return invalidFilesService.getInvalidPictureFiles();
  }

  @GetMapping(value = "/comic", produces = MediaType.APPLICATION_JSON_VALUE)
  @ResponseBody
  public MultiValueMap<String, Path> getInvalidComicFiles() {
    return invalidFilesService.getInvalidComicBookFiles();
  }
}
