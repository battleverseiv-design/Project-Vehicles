# Архитектура и анализ системы деформации кузова (Vehicle Mesh Deformation)

> [!NOTE]
> Данный документ сохраняет полную архитектуру экспериментальной системы деформации кузова при столкновениях, реализованной в проекте `car_decorations_mod`. Система была временно отключена по причине визуального конфликта с модульными/анимированными паками автомобилей. Документ служит техническим ориентиром для будущей переработки.

---

## 1. Исходная архитектура деформации

Система состояла из четырёх ключевых звеньев:

### 1.1. Детекция столкновения на сервере (`DrivableVehicleEntity.java`)
При соударении колёс или бампера машины с препятствием (блоками или другими сущностями) физический движок вычислял:
* Точку удара в локальных координатах автомобиля (`localX`, `localY`, `localZ`).
* Вектор силы удара (`dirX`, `dirY`, `dirZ`).
* Радиус зоны повреждения (`radius = Math.min(1.5f, impactSpeed * 0.25f)`).
* Глубину вмятины (`depth = Math.min(0.35f, impactSpeed * 0.08f)`).

Сервер отправлял пакет клиентам в радиусе видимости:
```java
ModNetwork.sendToTracking(new VehicleImpactDeformPacket(
    this.getId(), localX, localY, localZ, dirX, dirY, dirZ, radius, depth
), this);
```

### 1.2. Сетевой пакет (`VehicleImpactDeformPacket.java`)
Передавал `entityId` и 8 скалярных параметров удара по сети через Netty buffer.

### 1.3. Обработка на клиенте (`ClientPacketHandler.java`)
```java
public static void handleImpactDeform(int entityId, float localX, float localY, float localZ, 
                                      float dirX, float dirY, float dirZ, float radius, float depth) {
    Minecraft mc = Minecraft.getInstance();
    if (mc.level != null) {
        Entity ent = mc.level.getEntity(entityId);
        if (ent instanceof DrivableVehicleEntity vehicle) {
            ClientVehicleAssets assets = ClientVehiclePackLoader.getAssets(vehicle.getVehicleId());
            if (assets != null && assets.chassisMesh != null) {
                DynamicObjMesh mesh = DrivablePackVehicleRenderer.deformedChassisMeshes.get(entityId);
                if (mesh == null) {
                    mesh = assets.chassisMesh.createCopy();
                    DrivablePackVehicleRenderer.deformedChassisMeshes.put(entityId, mesh);
                }
                mesh.applyDent(localX, localY, localZ, dirX, dirY, dirZ, radius, depth);
            }
        }
    }
}
```

### 1.4. Модификация вершинной сетки (`DynamicObjMesh.java`)
Метод `applyDent` проходил по распакованным вершинам `packedData` (шаг 8 float: `[x, y, z, u, v, nx, ny, nz]`):
```java
public void applyDent(float localX, float localY, float localZ, float dirX, float dirY, float dirZ, float radius, float depth) {
    float r2 = radius * radius;
    for (int i = 0; i < len; i += 8) {
        float dx = data[i] - localX;
        float dy = data[i + 1] - localY;
        float dz = data[i + 2] - localZ;
        float distSq = dx * dx + dy * dy + dz * dz;
        if (distSq < r2) {
            float dist = (float) Math.sqrt(distSq);
            float factor = (1.0f - dist / radius);
            factor = factor * factor; // Квадратичный спад вмятины
            data[i] += dirX * depth * factor;
            data[i + 1] += dirY * depth * factor;
            data[i + 2] += dirZ * depth * factor;
        }
    }
}
```

---

## 2. Причина возникновения бага (серый плоский кузов)

В [`DrivablePackVehicleRenderer.java`](file:///B:/Developing/Battleverse/projects/car_decorations_mod/src/main/java/com/evgen/cardecor/client/DrivablePackVehicleRenderer.java) отрисовка кузова была реализована ветвлением:
```java
DynamicObjMesh deformedChassis = deformedChassisMeshes.get(entity.getId());
if (deformedChassis != null) {
    deformedChassis.render(poseStack, vertexConsumer, safePackedLight, 255, 255, 255, 255);
} else if (assets != null && ((assets.batchedStaticChassis != null && !assets.batchedStaticChassis.isEmpty()) 
           || (assets.animatedChassisParts != null && !assets.animatedChassisParts.isEmpty()))) {
    renderAnimatedChassis(entity, def, assets, partialTicks, steerAngleDeg, poseStack, buffer, safePackedLight, tex, isClose);
}
```

### Корень проблемы:
1. **Устаревший монолитный `assets.chassisMesh`**: 
   Все современные модели транспорта (например, винтажный пикап) собираются модульно через `renderAnimatedChassis`. В этой системе каждая деталь (кузов, двери, капот, лобовое стекло, салон, рама) имеет собственный материал, шейдер прозрачности (для стекол) и динамическую палитру покраски (`dynPalette`).
2. **Перехват рендера**: 
   Как только происходил удар, создавался `deformedChassis`, который являлся копией **сырого общего OBJ-файла**. 
3. **Потеря материалов и текстур**: 
   `deformedChassis.render` отправлял вершины в один общий `vertexConsumer` с текстурой `tex` (`FALLBACK_TEXTURE = iron_block.png`). В результате весь автомобиль (включая стекло, салон и двери) превращался в монолитный серый железный блок.

---

## 3. Рекомендации для будущей полноценной реализации

Когда потребуется вернуть деформацию кузова без потери текстур и модульности:

### Вариант А: Деформация на уровне групп анимированного кузова (`animatedChassisParts`)
* Вместо единого `assets.chassisMesh` применять `applyDent` к отдельным сеткам деталей внутри `assets.chassisGroups` (например, только к переднему крылу, капоту или бамперу).
* Каждая деталь сохранит свой `VertexConsumer`, палитру покраски и текстуру.

### Вариант Б: Вершинный шейдер деформации (GPU Vertex Shader Displacement)
* Не изменять вершины в CPU-памяти `float[] packedData`.
* Передавать в кастомный вершинный шейдер автомобиля массив сфер ударов `uniform vec4 uImpactSpheres[4]` (`vec3 center, float radius`) и `uniform vec3 uImpactDirections[4]`.
* Смещение вершин вычислять прямо на GPU во время отрисовки:
  ```glsl
  vec3 offset = vec3(0.0);
  for (int i = 0; i < uImpactCount; i++) {
      float d = distance(aPos, uImpactSpheres[i].xyz);
      if (d < uImpactSpheres[i].w) {
          float f = 1.0 - d / uImpactSpheres[i].w;
          offset += uImpactDirections[i] * (f * f * 0.25);
      }
  }
  vec4 worldPos = uModelMatrix * vec4(aPos + offset, 1.0);
  ```
  Это обеспечит 0% нагрузки на CPU, отсутствие аллокаций памяти и идеальную совместимость со всеми текстурами, палитрами и прозрачными стеклами.
