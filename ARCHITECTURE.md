# Architecture

## 1. Simulation

Game هو orchestrator صغير ولا يعتمد على Compose أو Filament.

Input -> Game -> Physics/Collision/AI/Race -> GameSnapshot

كل subsystem له مسؤولية منفصلة.

## 2. Renderer

GameRenderer هو boundary بين simulation والرسم.

Scene هو طبقة 3D الفعلية. لا يجب أن يحتوي على physics أو AI أو race rules.

## 3. Scene

Scene مسؤول عن:
- camera
- lighting
- environment
- world geometry
- model placement
- تحويل GameSnapshot إلى transforms

القالب يستخدم procedural geometry كـfallback. يمكن استبدالها بـGLB دون تغيير simulation.

## 4. Physics

Physics يحسب:
- acceleration
- braking
- drag
- steering
- speed
- progress

## 5. Collision

Collision يحسب:
- track bounds
- speed response
- car overlap

## 6. AI

AI ينتج input للسيارات المنافسة ثم يمرره إلى Physics.

AI لا يعدل renderer.

## 7. Race

Race مسؤول عن:
- laps
- position
- elapsed time
- finish state

## 8. Track

Track يحول progress وlateral offset إلى world pose.

يمكن لاحقًا استبدال التنفيذ بـspline أو checkpoints أو imported track segments.

## 9. Assets

Glb مسؤول عن:
- asset existence
- GLB validation
- JSON catalog parsing

JSON يصف البيانات. GLB يحتوي geometry/material/animation.

## 10. UI

GameHud يعرض GameSnapshot ويرسل GameInput.

لا يحتوي على physics أو AI.

## 11. Sound

Sound interface مستقلة. SilentSound يمنع إجبار القالب على شحن audio assets.

## 12. Testing

Physics وCollision وRace وGame قابلة للاختبار بدون Android UI.

## 13. Reuse

لبناء لعبة جديدة:
1. عدّل DataModels.
2. أضف أو استبدل systems.
3. استبدل Track.
4. استبدل Scene.
5. أضف GLB وJSON وaudio assets.
6. اترك Game rules خارج MainActivity وScene.

الهدف هو نفس الدرس الهندسي الأساسي: فصل GameRenderer وScene وPhysics وAI وRace وTrack وGlb وHudView وSound.
