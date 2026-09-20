# Hyouka Native Game Architecture

قالب Native Kotlin لألعاب Android مبني على الدروس الهندسية المستخلصة من CircuitRush3D.

## الحالة الحالية

- Native Kotlin + Android + Compose.
- فصل simulation عن renderer وعن Scene.
- Physics وCollision وAI وRace وTrack مستقلة.
- GameRenderer boundary واضح، مع ComposeGameRenderer كـadapter للواجهة.
- GameSnapshot immutable بين simulation والرسم.
- JSON catalogs مستخدمة فعليًا لتهيئة السيارات والحلبة.
- GLB assets حقيقية موجودة داخل app/src/main/assets/models/.
- SceneView/Filament لطبقة 3D.
- كاميرا تتبع اللاعب.
- المسار المرئي مبني من Track.pose نفسها المستخدمة في simulation.
- HUD يجمع حالات الأزرار بدل استبدال الإدخال كله عند كل ضغط.
- Sound subsystem يتم حقنه في Game.
- Unit tests للأنظمة الأساسية.
- GitHub Actions يشغل الاختبارات ويبني debug APK.

## قاعدة التصميم

الرسم لا يقرر قواعد اللعبة.
Game ينتج GameSnapshot، ثم يعبر renderer boundary قبل أن تعرضه Scene والـHUD.

## Assets

المشروع يحتوي على:
- starter_car.glb
- rival_car.glb
- start_gate.glb

Glb يتحقق من وجود الملفات ومن GLB magic header، ويقرأ cars.json وtracks.json.

## الأداء

Engine وModelInstance يتم الاحتفاظ بهما عبر Compose remember. لا يتم إنشاء Engine أو تحميل GLB لكل frame.

## إعادة الاستخدام

عند بناء لعبة جديدة، غيّر DataModels وTrack وScene وassets، واترك core rules في الأنظمة المستقلة.
