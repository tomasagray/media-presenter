package net.tomasbot.mp.api.service;

import static org.springframework.beans.factory.config.ConfigurableBeanFactory.SCOPE_SINGLETON;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import net.tomasbot.mp.model.Editable;
import net.tomasbot.mp.model.Tag;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
@Scope(value = SCOPE_SINGLETON)
public class TagManagementService {

  private static final Logger logger = LogManager.getLogger(TagManagementService.class);
  private static final Map<String, Tag> created = new ConcurrentHashMap<>();
  private static final int MAX_TAG_PREDICT_RESULTS = 5;

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

  @Transactional
  public synchronized Editable update(@NotNull Editable existing, @NotNull Editable update) {
    // update title
    existing.setTitle(update.getTitle());

    Set<Tag> existingTags = existing.getTags();
    Set<Tag> updateTags = update.getTags();
    // update Tags
    Set<Tag> updatedTags = getUpdateTags(existingTags, updateTags);
    removeUpdateTags(existingTags, updateTags);

    existingTags.clear(); // to handle deleted tags
    existingTags.addAll(updatedTags);
    return existing;
  }

  @SuppressWarnings("all")
  private @NotNull Set<Tag> getUpdateTags(
      @NotNull Set<Tag> existingTags, @NotNull Set<Tag> updateTags) {
    Set<Tag> updatedTags = new HashSet<>();

    for (Tag updateTag : updateTags) {
      if (!existingTags.contains(updateTag)) {
        Tag savedTag = getOrCreateTag(updateTag.getName());
        updatedTags.add(savedTag);

        int refCount = savedTag.increaseRefCount();
        logger.trace("Tag: {} added, used {} times", savedTag.getName(), refCount);
      } else {
        updatedTags.add(updateTag);
      }
    }

    return updatedTags;
  }

  private void removeUpdateTags(@NotNull Set<Tag> existingTags, @NotNull Set<Tag> updateTags) {
    for (Tag existingTag : existingTags) {
      if (!updateTags.contains(existingTag)) {
        // tag deleted
        int refCount = existingTag.decreaseRefCount();
        logger.trace("Tag: {} removed, used {} times", existingTag.getName(), refCount);
      }
    }
  }

  public List<Tag> findTagsMatching(String q) {
    if (q == null) return new ArrayList<>();

    final String normalized = q.trim();
    if (normalized.isEmpty()) return new ArrayList<>();

    return tagCreationService.findTagsStartingWith(normalized).stream()
        .limit(MAX_TAG_PREDICT_RESULTS)
        .toList();
  }
}
