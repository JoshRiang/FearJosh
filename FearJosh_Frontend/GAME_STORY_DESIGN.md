# FearJosh - Complete Story & Game Design Document

## 📖 LORE BACKGROUND

### The Tragedy of "Malam Abu" (Ash Night)

**Josh** adalah seorang siswa pendiam dan tidak berdaya. Selama bertahun-tahun, ia menjadi korban bullying di sekolahnya. Tidak ada yang membantunya - tidak teman-temannya, tidak guru-gurunya yang memilih diam.

Pada suatu malam, titik kesabaran Josh telah habis. Malam itu disebut **"Malam Abu"** oleh masyarakat sekitar karena terjadi kebakaran hebat yang menewaskan 213 orang.

**Kronologi Tragedi:**
1. Para siswa dan siswi sedang merayakan pesta kelulusan di Aula besar sekolah
2. Josh, dengan keputusasaan dan mata yang telah gelap, **mengunci seluruh pintu aula**
3. Ia membakar keliling aula tersebut
4. Siswa-siswi berteriak dan mencoba kabur, tetapi tidak ada yang bisa keluar
5. **213 orang meninggal dunia** terbakar mengerikan, termasuk Josh sendiri

**The Monster:**
Konon katanya, Josh dengan perasaan dendam yang kuat, tidak benar-benar meninggal. Ia bermutasi menjadi **sesosok monster api mengerikan**:
- Badan yang hangus dan kulit terkelupas
- Tubuh besar dan menakutkan
- Mata yang menyala seperti api
- Mengejar siapapun yang berani masuk ke sekolah, khususnya Aula

Sekarang, sekolah tersebut terbengkalai. Tidak ada satupun orang yang berani masuk, bahkan sang pemberani sekalipun.

---

## 🎮 MAIN STORY - Jonathan's Journey

### Protagonist: Jonathan (Kakak Josh)

**Dua tahun setelah Malam Abu**, **Jonathan** (kakak kandung Josh) pulang ke kampung halaman. Ia mendengar rumor tentang monster di sekolah dan mencurigai itu adalah adiknya, Josh.

**Motivasi Jonathan:**
- Mencari Josh dengan harapan bisa membawanya pulang
- Tidak percaya Josh benar-benar menjadi monster
- Ingin menyelamatkan adiknya yang tersiksa

---

## 🎯 GAME FLOW - Complete Gameplay Structure

### PHASE 1: OPENING SEQUENCE

#### **INTRO CUTSCENE** (4 scenes)
**Scene 1 - Flashback Malam Abu:**
- Narasi: "Dua tahun yang lalu... malam yang disebut 'Malam Abu'..."
- Visual: Siluet gedung sekolah terbakar
- Sound: Api berkobar, teriakan jauh

**Scene 2 - Tragedi:**
- Visual: Siluet Josh mengunci pintu, api menyebar
- Sound: Pintu dikunci, api membesar, teriakan panic
- Narasi: "213 jiwa meninggal malam itu... termasuk seorang siswa bernama Josh"

**Scene 3 - The Legend:**
- Visual: Mata merah menyala di kegelapan
- Narasi: "Tapi konon... Josh tidak pernah benar-benar mati..."
- Sound: Growl mengerikan
- Fade to black

**Scene 4 - Jonathan Arrives:**
- Visual: Jonathan berkendara di malam hari
- Musik pelan melambai
- Inner monolog: "Josh... Aku tahu kamu masih di sana. Aku akan membawamu pulang."
- Visual: Jonathan berdiri di depan gerbang sekolah terbengkalai
- Fade to black → **Game Start**

---

### PHASE 2: EXPLORATION & DISCOVERY

#### **OBJECTIVE 1: FIND JOSH**
**Duration:** ~5-10 minutes of exploration

**Gameplay:**
- Jonathan spawn di entrance sekolah (Room R5)
- Player bebas explore sekolah mencari jejak Josh
- Temukan clues: burnt photos, Josh's locker, bloodstains
- Goal: Reach **Aula** (specific room - bisa Room R1 atau R9) (tapi menyesuaikan nanti ama main map kalau udah jadi)

**Available Items (Tutorial):**
- **Flashlight**:
  - Tutorial prompt: "Tekan [F] untuk menyalakan flashlight"

**Environment:**
- Sekolah gelap dan terbengkalai
- Suara ambient: angin, langkah kaki jauh, pintu berderit
- Josh **BELUM** spawn (fase aman untuk belajar kontrol)

---

#### **CUTSCENE #1 - FIRST ENCOUNTER WITH JOSH** (Triggered saat masuk Aula)

**Scene Setup:**
- Jonathan masuk Aula besar yang hangus
- Lampu berkedip-kedip
- Suara nafas berat dari kegelapan

**Dialogue:**
```
Jonathan: "Josh...? Adik, apakah itu kamu?"
[Monster figure muncul dari bayangan - Josh transformed]
Jonathan: (shocked) "JOSH?! Tidak... tidak... apa yang terjadi padamu?!"
Josh: [ROAR mengerikan - tidak bisa bicara lagi]
Jonathan: "Josh, kumohon! Aku kakakmu! Ingatlah!"
[Josh menatap sebentar... lalu menyerang Jonathan]
Jonathan: [dodge] "TIDAK! Kamu sudah... tidak bisa diselamatkan lagi..."
[Jonathan tidak sengaja menembakan flash light ke wajah Josh, pengetahuan bertambah, bahwa Josh kena stun dan takut akan cahaya]
```

**Cutscene End:**
- Josh menghilang ke dalam bayangan
- **JOSH SPAWNS** - dari sini Josh aktif stalking
- Tutorial prompt: **"Gunakan Flashlight untuk STUN Josh! Tekan [F]"**


---

### PHASE 3: SURVIVAL & OBJECTIVES

#### **OBJECTIVE 2: FIND A WAY OUT**

**Gameplay:**
- Jonathan panic, berlari kembali ke pintu utama (entrance)
- Player harus navigate kembali ke R5 (entrance room)

**Trigger:** Saat player sampai di pintu utama:

**CUTSCENE #2 - BLOCKED EXIT:**
```
[Visual: Sprite kayu/debris blocking door]
Jonathan: (inner monolog) "Pintu keluar sudah ditutup...!"
Jonathan: "Apakah mungkin... ini ulah Josh?"
Jonathan: "Aku harus mencari jalan lain... mungkin ada denah darurat di sekolah ini."
```

**System Changes:**
- `currentObjective = FIND_MAP`
- Spawn prompt: "Cari School Map di Staff Room atau Library"

---

#### **OBJECTIVE 3: FIND SCHOOL MAP**

**Player diberi CHOICE - 2 Routes:**

---

##### **ROUTE A: STAFF ROOM** (Safe but requires puzzle)

**Characteristics:**
- **Difficulty:** Medium (puzzle-based)
- **Safety:** Josh tidak aktif patrol di area ini
- **Requirement:** Butuh Crowbar untuk membuka pintu

**Flow:**
1. Player menuju Staff Room
2. **Prompt:** "Pintu terkunci dengan rantai. Butuh Crowbar untuk membobol."
3. **Sub-objective:** FIND_CROWBAR
   - Crowbar location: **Janitor Room** atau **Maintenance Room**
4. Player cari Crowbar (harus explore, Josh masih stalking di jalur)
5. Return to Staff Room dengan Crowbar
6. **Interaction:** Break door (press E)
7. **Success:** Dapat School Map safely

**Pros:**
- Tidak ada chase sequence di Staff Room
- Dapat item tambahan di Staff Room (battery, dan coklat)

**Note:**
- Kita harus buat sistem inventory sederhana
- Coklat bisa menambah stamina

**Cons:**
- Butuh waktu lebih lama (cari crowbar dulu)
- Harus explore lebih banyak room dengan Josh aktif

---

##### **ROUTE B: LIBRARY** (Dangerous but faster)

**Characteristics:**
- **Difficulty:** Hard (tension-based)
- **Safety:** Josh **ACTIVELY PATROLS** area ini
- **Requirement:** Tidak butuh item, pintu terbuka

**Flow:**
1. Player menuju Library
2. **Trigger:** Josh spawn/move ke Library area (tingkatkan aggression)
3. **Gameplay:** Stealth section
   - Josh patrol di antara rak buku
   - Player harus sneak atau use flashlight tactically
   - Map terletak di meja tengah library
4. **Interaction:** Grab map (press E) → 3 detik animation
5. **Quick Cutscene** Josh notice player, chase sequence

**Pros:**
- Lebih cepat (tidak perlu cari item lain)
- High tension horror experience
- Skill-based challenge

**Cons:**
- Sangat berbahaya (Josh aktif)
- Bisa mati jika tidak hati-hati
- Battery flashlight bisa habis

---

#### **CUTSCENE #3 - MAP DISCOVERY** (Setelah dapat map)

```
[Visual: Jonathan membuka map, close-up]
Jonathan: "Ada pintu darurat di basement... Itu satu-satunya jalan keluar."
```

**Notes:**
Nanti tambahin kayak petunjuk ke arah basement mungkin di dalam gamenya atau UI.

---

#### **OBJECTIVE 4: REACH BASEMENT ENTRANCE**

**Required Items to Progress:**
1. **Fire Extinguisher** (Chemistry Lab)
   - Untuk memadamkan api yang blocking basement stairs
2. **Basement Key** (Teacher's Office atau Lost & Found)
   - Untuk unlock basement door

**Gameplay:**
- Player harus strategic movement
- Josh semakin aggressive (tingkatkan speed via difficulty)
- Battery management critical (cari Battery items)
- Multiple rooms to explore

**Battery Item Locations (randomly spawn 3-5):**
- Classroom drawers
- Janitor closet
- Vending machine
- Staff lounge
- Medical room

---

#### **CUTSCENE #4 - BASEMENT CONFRONTATION** (Sebelum masuk basement)

**Trigger:** Player sampai di basement door dengan Fire Extinguisher + Key

```
[Jonathan unlock basement door]
Jonathan: "Akhirnya... pintu keluar ada di sana."
[Josh muncul blocking the path - aggressive]
Josh: [ROAR]
Jonathan: "Josh! Kumohon, ingatlah siapa dirimu!"
[Josh pause... mata menyala berkurang sedikit]
Jonathan: "Kamu adalah adikku... Josh, fight it!"
[Beat of silence...]
Josh: [ROAR LOUDER - lunges]
Jonathan: "TIDAK!"
[Quick Time Event atau Escape Sequence]
```

**Gameplay:**
- Option 1: **QTE** (Quick Time Event) - mash [SPACE] to push Josh
- Option 2: **Escape Sequence** - run to basement, Josh chase intensely

**System Changes:**
- `currentObjective = FINAL_ESCAPE`
- Josh speed +50% (final chase)
- No stun cooldown reduction (high difficulty)

---

### PHASE 4: FINAL ESCAPE & ENDINGS

#### **OBJECTIVE 5: ESCAPE FROM BASEMENT**

**Gameplay:**
- Player navigate through dark basement
- Josh chasing relentlessly
- Find emergency exit door at end
- Final sprint to freedom

---

## 🎬 MULTIPLE ENDINGS

### **ENDING 1: BAD ENDING** - Ketika Habis Nyawa

**Trigger:** Player dies and runs out of lives (currentLives = 0)

**Death Cutscene:**
```
[Josh catches Jonathan, pins him down]
Josh: [Distorted voice] "...Jon...athan...?"
Jonathan: (gasping) "Josh... I'm sorry... I couldn't save you..."
[Josh pauses, moment of recognition... then consumed by rage]
[Screen fades to black]
[Text: "Jonathan was never seen again..."]
[Text: "The school remains cursed..."]
```

**Result:** Game Over → Return to Main Menu

---

### **ENDING 2: GOOD ENDING** - Survivor's Guilt

**Trigger:** Player successfully escapes basement

**Escape Cutscene:**
```
[Jonathan bursts through emergency exit door - outside]
Jonathan: [panting, looks back at building]
[Josh stands at basement door, silhouette in darkness]
[Josh doesn't chase - just watches]
Jonathan: (crying) "I'm sorry, Josh... I couldn't save you..."
Jonathan: "Forgive me... little brother..."
[Jonathan runs into the night]

[EPILOG TEXT:]
"Jonathan reported the incident to authorities."
"The school was demolished three months later."
"But late at night, locals still hear screams from the empty lot..."
"Some say Josh's spirit still wanders, searching for peace..."
"Jonathan never returned to the town."
```

**Result:** Credits roll → Return to Main Menu

---

### **ENDING 3: TRUE ENDING** - Redemption (Optional/Secret)

**Trigger:** 
- Collect all 5 **Josh's Memory Items** (hidden throughout school)
- Successfully reach basement exit

**Memory Items:**
1. **Burnt Photo** (Josh & Jonathan as kids) - Library
2. **Josh's Diary** (last entry about bullying) - Josh's Locker
3. **Yearbook** (Josh's page torn and burnt) - Staff Room
4. **Brotherhood Bracelet** (Jonathan gave to Josh) - Aula floor
5. **Letter from Josh** (never sent to Jonathan) - Rooftop

**Special Cutscene:**
```
[Before basement exit, Jonathan stops]
Jonathan: "Josh... I found these..."
[Shows memory items - flashback montage]
[Memories: Josh and Jonathan as kids, happy times, Josh's pain]

Jonathan: "I understand now... You were suffering all along..."
Jonathan: "And I wasn't there for you when you needed me most."

[Josh appears, less aggressive]
Josh: [Distorted but clearer] "Ka...ka..."
Jonathan: "Josh! You're still in there!"

[Choice appears on screen:]
> STAY WITH JOSH
> ESCAPE ALONE

--- IF CHOOSE "STAY" ---
Jonathan: "I won't leave you again, Josh. Not this time."
[Jonathan sits down, Josh approaches]
[Screen fades to white]
[Text: "Some say on quiet nights, two spirits can be seen in the old school grounds..."]
[Text: "Forever together... at peace..."]

--- IF CHOOSE "ESCAPE" ---
Jonathan: "I promise, Josh... I'll find a way to free you."
Jonathan: "Wait for me, little brother."
[Jonathan escapes, Josh watches peacefully]
[EPILOG:]
"Jonathan dedicated his life to researching the paranormal."
"He never stopped looking for a way to save Josh's soul."
"Some bonds transcend even death itself..."
```

**Result:** Special credits with emotional music → Unlock "True Ending" achievement

---

## ⚔️ COMBAT & DEATH SYSTEM

### **Flashlight Stun Mechanic**
- **Activation:** Hold [F] while facing Josh
- **Effect:** Josh stunned for 3-5 seconds (difficulty-based)
- **Battery Cost:** 10% per second active
- **Cooldown:** None (limited by battery only)
- **Range:** Short-medium (cone of light)

### **Josh Speed & Behavior**
- **Base Speed:** Player speed × 1.2 (20% faster)
- **States:**
  - **Patrol:** Slow walking, investigating rooms
  - **Alert:** Heard player noise, moving to investigate
  - **Chase:** Spotted player, full sprint
  - **Stunned:** Flashlight hit, covering face (3-5s)

### **Death & Lives System**

**Lives based on Difficulty:**
- **Easy Mode:** 3 lives
- **Medium Mode:** 2 lives
- **Hard Mode:** 1 life

**Death Sequence:**
1. **Capture Animation:** Josh catches Jonathan, lifts him up
2. **Struggle:** Brief struggle (3 seconds)
3. **Hang:** Josh "hangs" Jonathan (reference to victims)
4. **Screen fades to black**

**Respawn (if lives remain):**
```
[Text: "Jonathan wakes up... but where?"]
[Respawn in safe random room]
[Lives remaining displayed]
[Josh despawns for 30 seconds grace period]
```

**Game Over (no lives left):**
```
[Final death cutscene - Josh consumes Jonathan]
[BAD ENDING triggered]
```

---

## 🎒 ITEMS & COLLECTIBLES

### **Essential Items (Required for Progress)**

| Item | Location | Purpose |
|------|----------|---------|
| **Flashlight** | Janitor Room / Classroom | Stun Josh, see in darkness |
| **Crowbar** | Maintenance / Janitor | Open Staff Room (if chosen) |
| **School Map** | Staff Room / Library | Reveal basement location |
| **Fire Extinguisher** | Chemistry Lab | Clear fire blocking stairs |
| **Basement Key** | Teacher's Office | Unlock basement door |
| **Batery** | Semua ada | Menambah batery senter |
### **Battery Items (Randomly spawn 5-8 locations)**
- **Small Battery:** +25% charge
- Locations: Classrooms, lockers, vending machines, offices

### **Josh's Memory Items (Secret/Optional for True Ending)**
1. **Burnt Photo** - Library bookshelf
2. **Josh's Diary** - Josh's locker (need combination hint)
3. **Yearbook** - Staff Room filing cabinet
4. **Brotherhood Bracelet** - Aula floor (hidden)
5. **Letter from Josh** - Rooftop access (secret area)

---

## 🗺️ ROOM LAYOUT & ITEM DISTRIBUTION

### **Suggested Room Assignments (3x3 Grid)**
Ini masih pakai sistem lama yang room generate, tapi kalau pakai map, usahakan semua ruangan ini ada.

```
[R1 - Aula]        [R2 - Hallway]      [R3 - Library]
[R4 - Classroom]   [R5 - Entrance]     [R6 - Staff Room]
[R7 - Janitor]     [R8 - Chemistry]    [R9 - Teacher Office]
```

**Special Rooms:**
- **Basement Entrance:** Accessible from R8 (Chemistry) via stairs
- **Rooftop Access:** Secret ladder from R3 (Library) - True Ending

---

## 🎵 AUDIO & ATMOSPHERE

### **Cutscene Music**
- **Intro:** Melancholic piano (Jonathan's theme)
- **First Encounter:** Sudden orchestral hit (horror)
- **Chase Sequences:** Intense percussion
- **Endings:** 
  - Bad: Dissonant strings
  - Good: Somber piano
  - True: Emotional orchestral

### **Gameplay Ambience**
- Wind howling
- Distant dripping water
- Creaking doors
- Josh's breathing (proximity-based)
- Footsteps echo

### **Josh Audio Cues**
- **Distant:** Low growl (far away)
- **Alert:** Heavy breathing (investigating)
- **Chase:** Rapid footsteps + roar
- **Stun:** Pained screech + footsteps retreating

---

## 📊 DIFFICULTY BALANCING

| Aspect | Easy | Medium | Hard |
|--------|------|--------|------|
| **Lives** | 3 | 2 | 1 |
| **Josh Speed** | 1.1x player | 1.2x player | 1.3x player |
| **Stun Duration** | 5 seconds | 3 seconds | 2 seconds |
| **Battery Spawn** | 8 items | 5 items | 3 items |
| **Josh Aggression** | Low patrol | Medium patrol | High patrol |
| **Respawn Grace** | 45 seconds | 30 seconds | 15 seconds |

---

## 🎯 TECHNICAL IMPLEMENTATION CHECKLIST

### **GameManager Requirements**
- [ ] `GameObjective` enum tracking
- [ ] `MapRoute` choice system
- [ ] `joshSpawned` flag
- [ ] Item collection flags (flashlight, crowbar, map, key, memories)
- [ ] Battery system (current/max, usage rate, recharge)
- [ ] Lives system (max/current based on difficulty)
- [ ] Death & respawn logic

### **Cutscene System Requirements**
- [ ] `CutsceneScreen` class
- [ ] `CutsceneData` class (dialogue, sprites, transitions)
- [ ] `CutsceneManager` for sequencing
- [ ] Skip/Next button functionality
- [ ] Fade transitions (black, white)
- [ ] Text typewriter effect

### **UI Requirements**
- [ ] Objective display (top-left corner)
- [ ] Battery indicator (bottom-right)
- [ ] Lives counter (heart icons)
- [ ] Inventory display (items collected)
- [ ] Map UI (after obtaining map)
- [ ] Flashlight indicator (ON/OFF)

### **Enemy (Josh) Requirements**
- [ ] Spawn after Cutscene #1
- [ ] Patrol behavior (RoomDirector integration)
- [ ] Chase behavior (player spotted)
- [ ] Stun behavior (flashlight hit)
- [ ] Capture animation
- [ ] Speed scaling by difficulty

---

## 📝 DIALOGUE WRITING NOTES

### **Jonathan's Character Voice**
- Protective older brother
- Guilt-ridden (wasn't there for Josh)
- Determined but scared
- Emotional but trying to be brave

### **Josh's Transformation**
- Lost humanity (cannot speak clearly)
- Glimpses of recognition (rare moments)
- Trapped in rage and pain
- True Ending: slight humanity returns with memories

### **Tone**
- Horror but emotional
- Tragedy, not just scares
- Family bond as core theme
- Player should feel Jonathan's guilt and determination

---

## 🎮 ESTIMATED PLAYTIME

- **First Playthrough (Normal):** 20-30 minutes
- **Speedrun (Skip cutscenes):** 10-15 minutes
- **True Ending Route:** 30-40 minutes (finding all memories)
- **Replayability:** High (different routes, endings, difficulty modes)

---

## ✅ FINAL NOTES FOR TEAM

### **Priority Implementation Order:**
1. **Core Movement & Josh AI** (most critical)
2. **Objective System** (track progress)
3. **Intro Cutscene** (sets tone immediately)
4. **Flashlight Mechanic** (core survival tool)
5. **Death/Lives System** (difficulty balancing)
6. **Item Collection** (map route choice)
7. **Additional Cutscenes** (flesh out story)
8. **True Ending** (polish/optional)

### **Asset Needs:**
- **Sprites:**
  - Jonathan (player character - 4-direction walk)
  - Josh Monster (enemy - 4-direction, chase, stun, capture)
  - Cutscene character portraits (face closeups)
  - Item sprites (flashlight, crowbar, battery, etc.)
  - Environmental sprites (debris, fire, locked door)

- **Backgrounds:**
  - 9 room backgrounds (sekolah terbengkalai aesthetic)
  - Aula (burnt and destroyed)
  - Basement corridor (dark, claustrophobic)
  - Cutscene backgrounds (simplified/silhouette style)

- **Audio:**
  - Background music (4-5 tracks)
  - Josh sound effects (growl, roar, footsteps, breathing)
  - Ambient sounds (wind, drips, creaks)
  - UI sounds (item pickup, flashlight toggle, stun effect)
  - Cutscene voice acting (optional - atau text only)

### **Testing Focus:**
- Josh AI behavior (tidak terlalu mudah/sulit)
- Battery consumption rate (balance survival tension)
- Stun duration per difficulty (fair but challenging)
- Route choice viability (both options should feel valid)
- Respawn location fairness (tidak spawn tepat di depan Josh)

---

**Document Version:** 1.0  
**Last Updated:** December 20, 2025  
**Status:** Ready for Implementation
