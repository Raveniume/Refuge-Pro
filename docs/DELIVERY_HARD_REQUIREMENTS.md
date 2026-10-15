# Delivery hard requirements

Before a build is announced as delivered, the review must cover all four root routes: Hangar, Store, Terminal, and Profile.

- Each route must show the same bottom navigation geometry and the selected moving lens.
- Light and dark captures must show blur, refraction, highlight, and readable foreground labels.
- Pressing and dragging the top selector must show host and lens press feedback, then spring to the nearest item.
- A single tap on an unselected item must change the selection without requiring a drag.
- Open sheets must remain above the navigation layer while the navigation remains mounted at its original position.
- The review must record unit-test/build results and the exact APK path or release tag.

A failed item blocks delivery and requires another build and visual pass.
