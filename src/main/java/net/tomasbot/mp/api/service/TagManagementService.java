package net.tomasbot.mp.api.service;

import static org.springframework.beans.factory.config.ConfigurableBeanFactory.SCOPE_SINGLETON;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import net.tomasbot.mp.model.Editable;
import net.tomasbot.mp.model.Tag;
import org.jetbrains.annotations.NotNull;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
@Scope(value = SCOPE_SINGLETON)
public class TagManagementService {

  private static final Map<String, Tag> created = new ConcurrentHashMap<>();

  private final TagCreationService tagCreationService;

  public TagManagementService(TagCreationService tagCreationService) {
    this.tagCreationService = tagCreationService;
  }

  @Transactional(isolation = Isolation.SERIALIZABLE, propagation = Propagation.REQUIRES_NEW)
  public Tag getOrCreateTag(@NotNull String name) {
    synchronized (created) {
      Tag tag = created.get(name);
      if (tag != null) return tag;

      Tag createdTag =
          tagCreationService
              .fetchTagByName(name)
              .orElseGet(() -> tagCreationService.addNewTag(name));
      created.put(name, createdTag);
      return createdTag;
    }
  }

  @Transactional(isolation = Isolation.SERIALIZABLE, propagation = Propagation.REQUIRES_NEW)
  public synchronized Editable update(@NotNull Editable existing, @NotNull Editable update) {
    existing.setTitle(update.getTitle());

    Set<Tag> existingTags = existing.getTags();
    Set<Tag> updateTags = new HashSet<>();
    for (Tag updateTag : update.getTags()) {
      if (!existingTags.contains(updateTag)) {
        Tag savedTag = getOrCreateTag(updateTag.getName());
        updateTags.add(savedTag);
      } else {
        updateTags.add(updateTag);
      }
    }
    existingTags.clear(); // to handle deleted tags
    existingTags.addAll(updateTags);
    return existing;
  }
}
