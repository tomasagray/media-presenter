package net.tomasbot.mp.model;

import java.util.Set;

public interface Editable {

  String getTitle();

  void setTitle(String title);

  Set<Tag> getTags();
}
