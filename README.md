# Spacebreaker

**A realistic, technical space exploration mod for Minecraft (NeoForge 1.21.1).**
Design your own rockets, reach orbit, land on the Moon, then push on through the whole Solar System.

> Version 1.0.0 — release notes: [docs/releases/v1.0.0.md](docs/releases/v1.0.0.md)

---

## English

### What it is
- **Real physics, playable scale.** Rockets obey the rocket equation (Δv, specific impulse,
  thrust-to-weight). Worlds keep their real gravity, atmosphere and temperature; time and
  distances are scaled so the game stays fun (Realistic / Balanced / Easy presets).
- **The real Solar System:** 34 worlds and 3 orbits, from the Moon and Mars to the asteroid belt,
  four comets and the moons of Jupiter, Saturn, Uranus, Neptune and Pluto, under a sky computed
  from real orbital data.
- **Survival in space:** oxygen, temperature, radiation and pressure; EVA suits with a clean helmet HUD,
  and realistic sound that fades in thin air and vacuum.
- **Industry and power:** new ores, metals and alloys, fluid processing, solar, RTG and fission power,
  a planetary rover.
- **Life out there:** more than 30 creatures, each native to its world.
- **Hand-made look:** every texture is drawn pixel by pixel (by code), every machine has a real
  3D model, and all blocks ship LabPBR normal/specular maps for shader packs.
- **Compatible:** Sodium, Iris shader packs (space skies drawn over the pack, see `docs/COMPAT.md`),
  Distant Horizons, JEI, EMI, Jade and WTHIT.

Coming in 2.0: a procedural universe beyond the Solar System, with other stars and galaxies.

### Building
```
./gradlew build                 # jar in build/libs
./gradlew runGameTestServer     # server GameTests
./gradlew runShowcase           # automated client scenario + screenshots (run-showcase/screenshots)
python3 tools/textures/generate.py   # redraw all textures
python3 tools/models/gen_assets.py   # regenerate blockstates and models
```
Compatibility runs (shader packs, recipe viewers, info overlays, dynamic lights, GUI scales): see the showcase
properties in [`docs/COMPAT.md` → How to test](docs/COMPAT.md#4-how-to-test).

---

## Türkçe

**Gerçekçi ve teknik bir uzay keşif modu (NeoForge 1.21.1).** Kendi roketini tasarla, yörüngeye çık,
Ay'a in, sonra bütün Güneş Sistemi'ni gez.

- **Gerçek fizik, oynanabilir ölçek:** Δv, özgül itki, itki/ağırlık oranı. Dünyalar gerçek
  yerçekimi, atmosfer ve sıcaklık değerleriyle; zaman ve mesafeler eğlenceli kalacak şekilde ölçekli
  (Gerçekçi / Dengeli / Kolay).
- **Gerçek Güneş Sistemi:** 34 dünya ve 3 yörünge; Ay ve Mars'tan asteroit kuşağına, dört kuyruklu
  yıldıza, Jüpiter, Satürn, Uranüs, Neptün ve Plüton'un uydularına kadar. Gökyüzü gerçek yörünge verisiyle hesaplanır.
- **Uzayda hayatta kalma:** oksijen, sıcaklık, radyasyon ve basınç; kask göstergeli EVA giysileri,
  ince havada ve vakumda kısılan gerçekçi ses.
- **Endüstri ve enerji:** yeni madenler, metaller ve alaşımlar, sıvı işleme; güneş, RTG ve fisyon enerjisi;
  gezegen gezicisi.
- **Yaratıklar:** her dünyaya özgü 30'dan fazla canlı.
- **Elde yapılmış görünüm:** tüm dokular kodla piksel piksel çizilir, makinelerin gerçek 3B
  modelleri vardır, shader paketleri için normal/parlaklık haritaları hazırdır.
- **Uyumlu:** Sodium, Iris, Distant Horizons, JEI, EMI, Jade ve WTHIT.

2.0'da: Güneş Sistemi'nin ötesinde, başka yıldızlar ve galaksilerle prosedürel bir evren.

Ayrıntılı plan: [docs/DESIGN.md](docs/DESIGN.md) · İçerik listesi: [docs/CONTENT.md](docs/CONTENT.md)

---

**Credits:** PizzaParker07 / Pizzacı Çocuk · All rights reserved (see [LICENSE](LICENSE)).
