package self.me.mp.api.service;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.jetbrains.annotations.NotNull;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import self.me.mp.db.PerformerRepository;
import self.me.mp.db.TagRepository;
import self.me.mp.model.Performer;
import self.me.mp.model.Tag;

@Service
@Transactional
public class TagService {

    private final TagRepository tagRepository;
    private final PerformerRepository performerRepository;

    public TagService(TagRepository tagRepository, PerformerRepository performerRepository) {
        this.tagRepository = tagRepository;
        this.performerRepository = performerRepository;
    }

    public Optional<Tag> fetchTagByName(@NotNull String name) {
        return tagRepository.findByName(name);
    }

    public Optional<Performer> fetchPerformerByName(@NotNull String name) {
        return performerRepository.findByName(name);
    }

    public synchronized Tag addNewTag(@NotNull String name) {
        Tag tag = new Tag(name);
        return tagRepository.saveAndFlush(tag);
    }

    public synchronized Performer addNewPerformer(@NotNull String name) {
        Performer performer = new Performer(name);
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
        int names = resolved.getNameCount() - 1;    // skip filename
        for (int i = 0; i < names; i++) {
            String name = resolved.getName(i).toString().trim();
            Tag tag = getOrCreateTag(name);
            tags.add(tag);
        }
        return tags;
    }
}
