package net.tomasbot.mp.api.service;

import java.nio.file.Path;
import java.util.*;
import net.tomasbot.mp.db.PerformerRepository;
import net.tomasbot.mp.db.TagRepository;
import net.tomasbot.mp.model.Editable;
import net.tomasbot.mp.model.Performer;
import net.tomasbot.mp.model.Tag;
import org.jetbrains.annotations.NotNull;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class TagService {

  private static final int MIN_TAG_LEN = 3;
  private static final String ALLOWABLE_CHARS = "a-zA-Z0-9' ";

  private final TagRepository tagRepository;
  private final PerformerRepository performerRepository;

  public TagService(TagRepository tagRepository, PerformerRepository performerRepository) {
    this.tagRepository = tagRepository;
    this.performerRepository = performerRepository;
  }

  private static @NotNull String validateTagName(String name) {
    if (name == null || (name = name.trim()).isEmpty())
      throw new IllegalArgumentException("Tag name was empty");
    if (name.length() < MIN_TAG_LEN)
      throw new IllegalArgumentException(
          String.format("Tag must be at least %d chars", MIN_TAG_LEN));
    return name;
  }

  private static @NotNull String normalizeTagName(String name) {
    String normalized = name;
    normalized = validateTagName(normalized);

    // remove punctuation
    normalized = normalized.replaceAll("[^" + ALLOWABLE_CHARS + "]", "");
    // capitalize 1st letter
    normalized = normalized.substring(0, 1).toUpperCase() + normalized.substring(1);

    return validateTagName(normalized);
  }

  public Optional<Tag> fetchTagByName(@NotNull String name) {
    return tagRepository.findByNameIgnoreCase(name);
  }

  public Optional<Performer> fetchPerformerByName(@NotNull String name) {
    return performerRepository.findByNameIgnoreCase(name);
  }

  public synchronized Tag addNewTag(@NotNull String name) {
    String normalized = normalizeTagName(name);
    Tag tag = new Tag(normalized);
    return tagRepository.saveAndFlush(tag);
  }

  public synchronized Performer addNewPerformer(@NotNull String name) {
    String normalized = normalizeTagName(name);
    Performer performer = new Performer(normalized);
    return performerRepository.saveAndFlush(performer);
  }

  public synchronized Tag getOrCreateTag(@NotNull String name) {
    return fetchTagByName(name).orElseGet(() -> addNewTag(name));
  }

  public synchronized Performer getOrCreatePerformer(@NotNull String name) {
    return fetchPerformerByName(name).orElseGet(() -> addNewPerformer(name));
  }

  public synchronized @NotNull List<Tag> getTags(@NotNull Path resolved) {
    final List<Tag> tags = new ArrayList<>();
    int names = resolved.getNameCount() - 1; // skip filename
    for (int i = 0; i < names; i++) {
      String name = resolved.getName(i).toString().trim();
      Tag tag = getOrCreateTag(name);
      tags.add(tag);
    }
    return tags;
  }

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
