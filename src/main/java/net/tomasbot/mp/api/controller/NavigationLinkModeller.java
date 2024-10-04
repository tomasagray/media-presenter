package net.tomasbot.mp.api.controller;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Component;
import org.springframework.ui.Model;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

@Component
public class NavigationLinkModeller {

  private static @NotNull String getPageUrl(int pageNum) {
    // TODO: SECURITY RISK!!! Find a better way to do this!
    // see: https://devhub.checkmarx.com/cve-details/CVE-2024-22243/
    return ServletUriComponentsBuilder.fromCurrentRequest()
        .replaceQueryParam("page", pageNum)
        .build()
        .toUriString();
  }

  public void addSortNavLinks(@NotNull Model model, @NotNull String prefix) {
    model.addAttribute("is_sortable", true);
    model.addAttribute("latest_link", prefix + "/latest");
    model.addAttribute("random_link", prefix + "/random");
    model.addAttribute("fav_link", prefix + "/favorites");
  }

  public void addPagingAttributes(@NotNull Model model, @Nullable Page<?> page) {
    int totalPages;
    if (page == null || (totalPages = page.getTotalPages()) == 0) {
      model.addAttribute("is_paged", false);
      return;
    }

    model.addAttribute("is_paged", true);
    model.addAttribute("current_page", page.getNumber() + 1);
    model.addAttribute("total_pages", totalPages);
    if (page.hasPrevious()) {
      int previousPage = page.previousPageable().getPageNumber();
      String pageUrl = getPageUrl(previousPage);
      model.addAttribute("prev_page_url", pageUrl);
    }
    if (page.hasNext()) {
      int nextPage = page.nextPageable().getPageNumber();
      String pageUrl = getPageUrl(nextPage);
      model.addAttribute("next_page_url", pageUrl);
    }
  }
}
