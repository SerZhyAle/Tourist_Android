# INPUT-PARITY - pointer

| Field | Value |
| --- | --- |
| **Id** | `INPUT-PARITY` |
| **Version** | 0.4, draft (registry, read 2026-10-26, S0016) |
| **Home** | shared contracts catalog, `input-controls/` |
| **Owner** | shared (column stewards) |
| **Role here** | adopter - touch, hardware keys, gamepad and TV remote |

What this repository owes:

- Every user action reachable by touch, hardware keys, gamepad and TV remote, as the contract's table states,
  from the first screen (S0004) onward - on the phone and on the watch.
- The water flashlight's rule that no wet touch can switch it off is the product's signature and is ported
  exactly (handoff section 3); the hardware-key exit and the touch lock of the watch stay inside the same table.
- 0.3 to 0.4 in one reading (S0016). The amendment adds the shared media-viewer action vocabulary (zoom,
  rotate, frame-step, speed, slideshow, destination-move, thumbnail-panel, random-file, jump-number,
  first/last, step-ten, step-hundred) and the `VIEWER-KEYBOARD` defaults, plus - in `INPUT-CHORD` - physical
  trigger names to the canonical set. This product draws no media viewer and stores no chord: the viewer
  actions name no surface here, and `INPUT-CHORD` is not adopted (the stopwatch's volume-key participant
  switch is an in-window platform shortcut, `INPUT-PARITY` section 1, with its touch equivalent beside it).
  The sections 3.1 vocabulary and the device table this product conforms its screens to are unchanged by 0.4,
  so the shipped bindings carry over; the registry row moves to Reads 0.4 on that audit, and the
  per-screen parity walk stays with the screens' own tickets.
