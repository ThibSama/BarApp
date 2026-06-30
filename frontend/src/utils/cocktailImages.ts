import temporaryCocktailIllustration from '@/assets/images/cocktail-placeholder.webp';

const temporaryRemoteImageMarkers = ['photo-1544145945-f90425340c7e'];

export const cocktailPlaceholderImage = temporaryCocktailIllustration;

export function hasCocktailSpecificImage(imageUrl?: string): boolean {
  const trimmed = imageUrl?.trim();
  return Boolean(trimmed) && !temporaryRemoteImageMarkers.some((marker) => trimmed?.includes(marker));
}

export function resolveCocktailImageSrc(imageUrl?: string, failed = false): string {
  if (failed || !hasCocktailSpecificImage(imageUrl)) return cocktailPlaceholderImage;
  return imageUrl!.trim();
}

export function cocktailImageAlt(cocktailName?: string): string {
  return `Illustration du cocktail ${cocktailName?.trim() || 'du bar'}`;
}
