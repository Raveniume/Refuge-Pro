# Hangar Page Overrides

> **PROJECT:** RefugeNext Compose Clean Visual Rebuild
> **Generated:** 2026-08-19 23:03:07
> **Page Type:** Blog / Article

> ⚠️ **IMPORTANT:** Rules in this file **override** the Master file (`design-system/MASTER.md`).
> Only deviations from the Master are documented here. For all other rules, refer to the Master.

---

## Page-Specific Rules

### Layout Overrides

- **Max Width:** 1200px (standard)
- **Layout:** Full-width sections, centered content
- **Sections:** 1. Hero (Value Prop + Form), 2. Recent Issues/Archives, 3. Social Proof (Subscriber count), 4. About Author

### Spacing Overrides

- No overrides — use Master spacing

### Typography Overrides

- No overrides — use Master typography

### Color Overrides

- **Strategy:** Minimalist. Paper-like background. Text focus. Accent color for Subscribe.

### Component Overrides

- Avoid: Override system gestures
- Avoid: No feedback during loading
- Avoid: Overflow or broken layout

---

## Page-Specific Components

- No unique components for this page

---

## Recommendations

- Effects: Expo.out Bezier(0.16,1,0.3,1) easing; spring modals (damping:20 stiffness:90); haptic-linked press (Impact Light/Medium); animated ambient light blobs (Reanimated translateX/Y slow oscillation); BlurView glassmorphism headers/nav (intensity 20); scale press 0.97 → 1.0; avoid pure #000000 (OLED smear)
- Touch: Avoid horizontal swipe on main content
- Feedback: Show spinner/skeleton for operations > 300ms
- Content: Truncate with ellipsis and expand option
- CTA Placement: Hero inline form + Sticky header form
