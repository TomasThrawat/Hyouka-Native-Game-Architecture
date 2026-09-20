# Architecture Guide

## 1. Game

يمتلك حالة اللعبة العامة ويجمع الأنظمة، لكنه لا يرسم المشهد ولا يبني واجهة المستخدم.

المسؤوليات:
- game state
- lifecycle
- simulation tick
- ربط الأنظمة

## 2. GameRenderer

مسؤول فقط عن تحويل حالة اللعبة إلى rendering.

لا يجب أن يحتوي على:
- قواعد السباق
- حسابات الفيزياء
- منطق الـAI
- معالجة أزرار HUD كقواعد لعبة

## 3. Scene

يمثل العالم المرئي:
- camera
- lights
- scene nodes
- track objects
- cars
- environment

يمكن تغيير محرك الـ3D لاحقًا بدون نقل منطق الفيزياء إلى طبقة العرض.

## 4. Physics

مسؤول عن:
- acceleration
- braking
- steering
- velocity
- friction
- movement integration

## 5. Collision

مسؤول عن:
- car-vs-car
- car-vs-track
- barriers
- collision response

يفضل أن يبقى مستقلًا عن الـUI والـrenderer.

## 6. AI

مسؤول عن:
- target speed
- racing line
- steering target
- overtaking decisions
- recovery

الـAI يقرأ حالة العالم ويكتب input/intent، وليس transforms الخاصة بالـrenderer مباشرة.

## 7. Race

مسؤول عن:
- laps
- checkpoints
- race order
- countdown
- finish state

## 8. Track

مسؤول عن:
- track layout
- segments
- spawn points
- checkpoints
- map-specific data

يمكن تحميل بيانات المسار من JSON.

## 9. GLB

طبقة مخصصة لإدارة أصول 3D:
- loading
- caching
- validation
- model lookup
- animation metadata

الأصول الفعلية توضع داخل `app/src/main/assets`.

## 10. HudView

يعرض البيانات فقط:
- speed
- lap
- position
- buttons
- race messages

لا يجب أن يحتوي على Physics أو AI.

## 11. Sound

نظام مستقل:
- engine audio
- brakes
- collisions
- UI sounds
- music

لا تجعل renderer مسؤولًا عن الصوت.

## 12. JSON

البيانات التي تتغير كثيرًا يجب فصلها عن الكود.

مثال:

```json
{
  "id": "formula_01",
  "model": "models/formula_01.glb",
  "mass": 720,
  "maxSpeed": 310
}
```

## Dependency direction

يفضل أن يكون الاتجاه تقريبًا:

```
UI -> Game -> Systems
Renderer -> Scene -> Assets
Systems -> Data
Sound -> Game events
```

وتجنب:

```
Physics -> HudView
AI -> Compose UI
HudView -> Renderer internals
Renderer -> Race rules
```
