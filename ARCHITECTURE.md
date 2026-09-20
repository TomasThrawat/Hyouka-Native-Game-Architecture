# Architecture

## Simulation

Game orchestrates the core loop without depending on Compose or SceneView.

GameInput -> Physics / Collision / AI / Race -> GameSnapshot

## Renderer boundary

GameRenderer هو الحد الفاصل بين simulation والرسم.
ComposeGameRenderer هو adapter صغير يحتفظ بآخر GameSnapshot داخل Compose state.

## Scene

Scene مسؤول عن camera follow وlighting وenvironment وworld geometry وmodel placement وتحويل GameSnapshot إلى transforms.

هندسة الطريق المرئية تستخدم Track.pose نفسها التي يستخدمها simulation.

## Physics

Physics يحسب acceleration وbraking وdrag وspeed cap من CarDefinition وtrack-relative steering وprogress وlap transitions.

## Collision

Collision يحسب track bounds وbarrier response وcar overlap باستخدام طول الحلبة.

## AI

AI ينتج GameInput للمنافسين ثم يمرره إلى Physics.

## Race

Race مسؤول عن laps وposition وelapsed time وfinish state.

## Assets

Glb مسؤول عن asset existence وGLB validation وJSON catalog parsing.

## UI

GameHud يعرض snapshot ويرسل GameInput. حالات throttle وbrake وleft وright مستقلة.

## Sound

Sound interface مستقلة، وSilentSound هو default آمن، وGame يستخدم dependency injection.

## Testing

الاختبارات تعمل على JVM بدون Android UI وتشمل acceleration وsteering وcollision وrace وtrack wrapping وcar overlap.

## إعادة الاستخدام

غيّر DataModels، ثم systems وTrack وScene وassets حسب اللعبة الجديدة، مع إبقاء game rules خارج MainActivity وScene.
