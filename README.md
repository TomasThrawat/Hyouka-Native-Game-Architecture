# Hyouka Native Game Architecture

قالب Native Kotlin لألعاب Android، مبني على الدروس الهندسية المستخلصة من CircuitRush3D.

المستودع الآن قالب تشغيل فعلي، وليس مجرد README أو skeleton.

## المطبق

- Native Android + Kotlin.
- فصل Game عن GameRenderer وعن Scene.
- أنظمة مستقلة: Physics, Collision, AI, Race, Track.
- GLB/JSON asset layer.
- HUD مستقل عن simulation.
- Sound subsystem مستقل.
- Game loop على frame clock.
- immutable GameSnapshot بين simulation والرسم.
- unit tests للأنظمة الأساسية.
- SceneView/Filament لطبقة 3D.
- GitHub Actions للاختبار وبناء APK.

## البنية

MainActivity
 -> Game loop
 -> Game
    -> Physics
    -> Collision
    -> AI
    -> Race
    -> Track
 -> GameSnapshot
 -> GameScene / GameRenderer
 -> GameHud
 -> Glb / JSON
 -> Sound

## قاعدة التصميم

الرسم لا يقرر قواعد اللعبة.

Game ينتج GameSnapshot. Scene يحول snapshot إلى transforms مرئية. لذلك يمكن استبدال renderer أو GLB assets بدون إعادة كتابة الفيزياء والـAI والـRace.

## 3D

القالب يحتوي على مشهد procedural يعمل بدون أي GLB، حتى يكون APK قابلًا للتشغيل قبل إضافة assets حقيقية.

عند إضافة assets، ضع GLB داخل app/src/main/assets/models/ وحدد مساره في cars.json أو tracks.json. طبقة Glb تتحقق من وجود الملف ومن GLB magic header.

SceneView الحالي هو 4.37.0، وتوثيقه الرسمي يوضح SceneView وModelNode وCubeNode وPlaneNode وDynamicSkyNode، إضافة إلى أن تحميل GLB يتم عبر rememberModelInstance مع إدارة lifecycle. citeturn1search0turn4search0

## الأداء

لا تنشئ Engine أو ModelInstance في كل frame. لا تضع Filament JNI أو تحميل GLB في background thread. وثائق SceneView توصي بإعادة استخدام الموارد وتجنب allocations داخل مسار الرسم. citeturn6search0

## الملفات المهمة

- ARCHITECTURE.md
- docs/LESSONS.md
- app/src/main/java/.../Game.kt
- app/src/main/java/.../Physics.kt
- app/src/main/java/.../Ai.kt
- app/src/main/java/.../Race.kt
- app/src/main/java/.../Track.kt
- app/src/main/java/.../Scene.kt
- app/src/main/java/.../Glb.kt
- app/src/main/java/.../HudView.kt
- app/src/main/java/.../Sound.kt
