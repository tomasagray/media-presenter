package net.tomasbot.mp.api.service;

import net.tomasbot.mp.model.Tag;
import org.jetbrains.annotations.NotNull;
import org.springframework.stereotype.Service;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import static net.tomasbot.mp.api.service.TagCreationService.MIN_TAG_LEN;

@Service
public class PathTagService {

  private final TagManagementService tagService;

  public PathTagService(TagManagementService tagService) {
    this.tagService = tagService;
  }

  public synchronized @NotNull List<Tag> getTagsFrom(@NotNull Path resolved) {
    final List<Tag> tags = new ArrayList<>();
    int names = resolved.getNameCount() - 1; // skip filename

    for (int i = 0; i < names; i++) {
      String name = resolved.getName(i).toString().trim();

      if (name.length() >= MIN_TAG_LEN) {
        Tag tag = tagService.getOrCreateTag(name);
        tags.add(tag);
      }
    }

    return tags;
  }
}
