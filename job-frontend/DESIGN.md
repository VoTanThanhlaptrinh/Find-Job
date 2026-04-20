# Design System Strategy: High-Key Editorial Experience
 
## 1. Overview & Creative North Star
The Creative North Star for this design system is **"Atmospheric Precision."** 
 
In the high-stakes world of recruitment, the interface must balance the authority of a data-driven tool with the welcoming atmosphere of a premium concierge service. We are moving away from the "industrial" look of traditional SaaS—characterized by rigid grids and heavy borders—toward an editorial, airy layout. By utilizing high-key lighting, intentional asymmetry, and soft glowing overlaps, we create a workspace that feels like a physical office made of frosted glass and soft light. The goal is to provide a sense of "calm productivity" where candidates and recruiters feel invited, not processed.
 
## 2. Colors
Our palette is rooted in the clarity of high-altitude light. We use a bright, energetic Blue 600 primary to drive action, softened by a spectrum of sky-to-lavender transitions.
 
*   **Primary & Gradients:** Use `primary` (#004ac6) as your anchor. For signature elements like Hero headers or primary CTAs, apply a linear gradient from `primary` to `tertiary_container` (#943fe2). This "Visual Soul" moves the UI from a generic utility to a branded experience.
*   **The "No-Line" Rule:** To maintain the premium, high-key aesthetic, **do not use 1px solid borders to section content.** Definitions between functional areas must be achieved through background shifts. For example, a sidebar should use `surface_container_low` against a `surface` background. The eye should perceive the edge via tonal change, not a literal line.
*   **Surface Hierarchy & Nesting:** Treat the UI as layers of fine paper. 
    *   **Level 0 (Base):** `surface` (#f7f9fb).
    *   **Level 1 (Sectioning):** `surface_container_low` or `surface_container`.
    *   **Level 2 (Cards/Focus):** `surface_container_lowest` (#ffffff).
*   **The Glass & Gradient Rule:** For floating elements (modals, dropdowns, or hovering navigation), use a glassmorphic effect. Apply `surface_container_lowest` at 70% opacity with a 24px backdrop blur. This allows the "Sky Blue to Light Purple" glow of the background elements to bleed through, softening the interface.
 
## 3. Typography
The typography is an interplay between the structural precision of **Inter** and the editorial character of **Manrope**.
 
*   **Display & Headlines (Manrope):** Use `display-lg` through `headline-sm` for high-impact moments. Manrope’s geometric yet warm curves provide the "modern" feel required. Keep tracking tight (-2%) for headlines to maintain a bespoke, professional look.
*   **Body & Labels (Inter):** All functional data, candidate bios, and system labels use Inter. It provides the "precise" utility needed for reading resumes and analytics. Use `body-md` for standard text to maintain the "airy" layout.
*   **Hierarchy:** Distinguish importance through scale and weight rather than color. A `headline-md` in `on_surface` is far more premium than a small bold font in a bright color.
 
## 4. Elevation & Depth
In this system, depth is a result of light and stacking, not artificial shadows.
 
*   **Tonal Layering:** Avoid the "floating on a dark void" look. Place `surface_container_lowest` (Pure White) cards on a `surface_container_low` (Pale Blue-Grey) background. This creates a natural, soft "lift."
*   **Ambient Shadows:** When a physical shadow is required (e.g., a dragged candidate card), use a shadow tinted with `primary` at 5% opacity. The blur should be high (30px+) and the spread low to mimic natural ambient light.
*   **The "Ghost Border":** If a boundary is strictly required for accessibility, use `outline_variant` at 15% opacity. It should feel like a suggestion of an edge, not a container.
*   **Soft Glowing Overlaps:** Use the `tertiary_fixed` (#f0dbff) and `primary_fixed` (#dbe1ff) colors as large, low-opacity background "blobs" (blurs of 150px+). Overlaying these with glassmorphic containers creates the signature "glowing" recruiter portal look.
 
## 5. Components
Each component must feel light, responsive, and integrated.
 
*   **Buttons:**
    *   **Primary:** A gradient from `primary` to `primary_container`. High roundedness (`xl` or `full`). No border.
    *   **Secondary:** `surface_container_highest` background with `on_surface_variant` text.
    *   **Tertiary:** Ghost style. No background, `primary` text, with a subtle `surface_variant` hover state.
*   **Cards:** No dividers. Use 24px–32px of vertical padding (`xl` spacing) to separate content sections. Use `surface_container_lowest` as the card base.
*   **Input Fields:** Eschew the four-sided box. Use a subtle `surface_container_high` background with a 2px bottom-accent in `outline_variant`. On focus, the bottom accent transitions to the `primary` Blue 600.
*   **Chips:** Use `secondary_fixed` for filter chips with `on_secondary_fixed` text. The pill shape (`full`) is mandatory to contrast against the more structured grid.
*   **Recruiter Dashboard Metrics:** Instead of standard bar charts, use soft-edged "pill" charts with `primary` to `tertiary` gradients to visualize candidate pipelines.
 
## 6. Do's and Don'ts
 
**Do:**
*   **Do** embrace white space. If a layout feels crowded, increase the spacing to `xl` (1.5rem) or `lg` (1rem).
*   **Do** use asymmetrical layouts. A slightly wider left column for candidate details creates a more editorial, less "template" feel.
*   **Do** ensure all text on gradients meets WCAG AA contrast standards using the `on_primary` and `on_tertiary` tokens.
 
**Don't:**
*   **Don't** use black (#000000). Use `on_surface` (#191c1e) for maximum readability without the harshness of true black.
*   **Don't** use 1px grey dividers between list items. Use 8px–12px of white space or a subtle background toggle between `surface` and `surface_container_low`.
*   **Don't** use sharp corners. Always refer to the Roundedness Scale, favoring `md` (0.75rem) for cards and `full` for interactive elements.