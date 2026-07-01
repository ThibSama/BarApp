import { describe, expect, it } from 'vitest';
import {
  cocktailPlaceholderImage,
  hasCocktailSpecificImage,
  resolveCocktailImageSrc,
} from '@/utils/cocktailImages';

describe('cocktail image resolution', () => {
  it('passes a valid local image path through unchanged', () => {
    const path = '/images/cocktails/mojito.webp';
    expect(hasCocktailSpecificImage(path)).toBe(true);
    expect(resolveCocktailImageSrc(path)).toBe(path);
    expect(resolveCocktailImageSrc('  /images/cocktails/cosmopolitan.webp  ')).toBe(
      '/images/cocktails/cosmopolitan.webp',
    );
  });

  it('falls back to the placeholder for a missing image', () => {
    expect(hasCocktailSpecificImage(undefined)).toBe(false);
    expect(hasCocktailSpecificImage('')).toBe(false);
    expect(resolveCocktailImageSrc(undefined)).toBe(cocktailPlaceholderImage);
    expect(resolveCocktailImageSrc('   ')).toBe(cocktailPlaceholderImage);
  });

  it('falls back to the placeholder when the image failed to load', () => {
    expect(resolveCocktailImageSrc('/images/cocktails/pina-colada.webp', true)).toBe(
      cocktailPlaceholderImage,
    );
  });

  it('does not treat any remote/fake URL specially anymore', () => {
    // The old temporary example.com seed and remote-image marker are gone: a
    // non-empty URL is simply used as-is (the browser onError still falls back).
    expect(resolveCocktailImageSrc('https://example.com/img/cosmopolitan.jpg')).toBe(
      'https://example.com/img/cosmopolitan.jpg',
    );
  });
});
