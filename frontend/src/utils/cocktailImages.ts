import cocktailPlaceholderAsset from '@/assets/images/cocktail-placeholder.webp';

/**
 * Bundled placeholder, used only as a fallback: cocktails without an image,
 * empty/invalid URLs, missing local files, or a browser image-load failure.
 * The active demo cocktails ship real local photos (see
 * `frontend/public/images/cocktails/`) and therefore never show this.
 */
export const cocktailPlaceholderImage = cocktailPlaceholderAsset;

/** A cocktail has its own image as soon as it exposes a non-empty URL. */
export function hasCocktailSpecificImage(imageUrl?: string): boolean {
  return Boolean(imageUrl?.trim());
}

export function resolveCocktailImageSrc(imageUrl?: string, failed = false): string {
  if (failed || !hasCocktailSpecificImage(imageUrl)) return cocktailPlaceholderImage;
  return imageUrl!.trim();
}

export function cocktailImageAlt(cocktailName?: string): string {
  return `Illustration du cocktail ${cocktailName?.trim() || 'du bar'}`;
}
