package net.tomasbot.mp.api.resource;

import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public abstract class ImageResource<T> extends EntityResource<ImageResource<T>> {}
