package net.tomasbot.mp.model;

import java.util.Set;
import java.util.UUID;

public interface Editable {

  UUID getId();

  String getTitle();

  void setTitle(String title);

  Set<Tag> getTags();
}
