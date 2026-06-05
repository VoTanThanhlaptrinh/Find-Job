# Design System: The Luminous Curator

## 1. Overview & Creative North Star
### The Creative North Star: "Luminous Precision"
This design system moves away from the heavy, industrial "dark mode" admin archetypes of the past decade. Our vision is **Luminous Precision**: an atmosphere that feels like a high-end architectural studio—bathed in natural light, hyper-organized, and breathable. 

We break the "template" look by rejecting the rigid grid in favor of **intentional asymmetry**. We use large, editorial white space as a functional tool to reduce cognitive load. The UI is not a box filled with data; it is a curated gallery of insights where the vibrant primary blue acts as a surgical laser, drawing the eye only to the most critical actions and data points.

---

## 2. Colors & Surface Philosophy
The palette is rooted in a "High-Chroma Blue" set against a "Technological White" foundation.

### The "No-Line" Rule
**Strict Mandate:** Traditional 1px solid borders are prohibited for defining sections or containers. Separation must be achieved through **Background Color Shifts** or **Tonal Transitions**. 
*   *Implementation:* A side navigation bar should not have a right-hand border; instead, use `surface-container-low` (#f1efff) for the sidebar against a `surface` (#f8f5ff) main content area.

### Surface Hierarchy & Nesting
Treat the UI as a series of stacked, premium materials. Use the `surface-container` tiers to create depth:
*   **Base Layer:** `surface` (#f8f5ff) — The foundation of the application.
*   **Content Sections:** `surface-container-low` (#f1efff) — Large groupings of information.
*   **Interactive Cards:** `surface-container-lowest` (#ffffff) — Reserved for the highest-priority data cards to make them "pop" against the tinted background.
*   **Overlays/Popovers:** `surface-container-high` (#e0e0ff) — Used for transient elements that need to feel physically closer to the user.

### The "Glass & Gradient" Rule
To inject "soul" into the admin experience:
*   **Signature Textures:** For primary CTAs and Hero sections, use a subtle linear gradient from `primary` (#0050d4) to `primary_container` (#7b9cff) at a 135-degree angle. This prevents the "flat-bootstrap" look.
*   **Glassmorphism:** For floating navigation or top bars, use `surface` (#f8f5ff) at 80% opacity with a `20px` backdrop-blur.

---

## 3. Typography
We pair the technical precision of **Inter** with the editorial authority of **Manrope**.

*   **Display & Headlines (Manrope):** These are your "Anchors." Use `display-lg` and `headline-md` with tight letter-spacing (-0.02em) to create a sophisticated, high-end feel. They should feel like titles in a premium financial journal.
*   **Body & Labels (Inter):** These are your "Workhorses." Inter is used for all data-heavy contexts. Ensure `body-md` is used for primary data to maintain maximum legibility.
*   **The Power Scale:** Use high contrast in size between `headline-lg` (2rem) and `label-sm` (0.6875rem) to create a clear visual narrative without needing bold lines.

---

## 4. Elevation & Depth
In this system, elevation is a product of light and color, not shadow.

*   **The Layering Principle:** Do not use shadows for static layout elements. Stacking `surface-container-lowest` on `surface-container-low` provides enough "soft lift" to be perceived by the eye without cluttering the interface.
*   **Ambient Shadows:** For floating elements (Modals, Dropdowns), use an "Atmospheric Shadow":
    *   `box-shadow: 0 12px 32px -4px rgba(40, 43, 81, 0.08);`
    *   Note the use of `on_surface` (#282b51) as the shadow tint rather than pure black.
*   **The "Ghost Border" Fallback:** If accessibility requirements demand a container edge, use `outline_variant` (#a7aad7) at **15% opacity**. It should be felt, not seen.

---

## 5. Components

### Buttons: The Kinetic Core
*   **Primary:** Gradient of `primary` to `primary_dim`. Roundedness: `md` (0.375rem). No border. Text: `on_primary`.
*   **Secondary:** `surface-container-highest` background with `primary` text. This creates a "soft" interactive area that doesn't compete with the main CTA.
*   **Tertiary:** Ghost style. No background, `primary` text. Use only for low-priority utility actions.

### Cards & Lists: The Curator's Frame
*   **The "No-Divider" Rule:** Never use `<hr>` tags or border-bottoms for lists. Use 24px of vertical white space (from the Spacing Scale) or a subtle background toggle using `surface-container-low` vs `surface-container-lowest`.
*   **Interaction:** On hover, a card should shift from `surface-container-lowest` to a subtle `surface-container-highest` or apply the Ambient Shadow.

### Input Fields: Minimalist Utility
*   **Default State:** `surface-container-low` background, no border, `sm` roundedness.
*   **Focus State:** Background shifts to `surface-container-lowest` with a 2px `primary` "Ghost Border" (20% opacity).
*   **Error State:** Use `error` (#b31b25) for text and `error_container` (#fb5151) at 10% opacity for the background.

### Custom Component: The "Precision Metric"
A specialized component for financial ledger data. A large `headline-sm` value in `on_surface` paired with a small `label-md` sparkline using `primary` for growth or `error` for decline, nested in a `surface-container-lowest` card with a 12% `primary` tint glow.

---

## 6. Do's and Don'ts

### Do
*   **Do** use extreme white space. If you think there is enough padding, add 8px more.
*   **Do** use `primary` (#0050d4) sparingly as an accent to guide the user's eye to "The Next Best Action."
*   **Do** lean into the asymmetry. Align heavy text blocks to the left and let data-viz breathe on the right.

### Don't
*   **Don't** use 100% black (#000000) for text. Use `on_surface` (#282b51) for a softer, more premium contrast.
*   **Don't** use heavy "Slate" or "Navy" sidebars. Keep the navigation light using `surface-container-low`.
*   **Don't** use standard Material Design "elevated" shadows. They are too heavy for this "Luminous" aesthetic.