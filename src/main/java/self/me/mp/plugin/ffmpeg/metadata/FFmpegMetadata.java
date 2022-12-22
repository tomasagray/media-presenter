/*
 * Copyright (c) 2020.
 *
 * This file is part of Matchday.
 *
 * Matchday is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Matchday is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Matchday.  If not, see <http://www.gnu.org/licenses/>.
 */

package self.me.mp.plugin.ffmpeg.metadata;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OneToOne;
import java.util.List;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.ToString;

/** Represents audio/video file metadata returned by FFPROBE */
@Getter
@Setter
@ToString
@RequiredArgsConstructor
@Entity
public class FFmpegMetadata {

  @Id
  @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "ffmpeg_metadata_generator")
  private Long id;

  @OneToOne(cascade = CascadeType.ALL, fetch = FetchType.EAGER)
  private FFmpegFormat format;

  @OneToMany(cascade = CascadeType.ALL, fetch = FetchType.EAGER)
  private List<FFmpegStream> streams;

  @OneToMany(cascade = CascadeType.ALL, fetch = FetchType.EAGER)
  private List<FFmpegChapter> chapters;
}
