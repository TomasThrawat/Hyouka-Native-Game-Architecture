# Reusable Lessons

هذه هي الرسالة المحفوظة كمرجع هندسي:

- بناء ألعاب Native Kotlin على Android.
- فصل GameRenderer عن Scene.
- فصل أنظمة Physics وAI وRace وTrack.
- استخدام Glb لإدارة وتحميل أصول 3D.
- فصل HudView عن منطق اللعبة.
- جعل Sound نظامًا مستقلًا.
- استخدام ملفات JSON لبيانات السيارات والمسارات.
- تصميم المشروع بحيث يمكن إضافة سيارات وخرائط وأصول متعددة بسهولة.
- دمج الفيزياء والتصادمات والـAI والتوجيه كأنظمة مستقلة.
- إدارة أصول GLB داخل assets.
- عدم خلط منطق اللعبة مع الـrendering والـUI.
- استخدام هذا الهيكل كمرجع هندسي عند بناء ألعاب مستقبلية، مع التحقق من ملاءمة كل جزء للعبة الجديدة بدل نسخه بشكل أعمى.

## تطبيق الدروس

عند بدء لعبة جديدة، لا تبدأ من الـUI.

ابدأ بهذا الترتيب:

1. Data models.
2. World/Track representation.
3. Physics.
4. Collision.
5. AI.
6. Race/Game state.
7. Asset loading.
8. Renderer/Scene.
9. HUD.
10. Sound.
11. Integration testing.
