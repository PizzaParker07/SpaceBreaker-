# Performance notes

## Blocky planets (client/sky/BlockyBodyRenderer)

Measured with the showcase on software OpenGL (llvmpipe, 1920×1080, no GPU), so the milliseconds include the
rasterising a graphics card would do; real hardware is much faster. Cost per drawn body, frames that rebuilt nothing
("steady"), from `-PshotPrefix=blockyperf` (log lines `Blocky bodies:` and `Sky fps`).

| View | Before (first blocky version) | After | Rebuilds | Notes |
|---|---|---|---|---|
| Jupiter over Io (64 blocks) | 1.55 ms, 9.5 KB/draw | 1.45–1.8 ms, 8.8 KB/draw | 5 in 10 s | mostly filling a screen-sized ball |
| Saturn and rings over Enceladus | 16 ms, 4.7 MB/draw | 5.3 ms, 12 KB/draw | 8 in 10 s | rings were re-sorted every frame; now cached |
| Star map (GUI, many small bodies) | 1.6 ms, 186 KB/draw | 2.0 ms, 122 KB/draw | 30 in 10 s | flat views cached; ringed planets on the map are still drawn each time |
| Low Earth orbit, fine surface patch | 4.0 ms, 137 KB/draw, rebuilt almost every frame | 3.0 ms, 50 KB/draw | 169 in 10 s, 2.3 ms each | Earth turns 15°/s at the default time scale, so its lighting changes; rebuilds happen in steps |

Frame rate on the same views (software GL, noisy): Jupiter over Io 16 fps (old smooth disc) vs 13 fps (blocks);
Saturn over Enceladus 12 vs 13 fps; Earth orbit 37 vs 24 fps.

**How it stays cheap**

- A body is kept as a vertex buffer in its own frame (a unit ball). Turning, moving and growing it only changes the
  model matrix, so the buffer is rebuilt only when the light or the side in view changes by about a degree, or the
  brightness changes.
- Only faces toward the viewer are kept (with a small margin, the rest are hidden by depth). Distant bodies use 16
  blocks across; finer levels (32, 64, and 128 on a close approach) come in only as a body grows on screen.
- Big balls, their colours and close-up surface patches are built in the background; the screen keeps the previous
  level until they are ready.
- Rings and flat (GUI) bodies use the same cache. The every-frame path builds no keys and reuses its matrices.

**Known**

- About 9 KB per drawn body is still allocated each frame, 1.4 KB of it inside vanilla's `VertexBuffer.drawWithShader`;
  the rest is small and spread out (down from megabytes).
- With Sodium or Iris the sky goes through the same path (checked by compat with Iris + Complementary); no separate
  Sodium timing yet.
