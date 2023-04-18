package self.me.mp.api.service;

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

	public Optional<Tag> fetchByName(String name) {
		return tagRepository.findByName(name);
	}

	public Tag addNewTag(String name) {
		return tagRepository.save(new Tag(name));
	}
}
