package self.me.mp.api.service;

import org.jetbrains.annotations.NotNull;
import org.springframework.stereotype.Service;
import self.me.mp.db.TagRepository;
import self.me.mp.model.Tag;

import java.util.List;
import java.util.Optional;

@Service
public class TagService {

	private final TagRepository tagRepository;

	public TagService(TagRepository tagRepository) {
		this.tagRepository = tagRepository;
	}

	public List<Tag> fetchAll() {
		return tagRepository.findAll();
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
}
