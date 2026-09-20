# Hyouka Native Game Architecture

قالب هندسي لإعادة استخدام بنية ألعاب Android Native المكتوبة بـ Kotlin.

الفكرة الأساسية مأخوذة من الدروس المستخلصة من CircuitRush3D، لكن هذا المستودع لا يفترض أن كل لعبة يجب أن تستخدم نفس التنفيذ. كل نظام يجب اختباره واختياره حسب متطلبات اللعبة الجديدة.

## المبادئ

- Native Kotlin على Android.
- فصل الـ rendering عن منطق اللعبة.
- فصل Physics وAI وRace وTrack.
- إدارة أصول 3D وGLB في طبقة مستقلة.
- فصل HUD/UI عن منطق اللعبة.
- جعل الصوت نظامًا مستقلًا.
- استخدام JSON لبيانات السيارات والمسارات.
- تصميم قابل لإضافة سيارات وخرائط وأصول متعددة.
- إبقاء التصادمات والتوجيه والفيزياء والـAI كأنظمة مستقلة.
- تخزين أصول GLB داخل Android assets.
- عدم خلط game logic مع rendering أو UI.

## الهيكل

```
app/src/main/java/com/tomasthrawat/gamearchitecture/
├── MainActivity.kt
├── Game.kt
├── GameRenderer.kt
├── Scene.kt
├── Physics.kt
├── Collision.kt
├── Ai.kt
├── Race.kt
├── Track.kt
├── Glb.kt
├── HudView.kt
├── Sound.kt
└── DataModels.kt

app/src/main/assets/
├── cars.json
├── tracks.json
└── models/
```

## قاعدة مهمة

هذا المستودع **مرجع هندسي** وليس كودًا يجب نسخه بشكل أعمى.

قبل استخدام أي نظام في لعبة جديدة:
1. حدد متطلبات اللعبة.
2. اختبر النظام مستقلًا.
3. تحقق من الأداء والـlifecycle.
4. افصل dependencies قدر الإمكان.
5. لا تنقل مشكلة من لعبة إلى لعبة أخرى بمجرد نسخ الكود.

## مستوحى من

- CircuitRush3D architecture.
- Native Android/Kotlin game development.
- GLB-based 3D asset workflows.
