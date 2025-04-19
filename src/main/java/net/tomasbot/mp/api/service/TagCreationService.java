package net.tomasbot.mp.api.service;

import static org.springframework.beans.factory.config.ConfigurableBeanFactory.SCOPE_SINGLETON;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import net.tomasbot.mp.db.TagRepository;
import net.tomasbot.mp.model.Tag;
import org.apache.logging.log4j.*;
import org.jetbrains.annotations.NotNull;
import org.springframework.context.annotation.Scope;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
@Scope(value = SCOPE_SINGLETON)
public class TagCreationService {

  private static final Logger logger = LogManager.getLogger(TagCreationService.class);

  private static final int MIN_TAG_LEN = 3;
  private static final String ALLOWABLE_CHARS = "a-zA-Z0-9' ";

  private final TagRepository tagRepository;

  public TagCreationService(TagRepository tagRepository) {
    this.tagRepository = tagRepository;
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

  @Transactional(
      isolation = Isolation.READ_COMMITTED,
      propagation = Propagation.REQUIRED,
      readOnly = true)
  public synchronized Optional<Tag> fetchTagByName(@NotNull String name) {
    logger.trace("Getting tag with name: {}", name);
    return tagRepository.findByNameIgnoreCase(name);
  }

  @Transactional(isolation = Isolation.SERIALIZABLE, propagation = Propagation.REQUIRED)
  public synchronized Tag addNewTag(@NotNull String name) {
    logger.info("Adding new Tag with name: {}", name);

    String normalized = normalizeTagName(name);
    Tag tag = new Tag(normalized);
    return tagRepository.saveAndFlush(tag);
  }

  @Scheduled(fixedRate = 3, timeUnit = TimeUnit.HOURS)
  public synchronized void updateTagRefCounts() {
    logger.info("Updating tag reference counts...");
    tagRepository.findAll().forEach(this::updateTagRefCount);
  }

  public void updateTagRefCount(@NotNull Tag tag) {
    final String name = tag.getName();
    final int refCount = tag.getReferenceCount();
    final int total = getActualRefCount(tag);

    tag.setReferenceCount(total);
    tagRepository.save(tag);

    logger.info("Tag: [{}] is used {} times", name, total);
    if (refCount != total)
      logger.warn(
          "Tag: [{}] reference count mismatch: was {}, should be {}; corrected",
          name,
          refCount,
          total);
  }

  private int getActualRefCount(Tag tag) {
    int videoReferenceCount = tagRepository.findVideoReferenceCount(tag);
    int imageReferenceCount = tagRepository.findImageReferenceCount(tag);
    int imageSetReferenceCount = tagRepository.findImageSetReferenceCount(tag);

    return videoReferenceCount + imageReferenceCount + imageSetReferenceCount;
  }

  public List<Tag> findTagsStartingWith(String name) {
    return tagRepository.findTagsByNameStartingWith(name + "%");
  }
}
