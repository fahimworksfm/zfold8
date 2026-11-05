# Nano Banana prompts for Fold

Generate these in the Gemini app (Nano Banana) on your phone and save them to your gallery. Then:

- **Backgrounds:** Garage → Workshop → pick each screen's background.
- **Bay art:** tap a bay → Add bay art from gallery.

The launcher draws its own haze, ceiling lamps and wet-floor reflections over your backgrounds, so the images can stay calm. They need a dark middle, because the clock and bays sit there.

Nano Banana doesn't take a separate negative prompt, so each prompt states what to leave out.

---

## 1. Backgrounds

### Open screen (nearly square)

> Wide-angle interior of an empty underground parking garage in New York at 2 a.m., seen from the middle of the driving lane. Raw board-formed concrete pillars and low ceiling. Warm sodium-gold strip lights recede into the distance. Wet concrete floor with soft gold reflections. Painted bay lines in faded white, bay numbers stencilled on the pillars. Light haze in the air. Colour palette: charcoal, warm black, brushed gold, with a hint of cream. Mood: calm, cinematic, quiet luxury, like a car video game's garage menu. Keep the centre of the frame dark and uncluttered. Shallow depth of field, 24mm lens, high dynamic range, film grain. Square 1:1 aspect ratio. No cars, no people, no text, no logos, no watermark.

### Cover screen (tall)

> Tall vertical view straight down a narrow ramp of an underground parking garage at night. Concrete walls, a single line of warm gold ceiling lights running from the top of the frame to the bottom, wet floor reflecting them. The lower third of the frame is darker and plain. Charcoal and gold palette, light haze, cinematic, minimal, quiet luxury. Vertical 9:21 aspect ratio. No cars, no people, no text, no logos, no watermark.

### Tabletop (wide)

> Wide cinematic view of the back wall of an underground garage at night: a long row of six empty parking bays under warm gold tube lights. Concrete wall with faint stencilled bay numbers. Polished wet floor in the foreground, fading to black at the bottom of the frame. Charcoal, gold and cream palette, soft haze, anamorphic lens look, film grain. 16:9 aspect ratio. No cars, no people, no text, no logos, no watermark.

### Night shift variant (any screen)

Add this to any background prompt above:

> Same scene, later at night: lights dimmer, haze thicker, one light flickering, a faint neon-gold glow from around a corner.

---

## 2. Bay art: each project as a car

Every project gets its own car, parked in a bay, all shot the same way so the garage looks like one set. Generate the first car, then send its image back to Nano Banana with *"Same style, same camera angle, same lighting, same bay, now: …"* plus the next car's line. That keeps the whole set consistent.

**Shared style (paste it before every car line):**

> Studio-quality render of a single car parked in an underground garage bay. Three-quarter front view from a low angle, the car fills two-thirds of the frame and sits slightly to the right. Warm gold overhead tube light, wet concrete floor with reflections, dark charcoal background with light haze. Stylised like a premium racing-game garage menu: clean, glossy, cinematic. 3:2 landscape aspect ratio. No text, no badges, no logos, no number plates, no people, no watermark.

**One line per project (paste after the shared style):**

| Bay | Project | Car |
|---|---|---|
| 01 | Garage | A matte black widebody coupe with gold wheel rims and a fine gold pinstripe, the hero car of the collection. |
| 02 | Uplift Digital Partners | A sleek charcoal grand tourer with a subtle upward-sweeping gold light bar across the front, elegant and corporate. |
| 03 | Anima-Fy | A futuristic concept car with iridescent paint shifting from violet to teal, glowing seams like animation keyframe lines. |
| 04 | NeverForget | A smoky grey retro-modern sports car whose front grille is a round dial with gold tick marks, faint smoke curling around it. |
| 05 | Near Coffee | A vintage 1960s coupe in deep espresso brown with a cream roof and cream racing stripe, small orbiting ring motif on the hubcaps. |
| 06 | Sharif Adornment | An ultra-luxury coupe in charcoal with hand-engraved gold filigree along the sides and gold leaf accents, like jewellery. |
| 07 | Maria's Closet | A monochrome boutique cabriolet in pure white with black trim, top down, minimal and fashionable. |
| 08 | Hololand | A compact city car with holographic chrome wrap reflecting rainbow light. |
| 09 | Mozumder | A rugged heavy-duty truck pulling a single shipping container, dark navy with gold stripes. |
| 10 | BandarTrade | A sturdy cargo van in deep green with brass trim, roof rack loaded with crates, a merchant's vehicle. |
| 11 | Primé | A tiny pastel kei car in strawberry pink and white-chocolate cream, round and cheerful, with a cookie-crumb texture on the bumpers. |
| 12 | Tether | Two small classic cars parked close together, one cream and one ink black, a thin gold thread tied between their mirrors. |
| 13 | Curbside | A vintage street-food truck with a fold-out serving hatch, string lights, steam rising, warm and inviting. |
| 14 | Night Walker NYC | A black muscle car under a flickering magenta-and-gold neon sign, rain beading on the hood, film noir mood. |
| 15 | LiveGrid | A sleek electric race car with a glowing live-score LED strip across the front, stadium-light reflections. |
| 16 | Open-Meteo | An off-road explorer with a weather radar dome on the roof, light rain and mist around it. |
| 17 | Live Scanner | A retro 1980s car with a CRT-green glowing dashboard and a radar dish, scanlines in the reflections. |
| 18 | Ghosted | A pale translucent ghost car, half-faded, with a cheeky cartoon sheet draped over one mirror. Witty but subtle. |

Add new projects the same way: shared style plus one line describing the project as a car.

---

## 3. Tips

- If a background comes out too bright, add *"two stops darker, deep shadows in the centre"*.
- If cars come out with text or badges anyway, reply *"remove all text and badges, keep everything else identical"*.
- For a night version of a bay, send the image back with *"same image, lights off except one dim gold tube, dust on the car"*. It pairs well with a bay you've parked.
- Images up to about 2200 px on the long side are plenty; the launcher scales larger ones down.
