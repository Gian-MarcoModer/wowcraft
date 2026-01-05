# WowCraft HUD Texture Guide

This guide explains the texture files you can create to further enhance the HUD visuals.

## Current Implementation

The HUD now features:
- **Gradient-filled bars** with lighter top and darker bottom for depth
- **Beveled borders** with highlight/shadow for 3D effect
- **Glossy highlights** on the top edge of filled bars
- **Glowing combo points** with outer glow effect for active points
- **Enhanced combo points** that are 14x14 pixels with 6px spacing

All of these improvements are done programmatically and work without textures!

## Optional Texture Enhancements

If you want to add even more visual flair, you can create these texture files:

### 1. Player Frame Decorative Border
**Path:** `src/main/resources/assets/wowcraft/textures/gui/hud/player_frame.png`
**Size:** 170x90 pixels (allows 10px margin around the frame)
**Description:** Decorative frame border with WoW-style ornate corners and edges
**Tips:**
- Use golden/bronze colors to match WoW's aesthetic
- Include corner ornaments and edge decorations
- Make the center transparent so bars show through
- Use metallic gradients for depth

### 2. Combo Point Icon
**Path:** `src/main/resources/assets/wowcraft/textures/gui/hud/combo_point.png`
**Size:** 16x16 pixels (to allow some padding around the 14px actual size)
**Description:** Circular or diamond-shaped combo point indicator
**Tips:**
- Design for both active (yellow/gold) and inactive (gray) states
- Or create two separate files: `combo_point_active.png` and `combo_point_inactive.png`
- Add subtle inner glow or gem-like appearance
- Can be circular, diamond, or star-shaped

### 3. Bar Background Texture (Optional)
**Path:** `src/main/resources/assets/wowcraft/textures/gui/hud/bar_background.png`
**Size:** 150x16 pixels (matches BAR_WIDTH x BAR_HEIGHT)
**Description:** Textured background for health/resource bars
**Tips:**
- Dark, subtle pattern (stone, metal, etc.)
- Semi-transparent to allow gradient fill to show
- Add edge highlights for inset appearance

## How to Create Textures

### Option 1: AI Image Generation
Use tools like:
- DALL-E, Midjourney, or Stable Diffusion
- Prompt example: "WoW style UI frame border, golden ornate corners, transparent center, game UI asset, 170x90 pixels"

### Option 2: Image Editing Software
- **GIMP** (free): Great for creating simple geometric shapes and gradients
- **Photoshop/Affinity Photo**: Professional tools for detailed artwork
- **Aseprite**: Excellent for pixel art style textures

### Option 3: Extract from WoW
- Use WoW model/texture extractors (for personal use only)
- Adapt existing WoW UI elements
- **Note:** Only for personal use, not for distribution

## Current Visual Features (No Textures Needed!)

The HUD already looks great with the following programmatic enhancements:

1. **Gradient Bars**
   - Health bar: Green with gradient (red when low)
   - Resource bar: Blue/Red/Yellow with gradient based on class
   - XP bar: Purple gradient
   - All bars have lighter top, darker bottom

2. **3D Borders**
   - Light gray highlights on top/left edges
   - Dark gray shadows on bottom/right edges
   - Creates raised/beveled appearance

3. **Glossy Effect**
   - Thin white highlight on top edge of bar fill
   - Semi-transparent for subtle shine

4. **Combo Points**
   - Active: Bright yellow with gradient + outer glow
   - Inactive: Dark gray with subtle border
   - 14x14 pixels, nicely spaced

## Testing Your Textures

1. Place your PNG files in the correct paths listed above
2. The code will automatically try to load them
3. If textures aren't found, the HUD falls back to the programmatic rendering
4. No code changes needed - just add the PNG files!

## Recommended Colors (WoW Classic Style)

- **Frame borders:** #D4AF37 (gold), #8B7355 (bronze)
- **Health bar:** #22AA22 (green), #CC2222 (low health red)
- **Mana bar:** #3366FF (blue)
- **Rage bar:** #CC3333 (red)
- **Energy bar:** #FFFF00 (yellow)
- **XP bar:** #9933FF (purple)
- **Combo points active:** #FFF569 (yellow)
- **Combo points inactive:** #3A3A3A (dark gray)

Enjoy your enhanced WoW-style HUD!
