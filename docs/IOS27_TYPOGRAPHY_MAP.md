# iOS 27 Typography Map

The Apple UI Kit is the hierarchy source. The Compose lab keeps the project's approved Chinese-capable font resources rather than copying Apple's SF font files.

| Apple hierarchy | Local role | Current use in the lab |
|---|---|---|
| Large/title label | Screen and section heading | `Reference Replication Lab`, component section headings |
| Body / control label | Primary interactive text | `Tab 1`–`Tab 3`, `Sheet`, `Alert`, search placeholder |
| Secondary label | Reduced-emphasis control text | Unselected tab labels and secondary dark/light content |
| Caption / metadata | Accessibility and supporting metadata | Semantics names and evidence labels; not used as marketing copy |
| Number / value | Numeric content hierarchy | Reserved for future Refuge business data; not introduced in the reference lab |

## Rules carried into Compose

- Preserve a clear title > section > control-label hierarchy.
- Keep Chinese text in the approved project font stack; do not substitute an unbundled SF font.
- Use theme-aware primary and secondary colors rather than fixed light-only text colors.
- Keep labels inside stable touch targets; interaction transforms affect the glass layer, not surrounding layout.
- Typography is currently a hierarchy mapping, not a measured claim about Apple point sizes. Exact values require component-level Figma inspection.
