# Design System: Recruiter Portal - Atmospheric Precision

## 1. Visual Theme & Atmosphere
A clean, "Daily App Balanced" (Density 5) interface built for recruitment HR professionals. The atmosphere is an "Atmospheric Precision" high-key environment. It blends the authority of a data tool with the approachability of a premium concierge. It avoids industrial rigid grids, instead favoring bright, airy spaces, subtle tonal layering, and soft glass-like blurs.

## 2. Color Palette & Roles
- **Pure Surface** (#FFFFFF) — Card and innermost container fill
- **Canvas Edge** (#F7F9FB) — Primary background surface / Level 0
- **Soft Separator** (#F2F4F6) — Sidebar or secondary panel background
- **Charcoal Ink** (#191C1E) — Primary text, maximum readability without harsh #000000
- **Muted Blueprint** (#45464D) — Secondary text, metadata, subtle labels
- **Signature Blue / Accent** (#2563EB) — Single accent color for primary CTAs, active states, and focus rings. (Max 1 accent. No purple glows).
- **Subtle Outline** (#E0E3E5) — Minimal borders or 1px ghost lines at 40% opacity.

## 3. Typography Rules
- **Display/Headlines:** `Manrope` — Clean geometric curves, tight tracking (-2%). Shows authority through weight (600+).
- **Body/Input:** `Inter` — High-utility reading text, relaxed leading.
- **Labels:** `Inter` — Uppercase, slight tracking (0.05em) for meta-information.
- **Banned:** Generic system serifs. True black.

## 4. Component Stylings
* **Buttons:** Signature Blue (#2563EB) background, highly rounded (`xl` or `full`). Tactile feedback (translate-y on active/hover). No borders.
* **Cards:** Pure Surface (#FFFFFF) sitting on Canvas Edge. No hard borders, instead use a highly diffused light ambient shadow `shadow-sm` or `shadow-[0_4px_24px_rgba(37,99,235,0.04)]`. Rounded `2xl` to `3xl`.
* **Inputs:** No robust 4-sided boxes. Soft background `surface-container-low` with a subtle bottom border that turns Signature Blue (#2563EB) on focus. Focus ring is strict.
* **Chips/Badges:** Pill-shaped (`rounded-full`), soft colored background matching the sentiment (e.g. `blue-50` for Info, `green-50` for Success) with bold text.
* **Navigation:** Clear visual focus state, often with a subtle vertical active bar or rounded highlight.

## 5. Layout Principles
- **Grid-first:** Asymmetrical balance. A slightly wider main column against a narrower meta column.
- **No overlapping:** Every element occupies a clean spatial zone. Space components out generously (24px to 32px standard gaps).
- **No rigid lines:** Avoid sectioning via 1px solid gray borders. Group by white space and background tone.

## 6. Motion & Interaction
- Smooth hovered transform scaling.
- Focus rings fade in gracefully.
- No heavy physics, but elements feel responsive (`transition-all`).
- Skeleton loaders matching specific shapes rather than generic spinners for layout blocking.

## 7. Anti-Patterns (Banned)
- NEVER use pure black (#000000).
- NEVER use generic serifs (Times New Roman).
- NEVER use 3-column equal-width generic marketing card layouts.
- NEVER use glowing neon purple/pink dropshadows around buttons.
- NEVER make up fake placeholder metrics ("99.98% uptime SLA").
- NEVER use fake copywriting hype ("Elevate your recruitment next-gen AI").
- NEVER use "John Doe" or "Acme" — use realistic Vietnamese names if needed.
- NEVER overlap elements chaotically.
