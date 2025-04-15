package net.tomasbot.mp.api.service;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import net.tomasbot.mp.model.Tag;
import org.jetbrains.annotations.NotNull;
import org.springframework.stereotype.Service;

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
      Tag tag = tagService.getOrCreateTag(name);
      tags.add(tag);
    }

    return tags;
  }
}
