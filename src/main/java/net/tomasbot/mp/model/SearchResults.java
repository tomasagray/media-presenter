package net.tomasbot.mp.model;

import java.util.List;
import lombok.Getter;
import lombok.Setter;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

@Getter
@Setter
public class SearchResults<T> extends PageImpl<T> {

  public SearchResults(List<T> content, Pageable pageable, long total) {
    super(content, pageable, total);
  }

  public SearchResults(List<T> content) {
    super(content);
  }

  public boolean isEmpty() {
    return getContent().isEmpty();
  }
}
