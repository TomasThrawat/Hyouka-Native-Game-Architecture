# Reusable Lessons

هذه الدروس هي أساس القالب الحالي:

1. بناء ألعاب Native Kotlin على Android.
2. فصل GameRenderer عن Scene.
3. فصل Physics وAI وRace وTrack.
4. استخدام GLB لإدارة أصول 3D.
5. فصل HudView عن منطق اللعبة.
6. Sound كـsubsystem مستقل.
7. JSON لبيانات السيارات والحلبات.
8. عدم خلط game logic مع renderer أو Compose UI.
9. اختبار simulation بدون Android UI.
10. تمرير immutable GameSnapshot إلى الرسم.
11. تغيير assets بدون تغيير simulation code.
12. إضافة سيارات وخرائط وأنظمة جديدة بدون إعادة كتابة النواة.
13. procedural fallback أثناء تطوير assets.
14. عدم إعادة إنشاء Engine أو ModelInstance لكل frame.
15. إبقاء Filament JNI على main thread.
16. التحقق من كل subsystem قبل دمجه.

الهدف ليس نسخ CircuitRush3D. الهدف تحويل الدروس الهندسية المستخلصة منه إلى template reusable وقابل للتوسع.
