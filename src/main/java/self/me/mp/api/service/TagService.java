package self.me.mp.api.service;

import org.jetbrains.annotations.NotNull;
import org.springframework.stereotype.Service;
import self.me.mp.db.TagRepository;
import self.me.mp.model.Tag;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
public class TagService {

	private final TagRepository tagRepository;

	public TagService(TagRepository tagRepository) {
		this.tagRepository = tagRepository;
	}

	public Optional<Tag> fetchByName(@NotNull String name) {
		return tagRepository.findByName(name);
	}

	public Tag addNewTag(@NotNull String name) {
		return tagRepository.save(new Tag(name));
	}

	public Tag getOrCreateTag(@NotNull String name) {
		return fetchByName(name).orElseGet(() -> addNewTag(name));
	}

	public @NotNull List<Tag> getTags(@NotNull Path resolved) {
		final List<Tag> tags = new ArrayList<>();
		int names = resolved.getNameCount() - 1;    // skip filename
		for (int i = 0; i < names; i++) {
			String name = resolved.getName(i).toString();
			Tag tag = getOrCreateTag(name);
			tags.add(tag);
		}
		return tags;
	}
}
