# Hyouka Native Game Architecture

## Rendering

The runtime renderer now uses Google Filament directly. SceneView has been removed from the rendering dependency path.

Pipeline:

Game -> GameSnapshot -> DirectFilamentRenderer -> Filament Engine -> OpenGL ES

The renderer owns Engine, Renderer, Scene, View, Camera, SwapChain, glTF loading, and persistent procedural track meshes.

## Policy

- Filament backend is explicitly selected as OpenGL.
- Android declares OpenGL ES 3.0 as required.
- GLB assets are loaded through Filament gltfio.
- Simulation does not depend on the renderer.
- Track geometry is generated from the same Track.pose used by gameplay.
- GPU resources are created once and reused across frames.
- Dynamic resolution and FXAA are the initial mobile rendering baseline.

Selecting Filament's OpenGL backend does not force every device to expose an identical GLES implementation. The manifest still requires GLES 3.0.