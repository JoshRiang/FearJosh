# Cutscene System Documentation

## 📖 Overview

Sistem cutscene untuk FearJosh yang mendukung:
- **Multiple image layers** dengan animasi
- **Zoom animations** (in/out)
- **Pan animations** (left/right/up/down)
- **Kombinasi zoom + pan** untuk efek sinematik
- **Background music** untuk setiap cutscene
- **Dialog system** dengan speaker names

---

## 🚀 Quick Start

### 1. Membuat Cutscene Sederhana

```java
CutsceneData myCutscene = new CutsceneData.Builder("my_cutscene")
    .addLayer(new CutsceneLayer.Builder("Cutscenes/my_image.png")
        .scale(1.0f)
        .zoom(CutsceneAnimationType.ZOOM_OUT, 0.3f)
        .duration(10.0f)
        .build())
    .withMusic("Audio/Music/my_music.wav")
    .addDialog("Speaker", "Dialog text here...")
    .addDialog("Another dialog without speaker")
    .build();

// Register di CutsceneManager
CutsceneManager.getInstance().registerCutscene("my_cutscene", myCutscene);

// Mainkan cutscene
CutsceneManager.getInstance().playCutscene(game, "my_cutscene", nextScreen);
```

---

## 🎬 Animation Types

### Zoom Animations
- **ZOOM_IN** - Gambar membesar secara bertahap
- **ZOOM_OUT** - Gambar mengecil secara bertahap

### Pan Animations
- **PAN_LEFT** - Gambar bergeser ke kiri
- **PAN_RIGHT** - Gambar bergeser ke kanan
- **PAN_UP** - Gambar bergeser ke atas
- **PAN_DOWN** - Gambar bergeser ke bawah

---

## 📐 Layer Configuration

### Position
```java
.position(0.5f, 0.5f)  // Center screen (0-1 range)
```
- `0, 0` = Bottom-left
- `1, 1` = Top-right
- `0.5, 0.5` = Center

### Scale
```java
.scale(1.5f)  // 150% size
```
- `1.0` = Normal size
- `0.5` = Half size
- `2.0` = Double size

### Zoom Amount
```java
.zoom(CutsceneAnimationType.ZOOM_IN, 0.5f)  // Zoom in 50%
```
- `0.3f` = Zoom 30%
- `0.5f` = Zoom 50%
- `1.0f` = Zoom 100% (double)

### Pan Amount
```java
.pan(CutsceneAnimationType.PAN_LEFT, 200f)  // Pan 200 pixels left
```
- Amount dalam pixels
- `100f` = 100 pixels
- `200f` = 200 pixels

### Duration
```java
.duration(10.0f)  // 10 seconds
```
- Durasi dalam detik (seconds)

---

## 🎨 Examples

### Example 1: Simple Zoom Out
```java
CutsceneData intro = new CutsceneData.Builder("intro")
    .addLayer(new CutsceneLayer.Builder("Cutscenes/intro.png")
        .scale(1.5f)  // Start zoomed in 150%
        .zoom(CutsceneAnimationType.ZOOM_OUT, 0.5f)  // Zoom out to 100%
        .duration(8.0f)
        .build())
    .addDialog("Welcome to FearJosh...")
    .build();
```

### Example 2: Zoom + Pan Combination
```java
CutsceneData dramatic = new CutsceneData.Builder("dramatic")
    .addLayer(new CutsceneLayer.Builder("Cutscenes/hallway.png")
        .scale(1.2f)
        .zoom(CutsceneAnimationType.ZOOM_IN, 0.3f)  // Zoom in
        .pan(CutsceneAnimationType.PAN_RIGHT, 150f)  // Pan right
        .duration(12.0f)
        .build())
    .withMusic("Audio/Music/tension.wav")
    .addDialog("You hear footsteps...")
    .build();
```

### Example 3: Multiple Layers (Parallax Effect)
```java
CutsceneData multilayer = new CutsceneData.Builder("multilayer")
    // Background - slow pan
    .addLayer(new CutsceneLayer.Builder("Cutscenes/bg.png")
        .pan(CutsceneAnimationType.PAN_RIGHT, 80f)
        .duration(15.0f)
        .build())
    // Middle layer - zoom
    .addLayer(new CutsceneLayer.Builder("Cutscenes/middle.png")
        .position(0.2f, 0.3f)
        .zoom(CutsceneAnimationType.ZOOM_IN, 0.3f)
        .duration(15.0f)
        .build())
    // Foreground - fast pan (parallax!)
    .addLayer(new CutsceneLayer.Builder("Cutscenes/fg.png")
        .position(0.7f, 0.2f)
        .pan(CutsceneAnimationType.PAN_LEFT, 150f)
        .duration(15.0f)
        .build())
    .addDialog("The shadows move around you...")
    .build();
```

### Example 4: Dialog Only (No Images)
```java
CutsceneData tutorial = new CutsceneData.Builder("tutorial")
    .addDialog("Tutorial", "Use WASD to move.")
    .addDialog("Tutorial", "Press E to interact.")
    .addDialog("Tutorial", "Good luck!")
    .build();
```

---

## 🎮 Integration

### From MainMenuScreen
```java
playBtn.addListener(new ClickListener() {
    @Override
    public void clicked(InputEvent event, float x, float y) {
        Screen playScreen = new PlayScreen(game);
        CutsceneManager.getInstance().playCutscene(game, "intro", playScreen);
    }
});
```

### From PlayScreen (Player Dies)
```java
if (playerDied) {
    Screen mainMenu = new MainMenuScreen(game);
    CutsceneManager.getInstance().playCutscene(game, "game_over", mainMenu);
}
```

### Dynamic Cutscene Creation
```java
CutsceneData dynamic = new CutsceneData.Builder("dynamic")
    .addLayer(new CutsceneLayer.Builder("Cutscenes/room_" + roomId + ".png")
        .zoom(CutsceneAnimationType.ZOOM_OUT, 0.4f)
        .duration(5.0f)
        .build())
    .addDialog("You entered: " + roomName)
    .build();

game.setScreen(new CutsceneScreen(game, dynamic, this));
```

---

## 📝 Image Requirements

### Recommended Size
- **1920 x 1080 pixels** (Full HD)
- **1600 x 900 pixels** (HD+)
- **Aspect Ratio: 16:9**

### Content Area
- Dialog box mengambil **150 pixels dari bawah**
- Visible area: **800 x 450 pixels** (virtual resolution)
- Hindari elemen penting di 150px paling bawah

### Format
- **PNG** (recommended, supports transparency)
- **JPG** (for photos/backgrounds)

---

## 🎵 Audio Files

### Music Path
```java
.withMusic("Audio/Music/cutscene_music.wav")
```

### Audio Format
- **WAV** (recommended)
- **MP3** (supported)
- **OGG** (supported)

---

## 🎯 Controls

- **SPACE** - Advance to next dialog
- **0.2s cooldown** - Prevents accidental skipping

---

## 🔧 API Reference

### CutsceneData.Builder
```java
new CutsceneData.Builder(String cutsceneId)
    .addLayer(CutsceneLayer)           // Add animated layer
    .withMusic(String musicPath)       // Add background music
    .addDialog(String text)            // Add dialog (no speaker)
    .addDialog(String speaker, String text)  // Add dialog with speaker
    .build()
```

### CutsceneLayer.Builder
```java
new CutsceneLayer.Builder(String imagePath)
    .position(float x, float y)        // Set start position (0-1)
    .scale(float scale)                // Set start scale (1.0 = normal)
    .zoom(AnimationType, float amount) // Add zoom animation
    .pan(AnimationType, float amount)  // Add pan animation
    .duration(float seconds)           // Set animation duration
    .build()
```

### CutsceneManager
```java
CutsceneManager.getInstance()
    .registerCutscene(String id, CutsceneData data)
    .playCutscene(FearJosh game, String id, Screen nextScreen)
    .hasCutscene(String id)
    .getCutscene(String id)
```

---

## 💡 Tips

1. **Parallax Effect**: Gunakan multiple layers dengan kecepatan pan berbeda
2. **Dramatic Zoom**: Mulai dengan scale besar (1.5f) lalu zoom out
3. **Tension Build**: Kombinasikan slow zoom in + pan untuk efek menakutkan
4. **File Size**: Compress images untuk load time lebih cepat
5. **Testing**: Gunakan dialog-only cutscene untuk test flow tanpa gambar

---

## 📂 File Structure

```
assets/
  Cutscenes/
    intro_background.png
    josh_face.png
    dark_hallway.png
    ...
  Audio/
    Music/
      intro_music.wav
      tension_music.wav
      ...
```

---

## ⚠️ Common Issues

### Gambar tidak muncul
- Pastikan path benar: `"Cutscenes/nama_file.png"`
- Cek file ada di folder `assets/Cutscenes/`
- Lihat console untuk error messages

### Animasi terlalu cepat/lambat
- Adjust `.duration()` value (dalam detik)
- Default: 5.0 seconds

### Layer tidak centered
- Jika `position(0, 0)` tidak diset, akan auto-center
- Gunakan `position()` untuk kontrol manual

---

## 📄 Example in CutsceneManager.java

Lihat `CutsceneManager.initializeCutscenes()` untuk contoh lengkap:
- `intro` - Simple zoom out
- `game_over` - Two layers with animations
- `victory` - Zoom + pan combination
- `tutorial` - Dialog only
- `first_encounter` - Three layers with parallax

---

*Last updated: December 21, 2025*
